package com.lxc.machine.domain;

import lombok.Data;

@Data
public class LxcContainerState {
    /**
     * 实例名称
     */
    String instancename;
    /**
     * cpu使用情况
     */
    Double cpu;
    /**
     * 内存使用情况
     */
    Double memory;
    /**
     * 磁盘使用情况
     */
    Double disk;
    /**
     * 网络流量接收量
     */
    Double recv;
    /**
     * 网络流量发送量
     */
    Double send;
    /**
     * 时间戳，纳秒级
     */
    Long timestamp;
}
