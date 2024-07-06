package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;


/**
 * 母鸡和角色关联视图对象 sys_super_role
 *
 * @author lxc
 * @date 2022-11-16
 */
@Data
@ExcelIgnoreUnannotated
public class SysSuperRoleVo {

    private static final long serialVersionUID = 1L;

    /**
     * 母鸡ID
     */
    @ExcelProperty(value = "母鸡ID")
    private Long superId;

    /**
     * 角色ID
     */
    @ExcelProperty(value = "角色ID")
    private Long roleId;


}
