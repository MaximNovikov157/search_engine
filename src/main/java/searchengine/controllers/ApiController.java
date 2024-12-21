package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.index.IndexApiResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.PageIndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final PageIndexingService pageIndexingService;
    private final SearchService searchService;

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexApiResponse> indexPage(@RequestParam String url) {
        IndexApiResponse response = pageIndexingService.indexPage(url);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexApiResponse> startIndexing() {
        IndexApiResponse response = pageIndexingService.startIndexing();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexApiResponse> stopIndexing() {
        IndexApiResponse response = pageIndexingService.stopIndexing();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping(value = "/search")
    @ResponseStatus(HttpStatus.OK)
    public SearchResponse search(@RequestParam String query, @RequestParam(required = false) String site,
                                 @RequestParam(required = false, defaultValue = "0") Integer offset,
                                 @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return searchService.search(query, site, offset, limit);
    }

}