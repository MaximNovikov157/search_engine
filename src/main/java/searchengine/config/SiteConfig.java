package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteConfig {
    private String url;
    private String name;

    public SiteConfig() {}

    public SiteConfig(String url, String name) {
        this.url = url;
        this.name = name;
    }
}