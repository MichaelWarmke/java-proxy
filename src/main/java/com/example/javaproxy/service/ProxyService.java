
package com.example.javaproxy.service;

import com.example.javaproxy.config.ProxyProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

@Service
public class ProxyService {

    private final ProxyProperties proxyProperties;
    private final CloseableHttpClient httpClient;

    public ProxyService(ProxyProperties proxyProperties) {
        this.proxyProperties = proxyProperties;
        this.httpClient = HttpClients.createDefault();
    }

    public ResponseEntity<String> proxy(HttpServletRequest request) throws URISyntaxException, IOException {
        URI uri = new URI(proxyProperties.getTargetUrl() + request.getRequestURI());

        HttpUriRequest httpUriRequest = new org.apache.hc.client5.http.classic.methods.HttpUriRequestBase(request.getMethod(), uri);

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            httpUriRequest.addHeader(headerName, request.getHeader(headerName));
        }

        return httpClient.execute(httpUriRequest, response -> {
            HttpHeaders responseHeaders = new HttpHeaders();
            for (org.apache.hc.core5.http.Header header : response.getHeaders()) {
                responseHeaders.add(header.getName(), header.getValue());
            }

            String responseBody = EntityUtils.toString(response.getEntity());

            return ResponseEntity.status(response.getCode())
                    .headers(responseHeaders)
                    .body(responseBody);
        });
    }
}
