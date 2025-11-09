package com.lxc.machine.domain.bo;

import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * lxc用户信息业务对象 lxc_user
 *
 * @author lxc
 * @date 2022-10-11
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class LxcUserBo extends BaseEntity {

    /**
     * 站内UID
     */
    @NotNull(message = "站内UID不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * TG的个人ID
     */
    @NotBlank(message = "TG的个人ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String tgId;

    /**
     * 加密密码
     */
    @NotBlank(message = "加密密码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String password;

    /**
     * 用户名称-TG
     */
    @NotBlank(message = "用户名称-TG不能为空", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * 虚拟机配额
     */
    @NotNull(message = "虚拟机配额不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long lxcQuota;

    /**
     * 会员属性
     */
    @NotNull(message = "会员属性不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long vipType;

    /**
     * 用户流量月度限额
     */
    @NotNull(message = "用户流量月度限额不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long limitTraffic;

}
