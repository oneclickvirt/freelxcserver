package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.domain.entity.SysRole;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 小鸡信息对象 lxc_lxc
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lxc_lxc_and_traffic")
@Accessors(chain = true)
public class LxcLxc extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * @TableTd 设置type，默认为IdType.ASSIGN_ID，主键类型为number或String的雪花算法；只有当插入对象ID 为空，才自动填充
     * 在某些情况下，我们想提前获取这个ID，调用com.baomidou.mybatisplus.core.toolkit.IdWorker.getId()方法即可
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 母鸡名称
     */
    private String name;
    /**
     * 母鸡标签
     */
    private String tag;
    /**
     * 母鸡IP
     */
    private String superIp;
    /**
     * 容器所用镜像
     */
    private String mirrorImage;
    /**
     * 小鸡名称
     */
    private String childName;
    /**
     * ssh端口
     */
    private String sshPort;
    /**
     * 小鸡密码
     */
    private String password;
    /**
     * 开始端口
     */
    private String portStart;
    /**
     * 结束端口
     */
    private String portEnd;
    /**
     * 创建时间
     */
    private String superDate;
    /**
     * 过期时间
     */
    private String superDead;

    /**
     * 小鸡使用状态 1是占用 0是空闲
     */
    private Integer state;

    /**
     * 小鸡状态
     */
    private String lxcStatus;

    /**
     * 小鸡最近更新的状态
     */
    private String lastUpdatedStatus;

    /**
     * 小鸡累积使用流量
     */
    private BigDecimal lxcTraffic;

    /**
     * 网卡
     */
    private String hostInterface;

    /**
     * IPV6地址
     */
    private String ipv6Addr;

    @TableField(exist = false)
    private Integer count;

    /**
     * 关联的角色对象
     */
    @TableField(exist = false)
    private List<SysLxcRole> lxcRoles;

    /**
     * 角色对象
     */
    @TableField(exist = false)
    private List<SysRole> roles;

}
