package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lxc.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 邀请码对象 lxc_invitation
 *
 * @author lxc
 * @date 2023-10-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lxc_invitation")
public class LxcInvitation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * TG的个人ID
     */
    private String tgId;
    /**
     * 邀请码
     */
    @TableId(value = "invitation_code")
    private String invitationCode;

}
