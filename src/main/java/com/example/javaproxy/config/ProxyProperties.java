
package com.example.javaproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "proxy")
public class ProxyProperties {

    private String targetUrl;

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}
