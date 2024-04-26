package com.lxc.common.core.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShellDto {

    private String ip;
    private Integer sshPort;
    private String username;
    private String password;
    private String command;
    private String result;
    private String modifyType;
//    小鸡名称 内存大小 硬盘大小 SSH端口 外网起端口 外网止端口 上行带宽 下行带宽
    /**
     * 小鸡名称
     */
    private String childName;
    /**
     * ssh起始端口
     */
    private Long childSshPort;
    /**
     * 内存限制，默认256MB
     */
    private Long memoryLimit;
    /**
     * 硬盘限制，默认1G
     */
    private Long diskLimit;
    /**
     * 外网开始端口
     */
    private Integer portStart;
    /**
     * 外网结束端口
     */
    private Integer portEnd;
    /**
     * 上行速度限制
     */
    private Long upLimit;
    /**
     * 下行速度限制
     */
    private Long downLimit;
    /**
     * 是否启用IPV6(Y or N)
     */
    private String ipv6Flag;
}
