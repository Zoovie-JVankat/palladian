package ws.palladian.retrieval.ranking.services;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import ws.palladian.retrieval.ranking.RankingServiceException;

public class ArchiveOrgCachedPageTest {
    @Test
    public void testArchiveOrgCachedPage() throws RankingServiceException {
        var urls = Arrays.asList(new String[] { //
                "http://example.com", //
                "http://this-page-is-extremely-unlikely-to-be-in-the-google-cache.com/but-never-say-never", //
        });
        var rankingService = new ArchiveOrgCachedPage();
        var ranking = rankingService.getRanking(urls);
        assertEquals((short) 1, ranking.get(urls.get(0)).getRankingById("wayback_machine_cached"));
        assertEquals((short) 0, ranking.get(urls.get(1)).getRankingById("wayback_machine_cached"));
    }
}
