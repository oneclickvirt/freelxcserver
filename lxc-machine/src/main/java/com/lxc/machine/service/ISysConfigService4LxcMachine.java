package com.lxc.machine.service;

/**
 * 参数配置 服务层
 *
 * @author lxc
 */
public interface ISysConfigService4LxcMachine {
    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数键名
     * @return 参数键值
     */
    String selectConfigByKey(String configKey);
}
