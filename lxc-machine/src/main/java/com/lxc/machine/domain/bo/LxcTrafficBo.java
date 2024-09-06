package com.lxc.machine.domain.bo;

import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 小鸡流量统计业务对象 lxc_traffic
 *
 * @author lxc
 * @date 2022-11-28
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class LxcTrafficBo extends BaseEntity {

    /**
     * lxc_lxc_and_traffic小鸡表id
     */
    @NotNull(message = "lxc_lxc_and_traffic小鸡表id不能为空", groups = {EditGroup.class})
    private Long lxcId;

    /**
     * 小鸡接收流量
     */
    @NotBlank(message = "小鸡接收流量不能为空", groups = {AddGroup.class,EditGroup.class})
    private String received;

    /**
     * 小鸡发送流量
     */
    @NotBlank(message = "小鸡发送流量不能为空", groups = {AddGroup.class,EditGroup.class})
    private String sent;


    /**
     * 创建时间
     */
    @NotBlank(message = "创建时间不能为空", groups = {AddGroup.class,EditGroup.class})
    private Date createTime;
}
