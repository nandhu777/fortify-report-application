package com.fortify.report.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "fortify")
public class FortifyConfig {

    private Map<String, String> apps = new LinkedHashMap<>();

    public Map<String, String> getApps() {
        return apps;
    }

    public void setApps(Map<String, String> apps) {
        this.apps = apps;
    }
}
