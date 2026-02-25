
package com.example.javaproxy.controller;

import com.example.javaproxy.aspect.RecordWiremock;
import com.example.javaproxy.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
public class ProxyController {

    private final ProxyService proxyService;

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @RecordWiremock
    @RequestMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest request) throws URISyntaxException, IOException {
        return proxyService.proxy(request);
    }
}
