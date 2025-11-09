package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 小鸡和角色关联对象 sys_lxc_role
 *
 * @author lxc
 * @date 2022-11-16
 */
@Data
@TableName("sys_lxc_role")
public class SysLxcRole {

    private static final long serialVersionUID=1L;

    /**
     * 用户ID
     */
    @TableId(type = IdType.INPUT)
    private Long lxcId;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 逻辑删除：1-删除，0-存在
     */
    private int isDeleted;

}
