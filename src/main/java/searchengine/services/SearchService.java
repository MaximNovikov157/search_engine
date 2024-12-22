package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.exception.SearchException;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final LemmaParserService lemmaParserService;
    private final SnippetBuilderService snippetBuilderService;
    private final HtmlParserService htmlParserService;

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    private final static double LEMMA_EXCLUDE_THRESHOLD_PERCENTAGE = 75.0;

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    public SearchResponse search(String query, String siteUrl, Integer offset, Integer limit) {


        if (isInvalidQuery(query)) {
            throw new SearchException("Поисковый запрос пуст");
        }

        List<Site> sites = getSites(siteUrl);

        logger.info("Поиск начат с запросом: '{}', сайт: '{}', смещение: {}, лимит: {}",
                    query, sites.stream().map(Site::getUrl).collect(Collectors.joining(", ", "[", "]")), offset, limit);
        if (!areSitesIndexed(sites)) {
            throw new SearchException("Указанные сайты не были полностью проиндексированы");
        }
        Set<String> parsedLemmas = parseQueryLemmas(query);
        Map<Page, Double> pageRank = calculatePageRank(parsedLemmas, sites);
        logger.info("Ранжирование страниц выполнено. Всего страниц: {}", pageRank.size());

        List<SearchData> searchData = generateSearchData(query, pageRank);
        logger.info("Сформированы данные поиска. Всего результатов: {}", searchData.size());

        return new SearchResponse(true, searchData.size(), paginate(searchData, offset, limit));
    }

    private boolean isInvalidQuery(String query) {
        return query == null || query.isBlank();
    }

    private Set<String> parseQueryLemmas(String query) {
        return lemmaParserService.parseLemmas(query).keySet();
    }

    private Map<Page, Double> calculatePageRank(Set<String> parsedLemmas, List<Site> sites) {
        Map<Page, Double> pageRank = new HashMap<>();
        for (Site site : sites) {
            List<Lemma> filteredLemmas = getFilteredLemmas(parsedLemmas, site);
            Set<Page> pages = getPagesFromLemmas(filteredLemmas);
            pageRank.putAll(calculateRelevanceForPages(pages, new HashSet<>(filteredLemmas)));
        }
        return pageRank;
    }

    private List<Lemma> getFilteredLemmas(Set<String> parsedLemmas, Site site) {
        long threshold = calculateExcludeThreshold(site);
        return filterLemmas(parsedLemmas, threshold, site);
    }

    private List<Lemma> filterLemmas(Set<String> parsedLemmas, long threshold, Site site) {
        return parsedLemmas.stream()
                .map(lemma -> lemmaRepository.findByLemmaAndSite(lemma, site))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(lemma -> lemma.getFrequency() < threshold)
                .sorted(Comparator.comparing(Lemma::getFrequency))
                .collect(Collectors.toList());
    }

    private long calculateExcludeThreshold(Site site) {
        long totalPages = pageRepository.countAllBySite(site);
        return Math.round(totalPages * (LEMMA_EXCLUDE_THRESHOLD_PERCENTAGE / 100.0));
    }

    private Set<Page> getPagesFromLemmas(List<Lemma> lemmas) {
        if (lemmas.isEmpty()) {
            return Set.of();
        }
        Set<Page> pages = getInitialPages(lemmas.get(0));
        return specifyPagesWithLemmas(lemmas, pages);
    }

    private Set<Page> getInitialPages(Lemma lemma) {
        return lemma.getIndexesModel().stream()
                .map(Index::getPage)
                .collect(Collectors.toSet());
    }

    private Set<Page> specifyPagesWithLemmas(List<Lemma> lemmas, Set<Page> pages) {
        for (int i = 1; i < lemmas.size(); i++) {
            pages = indexRepository.findByLemmaAndPageIn(lemmas.get(i), pages).stream()
                    .map(Index::getPage)
                    .collect(Collectors.toSet());
            if (pages.isEmpty()) {
                return pages;
            }
        }
        return pages;
    }

    private Map<Page, Double> calculateRelevanceForPages(Set<Page> pages, Set<Lemma> lemmas) {
        return pages.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        page -> calculateAbsoluteRelevance(page, lemmas)
                ));
    }

    private double calculateAbsoluteRelevance(Page page, Set<Lemma> lemmas) {
        return indexRepository.findByPageAndLemmaIn(page, lemmas).stream()
                .mapToDouble(Index::getRank)
                .sum();
    }

    private List<SearchData> generateSearchData(String query, Map<Page, Double> pageRank) {
        Optional<Double> maxRankOpt = pageRank.values().stream().max(Double::compareTo);
        if (maxRankOpt.isEmpty()) {
            return List.of();
        }
        double maxRank = maxRankOpt.get();

        return pageRank.entrySet().stream()
                .map(entry -> createSearchData(query, entry, maxRank))
                .sorted(Comparator.comparing(SearchData::getRelevance).reversed())
                .collect(Collectors.toList());
    }

    private SearchData createSearchData(String query, Map.Entry<Page, Double> entry, double maxRank) {
        Page page = entry.getKey();
        double relevance = entry.getValue() / maxRank;
        String content = page.getContent();
        String snippet = snippetBuilderService.generateSnippet(query, htmlParserService.cleanHtml(content));
        return new SearchData(
                page.getSite().getUrl(),
                page.getSite().getName(),
                page.getPath(),
                htmlParserService.getTitle(content),
                snippet,
                (float) relevance
        );
    }

    private List<SearchData> paginate(List<SearchData> searchData, Integer offset, Integer limit) {
        int fromIndex = Math.min(offset, searchData.size());
        int toIndex = Math.min(fromIndex + limit, searchData.size());
        return searchData.subList(fromIndex, toIndex);
    }

    private boolean areSitesIndexed(List<Site> sites) {
        return sites.stream().allMatch(site -> site.getStatus().equals(Status.INDEXED));
    }

    private List<Site> getSites(String siteUrl) {
        if (siteUrl == null || siteUrl.isBlank()) {
            return siteRepository.findAll();
        }
        return List.of(siteRepository.findByUrlStartingWith(siteUrl));
    }

}