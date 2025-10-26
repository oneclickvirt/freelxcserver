package com.lxc.machine.domain.bo;

import lombok.Data;

@Data
public class LxcContainerStateBo {
    /**
     * tgid
     */
    String tgId;
    /**
     * 容器唯一 ID（非容器名）
     */
    String containerId;
    /**
     * 查询的母鸡ip
     */
    String superIp;
    /**
     * 实例名称
     */
    String instancename;
//    /**
//     * cpu使用情况
//     */
//    Double cpuUsage;
//    /**
//     * 内存使用情况
//     */
//    Double memory;
//    /**
//     * 磁盘使用情况
//     */
//    Double disk;
//    /**
//     * 截止当前小时（每小时统计一次），网络流量使用量
//     */
////    Double network;
//    /**
//     * 时间戳，纳秒级
//     */
//    Long timestamp;
}
