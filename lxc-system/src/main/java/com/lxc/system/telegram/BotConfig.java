package com.lxc.system.telegram;

import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * @author Administrator
 * @version V1.0
 * @date 2022/4/9
 */
@Configuration
public class BotConfig {

	// 梯子的IP，我的是本地的
//    public static final String proxyHost = "127.0.0.1";
//    public static final String proxyHost = "47.245.40.12";
    // 本地监听的端口
//    public static final int proxyPort = 10801;
//    public static final int proxyPort = 28331;
//    public static final int proxyPort = 19599;
//    public static final int proxyPort = 27749;

    @Bean
    @ConditionalOnProperty(name = "bot.enabled", havingValue = "true")
    public DefaultBotOptions defaultBotOptions() {
//        DefaultBotOptions botOptions = new DefaultBotOptions();
//        botOptions.setProxyHost(proxyHost);
//        botOptions.setProxyPort(proxyPort);
        //ProxyType是个枚举
//        botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
        return new DefaultBotOptions();
//        return botOptions;
    }

    @Bean
    @ConditionalOnProperty(name = "bot.enabled", havingValue = "true")
    public DefaultBotSession DefaultBotSession() {
        DefaultBotSession defaultBotSession = new DefaultBotSession();
        defaultBotSession.setOptions(defaultBotOptions());
        return defaultBotSession;
    }

    @SneakyThrows
    @Bean
    @ConditionalOnProperty(name = "bot.enabled", havingValue = "true")
    public TelegramBotsApi telegramBotsApi() {
        return new TelegramBotsApi(DefaultBotSession().getClass());
    }
}

