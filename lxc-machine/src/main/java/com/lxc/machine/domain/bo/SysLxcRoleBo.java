package com.lxc.machine.domain.bo;

import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.validate.EditGroup;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小鸡和角色关联业务对象 sys_lxc_role
 *
 * @author lxc
 * @date 2022-11-16
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class SysLxcRoleBo extends BaseEntity {

    /**
     * 小鸡ID
     */
    @NotNull(message = "小鸡ID不能为空", groups = {EditGroup.class})
    private Long lxcId;

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空", groups = { EditGroup.class })
    private Long roleId;

    /**
     * 是否删除
     */
    @NotNull(message = "是否删除不能为空", groups = { EditGroup.class })
    private Integer isDeleted;
}
