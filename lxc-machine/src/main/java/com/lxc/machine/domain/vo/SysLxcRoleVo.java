package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;



/**
 * 小鸡和角色关联视图对象 sys_lxc_role
 *
 * @author lxc
 * @date 2022-11-16
 */
@Data
@ExcelIgnoreUnannotated
public class SysLxcRoleVo {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long lxcId;

    /**
     * 角色ID
     */
    @ExcelProperty(value = "角色ID")
    private Long roleId;


}
