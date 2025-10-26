package com.lxc.machine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.machine.domain.LxcUserMonthlyTraffic;
import com.lxc.machine.domain.bo.LxcContainerStateBo;
import com.lxc.machine.domain.vo.LxcContainerStateVo;

import java.util.List;
import java.util.Map;

public interface ILxcStateService {
    Map<String, List<LxcContainerStateVo>> getRunningInfo(LxcContainerStateBo lxcContainerStateBo);
    Map<String, List<LxcContainerStateVo>> getRunningInfoList();

    Page<LxcUserMonthlyTraffic> getTrafficList(PageQuery pageQuery);
}
