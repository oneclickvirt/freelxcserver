package com.lxc.machine.domain.bo;

import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import com.lxc.common.core.validate.lxc.LxcAddGroup;
import com.lxc.common.core.validate.lxc.LxcEditGroup;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小鸡和用户关联表业务对象 lxc_queue
 *
 * @author lxc
 * @date 2022-10-11
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class LxcQueueBo extends BaseEntity {

    /**
     *
     */
    @NotNull(message = "不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 小鸡ID
     */
    @NotNull(message = "小鸡ID不能为空", groups = { LxcEditGroup.class, LxcAddGroup.class,AddGroup.class, EditGroup.class })
    private Long lxcId;

    /**
     * 母鸡名称
     */
    @NotBlank(message = "母鸡名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String superName;

    /**
     * 用户TG的ID
     */
    @NotBlank(message = "用户TG的ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String tgId;

    /**
     * 小鸡IP地址
     */
    @NotBlank(message = "小鸡IP地址不能为空", groups = { AddGroup.class, EditGroup.class })
    private String ip;

    /**
     * ssh端口
     */
    @NotBlank(message = "ssh端口不能为空", groups = { AddGroup.class, EditGroup.class })
    private String sshPort;

    /**
     * 结束时间
     */
    @NotBlank(message = "结束时间不能为空", groups = {AddGroup.class,EditGroup.class})
    private String endTime;

    /**
     * 执行类型
     */
    @NotBlank(message = "执行类型不能为空", groups = {LxcEditGroup.class})
    String modifyType;

    /**
     * 母鸡Id
     */
    Long superLxcId;
}
