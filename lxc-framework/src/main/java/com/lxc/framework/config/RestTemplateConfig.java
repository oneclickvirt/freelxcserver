package com.lxc.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

/**
 * RestTemplate配置类
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    /**
     * 忽略Https证书认证RestTemplate
     * @return unSSLRestTemplate
     */
    @Bean("unSSLRestTemplate")
    public RestTemplate unSSLRestTemplate() {
        try{
            RestTemplate restTemplate = new RestTemplate(RestTemplateConfig.generateHttpRequestFactory());
            return restTemplate;
        }catch (Exception e){
            log.error(e.getMessage());
           e.printStackTrace();
           return null;
        }
    }

    /**
     * 通过该工厂类创建的RestTemplate发送请求时，可忽略https证书认证
     * @return 工厂
     */
    private static HttpComponentsClientHttpRequestFactory generateHttpRequestFactory() throws Exception{
        TrustStrategy acceptingTrustStrategy = ((x509Certificates,authType) -> true);
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());

        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setSSLSocketFactory(connectionSocketFactory);
        CloseableHttpClient httpClient = httpClientBuilder.build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        return factory;
    }
}
