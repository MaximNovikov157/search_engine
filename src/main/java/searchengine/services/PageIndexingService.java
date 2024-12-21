package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexApiResponse;
import searchengine.dto.index.PageFetchResult;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class PageIndexingService {

    private final SiteRepository sitesRepository;
    private final PageRepository pageRepository;
    private final DatabaseHelperService databaseHelperService;
    private final LemmaParserService lemmaParserService;
    private final HtmlParserService htmlParserService;
    private final SitesList sitesList;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private static final Logger logger = LoggerFactory.getLogger(PageIndexingService.class);

    public IndexApiResponse indexPage(String url) {
        stopRequested.set(false);
        try {
            if (sitesRepository.count() == 0) {
                databaseHelperService.populateDatabaseFromConfig(sitesList);
            }
            Site site = sitesRepository.findByUrlStartingWith(url);
            if (site == null) {
                return new IndexApiResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурации");
            }
            Optional<Page> existingPage = pageRepository.findBySiteAndPath(site, url);
            existingPage.ifPresent(databaseHelperService::deletePageData);

            processPage(site, url, false);
            return new IndexApiResponse(true, null);
        } catch (Exception e) {
            return new IndexApiResponse(false, "Ошибка индексации страницы: " + e.getMessage());
        }
    }

    private void processPage(Site site, String url, boolean processAllPages) {
        try {
            if (stopRequested.get()) {
                logger.warn("Остановка индексации: {}", url);
                return;
            }
            if (pageRepository.existsBySiteAndPath(site, htmlParserService.getPath(url))) {
                return;
            }
            logger.info("Обрабатываю страницу: " + url + " в потоке: " + Thread.currentThread().getName());
            PageFetchResult fetchResult = htmlParserService.fetchDocumentWithStatus(url);

            if (stopRequested.get()) {
                logger.info("Индексация остановлена для сайта {}", site.getUrl());
                return;
            }

            if (fetchResult.getStatusCode() >= 400) {
                throw new IOException("Ошибка HTTP: " + fetchResult.getStatusCode());
            }

            Page page = databaseHelperService.savePage(site, htmlParserService.getPath(url), fetchResult.getStatusCode(), fetchResult.getDocument().html());
            String cleanContent = htmlParserService.cleanHtml(fetchResult.getDocument().html());

            Map<String, Integer> lemmaCounts = lemmaParserService.parseLemmas(cleanContent);

            databaseHelperService.updateLemmaAndIndex(site, page, lemmaCounts);

            logger.info("Страница {} успешно проиндексирована", url);

            if (processAllPages) {
                if (stopRequested.get()) {
                    return;
                }
                processLinks(site, fetchResult.getDocument());
            }
        } catch (UnsupportedMimeTypeException e) {
            logger.warn("UnsupportedMimeTypeException", e);
        } catch (Exception e) {
            logger.error("Ошибка при обработке страницы: {}", url, e);
        }
    }

    private void processLinks(Site site, Document doc) {
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String linkHref = link.absUrl("href");
            if (stopRequested.get()) {
                return;
            }
            if (linkHref.startsWith(site.getUrl())) {
                processPage(site, linkHref, true);
            }
        }
    }

    public IndexApiResponse startIndexing() {
        stopRequested.set(false);
        if (stopRequested.get()) {
            return new IndexApiResponse(false, "Индексация уже остановлена.");
        }

        List<SiteConfig> siteConfigs = sitesList.getSites();
        if (siteConfigs == null) {
            return new IndexApiResponse(false, "Отсутствуют сайты в конфигурационном файле.");
        }
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        List<ForkJoinTask<?>> tasks = new ArrayList<>();
        for (SiteConfig siteConfig : siteConfigs) {
            Site site = sitesRepository.findByUrlStartingWith(siteConfig.getUrl());
            if (site != null) {
                databaseHelperService.deleteOldSiteData(site);
            }

            Site newSite = databaseHelperService.saveSite(
                    siteConfig,
                    Status.INDEXING,
                    LocalDateTime.now()
            );
            ForkJoinTask<?> task = forkJoinPool.submit(() -> processPage(newSite, siteConfig.getUrl(), true));
            tasks.add(task);
        }
        for (ForkJoinTask<?> task : tasks) {
            if (!stopRequested.get()) {
                task.join();
            }
        }

        for (SiteConfig siteConfig : siteConfigs) {
            Site site = sitesRepository.findByUrlStartingWith(siteConfig.getUrl());
            if (site != null) {
                if (stopRequested.get()) {
                    site.setStatus(Status.FAILED);
                    site.setLastError("Индексация остановлена пользователем");
                } else if (site.getStatus() == Status.INDEXING) {
                    site.setStatus(Status.INDEXED);
                    site.setLastError(null);
                }
                sitesRepository.save(site);
            }
        }

        forkJoinPool.shutdown();
        stopRequested.set(false);

        return new IndexApiResponse(true, null);
    }


    public IndexApiResponse stopIndexing() {
        List<Site> indexingSites = sitesRepository.findByStatus(Status.INDEXING);
        if (indexingSites.isEmpty()) {
            return new IndexApiResponse(false, "Индексация не была запущена.");
        }
        logger.info("Метод stopIndexing вызван в потоке: " + Thread.currentThread().getName());
        stopRequested.set(true);
        for (Site site : indexingSites) {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            sitesRepository.save(site);
        }
        return new IndexApiResponse(true, "Индексация остановлена.");
    }
}