package com.fortify.report.config;


import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

@Configuration
public class FortifyConfig {

    /** Expose fortify.apps as a Map bean so all classes can inject it safely. */
    @Bean("fortifyApps")
    public Map<String, String> fortifyApps(Environment env) {
        return Binder.get(env)
                .bind("fortify.apps", Bindable.mapOf(String.class, String.class))
                .orElseGet(Map::of); // never null
    }
}
