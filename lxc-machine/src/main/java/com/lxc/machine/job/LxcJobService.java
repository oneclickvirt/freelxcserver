package com.lxc.machine.job;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lxc.common.constant.CacheNames;
import com.lxc.common.constant.LxcConstants;
import com.lxc.common.core.domain.dto.ShellDto;
import com.lxc.common.enums.LxcStatus;
import com.lxc.common.utils.BeanCopyUtils;
import com.lxc.common.utils.DateUtils;
import com.lxc.common.utils.ShellUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.machine.domain.*;
import com.lxc.machine.domain.bo.LxcLxcBo;
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.mapper.LxcQueueMapper;
import com.lxc.machine.mapper.TrafficApiSampleMapper;
import com.lxc.machine.mapper.TrafficStatsByTimeMapper;
import com.lxc.machine.mapper.UserContainerTrafficUsedMapper;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcQueueService;
import com.lxc.machine.service.ILxcSuperLxcService;
import com.lxc.machine.service.impl.LxcQueueServiceImpl;
import com.lxc.machine.utils.CommonUtils;
import com.lxc.machine.utils.InstancesUtils;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.relxd.lxd.ApiException;
import org.relxd.lxd.api.InstancesApi;
import org.relxd.lxd.model.BackgroundOperationResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.lxc.common.utils.ShellUtils.NET_INCUS_USED;
import static com.lxc.common.utils.ShellUtils.NET_USED;

@RequiredArgsConstructor
@Service
@Slf4j
public class LxcJobService {
    private final InstancesApi lxcApi;
    private final ILxcLxcService lxcService;
    private final ILxcQueueService lxcQueueService;
    private final LxcQueueMapper lxcQueueMapper;
    private final ILxcSuperLxcService lxcSuperLxcService;
    private final TrafficApiSampleMapper trafficApiSampleMapper;
    private final TrafficStatsByTimeMapper trafficStatsByTimeMapper;
    private final UserContainerTrafficUsedMapper userContainerTrafficUsedMapper;
    /**
     * 流转数据库内容器状态，执行modify.sh脚本
     */
    @XxlJob("initLxc")
    public void initLxc() {
        List<LxcLxcVo> lxcWaitngList = lxcService.getLxcList(
            new QueryWrapper<LxcLxc>().eq("lxc_status", LxcStatus.WAITING_FOR_PROCESS.getCode()).or()   // 1
                .eq("lxc_status", LxcStatus.RESETING.getCode())     // 0
                .orderBy(true, true, "create_time")
        );
        lxcWaitngList.stream()
            .collect(Collectors.groupingBy(LxcLxcVo::getSuperIp))
            .forEach(
                (superIp, lxcVoList) -> {
                    // 查询创建中(2)和创建失败(6)的小鸡，对不同的母鸡分开计算
                    long pendingCount = lxcService.count(
                        new QueryWrapper<LxcLxc>()
                            .eq("super_ip", superIp)
                            .and(i -> i.eq("lxc_status", LxcStatus.PENDING.getCode()).or()  // 2
                                .eq("lxc_status", LxcStatus.FAILED_TO_CREATE.getCode()))
                    );
                    log.info("宿主机{}的创建中(包含创建中和创建失败的)的小鸡有{}个", superIp, pendingCount);
                    // 一次只能创建一个小鸡
                    if (pendingCount == 0) {
                        int size = lxcVoList.size();
                        int index = -1;
                        for (int i = 0; i < size; i++) {
                            LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new QueryWrapper<LxcSuperLxc>().eq("ip", superIp).orderBy(true, true, "create_time").last("limit 1"));
                            String way = superLxc.getWay();
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + way);
                            String lxcPort = LxcConstants.LXD_PORT;
                            try {
                                //获取实例的运行时状态。如果获取不到信息，会报异常，进入catch
                                BackgroundOperationResponse instancesByNameState = lxcApi.of(superIp, lxcPort).getInstancesByNameState(lxcVoList.get(i).getChildName(), null, null);
//                                System.out.println("当前小鸡 " + lxcVoList.get(i).getChildName() + " 的状态：" + instancesByNameState);
                                log.info("等待创建的小鸡{}，它的信息为{}", lxcVoList.get(i).getChildName(), instancesByNameState);
                                index = i;
                                break;
                            } catch (ApiException e) {
                                XxlJobHelper.log("小鸡{}还在初始化中，检查下一个", lxcVoList.get(0).getChildName());
                                log.info("小鸡{}还在初始化中，检查下一个", lxcVoList.get(0).getChildName());
                            }
                        }
                        if (index == -1) {
                            return;
                        }
                        LxcLxcVo lxc = lxcVoList.get(index);
                        try {
                            XxlJobHelper.log("开始初始化小鸡：{}", lxc.getChildName());
                            log.info("开始初始化小鸡：{}", lxc.getChildName());
                            LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new QueryWrapper<LxcSuperLxc>().eq("ip", superIp));
                            assert Objects.nonNull(superLxc);
                            LxcSuperLxcVo superLxcVo = BeanCopyUtils.copy(superLxc, LxcSuperLxcVo.class);
                            ShellDto shellDto = LxcQueueServiceImpl.prepareCreateShellDto(superLxcVo, lxc);
                            XxlJobHelper.log("小鸡创建信息：{}", shellDto);
                            log.info("小鸡创建信息：{}", shellDto);
                            // 这里在linux服务器上调用modify.sh来初始化小鸡状态
                            // ⭐⭐⭐这里是执行modify.sh生成 container 和 container_init.log 两个文件的关键步骤，只有 container 文件生成了，checkLxcPending才会将容器的密码(脚本输出结果被保存在container 文件中)同步回来数据库

                            // todo 这里执行modify.sh，将modify.sh中所有命令拆开执行
                            /*ShellUtils.Shell shell = ShellUtils.initLxc(shellDto);
                            XxlJobHelper.log("小鸡{}初始化命令：{}", lxc.getChildName(), shell.getCmd());
                            log.info("小鸡{}初始化命令：{}", lxc.getChildName(), shell.getCmd());*/

                            InstancesUtils.target(shellDto.getIp(), LxcConstants.LXD_PORT).execModifySh(shellDto, superLxcVo.getWay());

                            LxcLxcBo temp = new LxcLxcBo();
                            temp.setLastUpdatedStatus(lxc.getLxcStatus());
                            temp.setLxcStatus(LxcStatus.PENDING.getCode()); // 2
                            temp.setId(lxc.getId());
                            lxcService.updateByBoForce(temp);
                        } catch (Exception e) {
                            LxcLxcBo temp = new LxcLxcBo();
                            temp.setLastUpdatedStatus(lxc.getLxcStatus());
                            temp.setLxcStatus(LxcStatus.FAILED_TO_CREATE.getCode());
                            temp.setId(lxc.getId());
                            lxcService.updateByBoForce(temp);
                            XxlJobHelper.log(e);
                        }
                    }
                }
            );
    }

    /**
     * 定时提交未创建的小鸡
     * 已被initLxc取代
     */
