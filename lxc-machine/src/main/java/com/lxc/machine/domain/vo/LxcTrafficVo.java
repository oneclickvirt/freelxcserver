package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 小鸡流量统计视图对象 lxc_traffic
 *
 * @author lxc
 * @date 2022-11-28
 */
@Data
@ExcelIgnoreUnannotated
public class LxcTrafficVo {

    private static final long serialVersionUID = 1L;

    /**
     * lxc_lxc小鸡表id
     */
    @ExcelProperty(value = "lxc_lxc_and_traffic小鸡表id")
    private Long lxcId;

    /**
     * 小鸡接收流量
     */
    @ExcelProperty(value = "小鸡接收流量")
    private String received;

    /**
     * 小鸡发送流量
     */
    @ExcelProperty(value = "小鸡发送流量")
    private String sent;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
