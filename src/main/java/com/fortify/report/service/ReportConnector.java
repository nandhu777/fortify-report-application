package com.fortify.report.service;


import com.fortify.report.model.ReportArtifacts;
import com.fortify.report.provider.ReportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReportConnector {

    private final ReportProvider provider;
    private final MailService mail;
    private static final Logger log = LoggerFactory.getLogger(ReportConnector.class);

    public ReportConnector(@Value("${fortify.provider:ssc-export}") String providerName,
                           @Qualifier("ssc-export") ReportProvider sscExportProvider,
                           MailService mail) {
        // for now we only have ssc-export
        this.provider = sscExportProvider;
        this.mail = mail;
    }

    /** Older flow some pages may use */
    public String process(String appName, String toEmail) throws Exception {
        ReportArtifacts artifact = provider.fetchLatest(appName.trim());
        mail.sendReport(toEmail, appName, artifact);
        return artifact.fileName();
    }

    /** The method your controller calls */
    public void fetchAndSend(String applicationName, String recipient) throws Exception {
        log.info("::::::::::::::::::: inside fetchandsend:::::::::::{} {}",applicationName,recipient);
        ReportArtifacts report = provider.fetchLatest(applicationName);
        mail.sendReport(recipient, applicationName, report);
    }
}
