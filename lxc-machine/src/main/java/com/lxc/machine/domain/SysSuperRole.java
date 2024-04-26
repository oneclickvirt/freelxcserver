package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 母鸡和角色关联对象 sys_lxc_role
 *
 * @author lxc
 * @date 2022-11-16
 */
@Data
@TableName("sys_super_role")
public class SysSuperRole {

    private static final long serialVersionUID = 1L;

    /**
     * 母鸡ID
     */
    @TableId(type = IdType.INPUT)
    private Long superId;

    /**
     * 角色ID
     */
    private Long roleId;

}
