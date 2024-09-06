package com.lxc.system.runner;

import com.lxc.common.constant.CacheNames;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.framework.config.LxcConfig;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcQueueService;
import com.lxc.machine.service.ILxcSuperLxcService;
import com.lxc.system.service.ISysConfigService;
import com.lxc.system.service.ISysDictTypeService;
import com.lxc.system.service.ISysOssConfigService;
import com.lxc.system.telegram.LxcBot;
import com.lxc.system.telegram.LxcHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;


/**
 * 初始化 system 模块对应业务数据
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SystemApplicationRunner implements ApplicationRunner {

    private final LxcConfig lxcConfig;
    private final ISysConfigService configService;
    private final ISysDictTypeService dictTypeService;
    private final ISysOssConfigService ossConfigService;
    private final ILxcLxcService lxcLxcService;
    private final ILxcSuperLxcService lxcSuperLxcService;
    private final ILxcQueueService lxcQueueService;

    @Autowired(required = false)
    TelegramBotsApi telegramBotsApi;
    @Autowired(required = false)
    DefaultBotOptions defaultBotOptions;

    private final LxcHandler lxcHandler;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ossConfigService.init();
        log.info("初始化OSS配置成功");
        if (defaultBotOptions != null) {
            // TODO SOCKS5是现加的
//            defaultBotOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
            LxcBot lxcBot = new LxcBot(defaultBotOptions,lxcHandler);
            telegramBotsApi.registerBot(lxcBot);
            log.info("telegram bot 启动成功");
        } else {
            log.warn("telegram bot 注入已关闭");
        }
        lxcSuperLxcService.loadingStockCache();
        RedisUtils.deleteKeys(CacheNames.FREE_LXC_LIST + "*");
        RedisUtils.deleteKeys(CacheNames.STATISTICS_LXC_LIST);
        if (lxcConfig.isCacheLazy()) {
            return;
        }
        configService.loadingConfigCache();
        log.info("加载参数缓存数据成功");
        dictTypeService.loadingDictCache();
        log.info("加载字典缓存数据成功");

    }

}
