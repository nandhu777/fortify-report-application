package com.fortify.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class FortifyReportService {

	public static void main(String[] args) {
		SpringApplication.run(FortifyReportService.class, args);
	}

}
