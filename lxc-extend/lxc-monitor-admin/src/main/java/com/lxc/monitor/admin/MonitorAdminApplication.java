package com.lxc.monitor.admin;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Admin 监控启动程序
 *
 * @author Lion Li
 */
@SpringBootApplication
public class MonitorAdminApplication {

    public static void main(String[] args) {
        // 设置时区为上海时区，即东八区
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.SHORT_IDS.get("CTT")));
        SpringApplication.run(MonitorAdminApplication.class,args);
        System.out.println("Admin 监控启动成功");
    }

}
