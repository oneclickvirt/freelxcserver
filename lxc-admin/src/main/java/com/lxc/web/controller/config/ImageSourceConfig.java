package com.lxc.web.controller.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "imagejsonurl")
public class ImageSourceConfig {
    private String lxcUrl;
    private String incusUrl;
}
