package ws.palladian.extraction.date.getter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.comparators.ContentDateComparator;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.helper.constants.DateFormat;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonObject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 * This class extracts all dates out of the content of webpages.
 *
 * @author Martin Gregor
 */
public class ContentDateGetter extends TechniqueDateGetter<ContentDate> {
    private final MetaDateGetter metaDateGetter = new MetaDateGetter();
    private final UrlDateGetter urlDateGetter = new UrlDateGetter();

    @Override
    public List<ContentDate> getDates(Document document) {
        List<ContentDate> result = getContentDates(document);
        setFeatures(result, document);
        return result;
    }

    private void setFeatures(List<ContentDate> dates, Document document) {
        List<ContentDate> posOrder = new ArrayList<>();
        List<ContentDate> ageOrder = new ArrayList<>();
        for (ContentDate date : dates) {
            if (date.get(ContentDate.DATEPOS_IN_DOC) != -1) {
                posOrder.add(date);
            }
            ageOrder.add(date);
        }

        posOrder.sort(ContentDateComparator.INSTANCE);
        ageOrder.sort(new DateComparator());

        List<MetaDate> metaDates = metaDateGetter.getDates(document, false);
        List<UrlDate> urlDates = urlDateGetter.getDates(document.getDocumentURI());

        for (ContentDate date : dates) {
            date.setRelSize(1.0 / dates.size());

            date.setOrdDocPos(MathHelper.round((posOrder.indexOf(date) + 1.0) / posOrder.size(), 3));

            date.setOrdAgePos(MathHelper.round((ageOrder.indexOf(date) + 1.0) / dates.size(), 3));

            if (DateExtractionHelper.countDates(date, metaDates, DateExactness.DAY) > 0) {
                date.setInMetaDates(true);
            }
            if (DateExtractionHelper.countDates(date, urlDates, DateExactness.DAY) > 0) {
                date.setInUrl(true);
            }

            double relCntSame = MathHelper.round((double) (DateExtractionHelper.countDates(date, dates, DateExactness.DAY) + 1) / dates.size(), 3);
            date.setRelCntSame(relCntSame);

            int datePosOrderAbs = posOrder.indexOf(date);
            if (datePosOrderAbs > 0) {
                date.setDistPosBefore(date.get(ContentDate.DATEPOS_IN_DOC) - posOrder.get(datePosOrderAbs - 1).get(ContentDate.DATEPOS_IN_DOC));
            }
            if (datePosOrderAbs < posOrder.size() - 1) {
                date.setDistPosAfter(posOrder.get(datePosOrderAbs + 1).get(ContentDate.DATEPOS_IN_DOC) - date.get(ContentDate.DATEPOS_IN_DOC));
            }
            int dateAgeOrderAbs = ageOrder.indexOf(date);
            if (dateAgeOrderAbs > 0) {
                date.setDistAgeBefore(Math.round(date.getDifference(ageOrder.get(dateAgeOrderAbs - 1), TimeUnit.HOURS)));
            }
            if (dateAgeOrderAbs < ageOrder.size() - 1) {
                date.setDistAgeAfter(Math.round(date.getDifference(ageOrder.get(dateAgeOrderAbs + 1), TimeUnit.HOURS)));
            }
        }
    }

