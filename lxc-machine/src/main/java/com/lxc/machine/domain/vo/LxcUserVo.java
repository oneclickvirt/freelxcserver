package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lxc.common.annotation.ExcelDictFormat;
import com.lxc.common.convert.ExcelDictConvert;
import lombok.Data;
import java.util.Date;



/**
 * lxc用户信息视图对象 lxc_user
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@ExcelIgnoreUnannotated
public class LxcUserVo {

    private static final long serialVersionUID = 1L;

    /**
     * 站内UID
     */
    @ExcelProperty(value = "站内UID")
    private Long id;

    /**
     * TG的个人ID
     */
    @ExcelProperty(value = "TG的个人ID")
    private String tgId;

    /**
     * 加密密码
     */
    @ExcelProperty(value = "加密密码")
    private String password;

    /**
     * 用户名称-TG
     */
    @ExcelProperty(value = "用户名称-TG")
    private String name;

    /**
     * 虚拟机配额
     */
    @ExcelProperty(value = "虚拟机配额")
    private Long lxcQuota;

    /**
     * 会员属性
     */
    @ExcelProperty(value = "会员属性")
    private Long vipType;


}
