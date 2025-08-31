package com.lxc.machine.mapper;

import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.machine.domain.LxcContainerMetricsDaily;
import com.lxc.machine.domain.vo.LxcContainerMetricsDailyVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LxcContainerMetricsDailyMapper extends BaseMapperPlus<LxcContainerMetricsDailyMapper, LxcContainerMetricsDaily, LxcContainerMetricsDailyVo> {
}