    /**
     * Get dates of text-nodes of body part of document.
     * Also look at ld+json
     *
     * @param document Document to be searched.
     * @return List of dates.
     */
    private List<ContentDate> getContentDates(Document document) {
        List<ContentDate> dates = new ArrayList<>();

        // get from HTML
        String content = XPathHelper.getXhtmlNodeTextContent(document, "//main//time/@datetime");
        if (content != null) {
            ExtractedDate date = DateParser.findDate(content);
            if (date != null) {
                ContentDate contentDate = new ContentDate(date);
                contentDate.setHasStructureDate(true);
                contentDate.setTag("time");
                contentDate.setKeyword("published");
                dates.add(contentDate);
            }
        }

        // get from JSON
        List<Node> ldJsonNodes = XPathHelper.tryGetXhtmlNodes(document, "//script[@type='application/ld+json']");
        for (Node ldJsonNode : ldJsonNodes) {
            String json = ldJsonNode.getTextContent();
            JsonObject jso = JsonObject.tryParse(json);
            if (jso != null) {
                String date = jso.tryQueryString("datePublished");
                if (date != null) {
                    ExtractedDate extractedDate = DateParser.findDate(date);
                    if (extractedDate != null) {
                        ContentDate contentDate = new ContentDate(extractedDate);
                        contentDate.setInLdJson(true);
                        contentDate.setHasStructureDate(true);
                        contentDate.setTag("time");
                        contentDate.setKeyword("published");
                        dates.add(contentDate);
                    }
                }
            }
        }

        List<Node> textNodes = XPathHelper.getNodes(document, "//text()");

        if (textNodes.isEmpty() && dates.isEmpty()) {
            return dates;
        }

        Node bodyNode = XPathHelper.getXhtmlNode(document, "//body");
        String documentString = StringHelper.removeDoubleWhitespaces(replaceHtmlSymbols(HtmlHelper.documentToReadableText(bodyNode)));
        // TODO: Check if an element is visible
        // checkVisiblityOfAllNodes(body.item(0));
        // Get webpage as text (for finding position).

        Map<Integer, String> contentKeywords = findContentKeywords(documentString);

        for (Node textNode : textNodes) {
            if (textNode.getNodeType() == Node.TEXT_NODE) {
                Node parent = textNode.getParentNode();
                String parentName = parent.getNodeName().toLowerCase();
                if (parent.getNodeType() != Node.COMMENT_NODE && !Arrays.asList("script", "style").contains(parentName)) {
                    dates.addAll(checkTextNode((Text) textNode, documentString, contentKeywords));
                }
            }
        }

        return dates;
    }

    /**
     * Find {@link ContentDate}s in a {@link Text} {@link Node}.
     *
     * @param textNode        The Text Node which to check, not <code>null</code>.
     * @param documentString  The String representation of the document.
     * @param contentKeywords {@link Map} with keywords and occurrence indices, not <code>null</code>.
     * @return {@link List} of {@link ContentDate}s extracted from the Node, or an empty List. Never <code>null</code>.
     */
    private List<ContentDate> checkTextNode(Text textNode, String documentString, Map<Integer, String> contentKeywords) {
        String text = replaceHtmlSymbols(textNode.getNodeValue());

        int index = -1;
        Node parent = textNode.getParentNode();
        Node tag = parent;

        while (HtmlHelper.isSimpleElement(parent)) {
            parent = parent.getParentNode();
        }

        List<ContentDate> dates = findAllDates(text);
        if (!dates.isEmpty()) {
            index = documentString.indexOf(text);
        }

        for (ContentDate date : dates) {
            boolean hasStructureDate = StructureDateGetter.getDate(tag) != null;
            if (!hasStructureDate && tag != parent) {
                hasStructureDate = StructureDateGetter.getDate(parent) != null;
            }

            date.setHasStructureDate(hasStructureDate);

            boolean keyword3Class = true;

            date.setTag(tag.getNodeName());
            date.setSimpleTag(HtmlHelper.isSimpleElement(tag));
            date.setHTag(HtmlHelper.isHeadlineTag(tag));

            if (index != -1) {
                int absDocPos = index + date.get(ContentDate.DATEPOS_IN_TAGTEXT);
                date.setAbsDocPos(absDocPos);
                date.setRelDocPos(MathHelper.round((double) absDocPos / documentString.length(), 3));
            }

            String keyword = getNodeKeyword(tag);

            if (keyword == null && tag != parent) {
                keyword = getNodeKeyword(parent);
            }

            if (keyword != null) {
                keyword3Class = KeyWords.getKeywordPriority(keyword) == KeyWords.OTHER_KEYWORD;
                date.setKeyLoc(1);
            }

            if (keyword == null || keyword3Class) {
                setClosestKeyword(date, documentString, contentKeywords);
                if (date.getKeyword() != null) {
                    date.setKeyLoc(2);
                    keyword = date.getKeyword();
                }
            }

            if (keyword != null) {
                date.setKeyword(keyword);
                date.setKeywordPriority(KeyWords.getKeywordPriority(keyword));
            }
        }
        return dates;
    }

