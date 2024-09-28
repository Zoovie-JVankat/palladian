package ws.palladian.retrieval.feeds.discovery;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.ParserException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.feeds.discovery.DiscoveredFeed.Type;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.SearcherException;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * FeedDiscovery works like the following:
 * <ol>
 * <li>Query search engine with some terms (see {@link WebSearcherManager} for available search engines)</li>
 * <li>Get root URLs for each hit</li>
 * <li>Check page for feeds using RSS/Atom autodiscovery feature</li>
 * <li>Write the discovered feed URLs to file</li>
 * </ol>
 * </p>
 *
 * @author Philipp Katz
 * @author David Urbansky
 * @see <a href="http://tools.ietf.org/id/draft-snell-atompub-autodiscovery-00.txt">Atom Feed Autodiscovery</a>
 * @see <a
 * href="http://web.archive.org/web/20110608053313/http://diveintomark.org/archives/2003/12/19/atom-autodiscovery">Notes
 * on Atom autodiscovery</a>
 */
public final class FeedDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedDiscovery.class);

    /**
     * XPath to get Atom and RSS links; this is relatively complicated to conform to the Atom autodiscovery "standard".
     */
    private static final String FEED_XPATH = "//link[contains(translate(@rel, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'alternate') and "
            + "(translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/atom+xml' or "
            + "translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/rss+xml')]";

    /**
     * DocumentRetriever for downloading pages.
     */
    private static final HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

    /**
     * Define which search engine to use, see {@link WebSearcherManager} for available constants.
     */
    private final Searcher<?> searcher;

    /**
     * The parser used for parsing HTML pages.
     */
    private static final DocumentParser parser = ParserFactory.createHtmlParser();

    private final int numThreads;

    /**
     * Store all urls for which we will do the autodiscovery.
     */
    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>();

    /**
     * The path of the file where the discovered feeds should be written to.
     */
    private final File resultFilePath;

    /**
     * Store a collection of all queries that are used to retrieve urlQueue from a search engine.
     */
    private final BlockingQueue<String> queryQueue = new LinkedBlockingQueue<String>();

    /**
     * The numver of feeds we discovered.
     */
    private final AtomicInteger feedCounter = new AtomicInteger();

    /**
     * The number of pages we checked.
     */
    private final AtomicInteger pageCounter = new AtomicInteger();

    /**
     * The number of errors, i.e. unreachable and unparsable pages.
     */
    private final AtomicInteger errorCounter = new AtomicInteger();

    /**
     * Track the time of the discovery process.
     */
    private StopWatch stopWatch;

    /**
     * Number of search engine results to retrieve for each query.
     */
    private final int numResults;

    /**
     * Whether to output full CSVs with feeds' meta data, instead of only URLs.
     */
    private final boolean csvOutput;

    /**
     * @param searcher       The searcher to use, not <code>null</code>.
     * @param resultFilePath The path for the result file. If file already exists, new entries will be appended. The
     *                       result file will be written continuously. If <code>null</code>, no result file will be written.
     * @param numThreads     The maximum number of concurrent autodiscovery requests.
     * @param numResults     The number of results to retrieve for each query.
     * @param csvOutput      <code>true</code> to output full CSVs with additional information, like feed title, type, page
     *                       link, <code>false</code> to only write feed URL.
     */
    public FeedDiscovery(Searcher<?> searcher, File resultFilePath, int numThreads, int numResults, boolean csvOutput) {
        Validate.notNull(searcher, "webSearcher must not be null");
        Validate.isTrue(numThreads > 0, "numThreads must be greater zero");
        Validate.isTrue(numResults > 0, "numResults must be greater zero");
        this.searcher = searcher;
        this.resultFilePath = resultFilePath;
        this.numThreads = numThreads;
        this.numResults = numResults;
        this.csvOutput = csvOutput;
    }

    /**
     * <p>
     * Search for Sites by specified query.
     * </p>
     *
     * @param query
     * @param totalResults
     * @return
     */
    private Set<String> searchSites(String query, int totalResults) {
        Set<String> sites = new HashSet<String>();
        try {
            List<String> resultUrls = searcher.searchUrls(query, totalResults, Language.ENGLISH);
            for (String resultUrl : resultUrls) {
                sites.add(UrlHelper.getDomain(resultUrl));
            }
        } catch (SearcherException e) {
            LOGGER.error("Searcher Exception: {}", e.getMessage());
        }
        return sites;
    }

    /**
     * Discovers feed links in supplied page URL.
     *
     * @param pageUrl
     * @return list of discovered feeds, empty list if no feeds are available, <code>null</code> if page could not
     * be parsed.
     */
    public static List<DiscoveredFeed> discoverFeeds(String pageUrl) {
        List<DiscoveredFeed> result = null;
        Document document = null;

        try {
            HttpResult httpResult = httpRetriever.httpGet(pageUrl);
            document = parser.parse(httpResult);
        } catch (Throwable t) {
            // NekoHTML produces various types of Exceptions, just catch them all here and log them.
            LOGGER.error("Error retrieving {} : {} ; {}", pageUrl, t.toString(), t.getMessage());
        }

        if (document != null) {
            result = discoverFeeds(document);
        }

        return result;
    }

    /**
     * <p>
     * Discovers feed links in the supplied HTML file.
     * </p>
     *
     * @param file
     * @return list of discovered feeds, empty list if no feeds are available, <code>null</code> if the document could
     * not be parsed.
     */
    public static List<DiscoveredFeed> discoverFeeds(File file) {
        List<DiscoveredFeed> result = null;
        try {
            Document document = parser.parse(file);
            result = discoverFeeds(document);
        } catch (ParserException e) {
            LOGGER.error("Error parsing file {}", file, e);
        }
        return result;
    }

    /**
     * <p>
     * Uses autodiscovery feature with MIME types "application/atom+xml" and "application/rss+xml" to find linked feeds
     * in the specified Document.
     * </p>
     *
     * @param document
     * @return list of discovered feed URLs or empty list.
     */
    public static List<DiscoveredFeed> discoverFeeds(Document document) {
        List<DiscoveredFeed> result = new LinkedList<DiscoveredFeed>();

        String pageUrl = document.getDocumentURI();
        String baseUrl = UrlHelper.getBaseUrl(document);

        // get all Nodes from the Document containing feeds
        List<Node> feedNodes = XPathHelper.getXhtmlNodes(document, FEED_XPATH);

        // check for Atom/RSS
        for (Node feedNode : feedNodes) {
            NamedNodeMap attributes = feedNode.getAttributes();

            // ignore if href attribute is missing
            Node hrefNode = attributes.getNamedItem("href");
            if (hrefNode == null) {
                LOGGER.warn("href attribute is missing");
                continue;
            }

            // ignore if href attribute is empty
            String feedUrl = hrefNode.getNodeValue();
            if (feedUrl.isEmpty()) {
                LOGGER.warn("href attribute is empty");
                continue;
            }

            // few feeds use a "Feed URI Scheme" which can look like
            // feed://example.com/entries.atom
            // feed:https://example.com/entries.atom
            // See ---> http://en.wikipedia.org/wiki/Feed_URI_scheme
            feedUrl = feedUrl.replace("feed://", "http://");
            feedUrl = feedUrl.replace("feed:", "");

            // make full URL
            feedUrl = UrlHelper.makeFullUrl(pageUrl, baseUrl, feedUrl);

            // validate URL

            // disabled for now; we can validate URLs as postprocessing step;
            // furthermore, Apache's UrlValidator seems to be oversensitive

            // UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https", "file" });
            // boolean isValidUrl = urlValidator.isValid(feedUrl);
            //
            // if (!isValidUrl) {
            // LOGGER.warn("invalid url : " + feedUrl);
            // continue;
            // }

            String type = attributes.getNamedItem("type").getNodeValue().toLowerCase();
            DiscoveredFeed.Type feedType = null;
            if (type.contains("atom")) {
                feedType = Type.ATOM;
            } else if (type.contains("rss")) {
                feedType = Type.RSS;
            }

            Node titleNode = attributes.getNamedItem("title");
            String feedTitle = null;
            if (titleNode != null) {
                feedTitle = titleNode.getNodeValue();
            }

            result.add(new DiscoveredFeed(feedType, feedUrl, feedTitle, pageUrl));

        }

        LOGGER.debug("{} feeds for {}", result.size(), pageUrl);
        return result;

    }

    /**
     * <p>
     * Find feeds in all pages in the urlQueue. We use threading here to check multiple pages simultaneously.
     * </p>
     */
    public void findFeeds() {

        stopWatch = new StopWatch();

        LOGGER.info("Start finding feeds with {} queries and {} results per query = max. {} URLs to check for feeds; number of threads = {}", queryQueue.size(), numResults,
                numResults * queryQueue.size(), numThreads);

        // prevent running through the discovery step when no search results are available yet.
        final Object lock = new Object();

        // do the search
        Thread searchThread = new Thread() {
            @Override
            public void run() {
                String query;
                int totalQueries = queryQueue.size();
                int currentQuery = 0;
                while ((query = queryQueue.poll()) != null) {
                    Set<String> foundSites = searchSites(query, numResults);
                    if (foundSites.size() > 0) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                    urlQueue.addAll(foundSites);

                    currentQuery++;
                    float percentage = (float) 100 * currentQuery / totalQueries;
                    float querySpeed = TimeUnit.MINUTES.toMillis(currentQuery / stopWatch.getElapsedTime());
                    LOGGER.info("Queried {}/{}: '{}'; # results: {}; progress: {}%; query speed: {} queries/min", currentQuery, totalQueries, query, foundSites.size(), percentage,
                            querySpeed);

                }
                LOGGER.info("Finished queries in {}", stopWatch.getElapsedTimeString());
                synchronized (lock) {
                    lock.notify();
                }
            }
        };
        searchThread.start();

        // wait here, until the first query iteration has finished
        try {
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Encountered InterruptedException");
        }

        // do the autodiscovery in parallel
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {

            threads[i] = new Thread() {
                @Override
                public void run() {
                    while (queryQueue.size() > 0 || urlQueue.size() > 0) {

                        String url = urlQueue.poll();

                        // we got no new URL from the queue, search engine might still be busy, so wait a moment before
                        // we continue looping.
                        if (url == null) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                LOGGER.warn("Encountered InterruptedException");
                            }
                            continue;
                        }

                        try {
                            List<DiscoveredFeed> discoveredFeeds = discoverFeeds(url);
                            writeDiscoveredFeeds(discoveredFeeds);
                            if (discoveredFeeds != null) {
                                feedCounter.addAndGet(discoveredFeeds.size());
                            } else {
                                errorCounter.incrementAndGet();
                            }

                            // log the current status each 1000 checked pages
                            if (pageCounter.incrementAndGet() % 1000 == 0) {
                                float elapsedMinutes = (float) stopWatch.getElapsedTime() / TimeUnit.MINUTES.toMillis(1);
                                float pageThroughput = pageCounter.get() / elapsedMinutes;
                                float feedThroughput = feedCounter.get() / elapsedMinutes;
                                LOGGER.info(
                                        "# checked pages: {}; # discovered feeds: {}; # errors: {}; elapsed time: {}; throughput: {} pages/min; discovery speed: {} feeds/min; url queue size: {}",
                                        pageCounter.intValue(), feedCounter.intValue(), errorCounter.intValue(), stopWatch.getElapsedTimeString(), pageThroughput, feedThroughput,
                                        urlQueue.size());
                            }

                        } catch (Throwable t) {
                            LOGGER.error("Encountered Exception", t);
                        }
                    }
                }
            };
            threads[i].start();
        }

        // wait, until all Threads are finished
        try {
            searchThread.join();
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Encountered InterruptedException");
        }

    }

    private synchronized void writeDiscoveredFeeds(List<DiscoveredFeed> discoveredFeeds) {
        if (discoveredFeeds != null) {
            for (DiscoveredFeed feed : discoveredFeeds) {
                String writeLine = csvOutput ? feed.toCsv() : feed.getFeedLink();
                FileHelper.appendFile(resultFilePath.getPath(), writeLine + "\n");
            }
        }
    }

    /**
     * <p>
     * Add a query for the search engine.
     * </p>
     *
     * @param query The query to add.
     */
    public void addQuery(String query) {
        this.queryQueue.add(query);
    }

    /**
     * <p>
     * Add multiple queries for the search engine.
     * </p>
     *
     * @param queries A collection of queries.
     */
    public void addQueries(Collection<String> queries) {
        this.queryQueue.addAll(queries);
    }

    /**
     * Add multiple queries from a newline separeted query file.
     *
     * @param filePath
     */
    public void addQueries(String filePath) {
        List<String> queries = FileHelper.readFileToArray(filePath);
        addQueries(queries);
    }

    /**
     * Use the given queries and combine them, to get the specified targetCount of queries.
     *
     * <ul>
     * <li>If targetCount is smaller than existing queries, existing queries are reduced to a random subset.</li>
     * <li>If targetCount is bigger than possible tuples or -1, all posible combinations are calculated.</li>
     * <li>Else, as many tuples as necessary are calculated, to get specified targetCount of queries.</li>
     * </ul>
     * <p>
     * For example:
     * queries: A, B, C
     * after combining: A, B, C, A B, A C, B C
     */
    public void combineQueries(int targetCount) {

        int availableQueries = queryQueue.size();

        List<String> singleQueries = new ArrayList<String>(queryQueue);
        List<String> combinedQueries = new ArrayList<String>(queryQueue);
        Collections.shuffle(singleQueries);

        int possibleCombinations = availableQueries * (availableQueries - 1) / 2;

        if (targetCount != -1 && availableQueries > targetCount) {

            // we have more queries than we want, create a random subset of specified size
            combinedQueries.addAll(singleQueries.subList(0, targetCount));

        } else if (targetCount == -1 || targetCount > possibleCombinations + availableQueries) {

            // target count is higher than possible tuple combinations; so just calculate all
            for (int i = 0; i < availableQueries; i++) {
                for (int j = i + 1; j < availableQueries; j++) {
                    combinedQueries.add("\"" + singleQueries.get(i) + "\" \"" + singleQueries.get(j) + "\"");
                }
            }

        } else {

            // create combined queries
            Random random = new Random();
            while (combinedQueries.size() < targetCount) {
                // get a query term randomly by combining two single queries
                String term1 = singleQueries.get(random.nextInt(availableQueries));
                String term2 = singleQueries.get(random.nextInt(availableQueries));
                combinedQueries.add(term1 + " " + term2);
            }
        }

        Collections.shuffle(combinedQueries);
        queryQueue.clear();
        queryQueue.addAll(combinedQueries);

    }
}
