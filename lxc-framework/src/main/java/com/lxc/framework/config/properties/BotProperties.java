package com.lxc.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "bot", ignoreInvalidFields = true)
public class BotProperties {
    Boolean enabled;
    String token;
    String botUserName;
    String downloadRootFolder;
    String webHookUrl;
    String downloadedRecord;
    String certPath;
}