    /**
     * Find all content keywords in a text.
     *
     * @param text The text which to search for keywords, not <code>null</code>.
     * @return A {@link Map} with indices of the keywords as keys and their text values.
     */
    private Map<Integer, String> findContentKeywords(String text) {
        Map<Integer, String> keyContentMap = new HashMap<>();
        String tempText = text.toLowerCase();
        for (String keyword : KeyWords.BODY_CONTENT_KEYWORDS_ALL) {
            int index = tempText.indexOf(keyword);
            while (index != -1) {
                keyContentMap.put(index, keyword);
                index = tempText.indexOf(keyword);
                tempText = tempText.replaceFirst(keyword, StringUtils.repeat('x', keyword.length()));
            }
        }
        return keyContentMap;
    }

    /**
     * Find the keyword closest to the date.
     *
     * @param date            The {@link ContentDate} for which to determine the closest keyword, not <code>null</code>.
     * @param documentString  The document as String representation, not <code>null</code>.
     * @param contentKeywords {@link Map} with keywords and occurrence indices, not <code>null</code>.
     */
    private void setClosestKeyword(ContentDate date, String documentString, Map<Integer, String> contentKeywords) {
        int datePos = date.get(ContentDate.DATEPOS_IN_DOC);

        if (datePos < 0) {
            return;
        }

        String keyword = null;
        int subStart = 0;
        int subEnd = 0;

        for (int i = 1; i < 151; i++) {
            int indexBefore = datePos - i;
            int indexAfter = datePos + i;

            String keywordBefore = contentKeywords.get(indexBefore);
            if (keywordBefore != null) {
                keyword = keywordBefore;
                subStart = indexBefore + keywordBefore.length();
                subEnd = datePos;
                break;
            }

            String keywordAfter = contentKeywords.get(indexAfter);
            if (keywordAfter != null) {
                keyword = keywordAfter;
                subStart = datePos + date.getDateString().length();
                subEnd = indexAfter;
                break;
            }
        }
        if (keyword != null) {
            date.setKeyword(keyword);
            int diff = -1;
            if (subEnd > subStart) {
                diff = StringHelper.countWhitespaces(documentString.substring(subStart, subEnd));
            }
            if (diff >= 30 || diff == -1) {
                date.setKeyDiff(0.0);
            } else {
                date.setKeyDiff(1 - MathHelper.round(diff / 30.0, 3));
            }
        }
    }

    /**
     * Check a {@link Node} for a date keyword (see {@link KeyWords#BODY_CONTENT_KEYWORDS_ALL}).
     *
     * @param node The node which to check for a keyword, not <code>null</code>.
     * @return The keyword if found, or an empty String if no keyword was found.
     */
    private static String getNodeKeyword(Node node) {
        String nodeText = HtmlHelper.xmlToString(node);
        return KeyWords.searchKeyword(nodeText, KeyWords.BODY_CONTENT_KEYWORDS_ALL);
    }

    static List<ContentDate> findAllDates(String text) {
        List<ContentDate> dates = new ArrayList<>();
        for (DateFormat format : RegExp.ALL_DATE_FORMATS) {
            Matcher matcher = format.getPattern().matcher(text);
            while (matcher.find()) {
                boolean digitNeighbor = false;
                int start = matcher.start();
                if (start > 0) {
                    digitNeighbor = Character.isDigit(text.charAt(start - 1));
                }
                int end = matcher.end();
                if (end < text.length()) {
                    digitNeighbor = Character.isDigit(text.charAt(end));
                }
                if (!digitNeighbor) {
                    String dateString = matcher.group();
                    ContentDate date = new ContentDate(DateParser.parseDate(dateString, format));
                    int datePosition = text.indexOf(date.getDateString());
                    date.setTagPos(datePosition);
                    text = text.replaceFirst(dateString, StringUtils.repeat('x', dateString.length()));
                    dates.add(date);
                }
            }
        }
        return dates;
    }

    /**
     * Sometimes texts in webpages have special code for character. E.g. <i>&ampuuml;</i> or whitespace. To evaluate
     * this text reasonably you need to convert this code.
     */
    private static String replaceHtmlSymbols(String text) {
        String result = StringEscapeUtils.unescapeHtml(text);
        result = StringHelper.replaceProtectedSpace(result);
        result = StringHelper.removeDoubleWhitespaces(result);

        // remove undesired characters
        result = result.replace("&#8203;", " "); // empty whitespace
        result = result.replace("\n", " ");
        result = result.replace("&#09;", " "); // html tabulator
        result = result.replace("\t", " ");
        result = result.replace(" ,", " ");

        return result;
    }
}
