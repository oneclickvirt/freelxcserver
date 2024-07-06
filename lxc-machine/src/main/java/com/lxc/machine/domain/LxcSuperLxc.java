package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lxc.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 母鸡信息对象 lxc_super_lxc
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lxc_super_lxc")
public class LxcSuperLxc extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     *
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 小鸡名称
     */
    private String name;
    /**
     * 母鸡标签
     */
    private String tag;
    /**
     * 追加操作容器方式
     */
//    private String way;
    /**
     * 母鸡IP
     */
    private String ip;
    /**
     * 母鸡密码
     */
    private String password;
    /**
     * 母鸡端口
     */
    private String sshPort;
    /**
     * 母鸡创建时间
     */
    private String date;
    /**
     * 母鸡过期时间
     */
    private String dead;
    /**
     * 可用状态
     */
    private String superStatus;
    /**
     * ssh起始端口
     */
    private Long sshPortStart;
    /**
     * 内存限制，默认256MB
     */
    private Long memoryLimit;
    /**
     * 硬盘限制，默认1G
     */
    private Long diskLimit;
    /**
     * 最大可开小鸡数量
     */
    private Long maxQuantity;
    /**
     * 到期标志（0代表未到期 1代表到期）
     */
    private String expireFlag;
    /**
     * 小鸡名称前缀
     */
    private String prefix;
    /**
     * 外网起始端口
     */
    private Long netStartPort;
    /**
     * 外网分配端口数
     */
    private Long netStep;

    /**
     * 上行速度限制,单位Mbps
     */
    private Long upLimit;

    /**
     * 下行速度限制,单位Mbps
     */
    private Long downLimit;

    /**
     * 是否启用IPV6(Y or N)
     */
    private String ipv6Flag;

    /**
     * 磁盘限制
     */
    private String rootLimits;

    /**
     * instanceConfig org.relxd.lxd.builders.InstanceConfigBuilder
     */
    private String instanceConfig;


    /**
     * 镜像配置
     */
    private String sourceConfig;
}
