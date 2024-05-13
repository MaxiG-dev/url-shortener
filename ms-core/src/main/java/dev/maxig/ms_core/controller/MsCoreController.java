package dev.maxig.ms_core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class MsCoreController {

    private final RestTemplate restTemplate;

    public MsCoreController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/api/v1/health")
    @ResponseBody
    public String health() {
        return "Service available!";
    }

    @GetMapping("/api/v1/{shortUrl}")
    @ResponseBody
    public String get(@PathVariable String shortUrl) {
        String url = "http://localhost:8081/api/v1/" + shortUrl;
        String longUrl = restTemplate.getForObject(url, String.class);
        return longUrl;
    }


    @GetMapping("/{shortUrl}")
    public RedirectView redirect(@PathVariable String shortUrl) {
        String url = "http://localhost:8081/api/v1/" + shortUrl;
        String longUrl = restTemplate.getForObject(url, String.class);
        return new RedirectView(longUrl);
    }
}
