package com.lxc.machine.domain.bo;

import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 申请小鸡业务对象 lxc_lxc
 *
 * @author lxc
 * @date 2023-10-1
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class LxcApplyBo extends BaseEntity {

    /**
     * 宿主机ID
     */
    @NotBlank(message = "宿主机不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long lxcSuperId;

    /**
     * 镜像模版
     */
    private String mirrorImage;

}
