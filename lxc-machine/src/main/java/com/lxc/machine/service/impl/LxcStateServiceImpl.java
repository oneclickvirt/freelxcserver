package com.lxc.machine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.constant.CacheNames;
import com.lxc.common.constant.LxcConstants;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.enums.LxcStatus;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.machine.domain.LxcContainerState;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.LxcUserMonthlyTraffic;
import com.lxc.machine.domain.bo.LxcContainerStateBo;
import com.lxc.machine.domain.vo.LxcContainerStateVo;
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.mapper.LxcUserMonthlyTrafficMapper;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcStateService;
import com.lxc.machine.service.ILxcSuperLxcService;
import com.lxc.machine.utils.CommonUtils;
import com.lxc.machine.utils.InstancesUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LxcStateServiceImpl implements ILxcStateService {
    @Autowired
    private ILxcSuperLxcService superLxcService;
    @Autowired
    private ILxcLxcService lxcService;
    @Autowired
    private LxcUserMonthlyTrafficMapper lxcUserMonthlyTrafficMapper;

    // 最近一次采样时间记录
    private Map<String, Map<String, Long>> lastSampleTimes = new ConcurrentHashMap<>();

    // 指标变化阈值（超过此值才认为需要更新）
    private static final Map<String, Double> METRIC_CHANGE_THRESHOLDS = new HashMap<>();
    static {
        METRIC_CHANGE_THRESHOLDS.put("cpuUsage", 1.0);     // CPU变化超过1%才更新
        METRIC_CHANGE_THRESHOLDS.put("memUsage", 1048576.0);     // 内存变化超过1MB才更新
        METRIC_CHANGE_THRESHOLDS.put("network", 10240.0);  // 网络变化超过10KB才更新
        METRIC_CHANGE_THRESHOLDS.put("disk", 1048576.0);   // 磁盘变化超过1MB才更新
    }

    /**
     * 判断指标变化是否足够大，需要传输更新
     */
    public static boolean needUpdate(LxcContainerState metric, String metricType, double oldValue) {
        Double newValue = 0.0;
        switch (metricType) {
            case "cpuUsage":
                newValue = metric.getCpu();
                break;
            case "memUsage":
                newValue = metric.getMemory();
                break;
            case "disk":
                newValue = metric.getDisk();
                break;
            case "network":
                newValue = metric.getNetwork();
                break;
            default:
                break;
        }
        // 数值类指标根据阈值判断
        Double threshold = METRIC_CHANGE_THRESHOLDS.get(metricType);
        if (threshold == null) {
            return true;  // 未知指标默认需要更新
        }
        return Math.abs(newValue - oldValue) >= threshold;
    }

    /**
     * 获取容器运行状态信息，更新每个用户流量使用情况（这个要异步，不然用户或容器数量多时，管理员查看到的信息）
     * todo
     * 按照lxcContainerStateBo提供的tgId、containerId、instancename查询，减轻返回数据的压力，每次只查询一页的数据
     *
     * @return
     */
    @Override
    public Map<String, List<LxcContainerStateVo>> getRunningInfo(LxcContainerStateBo lxcContainerStateBo) {
        List<LxcSuperLxc> updateLxcSuperLxc = superLxcService.list(new QueryWrapper<LxcSuperLxc>().eq("ip", lxcContainerStateBo.getSuperIp()));
        Map<String, List<LxcContainerStateVo>> map = new HashMap<>();
        if (!updateLxcSuperLxc.isEmpty()) {
            map = getRunningInfoForSuperLxcList(updateLxcSuperLxc);
        }
        return map;
    }

    /**
     * 返回所有母鸡下所有运行中的小鸡的状态信息，具体怎么用由前端匹配，不需要绑定tgid，其实跟谁领取到小鸡没有关系，如果指标出现异常，再考虑在前端加一个跳转链接到“小鸡列表”，点击后填充小鸡的name
     *
     * @return
     */
    @Override
    public Map<String, List<LxcContainerStateVo>> getRunningInfoList() {
        List<LxcSuperLxc> aliveLxcSuperLxcList = superLxcService.getAliveLxcSuperLxcList();
        Map<String, List<LxcContainerStateVo>> map = getRunningInfoForSuperLxcList(aliveLxcSuperLxcList);
        return map;
    }

    @NotNull
    private Map<String, List<LxcContainerStateVo>> getRunningInfoForSuperLxcList(List<LxcSuperLxc> aliveLxcSuperLxcList) {
        Map<String, List<LxcContainerStateVo>> map = new HashMap<>();
        // 1、查询所有可开小鸡的母鸡
        for (LxcSuperLxc superLxc : aliveLxcSuperLxcList) {
            List<LxcContainerStateVo> superLxcContainerStateVoList = new ArrayList<>();
            String superIp = superLxc.getIp();
            String memoryLimit = superLxc.getMemoryLimit(); // 当前宿主机的内存限制(数据库默认单位是MB)
            String diskLimit = superLxc.getDiskLimit(); // 当前宿主机的硬盘限制(数据库默认单位是GB)
            String trafficTotalLimit = superLxc.getTrafficTotalLimit(); // 当前宿主机的流量使用限制(单位由前端传入)
            String memoryLimitNum = CommonUtils.extractNumber(memoryLimit);
            String memoryLimitUnit = CommonUtils.extractUnit(memoryLimit);
            String diskLimitNum = CommonUtils.extractNumber(diskLimit);
            String diskLimitUnit = CommonUtils.extractUnit(diskLimit);
            // 2、查询该母鸡下的所有运行中(3)的小鸡
            List<LxcLxcVo> lxcList = lxcService.getLxcList(new QueryWrapper<LxcLxc>().eq("super_ip", superIp).eq("lxc_status", LxcStatus.RUNNING.getCode()));
            for (LxcLxcVo lxcVo : lxcList) {
                // 3、获取单个容器的状态
                Long containerId = lxcVo.getId();
                String childName = lxcVo.getChildName();
                LxcContainerState instancesByNameState = InstancesUtils.target(superIp, LxcConstants.LXD_PORT).getInstancesByNameState(childName);
                // 4、流量情况要更新到redis【mysql的更新会通过后台任务更新】
                if (instancesByNameState != null) {
//                    System.out.println("instancesByNameState:" + instancesByNameState);
                    // 3、将获取到的信息推送到前端 其中流量部分更新到数据库，用于每日统计以及每月统计
                    // cpu使用率计算方法： 单位时间内cpu使用的时间，容器信息记录的cpuUsage是创建以来cpu使用的纳秒数， (当前纳秒数-上一次记录的纳秒数)/间隔的记录时间
                    Double cpu = instancesByNameState.getCpu();
                    // 流量不在这里回显到前端，但需要进行更新
                    // recv和send的流量单位是字节，达到一定数量级会使用科学计数法，因此会被识别出Double类型，作为network存储时，先转换成long类型
                    BigDecimal recv = new BigDecimal(instancesByNameState.getRecv());
                    BigDecimal send = new BigDecimal(instancesByNameState.getSend());
                    BigDecimal network = recv.add(send);
                    // needUpdate的oldNetwork是上一次记录的network，单位是字节
                    BigDecimal oldNetworkObj = RedisUtils.getCacheObject(String.format(CacheNames.LXC_NETWORK_USAGE, containerId));
                    Double oldNetwork = 0.0;
                    if (null != oldNetworkObj) {
                        oldNetwork = oldNetworkObj.doubleValue();
                    }
                    // ⭐流量更新到redis
                    if (needUpdate(instancesByNameState, "network", oldNetwork)) {
                        updateTraffic2Redis(containerId, network, recv, send);
                    }
                    // 计算cpu使用率
                    // ⭐⭐⭐容器初次采样，数据库里还没有上一次的记录，要分类讨论，默认取当前采样到的值，前端显示为0%
                    Double cpuUsage = handleCpuUsage(lxcVo, instancesByNameState, cpu);
                    LxcContainerStateVo lxcContainerStateVo = new LxcContainerStateVo();
                    BeanUtils.copyProperties(instancesByNameState, lxcContainerStateVo);
                    lxcContainerStateVo.setCpuUsage(cpuUsage);
                    // 内存和硬盘限制要根据单位进行转换，默认单位是MB和GB
                    lxcContainerStateVo.setMemLimit(CommonUtils.unitUnifyToBytes(memoryLimitNum, memoryLimitUnit).doubleValue());
                    lxcContainerStateVo.setDiskLimit(CommonUtils.unitUnifyToBytes(diskLimitNum, diskLimitUnit).doubleValue());
                    superLxcContainerStateVoList.add(lxcContainerStateVo);
                    map.put(superIp, superLxcContainerStateVoList);
                } else {
                    // Running状态下的容器获取不到状态
                    log.info("Running状态下的容器获取不到状态，superIp：{}，childName：{}",superIp,childName);
                    throw new RuntimeException("Running状态下的容器获取不到状态，superIp：" + superIp + "，childName：" + childName);
                }
                map.forEach((k,v) -> {
                    System.out.println(k + "：" + v);
                });
            }
        }
        return map;
    }

    private static Double handleCpuUsage(LxcLxcVo lxcVo, LxcContainerState instancesByNameState, Double cpu) {
        Double cpuUsage = 0.0;
        String keyOfRunningState = String.format(CacheNames.LXC_RUNNING_STATE, lxcVo.getId());
        LxcContainerState containerState = new LxcContainerState();
        if (RedisUtils.hasKey(keyOfRunningState)) {
            // 如果redis之前有存cpu使用的纳秒值，那么就可以通过两次的间隔和timestamp之差来求得cpu使用率
            LxcContainerState oldContainerState = RedisUtils.getCacheObject(keyOfRunningState);
//            System.out.println("oldContainerState:" + oldContainerState);
            Double oldCpuUsage = 0.0;
            Long oldTimestamp = 0L;
            if (null != oldContainerState) {
                oldCpuUsage = oldContainerState.getCpu();
                oldTimestamp = oldContainerState.getTimestamp();
            }
            cpuUsage = (new BigDecimal(cpu).subtract(new BigDecimal(oldCpuUsage))).divide(new BigDecimal(instancesByNameState.getTimestamp() - oldTimestamp), 4, RoundingMode.HALF_UP).doubleValue();
        }
        // 将最新采样数据添加到缓存列表中，超时时间设置为一周
        RedisUtils.setCacheObject(keyOfRunningState, instancesByNameState, Duration.ofDays(7L));
        return cpuUsage;
    }

    @Override
    public Page<LxcUserMonthlyTraffic> getTrafficList(PageQuery pageQuery) {
        // 默认表中每个tgId只有一条流量使用情况，每次小时级统计都会更新表中所有的数据，按页查询
        Page<LxcUserMonthlyTraffic> trafficPage = lxcUserMonthlyTrafficMapper.selectPage(pageQuery.build(), null);
        return trafficPage;
    }

    /**
     * 流量更新到redis
     *
     * @param containerId
     * @param network
     * @param recv
     * @param send
     */
    private void updateTraffic2Redis(Long containerId, BigDecimal network, BigDecimal recv, BigDecimal send) {
        String keyOfNetUsage = String.format(CacheNames.LXC_NETWORK_USAGE, containerId);
        String keyOfNetUsageDelta = String.format(CacheNames.LXC_NETWORK_USAGE_DELTA, containerId);
        String keyOfNetRecv = String.format(CacheNames.LXC_NETWORK_RECV, containerId);
        String keyOfNetRecvDelta = String.format(CacheNames.LXC_NETWORK_RECV_DELTA, containerId);
        String keyOfNetSend = String.format(CacheNames.LXC_NETWORK_SEND, containerId);
        String keyOfNetSendDelta = String.format(CacheNames.LXC_NETWORK_SEND_DELTA, containerId);
        BigDecimal networkDelta = new BigDecimal(0);
        if (RedisUtils.hasKey(keyOfNetUsage)) {
            // 如果redis之前有存流量使用的纳秒值，那么就可以通过两次的间隔和timestamp之差来求得流量使用量
            networkDelta = network.subtract(RedisUtils.getCacheObject(keyOfNetUsage));
        }
        BigDecimal netRecvDelta = new BigDecimal(0);
        if (RedisUtils.hasKey(keyOfNetRecv)) {
            netRecvDelta = recv.subtract(RedisUtils.getCacheObject(keyOfNetRecv));
        }
        BigDecimal netSendDelta = new BigDecimal(0);
        if (RedisUtils.hasKey(keyOfNetSend)) {
            netSendDelta = send.subtract(RedisUtils.getCacheObject(keyOfNetSend));
        }
        // 流量的上次采样值更新到redis，过期时间设置为7天；如果出故障了，留出一周时间来关闭项目，否则丢失上一次采样流量数据，会导致下一次流量采样出现断层偏差【因为只是没有采样监控，并不是容器没在使用】
        RedisUtils.setCacheObject(keyOfNetUsage, network, Duration.ofDays(7L));
        RedisUtils.setCacheObject(keyOfNetRecv, recv, Duration.ofDays(7L));
        RedisUtils.setCacheObject(keyOfNetSend, send, Duration.ofDays(7L));
        // 增量值不设过期时间，后台任务每小时会清理
        List<String> addNetDeltaList = new ArrayList<>();
        List<String> addNetRecvDeltaList = new ArrayList<>();
        List<String> addNetSendDeltaList = new ArrayList<>();
        // BigDecimal不是任何一种redis中的基本类型，转换成字符串存储，不会丢失精度
        addNetDeltaList.add(networkDelta.toString());
        RedisUtils.setCacheList(keyOfNetUsageDelta, addNetDeltaList);
        addNetRecvDeltaList.add(netRecvDelta.toString());
        RedisUtils.setCacheList(keyOfNetRecvDelta, addNetRecvDeltaList);
        addNetSendDeltaList.add(netSendDelta.toString());
        RedisUtils.setCacheList(keyOfNetSendDelta, addNetSendDeltaList);
    }
}
