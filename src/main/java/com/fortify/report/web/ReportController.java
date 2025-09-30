package com.fortify.report.web;
import com.fortify.report.service.ReportConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;

@Controller
public class ReportController {

    private final ReportConnector connector;
    private final Map<String,String> appMap;
    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    public ReportController(ReportConnector connector, Environment env) {
        this.connector = connector;
        this.appMap = Binder.get(env)
                .bind("fortify.apps", Bindable.mapOf(String.class, String.class))
                .orElseGet(Map::of);   // never null
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("apps", appMap.keySet());
        return "index";
    }

    // debug endpoint
    @GetMapping("/api/apps")
    @ResponseBody
    public Map<String,String> apps() { return appMap; }


    @PostMapping("/send")
    public String send(
            @RequestParam String applicationName,
            @RequestParam(required = false) String recipient,
            Model model) {
        try {
            log.info("::::::::::inside send method::::::::{} {}",recipient,applicationName);
            connector.fetchAndSend(applicationName, recipient);
            model.addAttribute("message", "Report sent for " + applicationName);
            return "sent";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "index";
        }
    }
}
