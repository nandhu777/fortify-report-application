package com.fortify.report.service;


import com.fortify.report.model.ReportArtifacts;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private final JavaMailSender sender;
    private final String defaultTo;
    private final String from;

    public MailService(JavaMailSender sender,
                       @Value("${mail.defaultTo}") String defaultTo,
                       @Value("${mail.from}") String from) {
        this.sender = sender;
        this.defaultTo = defaultTo;
        this.from = from;
    }

    /** recipient may be null/blank -> falls back to defaultTo */
    public void sendReport(String recipient, String appName, ReportArtifacts artifact) throws Exception {
        String to = (recipient == null || recipient.isBlank()) ? defaultTo : recipient;

        MimeMessage msg = sender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true);
        h.setFrom(from);
        h.setTo(to);
        h.setSubject("Fortify report – " + appName);
        h.setText("Attached is the latest Fortify report for " + appName + ".", false);
        h.addAttachment(artifact.fileName(), new ByteArrayResource(artifact.bytes()), artifact.contentType());
        sender.send(msg);
    }
}
