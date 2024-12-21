package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository sitesRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        List<Site> allSites = sitesRepository.findAll();

        List<DetailedStatisticsItem> detailedStatistics = allSites.stream()
                .map(this::mapToDetailedStatisticsItem)
                .collect(Collectors.toList());

        TotalStatistics totalStatistics = calculateTotalStatistics(detailedStatistics, allSites.size());

        StatisticsData data = new StatisticsData();
        data.setTotal(totalStatistics);
        data.setDetailed(detailedStatistics);

        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);

        return response;
    }

    private DetailedStatisticsItem mapToDetailedStatisticsItem(Site site) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(site.getName());
        item.setUrl(site.getUrl());
        item.setStatus(site.getStatus().toString());
        item.setError(site.getLastError());
        item.setStatusTime(site.getStatusTime().toInstant(ZoneOffset.UTC).toEpochMilli());
        item.setPages(pageRepository.countBySite(site));
        item.setLemmas(lemmaRepository.countBySite(site));
        return item;
    }

    private TotalStatistics calculateTotalStatistics(List<DetailedStatisticsItem> detailedStatistics, int siteCount) {
        int totalPages = detailedStatistics.stream()
                .mapToInt(DetailedStatisticsItem::getPages)
                .sum();
        int totalLemmas = detailedStatistics.stream()
                .mapToInt(DetailedStatisticsItem::getLemmas)
                .sum();
        TotalStatistics total = new TotalStatistics();
        total.setPages(totalPages);
        total.setLemmas(totalLemmas);
        total.setSites(siteCount);
        total.setIndexing(true);
        return total;
    }
}