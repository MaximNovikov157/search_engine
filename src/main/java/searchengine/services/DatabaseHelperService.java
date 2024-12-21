package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DatabaseHelperService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    private static final Logger logger = LoggerFactory.getLogger(PageIndexingService.class);

    @Transactional
    public void deleteOldSiteData(Site site) {
        pageRepository.deleteBySite(site);
        lemmaRepository.deleteBySite(site);
        indexRepository.deleteByLemmaSite(site);
        siteRepository.delete(site);
    }

    @Transactional
    public void deletePageData(Page page) {
        List<Index> indices = indexRepository.findAllByPage(page);

        for (Index index : indices) {
            Lemma lemma = index.getLemma();
            lemma.setFrequency(lemma.getFrequency() - 1);

            if (lemma.getFrequency() <= 0) {
                lemmaRepository.delete(lemma);
            } else {
                lemmaRepository.save(lemma);
            }
        }

        indexRepository.deleteAllByPage(page);

        pageRepository.delete(page);
    }

    @Transactional
    public void populateDatabaseFromConfig(SitesList sitesList) {
        List<SiteConfig> siteConfigs = sitesList.getSites();
        if (siteConfigs != null) {
            for (SiteConfig siteConfig : siteConfigs) {
                Site site = new Site();
                site.setUrl(siteConfig.getUrl());
                site.setName(siteConfig.getName());
                site.setStatus(Status.FAILED);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }
        } else {
            logger.error("Список сайтов из конфигурации пуст или отсутствует");
        }
    }

    @Transactional
    public void updateLemmaAndIndex(Site site, Page page, Map<String, Integer> lemmaCounts) {
        for (Map.Entry<String, Integer> entry : lemmaCounts.entrySet()) {
            String lemmaText = entry.getKey();
            int countOnPage = entry.getValue();

            Lemma lemma = lemmaRepository.findByLemmaAndSite(lemmaText, site)
                    .orElse(new Lemma());

            if (lemma.getId() == null) {
                lemma.setSite(site);
                lemma.setLemma(lemmaText);
                lemma.setFrequency(1);
                lemmaRepository.save(lemma);
            } else {
                if (indexRepository.findByLemmaAndPage(lemma, page).isEmpty()) {
                    lemma.setFrequency(lemma.getFrequency() + 1);
                    lemmaRepository.save(lemma);
                }
            }

            if (indexRepository.findByLemmaAndPage(lemma, page).isEmpty()) {
                Index index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(countOnPage);
                indexRepository.save(index);
            }
        }
    }

    @Transactional
    public Page savePage(Site site, String url, int statusCode, String content) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(url);
        page.setCode(statusCode);
        page.setContent(content);
        pageRepository.save(page);
        return page;
    }

    @Transactional
    public Site saveSite(SiteConfig siteConfig, Status status, LocalDateTime localDateTime) {
        Site newSite = new Site();
        newSite.setUrl(siteConfig.getUrl());
        newSite.setName(siteConfig.getName());
        newSite.setStatus(status);
        newSite.setStatusTime(localDateTime);
        siteRepository.save(newSite);
        return newSite;
    }
}