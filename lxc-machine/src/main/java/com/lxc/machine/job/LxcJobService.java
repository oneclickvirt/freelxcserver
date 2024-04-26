package com.lxc.machine.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lxc.common.constant.LxcConstants;
import com.lxc.common.core.domain.dto.ShellDto;
import com.lxc.common.enums.LxcStatus;
import com.lxc.common.utils.BeanCopyUtils;
import com.lxc.common.utils.DateUtils;
import com.lxc.common.utils.ShellUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcLxcBo;
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcSuperLxcService;
import com.lxc.machine.service.impl.LxcQueueServiceImpl;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.relxd.lxd.ApiException;
import org.relxd.lxd.api.InstancesApi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
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
        lxcPendingList.stream()
            .collect(Collectors.groupingBy(LxcLxcVo::getSuperIp))
            .forEach(
                (superIp, lxcVoList) -> {
                    long pendingCount = lxcService.count(
                        new QueryWrapper<LxcLxc>()
                            .eq("super_ip", superIp)
                            .and(i -> i.eq("lxc_status", LxcStatus.PENDING.getCode()).or().eq("lxc_status", LxcStatus.FAILED_TO_CREATE.getCode()))
                    );
                    // 一次只能创建一个小鸡
                    if (pendingCount == 0) {
                        int size = lxcVoList.size();
                        int index = -1;
                        for (int i = 0; i < size; i++) {
                            try {
                                lxcApi.of(superIp).getInstancesByNameState(lxcVoList.get(i).getChildName(), null, null);
                                index = i;
                                break;
                            } catch (ApiException e) {
                                XxlJobHelper.log("小鸡{}还在初始化中，检查下一个", lxcVoList.get(0).getChildName());
                            }
                        }
                        if (index == -1) {
                            return;
                        }
                        LxcLxcVo lxc = lxcVoList.get(index);
                        try {
                            XxlJobHelper.log("开始初始化小鸡：{}", lxc.getChildName());
                            LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new QueryWrapper<LxcSuperLxc>().eq("ip", superIp));
                            assert Objects.nonNull(superLxc);
                            LxcSuperLxcVo superLxcVo = BeanCopyUtils.copy(superLxc, LxcSuperLxcVo.class);
                            ShellDto shellDto = LxcQueueServiceImpl.prepareCreateShellDto(superLxcVo, lxc);
                            XxlJobHelper.log("小鸡创建信息：{}", shellDto);
                            ShellUtils.Shell shell = ShellUtils.initLxc(shellDto);
                            XxlJobHelper.log("小鸡{}初始化命令：{}", lxc.getChildName(), shell.getCmd());
                            LxcLxcBo temp = new LxcLxcBo();
                            temp.setLxcStatus(LxcStatus.PENDING.getCode());
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
     */
    @XxlJob("createLxc")
    public void createLxc() {
        List<LxcLxcVo> lxcPendingList = lxcService.getLxcList(
            new QueryWrapper<LxcLxc>().eq("lxc_status", LxcStatus.WAITING_FOR_PROCESS.getCode())
                .orderBy(true, true, "create_time")
        );
        XxlJobHelper.log("处于未创建状态的小鸡有：{}个", lxcPendingList.size());
        lxcPendingList.stream()
            .collect(Collectors.groupingBy(LxcLxcVo::getSuperIp))
            .forEach(
                (k, lxcVoList) -> {
                    LxcLxcVo lxc = lxcVoList.get(0);
                    long pendingCount = lxcService.count(
                        new QueryWrapper<LxcLxc>()
                            .eq("super_ip", lxc.getSuperIp())
                            .and(i -> i.eq("lxc_status", LxcStatus.PENDING.getCode()).or().eq("lxc_status", LxcStatus.FAILED_TO_CREATE.getCode()))
                    );
                    // 一次只能创建一个小鸡
                    if (pendingCount == 0) {
                        try {
                            XxlJobHelper.log("开始创建小鸡：{}", lxc.getChildName());
                            LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new QueryWrapper<LxcSuperLxc>().eq("ip", k));
                            LxcSuperLxcVo superLxcVo = BeanCopyUtils.copy(superLxc, LxcSuperLxcVo.class);
                            assert superLxcVo != null;
                            ShellDto shellDto = LxcQueueServiceImpl.prepareCreateShellDto(superLxcVo, lxc);
                            XxlJobHelper.log("小鸡创建信息：{}", shellDto);
                            String format = ShellUtils.createLxc(shellDto);
                            XxlJobHelper.log("小鸡{}创建命令：{}", lxc.getChildName(), format);
                            LxcLxcBo temp = new LxcLxcBo();
                            temp.setLxcStatus(LxcStatus.PENDING.getCode());
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
     * 定时判断小鸡是否创建成功
     */
    @XxlJob("checkLxcPending")
    public void checkLxcPending() {
        List<LxcLxcVo> lxcPendingList = lxcService.getLxcList(
            new QueryWrapper<LxcLxc>()
                .eq("lxc_status", LxcStatus.PENDING.getCode())
                .or()
                .eq("lxc_status", LxcStatus.FAILED_TO_CREATE.getCode())
                .orderBy(true, true, "create_time")
        );
        if (!lxcPendingList.isEmpty()) {
            XxlJobHelper.log("当前有{}个小鸡正在创建中", lxcPendingList.size());
            lxcPendingList.forEach(lxc -> {
                try {
                    String superIp = lxc.getSuperIp();
                    String childName = lxc.getChildName();
                    if (StringUtils.equals(lxc.getLxcStatus(), LxcStatus.FAILED_TO_CREATE.getCode())) {
                        XxlJobHelper.log("ID: {}  小鸡：{}处于创建失败状态,跳过", lxc.getId(), childName);
                        return;
                    }
                    LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new QueryWrapper<LxcSuperLxc>().eq("ip", superIp).last("LIMIT 1"));
                    ShellDto shellDto = ShellDto.builder()
                        .ip(superLxc.getIp())
                        .sshPort(Integer.valueOf(superLxc.getSshPort()))
                        .username("root")
                        .password(superLxc.getPassword())
                        .build();
                    String createCommand = String.format(ShellUtils.IF_PASSWD_FILE_EXIST, childName);
                    String hostInterfaceCommand = String.format(ShellUtils.GET_LXC_INTERFACE, childName);
                    List<String> out = ShellUtils.execCmdOutList(createCommand, ShellUtils.getSession(shellDto));
                    if (out.size() == 2 && StringUtils.equals(out.get(0), "true")) {
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
                        List<String> interfaceRes = ShellUtils.execCmdOutList(hostInterfaceCommand, ShellUtils.getSession(shellDto));
                        if (interfaceRes.size() == 1) {
                            String interfaceName = interfaceRes.get(0);
                            if (StringUtils.equals("Error: Instance not found", interfaceName)) {
                                XxlJobHelper.log("{}  {}获取网卡失败，原因：{}", lxc.getId(), childName, interfaceName);
                                return;
                            }
                            temp.setHostInterface(interfaceName);
                            lxcService.updateByBoForce(temp);
                            try {
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
}
