package com.lxc.web.controller.monitor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.R;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.machine.domain.LxcUserMonthlyTraffic;
import com.lxc.machine.domain.bo.LxcContainerStateBo;
import com.lxc.machine.domain.vo.LxcContainerStateVo;
import com.lxc.machine.service.ILxcStateService;
import com.lxc.machine.service.ILxcSuperLxcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 该类用于监控lxc容器状态
 * 前端用轮询请求该控制层的方法
 *
 * @author lxc
 * @date 2025-07-26
 */
@RestController
@RequestMapping("/lxc/state")
@Slf4j
public class LxcStateController {
    @Autowired
    private ILxcSuperLxcService superLxcService;
    @Autowired
    private ILxcStateService stateService;
// todo 先写一版，后面再优化
    /**
     * 每5s轮询获取容器运行状态信息，前端展示部分返回，流量信息更新数据库
     * @return
     */
    @GetMapping("/instanceInfo")
    public R<LxcContainerStateVo> getInfo(LxcContainerStateBo lxcContainerStateBo) {
        LxcContainerStateVo lxcContainerStateVo = stateService.getRunningInfo(lxcContainerStateBo);
        return R.ok(lxcContainerStateVo);
    }

    @GetMapping("/instanceInfoList")
    public R<Map<String, List<LxcContainerStateVo>>> getInfoList() {
        Map<String, List<LxcContainerStateVo>> instanceInfoList = stateService.getRunningInfoList();
        return R.ok(instanceInfoList);
    }

    @GetMapping("/traffic")
    public R<TableDataInfo<LxcUserMonthlyTraffic>> getTraffic(PageQuery pageQuery) {
        Page<LxcUserMonthlyTraffic> page = stateService.getTrafficList(pageQuery);
        TableDataInfo<LxcUserMonthlyTraffic> trafficList = TableDataInfo.build(page);
        return R.ok(trafficList);
    }
}
