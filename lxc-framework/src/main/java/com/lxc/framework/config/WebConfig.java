package com.lxc.framework.config;

import com.lxc.common.filter.HttpMethodFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<HttpMethodFilter> httpMethodFilterRegistration() {
        FilterRegistrationBean<HttpMethodFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpMethodFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
}
