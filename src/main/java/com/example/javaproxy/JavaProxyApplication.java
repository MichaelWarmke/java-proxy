
package com.example.javaproxy;

import com.example.javaproxy.config.ProxyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProxyProperties.class)
public class JavaProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaProxyApplication.class, args);
    }
}
