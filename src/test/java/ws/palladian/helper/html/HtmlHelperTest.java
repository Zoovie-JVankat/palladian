package ws.palladian.helper.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.io.FileNotFoundException;

import junit.framework.Assert;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.ResourceHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * Test cases for the HTMLHelper class.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class HtmlHelperTest {

    // @Test
    // public void testGetHTMLContent() {
    //
    // Crawler crawler = new Crawler();
    // Document document = crawler.getWebDocument(ResourceHelper.getResourcePath("/webPages/newsPage1.html"));
    //
    // // System.out.println(PageAnalyzer.getRawMarkup(document));
    // System.out.println(HTMLHelper.htmlToReadableText(document));
    //
    // }

    @Test
    public void testCountTags() {
        assertEquals(4, HtmlHelper.countTags("everybody is <b>here</b> to do some <p>work</p>"));
        assertEquals(4, HtmlHelper.countTags("<br />everybody is <b>here</b> to do some <p>work"));
        assertEquals(4, HtmlHelper.countTags("<br />everybody<br /> is <b>here</b> to do some <p>work", true));
        assertEquals(7, HtmlHelper.countTags("<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <p>work"));
        assertEquals(6, HtmlHelper.countTags(
                "<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <a>abc</a> <p>work", true));
    }

    @Test
    public void testCountTagLength() {
        assertEquals(0, HtmlHelper.countTagLength("iphone 4"));
        assertEquals(15, HtmlHelper.countTagLength("<phone>iphone 4</Phone>"));
        assertEquals(20, HtmlHelper.countTagLength("everybody is <b>here<br /></b> to do some <p>work</p>"));
    }

    @Test
    public void testStripTags() throws FileNotFoundException {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        assertEquals("this is relevant", HtmlHelper.stripHtmlTags(htmlContent, true, true, true, true).trim());

        DocumentRetriever crawler = new DocumentRetriever();
        String content = crawler.getTextDocument(ResourceHelper
                .getResourcePath("/webPages/removeHTMLContentTest1.html"));
        String result = HtmlHelper.stripHtmlTags(content, true, true, true, false).replaceAll("(\\s){2,}", " ").trim();

        String stripped = "Samsung S8500 Wave 3D view, 360&deg; spin GSMArena.com HomeNewsReviewsBlogRankingsCoverageSoftwareGlossaryFAQLinksContact us Advanced search Samsung S8500 Wave 3D view - 360&deg; spin Samsung S8500 Wave review: Hello, world!Samsung S8500 Wave preview: First lookMWC 2010: Samsung overviewSpecifications Read opinions Compare Pictures Related &nbsp;(new) Manual Check Price WElectronicsPlemixOmio (UK)Mobile City OnlineSelectGSM Popularity Daily interest 48% Total hits: 1266454 Voting results Design 9.1 Features 9.1 Performance 9.1 12345678910 12345678910 12345678910 Votes: 38011 &nbsp; Drag to rotate, double-click to spin 360&deg;. In order to see the 360&deg; rotation the Flash plugin is required. &nbsp; &nbsp; NokiaSamsungMotorolaSony EricssonLGAppleHTCi-mateO2EtenHPGarmin- AsusGigabyteAcerPalmBlackBerryMicrosoftVodafoneT-MobileSagemAlcatelPhilipsSharpToshibaBenQHuaweiPantechi-mobileZTEiNQMicromaxVertu more rumor mill Phone finder Home News Reviews Blog Forum Compare Links Glossary &nbsp;RSS feed &nbsp;Facebook Privacy policy Contact us &copy; 2000 - 2010 GSMArena.com team. Terms of use.";
        result = result.replaceAll(System.getProperty("line.separator")," ");
        // System.out.println(DigestUtils.md5Hex(stripped));
        Assert.assertEquals(DigestUtils.md5Hex(stripped), DigestUtils.md5Hex(result));
//        assertThat(result,is(stripped));
    }

    @Test
    public void testHtmlToString() throws FileNotFoundException {
        DocumentRetriever c = new DocumentRetriever();
        Document doc = c.getWebDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test001.html"));
        String result = HtmlHelper.documentToReadableText(doc);
        Assert.assertEquals("489eb91cf94343d0b62e69c396bc6b6f", DigestUtils.md5Hex(result));
    }

    @Test
    public void testHtmlToString2() {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        System.out.println(HtmlHelper.documentToReadableText(htmlContent, true));
        // assertEquals("this is relevant", HTMLHelper.removeHTMLTags(htmlContent, true, true, true, false));

    }

    @Test
    public void testReplaceHTMLSymbols() {
        String htmlText = "&nbsp; &Auml; &auml; &Ouml; &ouml; &Uuml; &uuml; &szlig; &lt; &gt; &amp; &quot;";
        String clearText = "  Ä ä Ö ö Ü ü ß < > & \"";
        assertEquals(clearText, StringHelper.replaceProtectedSpace(StringEscapeUtils.unescapeHtml(htmlText)));
    }
}