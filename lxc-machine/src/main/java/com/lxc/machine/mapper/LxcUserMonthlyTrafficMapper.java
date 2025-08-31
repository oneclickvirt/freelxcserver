package com.lxc.machine.mapper;

import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.machine.domain.LxcUserMonthlyTraffic;
import com.lxc.machine.domain.vo.LxcUserMonthlyTrafficVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LxcUserMonthlyTrafficMapper extends BaseMapperPlus<LxcUserMonthlyTrafficMapper, LxcUserMonthlyTraffic, LxcUserMonthlyTrafficVo> {
}
