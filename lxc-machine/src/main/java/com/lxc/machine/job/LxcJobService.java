package com.lxc.machine.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcLxcBo;
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcSuperLxcService;
import com.lxc.machine.service.impl.LxcQueueServiceImpl;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lxc.common.utils.ShellUtils.NET_INCUS_USED;
import static com.lxc.common.utils.ShellUtils.NET_USED;

@RequiredArgsConstructor
@Service
@Slf4j
public class LxcJobService {
    private final InstancesApi lxcApi;

    private final ILxcLxcService lxcService;

    private final ILxcSuperLxcService lxcSuperLxcService;


    @XxlJob("initLxc")
    public void initLxc() {
        List<LxcLxcVo> lxcPendingList = lxcService.getLxcList(
            new QueryWrapper<LxcLxc>().eq("lxc_status", LxcStatus.WAITING_FOR_PROCESS.getCode())
                .orderBy(true, true, "create_time")
        );
        XxlJobHelper.log("处于WAITING_FOR_PROCESS未初始化状态的小鸡有：{}个", lxcPendingList.size());
        log.info("处于WAITING_FOR_PROCESS未初始化状态的小鸡有：{}个", lxcPendingList.size());
        lxcPendingList.stream()
            .collect(Collectors.groupingBy(LxcLxcVo::getSuperIp))
            .forEach(
                (superIp, lxcVoList) -> {
                    // 查询创建中的小鸡，对不同的母鸡分开计算
                    long pendingCount = lxcService.count(
                        new QueryWrapper<LxcLxc>()
                            .eq("super_ip", superIp)
                            .and(i -> i.eq("lxc_status", LxcStatus.PENDING.getCode()).or().eq("lxc_status", LxcStatus.FAILED_TO_CREATE.getCode()))
                    );
                    log.info("宿主机{}的创建中的小鸡有{}个", superIp, pendingCount);
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
                                // incus和lxc只会在服务器上存在其中一个，获取小鸡实例不涉及incus或lxc的命令前缀
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
                            // ⭐⭐⭐这里是执行modify.sh生成 container 和 container_init.log 两个文件的关键步骤，只有 container 文件生成了，checkLxcPending才会将容器的密码同步回来数据库

                            // todo 这里执行modify.sh，将modify.sh中所有命令拆开执行
                            /*ShellUtils.Shell shell = ShellUtils.initLxc(shellDto);
                            XxlJobHelper.log("小鸡{}初始化命令：{}", lxc.getChildName(), shell.getCmd());
                            log.info("小鸡{}初始化命令：{}", lxc.getChildName(), shell.getCmd());*/

                            InstancesUtils.target(shellDto.getIp(), LxcConstants.LXD_PORT).execModifySh(shellDto, superLxcVo.getWay());

                            LxcLxcBo temp = new LxcLxcBo();
                            temp.setLxcStatus(LxcStatus.PENDING.getCode()); // 2
                            temp.setId(lxc.getId());
                            lxcService.updateByBoForce(temp);
                        } catch (Exception e) {
                            LxcLxcBo temp = new LxcLxcBo();
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
        // 获取在排队或重建中的小鸡
        List<LxcLxcVo> lxcPendingList = lxcService.getLxcList(
            new QueryWrapper<LxcLxc>()
                .eq("lxc_status", LxcStatus.PENDING.getCode()) // 2
                .or()
                .eq("lxc_status", LxcStatus.FAILED_TO_CREATE.getCode()) // 6
                .or()
                .eq("lxc_status", LxcStatus.RESETING.getCode()) // 0
                .orderBy(true, true, "create_time")
        );
        // 如果小鸡创建有问题，使用按时间升序排列会一直卡在某一个小鸡，如果是真的创建失败可以该状态为failed_to_create，否则应该跳过 TODO
        if (!lxcPendingList.isEmpty()) {
            XxlJobHelper.log("当前有{}个小鸡正在创建中", lxcPendingList.size());
            log.info("当前有{}个小鸡正在创建中", lxcPendingList.size());

            // todo 要判断排队中的小鸡是否在服务器实际已经存在或创建完成，有可能因为网络原因或其他误操作使得状态更新失败，需要流转状态为“创建失败”，否则会阻塞新的小鸡创建【不止这一处】

            // 给排队或重建中的小鸡都安排创建
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
                        // ⭐⭐⭐容器密码返回
                        String createCommand = String.format(ShellUtils.IF_PASSWD_FILE_EXIST, childName);
                        String hostInterfaceCommand = "";
//                        String statusCommand = "";
                        // ⭐⭐⭐容器接口返回
                        if("lxd".equals(way)){
                            // todo 容器需要开机才能获取到veth接口
//                            statusCommand = String.format(ShellUtils.GET_LXC_STATUS, childName);
                            hostInterfaceCommand = String.format(ShellUtils.GET_LXC_INTERFACE, childName);
                        }
                        if("incus".equals(way)){
                            // todo 容器需要开机才能获取到veth接口
//                            statusCommand = String.format(ShellUtils.GET_INCUS_STATUS, childName);
                            hostInterfaceCommand = String.format(ShellUtils.GET_INCUS_INTERFACE, childName);
                        }

                        // 判断小鸡容器和日志文件是否创建，如果存在，会返回true和小鸡密码
                        List<String> out = ShellUtils.execCmdOutList(createCommand, ShellUtils.getSession(shellDto));
                        System.out.println("容器和日志文件创建成功，容器密码同步到数据库：\n" + out.toString());
                        // 这里获取不到小鸡密码也会导致最后创建失败
                        if (out.size() == 2 && StringUtils.equals(out.get(0), "true")) {
                            // such as :[true, 71a128da7]
                            LxcLxcBo temp = new LxcLxcBo();
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
//                            List<String> status = ShellUtils.execCmdOutList(statusCommand, ShellUtils.getSession(shellDto));
//                            // 目前只考虑容器创建后未开机默认为STOPPED的情况
//                            if ("STOPPED".equals(status)) {
//                                // 开机获取接口信息
//                                String res = ShellUtils.execCmdAndReturnStatus(
//                                    ShellDto.builder()
//                                        .ip(superIp)
//                                        .password(superLxc.getPassword())
//                                        .way(superLxc.getWay())
//                                        .modifyType("start")
//                                        .childName(childName)
//                                        .build()
//                                );
//                                log.info("{}  {}开机命令执行结果：{}", lxc.getId(), childName, res);
//                            }
                            List<String> interfaceRes = ShellUtils.execCmdOutList(hostInterfaceCommand, ShellUtils.getSession(shellDto));
                            // 如果获取接口命令返回值不以"veth"开头，则换一条命令
                            if(!interfaceRes.get(0).startsWith("veth")){
                                if("lxd".equals(way)){
                                    hostInterfaceCommand = ShellUtils.GET_LXC_INTERFACE_BACKUP;
                                }
                                if("incus".equals(way)){
                                    hostInterfaceCommand = ShellUtils.GET_INCUS_INTERFACE_BACKUP;
                                }
                                // veth获取不到接口名，换成获取eth0开头的的接口名
                                interfaceRes = ShellUtils.execCmdOutList(hostInterfaceCommand, ShellUtils.getSession(shellDto));
                            }
//                        if (interfaceRes.size() == 1) {
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
                                    // vnstat是通过网卡获取容器使用的流量的
                                    String vnstatCommand = String.format(ShellUtils.VNSTAT_ADD_INTERFACE, interfaceName);
                                    List<String> vnstatRes = ShellUtils.execCmdOutList(vnstatCommand, ShellUtils.getSession(shellDto));
                                    XxlJobHelper.log("{}  {}添加vnstat网卡：{}，结果：", lxc.getId(), childName, interfaceName);
                                    vnstatRes.forEach(XxlJobHelper::log);
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
                            String format = ShellUtils.IF_LXD_WORK;
                            List<String> work = ShellUtils.execCmdOutList(format, ShellUtils.getSession(shellDto));
                            if (work.isEmpty()) {
                                LxcLxcBo temp = new LxcLxcBo();
                                temp.setLxcStatus(LxcStatus.FAILED_TO_CREATE.getCode());
                                temp.setId(lxc.getId());
                                lxcService.updateByBoForce(temp);
                                XxlJobHelper.log("ID:{}  {}创建失败  {}", lxc.getId(), childName, format);
                            } else {
                                XxlJobHelper.log("ID:{}  {}还在创建中", lxc.getId(), childName);
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
     * 处理过期的母鸡
     */
    @XxlJob("handleExpiredSuperLxc")
    public void handleExpiredSuperLxc() {
        List<LxcSuperLxc> expiredList = lxcSuperLxcService.list(
            // 查询 母鸡过期时间(dead)在现在日期之前的记录
            new QueryWrapper<LxcSuperLxc>().lt("dead", DateUtils.getDate())
                .ne("expire_flag", LxcConstants.LXC_SUPER_EXPIRED)
        );
        XxlJobHelper.log("过期的母鸡有：{}个", expiredList.size());
        expiredList.forEach(superLxc -> {
            LxcSuperLxc temp = new LxcSuperLxc();
            temp.setExpireFlag(LxcConstants.LXC_SUPER_EXPIRED);
            temp.setId(superLxc.getId());
            try {
                XxlJobHelper.log("处理过期的母鸡：{} {} {}", superLxc.getIp(), superLxc.getName(), superLxc.getIp());
                lxcSuperLxcService.updateById(temp);
                lxcService.update(
                    new UpdateWrapper<LxcLxc>()
                        .set("lxc_status", LxcStatus.EXPIRED.getCode())
                        .eq("super_ip", superLxc.getIp())
                );
            } catch (Exception e) {
                XxlJobHelper.log(e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 重设重建小鸡前流量超限状态并冻结小鸡
     */
    @XxlJob("resetTrafficLimitStatus")
    public void resetTrafficLimitStatus() {
        // 查找所有运行的小鸡
        List<LxcLxcVo> lxcList = lxcService.getLxcList(new QueryWrapper<LxcLxc>().eq("lxc_status", LxcStatus.RUNNING.getCode()));
        if(lxcList.size() > 0 && lxcList != null) {
            for (LxcLxcVo lxcVo : lxcList) {
                Long lxcId = lxcVo.getId();
                // 获取母鸡
                String childName = lxcVo.getChildName();
                String superIp = lxcVo.getSuperIp();//这里可能为null
                LxcSuperLxcVo superLxc = lxcSuperLxcService.queryByIp(superIp);

                String way = superLxc.getWay();
//                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + way);

                /** TODO
                 * 暂时先用redis来存小鸡重建前状态，重建的小鸡默认为开机状态，但如果之前小鸡已流量超限，需要进行限流或冻结
                 */
                // 只用redis不可靠，考虑数据库多一张表或多加一个字段
                String keyOfLxcStatus = String.format(CacheNames.LXC_ORIGINAL_STATUS, lxcId);
                if (RedisUtils.hasKey(keyOfLxcStatus)) {
                    String lxcOriginalStatus = RedisUtils.getCacheObject(keyOfLxcStatus);
                    if (lxcOriginalStatus.equals(LxcStatus.TRAFFIC_LIMIT.getCode())) {
                        try {
                            ShellUtils.execCmdAndReturnStatus(ShellDto.builder()
                                .ip(superIp)
                                .password(superLxc.getPassword())
                                .way(superLxc.getWay())
                                .modifyType("traffic_limit")
                                .childName(childName)
                                .build());
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

    @XxlJob("checkAndSetNetExceed")
    public void checkAndSetNetExceed(){
        //对母鸡操作，在后台调出每只小鸡的名称和ip？然后注意获取网络流量，判断是否超过
        //阈值
        List<LxcLxcVo> lxcRunningList = lxcService.getLxcAndQueueList(
            LxcStatus.RUNNING.getCode()
        );

//        int cnt = 1;
        if(lxcRunningList.size() > 0 && lxcRunningList != null){

            // 查询正在运行的小鸡的发送和接收流量总量，超过限制就冻结服务器
            for (LxcLxcVo lxcLxcVo : lxcRunningList) {
//                System.out.println("第" + cnt + "个小鸡对象:" + lxcLxcVo);

                String childName = lxcLxcVo.getChildName();
                String superIp = lxcLxcVo.getSuperIp();//这里可能为null
                LxcSuperLxcVo superLxc = lxcSuperLxcService.queryByIp(superIp);
//                System.out.println("第" + cnt + "个母鸡对象:" + superLxc);
//                cnt++;
                // 通过母鸡ip和小鸡名称获取小鸡ip
                String way = superLxc.getWay();
                String getIpCommand = "";
                String command = "";
                // lxc/incus info ContainName 中有容器流量使用情况
                if("lxd".equals(way)){
                    getIpCommand = String.format(ShellUtils.GET_LXC_IP, childName);
                    command = String.format(NET_USED,childName);
                }
                if("incus".equals(way)){
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
                String lxcPassword = lxcLxcVo.getPassword();

                String result = "";
                // command放到上面的判断中
//                String command = String.format(NET_USED,childName);
                try {
                    List<String> list = ShellUtils.execCmdOutList(
                        // 连接母鸡执行lxc info <container>命令
                        command,
                        "root",
                        superLxc.getPassword(), superIp,
                        Optional.ofNullable(sshPort).orElse(22)
                    );
                    if(list == null || list.size() == 0){
                        System.out.println("没有获取到流量使用情况");
                    } else {
                        System.out.println("该小鸡的流量使用情况为："+list);
                    }

                    // TODO 用BigDecimal重写流量累积   lxcTraffic对应的java类型就是BigDecimal
                    BigDecimal summaryNetFlow = lxcLxcVo.getLxcTraffic();
//                    double summaryNetFlow = 0.0;
                    // TODO 将查出来的流量同步到数据库
                    String numberStr = "";
                    String unitStr = "";
                    for (String flow : list) {
                        // 这里没考虑发送和接收的流量单位不同的情况，可能要加个化单位步骤
                        numberStr = extractNumber(flow);
                        unitStr = extractUnit(flow);
                        // 将kB和B化为MB
                        BigDecimal flow2MB = unitUnify(numberStr,unitStr);
                        summaryNetFlow = summaryNetFlow.add(flow2MB);
                        // 限制每月小鸡流量为LXC_NETSTREAM_LIMIT(GB)，将小鸡状态修改为"流量超限"，另外要有一个每个月开始将小鸡状态恢复
                        try {
                            // compareTo ==> -1：左小于右；0：左等于右；1：左大于右
                            if(summaryNetFlow.compareTo(BigDecimal.valueOf(LxcConstants.LXC_NETSTREAM_LIMIT*1024)) == 1){
                                // 小鸡关机且不能让前端的开机操作响应；
                                ShellUtils.execCmdAndReturnStatus(ShellDto.builder()
                                    .ip(superIp)
                                    .password(superLxc.getPassword())
                                    .way(superLxc.getWay())
                                    .modifyType("traffic_limit")
                                    .childName(childName)
                                    .build());
                                // 网络流量超过限制
//                                    lxcLxcVo.setLxcStatus(LxcStatus.TRAFFIC_LIMIT.getCode());
                                lxcService.updateStatus(LxcStatus.TRAFFIC_LIMIT.getCode(),lxcLxcVo.getId());

                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing number: " + numberStr);
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
     * 检查实例使用的网络流量是否超过限量（用的double）
     */
//    @XxlJob("checkAndSetNetExceed")
//    public void checkAndSetNetExceed(){
//        //对母鸡操作，在后台调出每只小鸡的名称和ip？然后注意获取网络流量，判断是否超过
//        //阈值
//        List<LxcLxcVo> lxcRunningList = lxcService.getLxcAndQueueList(
//            LxcStatus.RUNNING.getCode()
//        );
//
////        int cnt = 1;
//        if(lxcRunningList.size() == 0){
//            XxlJobHelper.log("--------此时没有已开机的小鸡--------");
//        }else{
//            // 查询正在运行的小鸡的发送和接收流量总量，超过限制就冻结服务器
//            for (LxcLxcVo lxcLxcVo : lxcRunningList) {
////                System.out.println("第" + cnt + "个小鸡对象:" + lxcLxcVo);
//
//                String childName = lxcLxcVo.getChildName();
//                String superIp = lxcLxcVo.getSuperIp();//这里可能为null
//                LxcSuperLxcVo superLxc = lxcSuperLxcService.queryByIp(superIp);
////                System.out.println("第" + cnt + "个母鸡对象:" + superLxc);
////                cnt++;
//                // 通过母鸡ip和小鸡名称获取小鸡ip
//                String way = superLxc.getWay();
//                String getIpCommand = "";
//                String command = "";
//                // lxc/incus info ContainName 中有容器流量使用情况
//                if("lxd".equals(way)){
//                    getIpCommand = String.format(ShellUtils.GET_LXC_IP, childName);
//                    command = String.format(NET_USED,childName);
//                }
//                if("incus".equals(way)){
//                    getIpCommand = String.format(ShellUtils.GET_INCUS_IP,childName);
//                    command = String.format(NET_INCUS_USED,childName);
//                }
//
//                List<String> childIpList = new ArrayList<>();
//                String childIp = "";
//                try {
//                    childIpList = ShellUtils.execCmdOutList(getIpCommand,"root",superLxc.getPassword(),superIp,Integer.valueOf(superLxc.getSshPort()));
//                    childIp = childIp + childIpList.get(0);// 字符串拼接，拼接小鸡ip地址
//                    XxlJobHelper.log("正在监测流量使用的小鸡ip为：{}",childIp);
//                } catch (Exception e) {
//                    XxlJobHelper.log("--------获取活动的小鸡ip失败--------");
//                    e.printStackTrace();
//                }
//                // 小鸡的ip是动态变化的
//                Integer sshPort = Integer.valueOf(superLxc.getSshPort());
//                String lxcPassword = lxcLxcVo.getPassword();
//
//                String result = "";
//                // command放到上面的判断中
////                String command = String.format(NET_USED,childName);
//                try {
//                    List<String> list = ShellUtils.execCmdOutList(
//                        // 连接母鸡执行lxc info <container>命令
//                        command,
//                        "root",
//                        superLxc.getPassword(), superIp,
//                        Optional.ofNullable(sshPort).orElse(22)
//                    );
//                    if(list == null || list.size() == 0){
//                        System.out.println("没有获取到流量使用情况");
//                    }
//                    System.out.println("该小鸡的流量使用情况为："+list);
//                    double summaryNetFlow = 0.0;
//                    // TODO 将查出来的流量同步到数据库
//                    String numberStr = "";
//                    String unitStr = "";
//                    for (String flow : list) {
//                            // 这里没考虑发送和接收的流量单位不同的情况，可能要加个化单位步骤
//                            numberStr = extractNumber(flow);
//                            unitStr = extractUnit(flow);
//                            // 将kB和B化为GB
//                            double flow2GB = unitUnify(numberStr,unitStr);
//                            summaryNetFlow += flow2GB;
//                            // 限制每月小鸡流量为LXC_NETSTREAM_LIMIT(MB)，将小鸡状态修改为"流量超限"，另外要有一个每个月开始将小鸡状态恢复
//                            try {
//                                if(summaryNetFlow >= LxcConstants.LXC_NETSTREAM_LIMIT){
//                                    // 小鸡关机且不能让前端的开机操作响应；
//                                    ShellUtils.execCmdAndReturnStatus(ShellDto.builder()
//                                        .ip(superIp)
//                                        .password(superLxc.getPassword())
//                                        .way(superLxc.getWay())
//                                        .modifyType("traffic_limit")
//                                        .childName(childName)
//                                        .build());
//                                    // 网络流量超过限制
////                                    lxcLxcVo.setLxcStatus(LxcStatus.TRAFFIC_LIMIT.getCode());
//                                    lxcService.updateStatus(LxcStatus.TRAFFIC_LIMIT.getCode(),lxcLxcVo.getId());
//
//                                }
//                            } catch (NumberFormatException e) {
//                                System.err.println("Error parsing number: " + numberStr);
//                            }
//                    }
//                    System.out.println("小鸡 " + childName + ":" + childIp + " 目前用了" + summaryNetFlow + unitStr + "流量");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//    }

    /**
     * 流量单位换算 -> MB
     * @param numberStr
     * @param unitStr
     * @return
     */
    private BigDecimal unitUnify(String numberStr,String unitStr) {
        // 将浮点数字符串解析为浮点数
//        double flowNum = Double.parseDouble(numberStr);
        BigDecimal flowNum = new BigDecimal(numberStr);
        // 将kB、B、GB的单位化成MB
        if("kb".equals(unitStr.toLowerCase())){
            flowNum = flowNum.divide(new BigDecimal(1024),2, RoundingMode.HALF_UP);
        }else if("b".equals(unitStr.toLowerCase())){
            flowNum = flowNum.divide(new BigDecimal(1024*1024),2,RoundingMode.HALF_UP);
        }else if("gb".equals(unitStr.toLowerCase())){
            flowNum = flowNum.multiply(new BigDecimal(1024));
        }
//        if(flowNum.compareTo(BigDecimal.valueOf(0.01)) == -1){
//            // 流量使用单位还是B或kB时，直接返回0，否则后面的小数在控制台显示出来可能很长
//            flowNum = new BigDecimal(0);
//        }
        return flowNum;
    }

    /**
     * 重置流量超限小鸡
     */
    @XxlJob("resetNetExceed")
    public void resetNetExceed(){
        // 获取流量超限的小鸡列表和队列列表
        List<LxcLxcVo> lxcNetExceedList = lxcService.getLxcAndQueueList(
            LxcStatus.TRAFFIC_LIMIT.getCode()
        );

        if(lxcNetExceedList.size() == 0){
            XxlJobHelper.log("--------此时没有流量超限的小鸡--------");
        }else{
            for (LxcLxcVo lxcLxcVo : lxcNetExceedList) {

                String childName = lxcLxcVo.getChildName();
                String superIp = lxcLxcVo.getSuperIp();
                LxcSuperLxcVo superLxc = lxcSuperLxcService.queryByIp(superIp);

                String way = superLxc.getWay();
                // 通过母鸡ip和小鸡名称获取小鸡ip
                String getIpCommand = "";
                if("lxd".equals(way)){
                    getIpCommand = String.format(ShellUtils.GET_LXC_IP, childName);
                }
                if("incus".equals(way)){
                    getIpCommand = String.format(ShellUtils.GET_INCUS_IP,childName);
                }


                Integer sshPort = Integer.valueOf(superLxc.getSshPort());
                String lxcPassword = lxcLxcVo.getPassword();

                String result = "";
//                String command = String.format(NET_USED,childName);
                 // 将小鸡状态由"流量超限"修改为"运行中"，每个月开始将小鸡状态恢复
                 // 还要考虑重建小鸡后lxc_Status可能会被重置了
                try {
                     // 网络流量解除限制
//                     lxcLxcVo.setLxcStatus(LxcStatus.RUNNING.getCode());
                    ShellUtils.execCmdAndReturnStatus(ShellDto.builder()
                        .ip(superIp)
                        .password(superLxc.getPassword())
                        .way(superLxc.getWay())
                        .modifyType("release_limit") // 重新开机
                        .childName(childName)
                        .build());
                    lxcService.updateStatus(LxcStatus.RUNNING.getCode(), lxcLxcVo.getId());
                    // 重设流量计数，需要把缓存中的“上次状态”值为流量超限TRAFFIC_LIMIT的key改回RUNNING
                    String keyOfLxcStatus = String.format(CacheNames.LXC_ORIGINAL_STATUS,lxcLxcVo.getId());
                    if(RedisUtils.hasKey(keyOfLxcStatus) && RedisUtils.getCacheObject(keyOfLxcStatus).equals(LxcStatus.TRAFFIC_LIMIT)) {
//                        RedisUtils.setCacheObject(keyOfLxcStatus,LxcStatus.RUNNING.getCode());
                        // 直接删掉这条缓存也可以
                        RedisUtils.deleteObject(keyOfLxcStatus);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

    }

    /**
     * 正则表达式提取浮点数
     * @param input
     * @return
     */
    public static String extractNumber(String input) {
        // 定义一个正则表达式来匹配浮点数部分
        String numberRegex = "[+-]?([0-9]*[.])?[0-9]+";
        Pattern pattern = Pattern.compile(numberRegex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        } else {
            return null; // 或者返回一个默认值，取决于你的需求
        }
    }

    /**
     * 正则表达式提取字符串
     * @param input
     * @return
     */
    public static String extractUnit(String input) {
        // 定义一个正则表达式来匹配单位部分
        String unitRegex = "[a-zA-Z]+";
        Pattern pattern = Pattern.compile(unitRegex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        } else {
            return null; // 或者返回一个默认值，取决于你的需求
        }
    }

    /**
     * TODO 检查提供容器的服务器是否不再提供lxc或incus服务
     * @throws Exception
     */
//    @XxlJob("checkValidSuperLxc")
//    public void checkValidSuperLxc() {
//        lxcSuperLxcService.getAliveLxcSuperLxcList();
//    }


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


