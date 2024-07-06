package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lxc.common.annotation.ExcelDictFormat;
import com.lxc.common.convert.ExcelDictConvert;
import lombok.Data;

import java.io.Serializable;


/**
 * 母鸡信息视图对象 lxc_super_lxc
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@ExcelIgnoreUnannotated
public class LxcSuperLxcVo implements Serializable {

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
     * 追加操作容器方式
     */
//    @ExcelProperty(value = "操作方式")
//    private String way;

    /**
     * 母鸡IP
     */
    @ExcelProperty(value = "母鸡IP")
    private String ip;

    /**
     * 母鸡密码
     */
    @ExcelProperty(value = "母鸡密码")
    private String password;

    /**
     * 母鸡端口
     */
    @ExcelProperty(value = "母鸡端口")
    private String sshPort;

    /**
     * 母鸡创建时间
     */
    @ExcelProperty(value = "母鸡创建时间")
    private String date;

    /**
     * 母鸡过期时间
     */
    @ExcelProperty(value = "母鸡过期时间")
    private String dead;

    /**
     * 可用状态
     */
    @ExcelProperty(value = "可用状态")
    private String superStatus;

    /**
     * ssh起始端口
     */
    @ExcelProperty(value = "ssh起始端口")
    private Long sshPortStart;

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
     * 最大可开小鸡数量
     */
    @ExcelProperty(value = "最大可开小鸡数量")
    private Long maxQuantity;

    /**
     * 到期标志（0代表未到期 1代表到期）
     */
    @ExcelProperty(value = "到期标志", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String expireFlag;

    /**
     * 小鸡名称前缀
     */
    @ExcelProperty(value = "小鸡名称前缀")
    private String prefix;

    /**
     * 外网起始端口
     */
    @ExcelProperty(value = "外网起始端口")
    private Long netStartPort;
    /**
     * 外网分配端口数
     */
    @ExcelProperty(value = "外网分配端口数")
    private Long netStep;

    /**
     * 上行速度限制,单位Mbps
     */
    @ExcelProperty(value = "上行速度限制")
    private Long upLimit;

    /**
     * 下行速度限制,单位Mbps
     */
    @ExcelProperty(value = "下行速度限制")
    private Long downLimit;

    /**
     * 是否启用IPV6(Y or N)
     */
    @ExcelProperty(value = "是否启用IPV6", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_yes_no")
    private String ipv6Flag;

    /**
     * 角色组
     */
    private Long[] roleIds;

    /**
     * 剩余可开小鸡数量
     */
    @ExcelProperty(value = "剩余可开小鸡数量")
    private Long freeQuantity;

    /**
     * 已开小鸡数量
     */
    @ExcelProperty(value = "已开小鸡数量")
    private Long usedQuantity;

    /**
     * 已领取小鸡数量
     */
    @ExcelProperty(value = "已领取小鸡数量")
    private Long receivedQuantity;

    /**
     * 磁盘限制
     */
    @ExcelProperty(value = "磁盘限制模板")
    private String rootLimits;

    /**
     * instanceConfig org.relxd.lxd.builders.InstanceConfigBuilder
     */
    @ExcelProperty(value = "实例模板")
    private String instanceConfig;


    /**
     * 镜像配置
     */
    @ExcelProperty(value = "镜像配置模板")
    private String sourceConfig;
}
