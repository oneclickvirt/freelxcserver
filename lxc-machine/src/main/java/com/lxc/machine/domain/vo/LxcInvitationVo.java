package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;


/**
 * 邀请码视图对象 lxc_invitation
 *
 * @author lxc
 * @date 2023-10-16
 */
@Data
@ExcelIgnoreUnannotated
public class LxcInvitationVo {

    private static final long serialVersionUID = 1L;

    /**
     * TG的个人ID
     */
    @ExcelProperty(value = "TG的个人ID")
    private String tgId;

    /**
     * 邀请码
     */
    @ExcelProperty(value = "邀请码")
    private String invitationCode;


}
