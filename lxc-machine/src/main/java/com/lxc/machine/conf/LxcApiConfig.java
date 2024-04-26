package com.lxc.machine.conf;

import org.relxd.lxd.RelxdApiClient;
import org.relxd.lxd.api.InstancesApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LxcApiConfig {
    @Bean
    public InstancesApi lxcApi() {
        return new InstancesApi(new RelxdApiClient());
    }
}
