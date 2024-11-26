package ws.palladian.retrieval.feeds;

import org.apache.http.impl.cookie.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.StopWatch;
import ws.palladian.retrieval.*;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.helper.HttpHelper;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import static ws.palladian.retrieval.feeds.FeedTaskResult.*;

/**
 * <p>
 * The {@link FeedReader} schedules {@link FeedTask}s for each {@link Feed}. The {@link FeedTask} will run every time
 * the feed is checked and also performs all set {@link FeedProcessingAction}s.
 * </p>
 *
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * @see FeedReader
 */
class FeedTask implements Callable<FeedTaskResult> {
    private final static Logger LOGGER = LoggerFactory.getLogger(FeedTask.class);

    /** The feed retrieved by this task. */
    private final Feed feed;

    /** A collection of all intermediate results that can happen, e.g. when updating meta information or a data base. */
    private final Set<FeedTaskResult> resultSet = new HashSet<>();

    private final FeedReaderSettings settings;

    /**
     * Creates a new retrieval task for a provided feed.
     *
     * @param settings The configuration.
     * @param feed     The feed retrieved by this task.
     */
    FeedTask(FeedReaderSettings settings, Feed feed) {
        this.settings = settings;
        this.feed = feed;
    }

    @Override
    public FeedTaskResult call() {
        StopWatch timer = new StopWatch();
        try {
            LOGGER.debug("Start processing of feed id " + feed.getId() + " (" + feed.getFeedUrl() + ")");
            int recentMisses = feed.getMisses();

            HttpResult httpResult;
            try {
                HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

                httpRetriever.setMaxFileSize(settings.getMaximumFeedSize());
                // remember the time the feed has been checked
                feed.setLastPollTime(new Date());
                // download the document (not necessarily a feed)
                HttpRequest2 request = createRequest();
                httpResult = httpRetriever.execute(request);
            } catch (HttpException e) {
                LOGGER.error("Could not get Document for feed id " + feed.getId() + " , " + e.getMessage());
                feed.incrementUnreachableCount();
                resultSet.add(UNREACHABLE);

                doFinalStuff(timer);
                return getResult();
            }

            // process the returned header first
            // case 1: client or server error, statuscode >= 400
            if (httpResult.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                LOGGER.error("Could not get Document for feed id " + feed.getId() + ". Server returned HTTP status code " + httpResult.getStatusCode());
                feed.incrementUnreachableCount();
                resultSet.add(UNREACHABLE);

                try {
                    settings.getAction().onError(feed, httpResult);
                } catch (Exception e) {
                    resultSet.add(ERROR);
                }
                updateCheckIntervals(feed);
            } else {
                // case 2: document has not been modified since last request
                if (httpResult.getStatusCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    updateCheckIntervals(feed);
                    feed.setLastSuccessfulCheckTime(feed.getLastPollTime());
                    try {
                        settings.getAction().onUnmodified(feed, httpResult);
                    } catch (Exception e) {
                        resultSet.add(ERROR);
                    }

                    // case 3: default case, try to process the feed.
                } else {
                    // store http header information
                    feed.setLastETag(httpResult.getHeaderString("ETag"));
                    feed.setHttpLastModified(HttpHelper.getDateFromHeader(httpResult, "Last-Modified", false));

                    FeedParser feedParser = settings.getParserFactory().create();
                    Feed downloadedFeed;
                    try {
                        // parse the feed and get all its entries, do that here since that takes some time and this is a
                        // thread so it can be done in parallel
                        downloadedFeed = feedParser.getFeed(httpResult);
                    } catch (FeedParserException e) {
                        LOGGER.error("update items of feed id " + feed.getId() + " didn't work well, " + e.getMessage());
                        feed.incrementUnparsableCount();
                        resultSet.add(UNPARSABLE);
                        LOGGER.debug("Performing action on exception on feed: " + feed.getId() + "(" + feed.getFeedUrl() + ")");
                        try {
                            settings.getAction().onException(feed, httpResult);
                        } catch (Exception e2) {
                            resultSet.add(ERROR);
                        }

                        doFinalStuff(timer);
                        return getResult();
                    }
                    feed.setItems(downloadedFeed.getItems());
                    feed.setLastSuccessfulCheckTime(feed.getLastPollTime());
                    feed.setWindowSize(downloadedFeed.getItems().size());
                    feed.setFeedMetaInformation(downloadedFeed.getMetaInformation());
                    // if (LOGGER.isDebugEnabled()) {
                    // LOGGER.debug("Activity Pattern: " + feed.getActivityPattern());
                    // LOGGER.debug("Current time: " + System.currentTimeMillis());
                    // LOGGER.debug("Last poll time: " + feed.getLastPollTime().getTime());
                    // LOGGER.debug("Current time - last poll time: "
                    // + (System.currentTimeMillis() - feed.getLastPollTime().getTime()));
                    // LOGGER.debug("Milliseconds in a month: " + DateHelper.MONTH_MS);
                    // }

                    updateCheckIntervals(feed);

                    // perform actions on this feeds entries.
                    LOGGER.debug("Performing action on feed: " + feed.getId() + "(" + feed.getFeedUrl() + ")");
                    try {
                        settings.getAction().onModified(feed, httpResult);
                    } catch (Exception e) {
                        resultSet.add(ERROR);
                    }
                }

                if (recentMisses < feed.getMisses()) {
                    resultSet.add(MISS);
                } else {
                    // finally, if no other status has been assigned, the task seems to bee successful
                    resultSet.add(SUCCESS);
                }
            }

            doFinalStuff(timer);
            return getResult();

            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals. Errors are logged only and not written to database.
        } catch (Throwable th) {
            LOGGER.error("Error processing feedID " + feed.getId() + ": " + th, th);
            resultSet.add(ERROR);
            doFinalLogging(timer);
            return getResult();
        }
    }