//    @XxlJob("createLxc")
//    public void createLxc() {
//        List<LxcLxcVo> lxcPendingList = lxcService.getLxcList(
//            new QueryWrapper<LxcLxc>().eq("lxc_status", LxcStatus.WAITING_FOR_PROCESS.getCode())
//                .orderBy(true, true, "create_time")
//        );
//        XxlJobHelper.log("处于未创建状态的小鸡有：{}个", lxcPendingList.size());
//        lxcPendingList.stream()
//            .collect(Collectors.groupingBy(LxcLxcVo::getSuperIp))
//            .forEach(
//                (k, lxcVoList) -> {
//                    LxcLxcVo lxc = lxcVoList.get(0);
//                    long pendingCount = lxcService.count(
//                        new QueryWrapper<LxcLxc>()
//                            .eq("super_ip", lxc.getSuperIp())
//                            .and(i -> i.eq("lxc_status", LxcStatus.PENDING.getCode()).or().eq("lxc_status", LxcStatus.FAILED_TO_CREATE.getCode()))
//                    );
//                    // 一次只能创建一个小鸡
//                    if (pendingCount == 0) {
//                        try {
//                            XxlJobHelper.log("开始创建小鸡：{}", lxc.getChildName());
//                            LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new QueryWrapper<LxcSuperLxc>().eq("ip", k));
//                            LxcSuperLxcVo superLxcVo = BeanCopyUtils.copy(superLxc, LxcSuperLxcVo.class);
//                            assert superLxcVo != null;
//                            ShellDto shellDto = LxcQueueServiceImpl.prepareCreateShellDto(superLxcVo, lxc);
//                            XxlJobHelper.log("小鸡创建信息：{}", shellDto);
//                            String format = ShellUtils.createLxc(shellDto);
//                            XxlJobHelper.log("小鸡{}创建命令：{}", lxc.getChildName(), format);
//                            LxcLxcBo temp = new LxcLxcBo();
//                            temp.setLxcStatus(LxcStatus.PENDING.getCode());
//                            temp.setId(lxc.getId());
//                            lxcService.updateByBoForce(temp);
//                        } catch (Exception e) {
//                            LxcLxcBo temp = new LxcLxcBo();
//                            temp.setLxcStatus(LxcStatus.FAILED_TO_CREATE.getCode());
//                            temp.setId(lxc.getId());
//                            lxcService.updateByBoForce(temp);
//                            XxlJobHelper.log(e);
//                        }
//                    }
//                }
//            );
//    }


    /**
     * 定时判断小鸡是否创建成功
     */
    @XxlJob("checkLxcPending")
    public void checkLxcPending() {
        // 获取在创建中的小鸡
        List<LxcLxcVo> lxcPendingList = lxcService.getLxcList(
            new QueryWrapper<LxcLxc>()
                .eq("lxc_status", LxcStatus.PENDING.getCode()) // 2
                .or()
                .eq("lxc_status", LxcStatus.FAILED_TO_CREATE.getCode()) // 6
                .or()
                .orderBy(true, true, "create_time")
        );
        // 如果小鸡创建有问题，使用按时间升序排列会一直卡在某一个小鸡，如果是真的创建失败可以该状态为failed_to_create，否则应该跳过 TODO
        if (!lxcPendingList.isEmpty()) {
            XxlJobHelper.log("当前有{}个小鸡正在创建中", lxcPendingList.size());
            log.info("当前有{}个小鸡正在创建中", lxcPendingList.size());
            // todo 要判断排队中的小鸡是否在服务器实际已经存在或创建完成，有可能因为网络原因或其他误操作使得状态更新失败，需要流转状态为“创建失败”，否则会阻塞新的小鸡创建【不止这一处】
            // 给等待（创建）中的小鸡都安排创建
            lxcPendingList.forEach(lxc -> {
                try {
                    String superIp = lxc.getSuperIp();
                    String childName = lxc.getChildName();
                    if (StringUtils.equals(lxc.getLxcStatus(), LxcStatus.FAILED_TO_CREATE.getCode())) {
                        XxlJobHelper.log("ID: {}  小鸡：{}处于创建失败状态,跳过", lxc.getId(), childName);
                        return;
                    }
                    LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new QueryWrapper<LxcSuperLxc>().eq("ip", superIp).last("LIMIT 1"));
                    String way = superLxc.getWay();
                    try {
                        ShellDto shellDto = ShellDto.builder()
                            .ip(superLxc.getIp())
                            .sshPort(Integer.valueOf(superLxc.getSshPort()))
                            .username("root")
                            .password(superLxc.getPassword())
                            .way(way)
                            .build();
                        String createCommand = String.format(ShellUtils.IF_PASSWD_FILE_EXIST, childName);
                        String hostInterfaceCommand = "";
                        // ⭐⭐⭐容器接口返回
                        hostInterfaceCommand = "lxd".equals(way) ? String.format(ShellUtils.GET_LXC_INTERFACE, childName) : "incus".equals(way) ? String.format(ShellUtils.GET_INCUS_INTERFACE, childName) : "";
                        // 判断小鸡容器和日志文件是否创建，如果存在，会返回true和小鸡密码
                        List<String> out = ShellUtils.execCmdOutList(createCommand, ShellUtils.getSession(shellDto));
                        // 设置重新执行modify.sh次数
                        int cnt = 1;
                        while (out.contains("false") && cnt++ <= 3) {
                            System.out.println("容器和日志文件创建失败：" + out.toString() + ", 重新执行modify.sh的方法");
                            LxcSuperLxcVo superLxcVo = BeanCopyUtils.copy(superLxc, LxcSuperLxcVo.class);
                            ShellDto redoModify = LxcQueueServiceImpl.prepareCreateShellDto(superLxcVo, lxc);
                            InstancesUtils.target(redoModify.getIp(), LxcConstants.LXD_PORT).execModifySh(redoModify, way);
                            try {
                                Thread.sleep(10000); // 等待10秒，确保modify.sh执行完成，再判断容器和日志文件是否创建成功
                                out = ShellUtils.execCmdOutList(createCommand, ShellUtils.getSession(shellDto));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("容器和日志文件创建成功，容器密码同步到数据库：" + out.toString());
                        // 这里获取不到小鸡密码也会导致最后创建失败
                        if (out.size() == 2 && StringUtils.equals(out.get(0), "true")) {
                            // such as :[true, 71a128da7]
                            LxcLxcBo temp = new LxcLxcBo();
                            temp.setLastUpdatedStatus(lxc.getLxcStatus());
                            temp.setLxcStatus(LxcStatus.RUNNING.getCode());
                            temp.setId(lxc.getId());
                            temp.setPassword(out.get(1));
                            if (StringUtils.equals(superLxc.getIpv6Flag(), LxcConstants.LXC_IPV6_FLAG)) {
                                String checkV6Command = String.format(ShellUtils.IF_IPV6_FILE_EXIST, childName);
                                List<String> v6 = ShellUtils.execCmdOutList(checkV6Command, ShellUtils.getSession(shellDto));
                                if (v6.size() == 1) {
                                    temp.setIpv6Addr(v6.get(0));
                                } else {
                                    XxlJobHelper.log("{}  {}获取ipv6地址失败", lxc.getId(), childName);
                                }
                            }
                            lxcService.updateByBoForce(temp);
                            XxlJobHelper.log("{}  {}创建成功", lxc.getId(), childName);
                            List<String> interfaceRes = ShellUtils.execCmdOutList(hostInterfaceCommand, ShellUtils.getSession(shellDto));
                            // 如果获取接口命令返回值不以"veth"开头，则换一条命令
                            if(!interfaceRes.get(0).startsWith("veth")){
                                hostInterfaceCommand = "lxd".equals(way) ? ShellUtils.GET_LXC_INTERFACE_BACKUP : "incus".equals(way) ? ShellUtils.GET_INCUS_INTERFACE_BACKUP : "";
                                // veth获取不到接口名，换成获取eth0开头的的接口名
                                interfaceRes = ShellUtils.execCmdOutList(hostInterfaceCommand, ShellUtils.getSession(shellDto));
                            }
                            // 用eth0返回的主机接口名可能会有重复
                            if (interfaceRes.size() >= 1) {
                                String interfaceName = interfaceRes.get(0);
                                if (StringUtils.equals("Error: Instance not found", interfaceName)) {
                                    XxlJobHelper.log("{}  {}获取网卡失败，原因：{}", lxc.getId(), childName, interfaceName);
                                    return;
                                }
                                temp.setHostInterface(interfaceName);
                                lxcService.updateByBoForce(temp);
                                try {
                                    // vnstat是通过网卡获取容器使用的流量的，用户点击容器弹出的关于流量显示的图片就是通过vnstat获取的
                                    String vnstatCommand = String.format(ShellUtils.VNSTAT_ADD_INTERFACE, interfaceName);
                                    List<String> vnstatRes = ShellUtils.execCmdOutList(vnstatCommand, ShellUtils.getSession(shellDto));
                                    XxlJobHelper.log("{}  {}添加vnstat网卡：{}，结果：", lxc.getId(), childName, interfaceName);
                                    vnstatRes.forEach(XxlJobHelper::log);
                                    vnstatRes.forEach(log::info);
                                    if (vnstatRes.size() > 1 && StringUtils.contains(vnstatRes.get(0), "Adding interface")) {
                                        XxlJobHelper.log("{}  {}添加vnstat网卡成功", lxc.getId(), childName);
                                    } else {
                                        XxlJobHelper.log("{}  {}添加vnstat网卡失败", lxc.getId(), childName);
                                    }
                                } catch (Exception e) {
                                    XxlJobHelper.log("{}  {}添加vnstat网卡异常,{}", lxc.getId(), childName, e.getCause());
                                }
                            }
                        } else {
                            String format = ShellUtils.IF_LXD_WORK; // 判断宿主机上是否安装了lxd/incus
                            List<String> work = ShellUtils.execCmdOutList(format, ShellUtils.getSession(shellDto));
                            LxcLxcBo temp = new LxcLxcBo();
                            temp.setLastUpdatedStatus(lxc.getLxcStatus());
                            temp.setLxcStatus(LxcStatus.FAILED_TO_CREATE.getCode());
                            temp.setId(lxc.getId());
                            lxcService.updateByBoForce(temp);
                            if (work.isEmpty()) {
                                XxlJobHelper.log("ID:{}  {}创建失败  {},原因是:宿主机上找不到lxd/incus服务", lxc.getId(), childName, format);
                            } else {
                                System.out.println("ID:" + lxc.getId() + childName + "已重试了三次创建脚本，创建失败");
                                XxlJobHelper.log("ID:{}  {}已重试了三次创建脚本，创建失败", lxc.getId(), childName);
                            }
                        }
                    } catch (Exception e) {
                        log.info("宿主机密码错误，ssh连接失败");
                        XxlJobHelper.log(e);
                    }
                } catch (Exception e) {
                    log.info("创建小鸡异常：{}", e);
                    XxlJobHelper.log(e);
                }
            });
        } else {
            XxlJobHelper.log("没有待创建的小鸡");
        }
    }

    /**
     * 处理过期的母鸡，包括母鸡到期，母鸡上并没有容器，先给个中间状态，不要一直获取状态
     */
    @XxlJob("handleExpiredSuperLxc")
    public void handleExpiredSuperLxc() {
        List<LxcSuperLxc> expiredList = lxcSuperLxcService.list(
            // 查询 宿主机过期时间(dead)在现在日期之前且为被标记为过期的宿主机的记录
            new QueryWrapper<LxcSuperLxc>().lt("dead", DateUtils.getDate())
                .ne("expire_flag", LxcConstants.LXC_SUPER_EXPIRED)
        );
        XxlJobHelper.log("过期但未标记的母鸡有：{}个", expiredList.size());
        expiredList.forEach(superLxc -> {
            // 先实际上将所有容器冻结，再改变数据库中的状态，否则用户仍然可以从终端连接容器
            ShellDto shellDto = ShellDto.builder()
               .ip(superLxc.getIp())
               .sshPort(Integer.valueOf(superLxc.getSshPort()))
               .username("root")
               .password(superLxc.getPassword())
               .way(superLxc.getWay())
               .build();
            String prefixCmd = "incus".equals(superLxc.getWay()) ? "incus" : "lxc";
            lxcService.getLxcList(new QueryWrapper<LxcLxc>().eq("super_ip", superLxc.getIp())).forEach(lxc -> {
                    ShellUtils.execCmdOutList(prefixCmd + "pause " + lxc.getChildName(), ShellUtils.getSession(shellDto));
                });
            // 更新数据库状态
            LxcSuperLxc temp = new LxcSuperLxc();
            temp.setExpireFlag(LxcConstants.LXC_SUPER_EXPIRED);
            temp.setId(superLxc.getId());
            try {
                XxlJobHelper.log("处理过期的母鸡：{} {} {}", superLxc.getIp(), superLxc.getName(), superLxc.getIp());
                lxcSuperLxcService.updateById(temp);
                lxcService.update(
                    new UpdateWrapper<LxcLxc>()
                        .set("lxc_status ", LxcStatus.EXPIRED.getCode())
                        .eq("super_ip", superLxc.getIp())
                );
            } catch (Exception e) {
                XxlJobHelper.log(e);
                throw new RuntimeException(e);
            }
        });
        // 处理正常的母鸡
        List<LxcSuperLxc> runningSuperLxcList = lxcSuperLxcService.list(new QueryWrapper<LxcSuperLxc>().eq("super_status", "1"));
        LxcSuperLxc curSuperLxc = new LxcSuperLxc();
        try {
            for (LxcSuperLxc superLxc : runningSuperLxcList) {
                curSuperLxc = superLxc;
                ShellDto shellDto = ShellDto.builder()
                    .ip(superLxc.getIp())
                    .sshPort(Integer.valueOf(superLxc.getSshPort()))
                    .username("root")
                    .password(superLxc.getPassword())
                    .way(superLxc.getWay())
                    .build();
                String format = ShellUtils.IF_LXD_WORK;
                List<String> work = ShellUtils.execCmdOutList(format, ShellUtils.getSession(shellDto));
                if (work.isEmpty()) {
                    System.out.println("宿主机上找不到lxd/incus服务");
                    XxlJobHelper.log(" {},宿主机上找不到lxd/incus服务", format);
                    setErrorContainers(superLxc, "noService");
                } else {
                    // 虽然有lxd/incus服务，但是容器列表不含数据库中的容器
                    setErrorContainers(superLxc, "noContainer");
                }
            }
        } catch (Exception e) {
            // 连接不到的宿主机，先将所有容器标记为已销毁
            setErrorContainers(curSuperLxc, "error");
            XxlJobHelper.log("该母鸡ssh密码错误或不存在:" + e);
            throw new RuntimeException("该母鸡ssh密码错误或不存在", e);
        }
    }

    private void setErrorContainers(LxcSuperLxc superLxc, String type) {
        String status = "";
        List<String> instancesList = new ArrayList<>();
        if ("noContainer".equals(type)) {
            try {
                BackgroundOperationResponse allInstancesBase = lxcQueueService.getAllInstancesBase(superLxc.getIp());
                Object metadata = allInstancesBase.getMetadata();
                instancesList = JSONUtil.parseArray(JSONUtil.toJsonStr(metadata)).toList(String.class);
            } catch (Exception e) {
                XxlJobHelper.log(e);
                System.out.println("宿主机" + superLxc.getIp() + "获取容器列表失败，请求超时");
            }
        }
        List<LxcLxcVo> lxcList = lxcService.getLxcList(new QueryWrapper<LxcLxc>().eq("super_ip", superLxc.getIp()));
        if ("noService".equals(type)) {
            status = LxcStatus.DESTROYED.getCode();
        }
        for (LxcLxcVo lxcVo : lxcList) {
            if (instancesList.isEmpty() || !instancesList.contains(lxcVo.getChildName())) {
                // 宿主机中没有该容器，标记为已销毁
                status = LxcStatus.DESTROYED.getCode();
            } else {
                // 宿主机中存在该容器，标记为上次被修改时的状态
                status = lxcVo.getLastUpdatedStatus();
            }
            LxcLxcBo temp = new LxcLxcBo();
            if (!LxcStatus.DESTROYED.getCode().equals(lxcVo.getLxcStatus())) {
                // 除了销毁外，其他的状态再机器恢复之后都是有意义的，不能更新到last_updated_status中
                temp.setLastUpdatedStatus(lxcVo.getLxcStatus());
            }
            temp.setLxcStatus(status);
            temp.setId(lxcVo.getId());
            lxcService.updateByBoForce(temp);
            System.out.println("ID为" + lxcVo.getId() + " 的容器原状态为" + lxcVo.getLastUpdatedStatus() + "，当前状态为" + status);
        }
    }

    /**
     * 采样流量（通过API），api是按照宿主机来采样的，每个宿主机采样一次——traffic_api_sample，采样频率不需要太高
     */
    @XxlJob("sampleTrafficByApi")
    public void sampleTrafficByApi() {
        // 遍历所有运行中的母鸡
        List<LxcSuperLxc> superLxcList = lxcSuperLxcService.getAliveLxcSuperLxcList();
        if(!superLxcList.isEmpty()) {
            for (LxcSuperLxc superLxc : superLxcList) {
                Long superId = superLxc.getId();
                String superIp = superLxc.getIp();
                // 获取当前母鸡下运行中的小鸡，id+name
                List<LxcLxcVo> lxcList = lxcService.getLxcList(new QueryWrapper<LxcLxc>().eq("super_ip", superIp).eq("lxc_status", LxcStatus.RUNNING.getCode()));
                if (!lxcList.isEmpty()) {
                    for (LxcLxcVo lxcVo : lxcList) {
                        Long lxcId = lxcVo.getId();
                        String containerName = lxcVo.getChildName();
                        // 采样流量
                        LxcContainerState instancesByNameState = InstancesUtils.target(superIp, LxcConstants.LXD_PORT).getInstancesByNameState(containerName);
                        if (instancesByNameState != null) {
                            // 流量信息
                            BigDecimal recv = BigDecimal.valueOf(instancesByNameState.getRecv());
                            Long rxBytes = recv.longValue();
                            BigDecimal send = BigDecimal.valueOf(instancesByNameState.getSend());
                            Long txBytes = send.longValue();
                            BigDecimal network = BigDecimal.valueOf(instancesByNameState.getNetwork());
                            Long networkBytes = network.longValue();
                            // 保存流量信息
                            TrafficApiSample trafficApiSample = new TrafficApiSample();
                            trafficApiSample.setSuperId(superId);
                            trafficApiSample.setContainerId(lxcId);
                            trafficApiSample.setContainerName(containerName);
                            trafficApiSample.setRxBytes(rxBytes);
                            trafficApiSample.setTxBytes(txBytes);
                            trafficApiSample.setLastSampledNetwork(networkBytes);
                            trafficApiSample.setIsRunning(LxcConstants.IS_RUNNING_TRUE);
                            trafficApiSample.setUpdateTime(new Date());
                            trafficApiSampleMapper.insert(trafficApiSample);
                        }
                    }
                }
            }
        }
    }

    /**
     * 将api采样表的数据每小时采样到数据库中，并删除两个月前的数据（这个可以分出来凌晨时执行）
     */
    @XxlJob("TrafficStatsPerHour")
    public void TrafficStatsPerHour() {
        LocalDateTime currentTime = LocalDateTime.now();
        int year = currentTime.getYear();
        int month = currentTime.getMonthValue();
        int day = currentTime.getDayOfMonth();
        int hour = currentTime.getHour();
        // 构建当前时间的开始时间（hh:00:00）
        LocalDateTime startTime = LocalDateTime.of(year, month, day, hour, 0, 0);
        // 构建当前时间的结束时间（hh:59:59）
        LocalDateTime endTime = LocalDateTime.of(year, month, day, hour, 59, 59);
        // 查询update_time在startTime和endTime之间并按super_id、container_id、container_name分组的TrafficApiSample列表数据，每个分组作为一个子列表
        List<List<TrafficApiSample>> result = new ArrayList<>(
            trafficApiSampleMapper.selectList(
                    new QueryWrapper<TrafficApiSample>()
                        .eq("is_running", LxcConstants.IS_RUNNING_TRUE)
                        .between("update_time", startTime, endTime)
                ).stream()
                .collect(Collectors.groupingBy(sample -> Arrays.asList(
                    sample.getSuperId(),
                    sample.getContainerId(),
                    sample.getContainerName()
                ))).values()
        );
        if(!result.isEmpty()) {
            for (List<TrafficApiSample> trafficApiSampleList : result) { // 每一组代表一个容器某个小时的采样数据
                // 取第一个和最后一个作差计算————该做法会导致一个小时最后一次采样和下一个小时第一次采样的流量被忽略，目前不对这部分误差做处理。如果有必要，就以一天或者一个月为单位对这些跨hour的流量进行处理，误差会减小到单位时间的维度
                TrafficApiSample trafficApiSampleFirst = trafficApiSampleList.get(0);
                TrafficApiSample trafficApiSampleLast = trafficApiSampleList.get(trafficApiSampleList.size() - 1);
                Long superId = trafficApiSampleFirst.getSuperId();
                Long containerId = trafficApiSampleFirst.getContainerId();
                String containerName = trafficApiSampleFirst.getContainerName();
                LxcQueue currentContainerQueue = lxcQueueService.getOne(new LambdaQueryWrapper<LxcQueue>().eq(LxcQueue::getLxcId, containerId));
                Long rxBytes = trafficApiSampleLast.getRxBytes() - trafficApiSampleFirst.getRxBytes();
                Long txBytes = trafficApiSampleLast.getTxBytes() - trafficApiSampleFirst.getTxBytes();
                // 保存流量信息，当前小时，每个容器的流量使用量
                insertTrafficStatsByTimeTable(year, month, day, hour, superId, containerId, containerName, rxBytes, txBytes);
                /**
                 * 业务流量记录表（用户-容器-流量处理）——user_container_traffic_used，每小时计算一次，不用版本来控制，在上一个统计表时同时更新，每y-m-d-h根据用户覆盖或更新上去
                 */
                insertOrUpdateUserContainerTrafficUsedTable(currentTime, superId, containerId, containerName, currentContainerQueue, rxBytes, txBytes);
                /**
                 * 用户流量汇总记录表——user_traffic_record
                 */

                /**
                 * 宿主机流量汇总记录表——traffic_super_lxc_record
                 */
            }
        }
    }

    private void insertTrafficStatsByTimeTable(int year, int month, int day, int hour, Long superId, Long containerId, String containerName, Long rxBytes, Long txBytes) {
        TrafficStatsByTime trafficStats = new TrafficStatsByTime();
        trafficStats.setSuperId(superId);
        trafficStats.setContainerId(containerId);
        trafficStats.setContainerName(containerName);
        trafficStats.setRxBytes(rxBytes);
        trafficStats.setTxBytes(txBytes);
        trafficStats.setYear(year);
        trafficStats.setMonth(month);
        trafficStats.setDay(day);
        trafficStats.setHour(hour);
        trafficStatsByTimeMapper.insert(trafficStats);
    }

    private void insertOrUpdateUserContainerTrafficUsedTable(LocalDateTime currentTime, Long superId, Long containerId, String containerName, LxcQueue currentContainerQueue, Long rxBytes, Long txBytes) {
        List<UserContainerTrafficUsed> userTrafficUsedList = userContainerTrafficUsedMapper.selectList(new LambdaQueryWrapper<UserContainerTrafficUsed>()
                .eq(UserContainerTrafficUsed::getTgId, currentContainerQueue.getTgId())
                .eq(UserContainerTrafficUsed::getSuperId, superId)
                .eq(UserContainerTrafficUsed::getContainerId, containerId));
        if (userTrafficUsedList.isEmpty()) {
            UserContainerTrafficUsed newUserTrafficUsed = new UserContainerTrafficUsed();
            newUserTrafficUsed.setTgId(currentContainerQueue.getTgId());
            newUserTrafficUsed.setSuperId(superId);
            newUserTrafficUsed.setContainerId(containerId);
            newUserTrafficUsed.setContainerName(containerName);
            newUserTrafficUsed.setTrafficIn(rxBytes);  // 叠加，当前小时的流量入累加
            newUserTrafficUsed.setTrafficOut(txBytes);    // 叠加，当前小时的流量出累加
            newUserTrafficUsed.setTotalUsed(rxBytes + txBytes);
            newUserTrafficUsed.setLastSampled(Date.from(currentTime.atZone(ZoneId.systemDefault()).toInstant()));
            userContainerTrafficUsedMapper.insert(newUserTrafficUsed);
        } else if (userTrafficUsedList.size() > 1) {
            // 理论上不会出现，但是为了保险起见，这里先删除所有记录，再插入新记录
            userContainerTrafficUsedMapper.delete(new LambdaQueryWrapper<UserContainerTrafficUsed>()
                    .eq(UserContainerTrafficUsed::getTgId, currentContainerQueue.getTgId())
                    .eq(UserContainerTrafficUsed::getSuperId, superId)
                    .eq(UserContainerTrafficUsed::getContainerId, containerId));
        } else {
            UserContainerTrafficUsed userTrafficUsed = userTrafficUsedList.get(0);
            userTrafficUsed.setTgId(currentContainerQueue.getTgId());
            userTrafficUsed.setSuperId(superId);
            userTrafficUsed.setContainerId(containerId);
            userTrafficUsed.setContainerName(containerName);
            Long lastTrafficIn = userTrafficUsed.getTrafficIn();
            Long lastTrafficOut = userTrafficUsed.getTrafficOut();
            Long lastTotalUsed = userTrafficUsed.getTotalUsed();
            userTrafficUsed.setTrafficIn(rxBytes + lastTrafficIn);  // 叠加，当前小时的流量入累加
            userTrafficUsed.setTrafficOut(txBytes + lastTrafficOut);    // 叠加，当前小时的流量出累加
            userTrafficUsed.setTotalUsed(rxBytes + txBytes + lastTotalUsed);
            userTrafficUsed.setLastSampled(Date.from(currentTime.atZone(ZoneId.systemDefault()).toInstant()));
            userContainerTrafficUsedMapper.update(userTrafficUsed, new LambdaUpdateWrapper<UserContainerTrafficUsed>()
                    .eq(UserContainerTrafficUsed::getTgId, currentContainerQueue.getTgId())
                    .eq(UserContainerTrafficUsed::getSuperId, superId)
                    .eq(UserContainerTrafficUsed::getContainerId, containerId));
        }
    }


    /**
     * todo 重建小鸡需要跟原小鸡的流量计算区分开来 重建小鸡时会把原来小鸡的信息删除，可能需要留着，方便后面统计流量信息（containerId）
     * 重设重建小鸡前流量超限状态并冻结小鸡
     */
    @XxlJob("resetTrafficLimitStatus")
    public void resetTrafficLimitStatus() {
        // 查找所有运行的小鸡
        List<LxcLxcVo> lxcList = lxcService.getLxcList(new QueryWrapper<LxcLxc>().eq("lxc_status", LxcStatus.RUNNING.getCode()));
        if(!lxcList.isEmpty()) {
            for (LxcLxcVo lxcVo : lxcList) {
                Long lxcId = lxcVo.getId();
                // 获取母鸡
                String childName = lxcVo.getChildName();
                String superIp = lxcVo.getSuperIp();
                LxcSuperLxcVo superLxc = lxcSuperLxcService.queryByIp(superIp);

                /** TODO
                 * 暂时先用redis来存小鸡重建前状态，重建的小鸡默认为开机状态，但如果之前小鸡已流量超限，需要进行限流或冻结
                 */
                // 只用redis不可靠，考虑数据库多一张表或多加一个字段
                String keyOfLxcStatus = String.format(CacheNames.LXC_ORIGINAL_STATUS, lxcId);
                if (RedisUtils.hasKey(keyOfLxcStatus)) {
                    String lxcOriginalStatus = RedisUtils.getCacheObject(keyOfLxcStatus);
                    if (lxcOriginalStatus.equals(LxcStatus.TRAFFIC_LIMIT.getCode())) {
                        try {
                            String res = ShellUtils.execCmdAndReturnStatus(ShellDto.builder()
                                .ip(superIp)
                                .password(superLxc.getPassword())
                                .way(superLxc.getWay())
                                .modifyType("traffic_limit")
                                .childName(childName)
                                .build());
                            log.info(res);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // 重设为流量超限状态
                        lxcService.updateStatus(LxcStatus.TRAFFIC_LIMIT.getCode(), lxcId);
                    }

//                RedisUtils.deleteKeys(keyOfLxcStatus);  // 会使该键变成一次性
                } else {
                    // 如果redis中没有keyOfLxcStatus，从哪里获取先前的状态完成还原   TODO

                }
            }
        }
    }

    /**
     * todo 判断超限的逻辑在hourLevelTraffic方法中处理
     * 判断网络流量使用是否超过阈值
     */
    @XxlJob("checkAndSetNetExceed")
    public void checkAndSetNetExceed() {
        //对母鸡操作，在后台调出每只小鸡的名称和ip？然后注意获取网络流量，判断是否超过阈值
        List<LxcLxcVo> lxcRunningList = lxcService.getLxcAndQueueList(LxcStatus.RUNNING.getCode());
        if(!lxcRunningList.isEmpty()){
            // 查询正在运行的小鸡的发送和接收流量总量，超过限制就冻结服务器
            for (LxcLxcVo lxcLxcVo : lxcRunningList) {
                String childName = lxcLxcVo.getChildName();
                String superIp = lxcLxcVo.getSuperIp();//这里可能为null
                LxcSuperLxcVo superLxc = lxcSuperLxcService.queryByIp(superIp);
                // 通过母鸡ip和小鸡名称获取小鸡ip
                String way = superLxc.getWay();
                String getIpCommand = "";
                String command = "";
                // lxc/incus info ContainName 中有容器流量使用情况
                if("lxd".equals(way)){
                    getIpCommand = String.format(ShellUtils.GET_LXC_IP, childName);
                    command = String.format(NET_USED,childName);
                } else if("incus".equals(way)){
                    getIpCommand = String.format(ShellUtils.GET_INCUS_IP,childName);
                    command = String.format(NET_INCUS_USED,childName);
                }
                List<String> childIpList = new ArrayList<>();
                String childIp = "";
                try {
                    childIpList = ShellUtils.execCmdOutList(getIpCommand,"root",superLxc.getPassword(),superIp,Integer.valueOf(superLxc.getSshPort()));
                    childIp = childIp + childIpList.get(0);// 字符串拼接，拼接小鸡ip地址
                    XxlJobHelper.log("正在监测流量使用的小鸡ip为：{}",childIp);
                } catch (Exception e) {
                    XxlJobHelper.log("--------获取活动的小鸡ip失败--------");
                    e.printStackTrace();
                }
                // 小鸡的ip是动态变化的
                Integer sshPort = Integer.valueOf(superLxc.getSshPort());
                // todo 后续要改为通过LXC API获取容器基本信息，前端单独利用这些信息绘制图表
                try {
                    List<String> list = ShellUtils.execCmdOutList(
                        // 连接母鸡执行lxc info <container>命令
                        command,
                        "root",
                        superLxc.getPassword(), superIp,
                        Optional.ofNullable(sshPort).orElse(22)
                    );
                    if(list == null || list.isEmpty()){
                        System.out.println("没有获取到流量使用情况");
                    } else {
                        System.out.println(childName + "的流量使用情况为："+list);
                    }
                    String trafficTotalLimit = superLxc.getTrafficTotalLimit();
                    BigDecimal tTL2MB = CommonUtils.numUnit2Decimal(trafficTotalLimit);
                    BigDecimal summaryNetFlow = lxcLxcVo.getLxcTraffic();
                    for (String flow : list) {
                        // 将kB和B化为MB
                        summaryNetFlow = summaryNetFlow.add(CommonUtils.numUnit2Decimal(flow));
                        try {
                            if(summaryNetFlow.compareTo(tTL2MB) == 1){
                                // 小鸡关机且不能让前端的开机操作响应；
                                String res = ShellUtils.execCmdAndReturnStatus(ShellDto.builder().ip(superIp).password(superLxc.getPassword()).way(superLxc.getWay()).modifyType("traffic_limit").childName(childName).build());
                                log.info(res);
                                lxcLxcVo.setLastUpdatedStatus(lxcLxcVo.getLxcStatus());
                                lxcLxcVo.setLxcStatus(LxcStatus.TRAFFIC_LIMIT.getCode());
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("流量使用情况解析失败: " + e);
                        }
                    }
                    // 将流量使用量写入到数据库
                    LxcLxcBo lxcLxcBo = new LxcLxcBo();
                    BeanUtils.copyProperties(lxcLxcVo,lxcLxcBo);
                    lxcLxcBo.setLxcTraffic(summaryNetFlow);
                    lxcService.updateTraffic(lxcLxcBo);
                    System.out.println("小鸡 " + childName + ":" + childIp + " 目前用了 " + summaryNetFlow + " MB流量");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            XxlJobHelper.log("-------------此时没有小鸡正在运行-----------");
        }
    }




    /**
     * todo 判断超限的数据要从新表中获取
     * 重置流量超限小鸡，除了解除冻结，还要重新考虑流量计数
     */
    @XxlJob("resetNetExceed")
    public void resetNetExceed(){
        /**
         * 可能出现的情况：1、一个容器在一个用户手上的时间经过了一个月的1号（正）；2、容器流量超限了，但用户企图重建来刷新流量使用情况（反）；3、每个月1号要清除数据库中记录的流量（正）
         */
        // 获取流量超限的小鸡列表和队列列表
        List<LxcLxcVo> lxcNetExceedList = lxcService.getLxcAndQueueList(
            LxcStatus.TRAFFIC_LIMIT.getCode()
        );
        if(lxcNetExceedList.isEmpty()){
            XxlJobHelper.log("--------此时没有流量超限的小鸡--------");
        }else{
            for (LxcLxcVo lxcLxcVo : lxcNetExceedList) {
                String childName = lxcLxcVo.getChildName();
                String superIp = lxcLxcVo.getSuperIp();
                LxcSuperLxcVo superLxc = lxcSuperLxcService.queryByIp(superIp);
//                String way = superLxc.getWay();
                // 通过母鸡ip和小鸡名称获取小鸡ip
//                String getNetUsedCommand = "";
//                if("lxd".equals(way)){
//                    getNetUsedCommand = String.format(NET_USED, childName);
//                } else if("incus".equals(way)){
//                    getNetUsedCommand = String.format(NET_INCUS_USED, childName);
//                }
//                List<String> result = execCmdOutList(getNetUsedCommand, getSession(ShellDto.builder().ip(superIp).sshPort(Integer.valueOf(superLxc.getSshPort())).username("root").password(superLxc.getPassword()).way(superLxc.getWay()).childName(childName).build()));
                // 将小鸡状态由"流量超限"修改为"运行中"，每个月开始将小鸡状态恢复
                 // 还要考虑重建小鸡后lxc_Status可能会被重置了
                try {
                     // 网络流量使用解除限制
                    String res = ShellUtils.execCmdAndReturnStatus(ShellDto.builder().ip(superIp).password(superLxc.getPassword()).way(superLxc.getWay()).modifyType("release_limit").childName(childName).build());
                    log.info(res);
                    lxcService.updateStatus(LxcStatus.RUNNING.getCode(), lxcLxcVo.getId());
                    // 重设流量计数，需要把缓存中的“上次状态”值为流量超限TRAFFIC_LIMIT的key改回RUNNING
                    String keyOfLxcStatus = String.format(CacheNames.LXC_ORIGINAL_STATUS,lxcLxcVo.getId());
                    if(RedisUtils.hasKey(keyOfLxcStatus) && RedisUtils.getCacheObject(keyOfLxcStatus).equals(LxcStatus.TRAFFIC_LIMIT)) {
                        // 直接删掉这条缓存也可以
                        RedisUtils.deleteObject(keyOfLxcStatus);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }



    @Test
    public void test() throws Exception {
        List<String> list = null;

        String command = String.format(NET_USED,"FourthTest4");
        list = ShellUtils.execCmdOutList(
            command,
            "root",
            "e41f39a12", "185.206.69.5",
            22
        );
        for (String s : list) {
            System.out.println(s);
        }
    }

}


