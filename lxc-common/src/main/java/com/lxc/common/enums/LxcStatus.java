package com.lxc.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 小鸡状态
 */
@Getter
@AllArgsConstructor
public enum LxcStatus {
    /**
     * 重建中
     */
    RESETING("0"),
    /**
     * 未创建
     */
    WAITING_FOR_PROCESS("1"),

    /**
     * 创建中
     */
    PENDING("2"),

    /**
     * 已开机
     */
    RUNNING("3"),
    /**
     * 已关机
     */
    STOPPING("4"),
    /**
     * 已销毁
     */
    DESTROYED("5"),
    /**
     * 创建失败，将被删除回收
     */
    FAILED_TO_CREATE("6"),
    /**
     * 已过期
     */
    EXPIRED("7"),
    /**
     * 流量超限
     */
    TRAFFIC_LIMIT("8"),
    /**
     * 已冻结
     */
    FROZEN("9");

    private final String code;
}
