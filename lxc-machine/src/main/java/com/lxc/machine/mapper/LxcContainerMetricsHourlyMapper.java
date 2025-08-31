package com.lxc.machine.mapper;

import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.machine.domain.LxcContainerMetricsHourly;
import com.lxc.machine.domain.vo.LxcContainerMetricsHourlyVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LxcContainerMetricsHourlyMapper extends BaseMapperPlus<LxcContainerMetricsHourlyMapper, LxcContainerMetricsHourly, LxcContainerMetricsHourlyVo> {
}
