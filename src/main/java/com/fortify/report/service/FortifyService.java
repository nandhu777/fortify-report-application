package com.fortify.report.service;

import com.fortify.report.controller.FortifyReportController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FortifyService {

    @Value("${fortify.ssc.baseUrl}")
    private String baseUrl;

    @Value("${fortify.ssc.token}")
    private String token;

    @Value("${fortify.ssc.authHeaderPrefix}")
    private String authHeaderPrefix;
    private static final Logger log = LoggerFactory.getLogger(FortifyService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchIssuesForProject(Long projectVersionId) {
        log.info("::::::::::::: inside fetchIssueForProject::::::::");
        String url = baseUrl + "/projectVersions/" + projectVersionId + "/issues";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeaderPrefix + " " + token);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error fetching issues: " + ex.getMessage();
        }
    }
}
