package com.fortify.report.controller;
import com.fortify.report.config.FortifyConfig;
import com.fortify.report.service.FortifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/fortify")
public class FortifyReportController {
    private static final Logger log = LoggerFactory.getLogger(FortifyReportController.class);
    private final FortifyService fortifyService;
    private final FortifyConfig props;
    public FortifyReportController(FortifyService fortifyService,FortifyConfig props) {
        this.props=props;
        this.fortifyService=fortifyService;
    }



    @GetMapping
    public String index(Model model) {
        model.addAttribute("apps", props.getApps()); // a Map<String,String>
        return "index";
    }


    @GetMapping("/issues/{projectVersionId}")
    public ResponseEntity<String> getFortifyIssues(@PathVariable Long projectVersionId) {
        log.info("::::::::::: INSIDE getFortifyIssues:::::::::::{}",projectVersionId);
        String result = fortifyService.fetchIssuesForProject(projectVersionId);
        return ResponseEntity.ok(result);
    }
}
