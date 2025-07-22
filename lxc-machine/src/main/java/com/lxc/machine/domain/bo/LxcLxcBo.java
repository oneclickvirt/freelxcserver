package com.lxc.machine.domain.bo;

import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 小鸡信息业务对象 lxc_lxc
 *
 * @author lxc
 * @date 2022-10-11
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class LxcLxcBo extends BaseEntity {

    /**
     *
     */
    @NotNull(message = "不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 母鸡id
     */
    private Long lxcSuperId;

    /**
     * 小鸡名称
     */
    @NotBlank(message = "小鸡名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * 小鸡标签
     */
    @NotBlank(message = "小鸡标签不能为空", groups = {AddGroup.class, EditGroup.class})
    private String tag;

    /**
     * 母鸡IP
     */
    @NotBlank(message = "母鸡IP不能为空", groups = {AddGroup.class, EditGroup.class})
    private String superIp;

    /**
     * 容器所用镜像
     */
    private String mirrorImage;

    /**
     * IPV6地址
     */
    private String ipv6Addr;

    /**
     * 子级标签
     */
    @NotBlank(message = "子级标签不能为空", groups = {AddGroup.class, EditGroup.class})
    private String childName;

    /**
     * ssh端口
     */
    @NotBlank(message = "ssh端口不能为空", groups = { AddGroup.class, EditGroup.class })
    private String sshPort;

    /**
     * 小鸡密码
     */
    @NotBlank(message = "小鸡密码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String password;

    /**
     * 开始端口
     */
    @NotBlank(message = "开始端口不能为空", groups = { AddGroup.class, EditGroup.class })
    private String portStart;

    /**
     * 结束端口
     */
    @NotBlank(message = "结束端口不能为空", groups = { AddGroup.class, EditGroup.class })
    private String portEnd;

    /**
     *
     */
//    @NotBlank(message = "不能为空", groups = { AddGroup.class, EditGroup.class })
    private String superDate;

    /**
     *
     */
//    @NotBlank(message = "不能为空", groups = {AddGroup.class,EditGroup.class})
    private String superDead;

    /**
     * 小鸡使用状态 1是占用 0是空闲
     */
    @NotNull(message = "小鸡使用状态 1是占用 0是空闲不能为空", groups = {AddGroup.class,EditGroup.class})
    private Integer state;


    /**
     * 小鸡状态
     */
    @NotNull(message = "小鸡状态不能为空", groups = {AddGroup.class,EditGroup.class})
    private String lxcStatus;

    /**
     * 小鸡累积使用流量
     */
    private BigDecimal lxcTraffic;

    /**
     * 网卡
     */
    private String hostInterface;

}
