package ws.palladian.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ws.palladian.helper.io.ResourceHelper;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * @formatter:off
 */
public class UrlHelperTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testGetCleanUrl() {
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("http://www.amazon.com/"));
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("http://amazon.com/"));
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("https://www.amazon.com/"));
        assertEquals("amazon.com", UrlHelper.getCleanUrl("https://amazon.com"));
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("www.amazon.com/"));
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("amazon.com/"));
    }

    @Test
    public void testGetDomain() {
        collector.checkThat(UrlHelper.getDomain("https://www.ashland.or.us/", false, false), is("ashland.or.us"));
        collector.checkThat(UrlHelper.getDomain("http://ailejeunesse.ccirs.qc.ca/", false, false), is("ccirs.qc.ca"));
        collector.checkThat(UrlHelper.getDomain("https://ashland.municipal.codes/", false, false), is("municipal.codes"));
        collector.checkThat(UrlHelper.getDomain("https://ashland.municipal.codes/", false, true), is("ashland.municipal.codes"));
        collector.checkThat(UrlHelper.getDomain("http://www.amazon.co.uk", false, false), is("amazon.co.uk"));
        collector.checkThat(UrlHelper.getDomain("http://amazon.co.uk", false, false), is("amazon.co.uk"));
        collector.checkThat(UrlHelper.getDomain("http://test.com", false, false), is("test.com"));

        collector.checkThat(UrlHelper.getDomain("http://bb.rentokil.com", false, false), is("rentokil.com"));
        collector.checkThat(UrlHelper.getDomain("http://bb.rentokil.com", false, true), is("bb.rentokil.com"));


        collector.checkThat(UrlHelper.getDomain("http://sub.domain.with.points.test.ac.uk", false, false), is("test.ac.uk"));
        collector.checkThat(UrlHelper.getDomain("http://www.companies-reviews.com/review/3168408/Sales-Promotion-Agency-Expression/", false, false), is("companies-reviews.com"));

        assertEquals("http://www.flashdevices.net",
                UrlHelper.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html", true));
        assertEquals("www.flashdevices.net",
                UrlHelper.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html", false));

        assertEquals("http://blog.wired.com",
                UrlHelper.getDomain("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html", true));
        assertEquals("blog.wired.com",
                UrlHelper.getDomain("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html", false));

        // added by Philipp
        assertEquals("https://example.com", UrlHelper.getDomain("https://example.com/index.html"));
        assertEquals("", UrlHelper.getDomain(""));
        assertEquals("", UrlHelper.getDomain(null));
        assertEquals("", UrlHelper.getDomain("file:///test.html")); // TODO return localhost here?
        assertEquals("localhost", UrlHelper.getDomain("file://localhost/test.html", false));

        // https://forum.knime.com/t/bug-url-domain-extractor-does-not-recognize-tld-and-breaks-for-upper-case/45642
        assertEquals("upper-case.com", UrlHelper.getDomain("https://UPPER-CASE.COM", false, false));
        assertEquals("uppercase.com", UrlHelper.getDomain("https://UPPERCASE.COM", false, false));
        assertEquals("domain.co.ke", UrlHelper.getDomain("https://domain.domain.co.ke", false, false));
        assertEquals("domain.com.mm", UrlHelper.getDomain("https://domain.com.mm", false, false));
        // https://forum.knime.com/t/url-domain-extractor-doesnt-work-with-new-tlds/42828
        assertEquals("abc.crypto", UrlHelper.getDomain("http://abc.crypto", false, false));
        // assertEquals("abc.blockchain", UrlHelper.getDomain("http://abc.blockchain", false, false));
        // assertEquals("abc.bitcoin", UrlHelper.getDomain("http://abc.bitcoin", false, false));
        assertEquals("abc.coin", UrlHelper.getDomain("http://abc.coin", false, false));
        // assertEquals("abc.nft", UrlHelper.getDomain("http://abc.nft", false, false));
        // assertEquals("abc.wallet", UrlHelper.getDomain("http://abc.wallet", false, false));
        // assertEquals("abc.dao", UrlHelper.getDomain("http://abc.dao", false, false));
        // assertEquals("abc.x", UrlHelper.getDomain("http://abc.x", false, false));
        assertEquals("abc.com", UrlHelper.getDomain("http://abc.com", false, false));
    }

    @Test
    public void testGetDomainFast() {
        collector.checkThat(UrlHelper.getDomainFast("https://example.com/index.html"), is("example.com"));
        collector.checkThat(UrlHelper.getDomainFast("https://www.ashland.or.us/"), is("ashland.or.us"));
        collector.checkThat(UrlHelper.getDomainFast("http://ailejeunesse.ccirs.qc.ca/"), is("ccirs.qc.ca"));
        collector.checkThat(UrlHelper.getDomainFast("https://ashland.municipal.codes/"), is("municipal.codes"));
        collector.checkThat(UrlHelper.getDomainFast("http://www.amazon.co.uk"), is("amazon.co.uk"));
        collector.checkThat(UrlHelper.getDomainFast("http://amazon.co.uk"), is("amazon.co.uk"));
        collector.checkThat(UrlHelper.getDomainFast("http://test.com"), is("test.com"));

        collector.checkThat(UrlHelper.getDomainFast("http://bb.rentokil.com"), is("rentokil.com"));

        collector.checkThat(UrlHelper.getDomainFast("http://sub.domain.with.points.test.ac.uk"), is("test.ac.uk"));
        collector.checkThat(UrlHelper.getDomainFast("http://www.companies-reviews.com/review/3168408/Sales-Promotion-Agency-Expression/"), is("companies-reviews.com"));

        // added by Philipp
        assertEquals("", UrlHelper.getDomainFast(""));
        assertEquals("", UrlHelper.getDomainFast(null));

        // https://forum.knime.com/t/bug-url-domain-extractor-does-not-recognize-tld-and-breaks-for-upper-case/45642

        // https://forum.knime.com/t/url-domain-extractor-doesnt-work-with-new-tlds/42828
        assertEquals("abc.crypto", UrlHelper.getDomainFast("http://abc.crypto"));
        // assertEquals("abc.blockchain", UrlHelper.getDomain("http://abc.blockchain", false, false));
        // assertEquals("abc.bitcoin", UrlHelper.getDomain("http://abc.bitcoin", false, false));
        assertEquals("abc.coin", UrlHelper.getDomainFast("http://abc.coin"));
        // assertEquals("abc.nft", UrlHelper.getDomain("http://abc.nft", false, false));
        // assertEquals("abc.wallet", UrlHelper.getDomain("http://abc.wallet", false, false));
        // assertEquals("abc.dao", UrlHelper.getDomain("http://abc.dao", false, false));
        // assertEquals("abc.x", UrlHelper.getDomain("http://abc.x", false, false));
        assertEquals("abc.com", UrlHelper.getDomainFast("http://abc.com"));

//        assertEquals("flashdevices.net",
//                UrlHelper.getDomainFast("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html"));
//
//        assertEquals("wired.com",
//                UrlHelper.getDomainFast("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html"));
//
//        assertEquals("domain.co.ke", UrlHelper.getDomainFast("https://domain.domain.co.ke"));
//        assertEquals("domain.com.mm", UrlHelper.getDomainFast("https://domain.com.mm"));

    }

    @Test
    public void testMakeFullUrl() {
        assertEquals("https://www.software-express.de/hersteller/microsoft/microsoft-365/add-ons/e5-compliance/", UrlHelper.makeFullUrl("https://www.software-express.de", null, "/hersteller/microsoft/microsoft-365/add-ons/10-year-audit-log-retention/../e5-compliance/"));
        assertEquals("https://www.software-express.de/hersteller/microsoft/microsoft-365/e5-compliance/", UrlHelper.makeFullUrl("https://www.software-express.de", null, "/hersteller/microsoft/microsoft-365/add-ons/10-year-audit-log-retention/../../e5-compliance/"));
        assertEquals("http://big-planet.biz/vacancies.php?d=aaa", UrlHelper.makeFullUrl("http://big-planet.biz/vacancies.php?d=123&b=whatever", null, "?d=aaa"));
        assertEquals("http://big-planet.biz/vacancies.php?d=314", UrlHelper.makeFullUrl("http://big-planet.biz/vacancies.php", null, "?d=314"));
        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullUrl("http://www.xyz.de", "", "page.html"));
        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullUrl("http://www.xyz.de", null, "page.html"));
        assertEquals("http://www.xyz.de/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de/index.html", "", "page.html"));
        assertEquals("http://www.xyz.de/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de/index.html", "/directory", "/page.html"));
        assertEquals("http://www.xyz.de/directory/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de/index.html", "/directory", "./page.html"));
        assertEquals("http://www.xyz.de/directory/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de/directory/", "./page.html"));

        assertEquals("http://www.xyz.de/directory/page.html", UrlHelper.makeFullUrl("http://www.xyz.de/directory/directory/", "../page.html"));
        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullUrl("http://www.xyz.de/directory/", "../page.html"));
        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullUrl("http://www.xyz.de/directory", "../page.html"));
        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullUrl("http://www.xyz.de/", "../page.html"));
        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullUrl("http://www.xyz.de", "../page.html"));

        assertEquals("http://www.xyz.de/directory/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de/index.html", "/directory/directory", "../page.html"));

        assertEquals("http://www.abc.de/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de", "", "http://www.abc.de/page.html"));
        assertEquals("http://www.abc.de/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de", "http://www.abc.de/", "/page.html"));

        assertEquals("http://www.example.com/page.html",
                UrlHelper.makeFullUrl("/some/file/path.html", "http://www.example.com/page.html"));
        assertEquals("mailto:example@example.com", UrlHelper.makeFullUrl("http://www.xyz.de", "mailto:example@example.com"));

        assertEquals("http://www.example.com/page.html",
                UrlHelper.makeFullUrl(null, null, "http://www.example.com/page.html"));

        // when no linkUrl is supplied, we cannot determine the full URL, UrlHelper throws NPE
        try {
            assertEquals(null, UrlHelper.makeFullUrl(null, "http://www.example.com", null));
            assertEquals("", UrlHelper.makeFullUrl("http://www.example.com", null, null));
            assertEquals("", UrlHelper.makeFullUrl(null, null, "/page.html"));
            fail();
        } catch (NullPointerException e) {

        }
    }

    @Test
    public void testExtractUrls() {
        String text = "The quick brown fox jumps over the lazy dog. Check out: http://microsoft.com, www.apple.com, google.com. (www.tu-dresden.de), http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars.";
        List<String> urls = UrlHelper.extractUrls(text);
        assertThat(urls, hasItem("http://microsoft.com"));
        assertThat(urls, hasItem("www.apple.com"));
        assertThat(urls, hasItem("google.com"));
        assertThat(urls, hasItem("www.tu-dresden.de"));
        assertThat(urls, hasItem("http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars"));

        // test URLs from <http://daringfireball.net/2010/07/improved_regex_for_matching_urls>

        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("http://foo.com/blah_blah").get(0));
        assertEquals("http://foo.com/blah_blah/", UrlHelper.extractUrls("http://foo.com/blah_blah/").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("(Something like http://foo.com/blah_blah)").get(0));
        assertEquals("http://foo.com/blah_blah_(wikipedia)", UrlHelper.extractUrls("http://foo.com/blah_blah_(wikipedia)").get(0));
        assertEquals("http://foo.com/more_(than)_one_(parens)", UrlHelper.extractUrls("http://foo.com/more_(than)_one_(parens)").get(0));
        assertEquals("http://foo.com/blah_blah_(wikipedia)", UrlHelper.extractUrls("(Something like http://foo.com/blah_blah_(wikipedia))").get(0));
        assertEquals("http://foo.com/blah_(wikipedia)#cite-1", UrlHelper.extractUrls("http://foo.com/blah_(wikipedia)#cite-1").get(0));
        assertEquals("http://foo.com/blah_(wikipedia)_blah#cite-1", UrlHelper.extractUrls("http://foo.com/blah_(wikipedia)_blah#cite-1").get(0));
        assertEquals("http://foo.com/unicode_(✪)_in_parens", UrlHelper.extractUrls("http://foo.com/unicode_(✪)_in_parens").get(0));
        assertEquals("http://foo.com/(something)?after=parens", UrlHelper.extractUrls("http://foo.com/(something)?after=parens").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("http://foo.com/blah_blah.").get(0));
        assertEquals("http://foo.com/blah_blah/", UrlHelper.extractUrls("http://foo.com/blah_blah/.").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("<http://foo.com/blah_blah>").get(0));
        assertEquals("http://foo.com/blah_blah/", UrlHelper.extractUrls("<http://foo.com/blah_blah/>").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("http://foo.com/blah_blah,").get(0));
        assertEquals("http://www.extinguishedscholar.com/wpglob/?p=364", UrlHelper.extractUrls("http://www.extinguishedscholar.com/wpglob/?p=364.").get(0));
        assertEquals("http://example.com", UrlHelper.extractUrls("<tag>http://example.com</tag>").get(0));
        assertEquals("www.example.com", UrlHelper.extractUrls("Just a www.example.com link.").get(0));
        assertEquals("http://example.com/something?with,commas,in,url", UrlHelper.extractUrls("http://example.com/something?with,commas,in,url, but not at end").get(0));
        assertEquals("bit.ly/foo", UrlHelper.extractUrls("bit.ly/foo").get(0));
        //        assertEquals("is.gd/foo/", UrlHelper.extractUrls("“is.gd/foo/”").get(0));
        assertEquals("WWW.EXAMPLE.COM", UrlHelper.extractUrls("WWW.EXAMPLE.COM").get(0));
        ////        assertEquals("http://www.asianewsphoto.com/(S(neugxif4twuizg551ywh3f55))/Web_ENG/View_DetailPhoto.aspx?PicId=752", UrlHelper.extractUrls("http://www.asianewsphoto.com/(S(neugxif4twuizg551ywh3f55))/Web_ENG/View_DetailPhoto.aspx?PicId=752").get(0));
        ////        assertEquals("http://www.asianewsphoto.com/(S(neugxif4twuizg551ywh3f55))", UrlHelper.extractUrls("http://www.asianewsphoto.com/(S(neugxif4twuizg551ywh3f55))").get(0));
        ////        assertEquals("http://lcweb2.loc.gov/cgi-bin/query/h?pp/horyd:@field(NUMBER+@band(thc+5a46634))", UrlHelper.extractUrls("http://lcweb2.loc.gov/cgi-bin/query/h?pp/horyd:@field(NUMBER+@band(thc+5a46634))").get(0));
        assertEquals("http://example.com/quotes-are-“part”", UrlHelper.extractUrls("http://example.com/quotes-are-“part”").get(0));
        assertEquals("example.com", UrlHelper.extractUrls("example.com").get(0));
        assertEquals("example.com/", UrlHelper.extractUrls("example.com/").get(0));
        assertThat(UrlHelper.extractUrls("[url=http://foo.com/blah_blah]http://foo.com/blah_blah[/url]"), hasItem("http://foo.com/blah_blah"));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("'http://foo.com/blah_blah'").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("\"http://foo.com/blah_blah\"").get(0));

        assertEquals("cinefreaks.com/coolstuff.zip", UrlHelper.extractUrls("You can download it here: cinefreaks.com/coolstuff.zip but be aware of the size.").get(0));
        assertEquals("1-2-3.net/auctions-Are-out.jpg", UrlHelper.extractUrls("You can download it here: 1-2-3.net/auctions-Are-out.jpg but be aware of the size.").get(0));
        assertEquals("http://www.cinefreaks.com/coolstuff.zip", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com/coolstuff.zip but be aware of the size.").get(0));
        assertEquals("www.cinefreaks.com/coolstuff.zip", UrlHelper.extractUrls("You can download it here: www.cinefreaks.com/coolstuff.zip but be aware of the size.").get(0));
        assertEquals("http://www.cinefreaks.com/", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com/").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com.").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com?").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com! Or somewhere else").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com. This is the next sentence").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com, this is the next...").get(0));
        assertEquals("http://www.google.com/search?tbm=isch&hl=en&source=hp&biw=1660&bih=751&q=alfred+neuman+mad+magazine&gbv=2&aq=1s&aqi=g1g-s1g-sx1&aql=&oq=alfred+newman+m", UrlHelper.extractUrls("http://www.google.com/search?tbm=isch&hl=en&source=hp&biw=1660&bih=751&q=alfred+neuman+mad+magazine&gbv=2&aq=1s&aqi=g1g-s1g-sx1&aql=&oq=alfred+newman+m").get(0));

        assertEquals("http://add.my.yahoo.com/rss?url=http://feeds.reuters.com/news/artsculture", UrlHelper.extractUrls("http://add.my.yahoo.com/rss?url=http://feeds.reuters.com/news/artsculture").get(0));
        assertEquals("http://stockscreener.us.reuters.com/Stock/US/Index?quickscreen=gaarp", UrlHelper.extractUrls("http://stockscreener.us.reuters.com/Stock/US/Index?quickscreen=gaarp").get(0));

        // no URLs
        assertEquals(0, UrlHelper.extractUrls("6:00p").size());
        assertEquals(0, UrlHelper.extractUrls("filename.txt").size());

        assertEquals(0, UrlHelper.extractUrls("16-28-33.0.backup.allcues.update.7z").size());
        assertEquals(0, UrlHelper.extractUrls("09.Sep.11").size());
        assertEquals(0, UrlHelper.extractUrls("Environment.CurrentDirectory").size());
        assertEquals(0, UrlHelper.extractUrls("zipProcess.StandardOutput.ReadToEnd()").size());

        assertEquals(0, UrlHelper.extractUrls("check_lang.sh").size());
    }

    @Test
    public void testRemoveSessionId() {
        assertEquals("http://www.idealo.de/preisvergleich/OffersOfProduct/3914600_-lumia-925-16gb-white-nokia.html", UrlHelper.removeSessionId(
                "http://www.idealo.de/preisvergleich/OffersOfProduct/3914600_-lumia-925-16gb-white-nokia.html;jsessionid=a1jUi00AR7u-"));
        assertEquals("http://brbb.freeforums.org/viewforum.php?f=3", UrlHelper.removeSessionId(
                "http://brbb.freeforums.org/viewforum.php?f=3&sid=5c2676a9f621ffbadb6962da7e0c50d4"));
        assertEquals("http://brbb.freeforums.org/viewforum.php", UrlHelper.removeSessionId(
                "http://brbb.freeforums.org/viewforum.php?sid=5c2676a9f621ffbadb6962da7e0c50d4"));
        assertEquals("http://brbb.freeforums.org/viewforum.php?f=3", UrlHelper.removeSessionId(
                "http://brbb.freeforums.org/viewforum.php?sid=5c2676a9f621ffbadb6962da7e0c50d4&f=3"));
        assertEquals("http://brbb.freeforums.org/viewforum.php?f=3", UrlHelper.removeSessionId(
                "http://brbb.freeforums.org/viewforum.php?f=3;sid=5c2676a9f621ffbadb6962da7e0c50d4"));
        assertEquals("http://www.hagebau.de/Garten-und-Freizeit/Pavillons/sh3391862", UrlHelper.removeSessionId(
                "http://www.hagebau.de/Garten-und-Freizeit/Pavillons/sh3391862;sid=QyZVAH0QUTdSAC95WwlCaREaDHk7KDmlYK6T5C8iB5N2iJYpHCTsIwPsB5N2iA1M7nBXWZ_h"));
        assertEquals("http://www.hagebau.de/Haus-und-Wohnen/Bodenbelaege/sh3643539", UrlHelper.removeSessionId(
                "http://www.hagebau.de/Haus-und-Wohnen/Bodenbelaege/sh3643539;sid=9mu2ltD6VX66loKT7-yh_7zwuTTYvjkytL1-x8VEst6VHl1dpMdkY4dIst6VHts2TAsZslNV"));
    }

    @Test
    public void testGetCanonicalUrl() {
        assertEquals("http://www.funs.co.uk/comic/",
                UrlHelper.getCanonicalUrl("http://www.funs.co.uk/comic/index.html"));
        assertEquals(
                "http://sourceforge.net/tracker/?aid=1954302&atid=377408&func=detail&group_id=23067",
                UrlHelper
                        .getCanonicalUrl("http://sourceforge.net/tracker/?func=detail&aid=1954302&group_id=23067&atid=377408"));
        assertEquals("http://sourceforge.net/", UrlHelper.getCanonicalUrl("http://sourceforge.net/"));
        assertEquals(
                "http://sourceforge.net/tracker/?aid=3492945&atid=377408&func=detail&group_id=23067",
                UrlHelper
                        .getCanonicalUrl("http://sourceforge.net/tracker/?func=detail&aid=3492945&group_id=23067&atid=377408#artifact_comment_6199621"));
    }

    @Test
    public void testMakeAbsoluteUrls() throws Exception {
        Document document = ParseUtil.parseXhtml(ResourceHelper.getResourceFile("/w3c_xhtml_strict.html"));
        document.setDocumentURI("http://www.w3.org/TR/xhtml1/");

        UrlHelper.makeAbsoluteUrls(document);

        NodeList aNodes = document.getElementsByTagName("a");
        Node aNode = aNodes.item(8);
        assertEquals("http://www.w3.org/TR/xhtml1/xhtml1-diff.html", aNode.getAttributes().getNamedItem("href").getTextContent());
    }

    @Test
    public void testParseParams() {
        String url = "http://de.wikipedia.org/wiki/Spezial:Search?search=San%20Francisco&go=Artikel";
        List<Pair<String, String>> params = UrlHelper.parseParams(url);
        assertEquals(2, params.size());
        assertEquals(Pair.of("search", "San Francisco"), params.get(0));
        assertEquals(Pair.of("go", "Artikel"), params.get(1));
        // CollectionHelper.print(params);

        url = "https://ksuster.ch/portraet/auszeichnungen?tx_news_pi1%5Bcontroller%5D=News&tx_news_pi1%5BcurrentPage%5D=3&cHash=98018db6c83d2c41608f56c297779755";
        params = UrlHelper.parseParams(url);
        // CollectionHelper.print(params);
        assertEquals(3, params.size());
        assertEquals(Pair.of("tx_news_pi1[controller]", "News"), params.get(0));
        assertEquals(Pair.of("tx_news_pi1[currentPage]", "3"), params.get(1));
        assertEquals(Pair.of("cHash", "98018db6c83d2c41608f56c297779755"), params.get(2));

        url = "https://xxxxxxxx.de/gp/associates/network/reports/report.html?__mk_de_DE=xxxxxxtag=&reportType=earningsReport&program=all&deviceType=all&periodTyp";
        params = UrlHelper.parseParams(url);
        // CollectionHelper.print(params);
        assertEquals(5, params.size());
        assertEquals(Pair.of("__mk_de_DE", "xxxxxxtag="), params.get(0));
        assertEquals(Pair.of("reportType", "earningsReport"), params.get(1));
        assertEquals(Pair.of("program", "all"), params.get(2));
        assertEquals(Pair.of("deviceType", "all"), params.get(3));
        assertEquals(Pair.of("periodTyp", null), params.get(4));

        // https://tech.knime.org/forum/palladian/http-retriever-problem-with-some-urls
        url = "https://idw-online.de/de/pressreleasesrss?country_ids=35&country_ids=36&country_ids=46&country_ids=188&country_ids=65&country_ids=66&country_ids=68&country_ids=95&country_ids=97&country_ids=121&country_ids=126&country_ids=146&country_ids=147&country_ids=180&category_ids=10&category_ids=7&field_ids=100&field_ids=101&field_ids=401&field_ids=603&field_ids=600&field_ids=400&field_ids=606&field_ids=204&field_ids=102&field_ids=306&langs=de_DE&langs=en_US";
        params = UrlHelper.parseParams(url);
        assertEquals(28, params.size());

        // https://tech.knime.org/forum/palladian/problems-with-url-parameters
        url = "http://www.apvigo.com/control.php?sph=o_lsteventos_fca=13/11/2015%%a_iap=1351%%a_lsteventos_vrp=0";
        // url = "http://www.apvigo.com/control.php?sph=o_lsteventos_fca%3D13/11/2015%25%25a_iap%3D1351%25%25a_lsteventos_vrp%3D0";
        params = UrlHelper.parseParams(url);
        assertEquals(1, params.size());
        assertEquals(Pair.of("sph", "o_lsteventos_fca=13/11/2015%%a_iap=1351%%a_lsteventos_vrp=0"), params.get(0));

        // http://www.ideastorm.com/idea2ExploreMore?v=1538483000186&Type=AllIdeas&pagenum=2#comments
        url = "http://www.ideastorm.com/idea2ExploreMore?v=1538483000186&Type=AllIdeas&pagenum=2#comments";
        params = UrlHelper.parseParams(url);
        assertEquals(3, params.size());
        assertEquals(Pair.of("v", "1538483000186"), params.get(0));
        assertEquals(Pair.of("Type", "AllIdeas"), params.get(1));
        assertEquals(Pair.of("pagenum", "2"), params.get(2));

        // distinguish between &foo and &foo=
        url = "http://example.com?foo";
        params = UrlHelper.parseParams(url);
        assertEquals(1, params.size());
        assertEquals(Pair.of("foo", null), params.get(0));

        url = "http://example.com?foo=";
        params = UrlHelper.parseParams(url);
        assertEquals(1, params.size());
        assertEquals(Pair.of("foo", ""), params.get(0));
    }

    @Test
    public void testCreateParameterString() {
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(Pair.of("search", "San Francisco"));
        params.add(Pair.of("go", "Artikel"));
        String parameterString = UrlHelper.createParameterString(params);
        assertEquals("search=San+Francisco&go=Artikel", parameterString);

        params = new ArrayList<>();
        params.add(Pair.of("param", "value"));
        params.add(Pair.of("emptyParam", StringUtils.EMPTY));
        parameterString = UrlHelper.createParameterString(params);
        assertEquals("param=value&emptyParam=", parameterString);

        // distinguish between &foo and &foo=
        params = new ArrayList<>();
        params.add(Pair.of("foo", null));
        parameterString = UrlHelper.createParameterString(params);
        assertEquals("foo", parameterString);

        params = new ArrayList<>();
        params.add(Pair.of("foo", ""));
        parameterString = UrlHelper.createParameterString(params);
        assertEquals("foo=", parameterString);
    }

    @Test
    public void testGetBaseUrl() {
        String url = "https://api.twitter.com/1/statuses/update.json?include_entities=true";
        assertEquals("https://api.twitter.com/1/statuses/update.json", UrlHelper.parseBaseUrl(url));
        url = "http://www.example.org/foo.html#bar";
        assertEquals("http://www.example.org/foo.html", UrlHelper.parseBaseUrl(url));
        url = "http://www.example.org/foo.html?baz=boom#bar";
        assertEquals("http://www.example.org/foo.html", UrlHelper.parseBaseUrl(url));
        url = "http://www.example.org/foo.html";
        assertEquals("http://www.example.org/foo.html", UrlHelper.parseBaseUrl(url));
    }
}
