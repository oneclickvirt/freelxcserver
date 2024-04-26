package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.domain.entity.SysRole;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 小鸡信息对象 lxc_lxc
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lxc_lxc")
@Accessors(chain = true)
public class LxcLxc extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     *
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
