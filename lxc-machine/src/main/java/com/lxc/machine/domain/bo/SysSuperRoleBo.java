package com.lxc.machine.domain.bo;

import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.validate.EditGroup;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 母鸡和角色关联业务对象 sys_super_role
 *
 * @author lxc
 * @date 2022-11-16
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class SysSuperRoleBo extends BaseEntity implements Serializable {

    /**
     * 母鸡ID
     */
    @NotNull(message = "母鸡ID不能为空", groups = {EditGroup.class})
    private Long superId;

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空", groups = {EditGroup.class})
    private Long roleId;


}
