package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;


/**
 * 母鸡信息视图对象 lxc_super_lxc
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@ExcelIgnoreUnannotated
public class LxcCartVo {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 母鸡名称
     */
    @ExcelProperty(value = "母鸡名称")
    private String name;

    /**
     * 母鸡标签
     */
    @ExcelProperty(value = "母鸡标签")
    private String tag;

    /**
     * 母鸡过期时间
     */
    @ExcelProperty(value = "母鸡过期时间")
    private String dead;

    /**
     * 内存限制，默认256m
     */
    @ExcelProperty(value = "内存限制，默认256MB")
    private Long memoryLimit;

    /**
     * 硬盘限制，默认1G
     */
    @ExcelProperty(value = "硬盘限制，默认1G")
    private Long diskLimit;

    /**
     * 是否支持IPV6
     */
    @ExcelProperty(value = "是否支持IPV6")
    private String ipv6Flag;

    /**
     * 镜像模板
     */
    @ExcelProperty(value = "镜像模板")
    private String sourceConfig;
}
