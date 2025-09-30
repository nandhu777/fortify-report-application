package com.fortify.report.provider;

import com.fortify.report.model.ReportArtifacts;          // <- singular DTO
import com.fortify.report.ssc.SscAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;

@Component("ssc-export")
public class SscExportProvider implements ReportProvider {

    private final RestClient http;
    private final SscAuthService auth;
    private final Map<String, String> appToVersionId;
    private final String templateName;
    private final String format;
    private final long pollEveryMs;
    private final long timeoutMs;

    // allow endpoint overrides via config if your SSC differs
    private final String createPath;
    private final String statusPath;
    private final String downloadPath;
    private static final Logger log = LoggerFactory.getLogger(SscExportProvider.class);
    public SscExportProvider(


            @Value("${fortify.ssc.baseUrl}") String baseUrl,
            SscAuthService auth,
            @Qualifier("fortifyApps") Map<String,String> appToVersionId,
            @Value("${fortify.ssc.export.templateName}") String templateName,
            @Value("${fortify.ssc.export.format:PDF}") String format,
            @Value("${fortify.ssc.export.pollIntervalMillis:1500}") long pollEveryMs,
            @Value("${fortify.ssc.export.pollTimeoutSeconds:120}") long pollTimeoutSeconds,
            @Value("${fortify.ssc.export.createPath:/api/v1/reports}") String createPath,
            @Value("${fortify.ssc.export.statusPath:/api/v1/reports/{id}}") String statusPath,
            @Value("${fortify.ssc.export.downloadPath:/api/v1/reports/{id}/download}") String downloadPath
    ) {
        this.auth = auth;
        this.appToVersionId = appToVersionId;
        this.templateName = templateName;
        this.format = format;
        this.pollEveryMs = pollEveryMs;
        this.timeoutMs = Duration.ofSeconds(pollTimeoutSeconds).toMillis();
        this.createPath = createPath;
        this.statusPath = statusPath;
        this.downloadPath = downloadPath;

        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public ReportArtifacts fetchLatest(String applicationName) throws Exception {
        log.info(":::::::::::::::::::::: inside reportArtifacts:::::::::{}",applicationName);
        String versionId = appToVersionId.get(applicationName);
        if (versionId == null) {
            throw new IllegalArgumentException("Unknown application: " + applicationName);
        }
        log.info(":::::::::::::::::::::: inside reportArtifacts:::::::::{}",versionId);
        // 1) Create export job
        Map<String, Object> createBody = Map.of(
                "projectVersionId", versionId,
                "templateName", templateName,
                "format", format
        );

        Map<String, Object> createRes = getJsonWithAuth(
                () -> http.post()
                        .uri(createPath)
                        .header(HttpHeaders.AUTHORIZATION, auth.authHeaderValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(createBody)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {})
        );

        String reportId = str(safeMap(createRes.get("data")).get("id"));
        if (isBlank(reportId)) {
            throw new IllegalStateException("SSC did not return a report id");
        }

        // 2) Poll until ready
        long start = System.currentTimeMillis();
        String status;
        do {
            Thread.sleep(pollEveryMs);

            Map<String, Object> statusRes = getJsonWithAuth(
                    () -> http.get()
                            .uri(statusPath, reportId)
                            .header(HttpHeaders.AUTHORIZATION, auth.authHeaderValue())
                            .retrieve()
                            .body(new ParameterizedTypeReference<Map<String, Object>>() {})
            );

            status = str(safeMap(statusRes.get("data")).get("status"));
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new RuntimeException("Timed out waiting for SSC export to complete");
            }
        } while (!"COMPLETED".equalsIgnoreCase(status) && !"READY".equalsIgnoreCase(status));

        // 3) Download
        byte[] bytes = getBytesWithAuth(
                () -> http.get()
                        .uri(downloadPath, reportId)
                        .header(HttpHeaders.AUTHORIZATION, auth.authHeaderValue())
                        .retrieve()
                        .body(byte[].class)
        );

        String ext = format.equalsIgnoreCase("CSV") ? "csv" : "pdf";
        String ct = format.equalsIgnoreCase("CSV") ? "text/csv" : "application/pdf";
        String filename = applicationName + "-fortify-export." + ext;

        return new ReportArtifacts(filename, bytes, ct);
    }

    /* ---------------- helpers ---------------- */

    private Map<String, Object> getJsonWithAuth(ThrowingSupplier<Map<String, Object>> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            if (e.getRawStatusCode() == 401) { // unauthorized → refresh token once
                auth.invalidate();
                return call.get();
            }
            throw e;
        }
    }

    private byte[] getBytesWithAuth(ThrowingSupplier<byte[]> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            if (e.getRawStatusCode() == 401) {
                auth.invalidate();
                return call.get();
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : Map.of();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}
