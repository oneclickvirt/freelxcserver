package com.lxc.machine.domain.bo;

import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 邀请码业务对象 lxc_invitation
 *
 * @author lxc
 * @date 2023-10-16
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class LxcInvitationBo extends BaseEntity {

    /**
     * TG的个人ID
     */
    private String tgId;

    /**
     * 邀请码
     */
    private String invitationCode;

    @NotNull(message = "生成数量不能为空", groups = {AddGroup.class, EditGroup.class})
    @Min(value = 1, message = "生成数量不能小于1", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 999, message = "生成数量不能大于999", groups = {AddGroup.class, EditGroup.class})
    private Integer count;

    /**
     * 是否已使用
     */
    private String used;

}
