package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lxc.common.annotation.ExcelDictFormat;
import com.lxc.common.convert.ExcelDictConvert;
import lombok.Data;

import java.util.Date;


/**
 * 小鸡信息视图对象 lxc_lxc
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@ExcelIgnoreUnannotated
public class LxcLxcVo {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 小鸡名称
     */
    @ExcelProperty(value = "小鸡名称")
    private String name;

    /**
     * 小鸡标签
     */
    @ExcelProperty(value = "小鸡标签")
    private String tag;

    /**
     * 母鸡IP
     */
    @ExcelProperty(value = "母鸡IP")
    private String superIp;

    /**
     * 子级标签
     */
    @ExcelProperty(value = "子级标签")
    private String childName;

    /**
     * ssh端口
     */
    @ExcelProperty(value = "ssh端口")
    private String sshPort;

    /**
     * 小鸡密码
     */
    @ExcelProperty(value = "小鸡密码")
    private String password;

    /**
     * 开始端口
     */
    @ExcelProperty(value = "开始端口")
    private String portStart;

    /**
     * 结束端口
     */
    @ExcelProperty(value = "结束端口")
    private String portEnd;

    /**
     *
     */
    @ExcelProperty(value = "")
    private String superDate;

    /**
     *
     */
    @ExcelProperty(value = "")
    private String superDead;

    /**
     * 小鸡使用状态 1是占用 0是空闲
     */
    @ExcelProperty(value = "小鸡使用状态 1是占用 0是空闲", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "lxc_state")
    private Integer state;

    /**
     * 小鸡状态
     */
    @ExcelProperty(value = "小鸡状态 1是占用 0是空闲", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "lxc_status")
    private String lxcStatus;

    /**
     * 网卡
     */
    @ExcelProperty(value = "网卡名称")
    private String hostInterface;

    /**
     * IPV6
     */
    @ExcelProperty(value = "IPV6")
    private String ipv6Addr;


    private Integer count;

    private LxcQueueVo lxcQueue;


    /**
     * 创建时间
     */
    private Date createTime;
}
