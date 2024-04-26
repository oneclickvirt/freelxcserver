package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.List;



/**
 * 小鸡和用户关联表视图对象 lxc_queue
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@ExcelIgnoreUnannotated
public class LxcQueueVo {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 小鸡ID
     */
    @ExcelProperty(value = "小鸡ID")
    private Long lxcId;

    /**
     * 母鸡名称
     */
    @ExcelProperty(value = "母鸡名称")
    private String superName;

    /**
     * 用户TG的ID
     */
    @ExcelProperty(value = "用户TG的ID")
    private String tgId;

    /**
     * 小鸡IP地址
     */
    @ExcelProperty(value = "小鸡IP地址")
    private String ip;

    /**
     * ssh端口
     */
    @ExcelProperty(value = "ssh端口")
    private String sshPort;

    /**
     * 结束时间
     */
    @ExcelProperty(value = "结束时间")
    private String endTime;


    private String portStart;
    private String password;
    private String portEnd;
    /**
     * 小鸡名称
     */
    private String childName;

    /**
     * IPV6
     */
    private String ipv6Addr;

    /**
     * 小鸡标签流量使用
     */
    private List<String> netUsage;

}