    /**
     * Sets the feed task result and processing time of this task, saves the feed to database, does the final logging
     * and frees the feed's memory.
     *
     * @param timer The {@link StopWatch} to estimate processing time
     */
    private void doFinalStuff(StopWatch timer) {
        if (timer.getElapsedTime() > settings.getExecutionWarnTime()) {
            LOGGER.warn("Processing feed id " + feed.getFeedUrl() + " took very long: " + timer.getElapsedTimeString());
            resultSet.add(EXECUTION_TIME_WARNING);
        }

        // Caution. The result written to db may be wrong since we can't write a db-error to db that occurs while
        // updating the database. This has no effect as long as we do not restart the FeedReader in this case.
        feed.setLastFeedTaskResult(getResult());
        feed.increaseTotalProcessingTimeMS(timer.getElapsedTime());
        updateFeed();

        doFinalLogging(timer);
        // since the feed is kept in memory we need to remove all items and the document stored in the feed
        feed.freeMemory();
    }

    /**
     * Update http request headers to use conditional requests (head requests). This is done only in case the last
     * FeedTaskResult was success, miss or execution time warning. In all other cases, no conditional header is build.
     */
    private HttpRequest2 createRequest() {
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.GET, feed.getFeedUrl());
        requestBuilder.addHeader("cache-control", "no-cache");

        if (Arrays.asList(SUCCESS, MISS, EXECUTION_TIME_WARNING).contains(feed.getLastFeedTaskResult())) {
            if (feed.getLastETag() != null && !feed.getLastETag().isEmpty()) {
                requestBuilder.addHeader("If-None-Match", feed.getLastETag());
            }
            if (feed.getHttpLastModified() != null) {
                requestBuilder.addHeader("If-Modified-Since", DateUtils.formatDate(feed.getHttpLastModified()));
            }
        }
        return requestBuilder.create();
    }

    /**
     * Decide the status of this FeedTask. This is done here to have a fixed ranking on the values.
     *
     * @return The (current) result of the feed task.
     */
    private FeedTaskResult getResult() {
        if (resultSet.contains(ERROR)) {
            return ERROR;
        } else if (resultSet.contains(UNREACHABLE)) {
            return UNREACHABLE;
        } else if (resultSet.contains(UNPARSABLE)) {
            return UNPARSABLE;
        } else if (resultSet.contains(EXECUTION_TIME_WARNING)) {
            return EXECUTION_TIME_WARNING;
        } else if (resultSet.contains(MISS)) {
            return MISS;
        } else if (resultSet.contains(SUCCESS)) {
            return SUCCESS;
        } else {
            return OPEN;
        }
    }

    /**
     * Do final logging of result to error or debug log, depending on the FeedTaskResult.
     *
     * @param timer the {@link StopWatch} started when started processing the feed.
     */
    private void doFinalLogging(StopWatch timer) {
        FeedTaskResult result = getResult();
        String msg = "Finished processing of feed id " + feed.getId() + ". Result: " + result + ". Processing took " + timer.getElapsedTimeString();
        if (result == FeedTaskResult.ERROR) {
            LOGGER.error(msg);
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(msg);
        }
    }

    /**
     * Save the feed back to the database. In case of database errors, add error to {@link #resultSet}.
     */
    private void updateFeed() {
        boolean dbSuccess = settings.getStore().updateFeed(feed, feed.hasNewItem());
        if (!dbSuccess) {
            resultSet.add(FeedTaskResult.ERROR);
        }
    }

    /**
     * Update the check interval depending on the chosen approach. Update the feed accordingly and return it.
     *
     * @param feed The feed to update.
     */
    private void updateCheckIntervals(Feed feed) {
        settings.getUpdateStrategy().update(feed, new FeedPostStatistics(feed), false);
        feed.increaseChecks();
    }
}
