package com.lxc.machine.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lxc.common.constant.CacheNames;
import com.lxc.common.core.service.ConfigService;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.spring.SpringUtils;
import com.lxc.machine.domain.SysConfig4LxcMachine;
import com.lxc.machine.mapper.SysConfigMapper4LxcMachine;
import com.lxc.machine.service.ISysConfigService4LxcMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 参数配置 服务层实现
 *
 * @author lxc
 */
@RequiredArgsConstructor
@Service
public class SysConfigServiceImpl4LxcMachine implements ISysConfigService4LxcMachine, ConfigService {

    private final SysConfigMapper4LxcMachine baseMapper;

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数key
     * @return 参数键值
     */
    @Cacheable(cacheNames = CacheNames.SYS_CONFIG, key = "#configKey")
    @Override
    public String selectConfigByKey(String configKey) {
        SysConfig4LxcMachine retConfig = baseMapper.selectOne(new LambdaQueryWrapper<SysConfig4LxcMachine>()
            .eq(SysConfig4LxcMachine::getConfigKey, configKey));
        if (ObjectUtil.isNotNull(retConfig)) {
            return retConfig.getConfigValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 根据参数 key 获取参数值
     *
     * @param configKey 参数 key
     * @return 参数值
     */
    @Override
    public String getConfigValue(String configKey) {
        return SpringUtils.getAopProxy(this).selectConfigByKey(configKey);
    }

}
