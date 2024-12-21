package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "indexing-settings")
@EnableTransactionManagement
public class AppConfig {
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36";
    private String referrer = "http://www.google.com";
    private SitesList sites;
}