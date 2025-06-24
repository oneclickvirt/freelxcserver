package com.lxc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * 启动程序
 *
 * @author lxc
 */

@ConfigurationPropertiesScan
@SpringBootApplication
public class LxcApplication {

    public static void main(String[] args) {
        // 设置时区为上海时区，即东八区
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.SHORT_IDS.get("CTT")));
        System.setProperty("spring.devtools.restart.enabled","false");
        System.setProperty("https.protocols", "TLSv1.3,TLSv1.2");
        SpringApplication application = new SpringApplication(LxcApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  Lxc-Vue-Plus启动成功   ლ(´ڡ`ლ)ﾞ");
    }

    @Bean
    @Lazy
    RestTemplate restTemplate(){
        return new RestTemplate();
    }


}
