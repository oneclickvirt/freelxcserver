package com.lxc.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lxc.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * lxc用户信息对象 lxc_user
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lxc_user")
@Accessors(chain = true)
public class LxcUser extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 站内UID
     */
    @TableId(value = "id")
    private Long id;
    /**
     * TG的个人ID
     */
    private String tgId;
    /**
     * 加密密码
     */
    private String password;
    /**
     * 用户名称-TG
     */
    private String name;
    /**
     * 虚拟机配额
     */
    private Long lxcQuota;
    /**
     * 会员属性
     */
    private Long vipType;
    /**
     * 用户流量月度限额
     */
    private Long limitTraffic;
    /**
     * 剩余配额
     */
    @TableField(exist = false)
    private long surplusQuota;

    /**
     * 使用配额
     */
    @TableField(exist = false)
    private long usingQuota;

    /**
     * 管理员标识
     */
    @TableField(exist = false)
    private boolean isAdmin;


    /**
     * 最大配额角色
     */
    @TableField(exist = false)
    private String roleName;
}
