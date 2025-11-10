package com.lxc.machine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lxc.common.core.domain.dto.ShellDto;
import com.lxc.common.enums.LxcStatus;
import com.lxc.common.utils.DateUtils;
import com.lxc.common.utils.ShellUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.domain.vo.LxcQueueVo;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcQueueService;
import com.lxc.machine.service.ILxcSuperLxcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

@Service
public class LxcAsyncService {
    private static final Logger log = LoggerFactory.getLogger(LxcAsyncService.class);
    private final ILxcQueueService lxcQueueService;
    private final ILxcLxcService lxcService;
    private final ILxcSuperLxcService lxcSuperLxcService;
    // 构造器注入
    public LxcAsyncService(ILxcQueueService lxcQueueService,
                           ILxcLxcService lxcService,
                           ILxcSuperLxcService lxcSuperLxcService) {
        this.lxcQueueService = lxcQueueService;
        this.lxcService = lxcService;
        this.lxcSuperLxcService = lxcSuperLxcService;
    }

    /**
     * 异步冻结用户所有容器（内部串行，外部非阻塞）
     * @param tgId 用户标识（必须）
     * @return CompletableFuture 供可选回调
     */
    @Async("lxcAsyncExecutor") // 指定自定义线程池，异步执行
    public CompletableFuture<Void> freezeUserContainersAsync(String tgId, String queryDate) {
        try {
            log.info("开始异步冻结用户容器，tgId: {}，查询日期: {}", tgId, DateUtils.getDate());
            // 1. 查询用户的所有容器
            List<LxcQueueVo> lxcQueueVos = lxcQueueService.queryAllLxc(tgId, queryDate);
            if (lxcQueueVos.isEmpty()) {
                log.warn("用户无可用容器，无需冻结，tgId: {}", tgId);
                return CompletableFuture.completedFuture(null);
            }
            // 2. 内部串行处理：按顺序冻结每个容器（前一个完成才执行下一个）
            for (LxcQueueVo lxcQueueVo : lxcQueueVos) {
                Long lxcId = lxcQueueVo.getLxcId();
                log.info("开始冻结容器，tgId: {}，lxcId: {}", tgId, lxcId);
                try {
                    // 查LxcLxc信息
                    LxcLxc lxc = lxcService.getOne(new LambdaQueryWrapper<LxcLxc>()
                            .eq(LxcLxc::getId, lxcId));
                    if (lxc == null) {
                        log.warn("容器不存在，跳过冻结，tgId: {}，lxcId: {}", tgId, lxcId);
                        continue;
                    }
                    // 查宿主机信息
                    String childName = lxc.getChildName();
                    String superIp = lxc.getSuperIp();
                    LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new LambdaQueryWrapper<LxcSuperLxc>()
                            .eq(LxcSuperLxc::getIp, superIp));
                    if (superLxc == null) {
                        log.warn("宿主机不存在，跳过冻结，tgId: {}，superIp: {}", tgId, superIp);
                        continue;
                    }
                    // 执行冻结Shell命令（串行执行，确保顺序）
                    String res = ShellUtils.execCmdAndReturnStatus(ShellDto.builder()
                            .ip(superIp)
                            .password(superLxc.getPassword())
                            .way(superLxc.getWay())
                            .modifyType("traffic_limit")
                            .childName(childName)
                            .build());
                    // 更新lxc表的容器状态为冻结，记录当前状态到lastUpdatedStatus
                    lxc.setLastUpdatedStatus(lxc.getLxcStatus());
                    lxc.setLxcStatus(LxcStatus.FROZEN.getCode());
                    lxcService.updateById(lxc);
                    log.info("容器冻结成功，tgId: {}，lxcId: {}，childName: {}，执行结果: {}",
                            tgId, lxcId, childName, res);
                } catch (Exception e) {
                    // 单个容器冻结失败，记录日志，继续冻结下一个（不中断整个任务）
                    log.error("容器冻结失败，tgId: {}，lxcId: {}", tgId, lxcId, e);
                }
            }

            log.info("用户所有容器冻结任务完成，tgId: {}", tgId);
        } catch (Exception e) {
            // 整个冻结任务异常（如查询容器失败），记录日志
            log.error("用户容器冻结任务整体异常，tgId: {}", tgId, e);
            throw new CompletionException(e); // 可选：传递异常给调用方
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletionStage<Object> freezeSuperLxcContainersAsync(Long superId) {
        LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new LambdaQueryWrapper<LxcSuperLxc>().eq(LxcSuperLxc::getId, superId));
        List<LxcLxcVo> lxcList = lxcService.getLxcList(new QueryWrapper<LxcLxc>().eq("super_ip", superLxc.getIp()));
        if (lxcList.isEmpty()) {
            log.warn("母鸡下无小鸡，无需冻结，superId: {}", superId);
            return CompletableFuture.completedFuture(null);
        } else {
            for (LxcLxcVo lxcVo : lxcList) {
                try {
                    String res = ShellUtils.execCmdAndReturnStatus(ShellDto.builder()
                        .ip(superLxc.getIp())
                        .password(superLxc.getPassword())
                        .way(superLxc.getWay())
                        .modifyType("traffic_limit")
                        .childName(lxcVo.getChildName())
                        .build());
                    LxcLxc lxc = new LxcLxc();
                    BeanUtils.copyProperties(lxcVo, lxc);
                    lxc.setLastUpdatedStatus(lxc.getLxcStatus());
                    lxc.setLxcStatus(LxcStatus.FROZEN.getCode());
                    lxcService.updateById(lxc);
                    log.info("容器冻结成功，superId: {}，lxcId: {}，childName: {}，执行结果: {}",
                        superId, lxc.getId(), lxcVo.getChildName(), res);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
