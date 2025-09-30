package com.fortify.report.provider;


import com.fortify.report.model.ReportArtifacts;

public interface ReportProvider {
    ReportArtifacts fetchLatest(String applicationName) throws Exception;
}
