package com.lxc.machine.domain.bo;

import com.lxc.common.CustomValidators.IsJson;
import com.lxc.common.core.domain.BaseEntity;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 母鸡信息业务对象 lxc_super_lxc
 *
 * @author lxc
 * @date 2022-10-11
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class LxcSuperLxcBo extends BaseEntity {

    /**
     *
     */
    @NotNull(message = "不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 小鸡名称
     */
    @NotBlank(message = "小鸡名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * 小鸡标签
     */
    @NotBlank(message = "小鸡标签不能为空", groups = { AddGroup.class, EditGroup.class })
    private String tag;

    /**
     * 母鸡IP
     */
    @NotBlank(message = "母鸡IP不能为空", groups = { AddGroup.class, EditGroup.class })
    private String ip;

    /**
     * 母鸡密码
     */
    @NotBlank(message = "母鸡密码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String password;

    /**
     * 母鸡操作容器方式
     */
//    @NotBlank(message = "母鸡操作容器不能为空", groups = { AddGroup.class, EditGroup.class })
    private String way;

    /**
     * 母鸡端口
     */
    @NotBlank(message = "母鸡端口不能为空", groups = { AddGroup.class, EditGroup.class })
    private String sshPort;

    /**
     * 母鸡创建时间
     */
    @NotBlank(message = "母鸡创建时间", groups = {AddGroup.class,EditGroup.class})
    private String date;

    /**
     * 母鸡过期时间
     */
    @NotBlank(message = "母鸡过期时间", groups = {AddGroup.class,EditGroup.class})
    private String dead;

    /**
     * 是否可开小鸡
     */
    @NotBlank(message = "是否可开小鸡", groups = {AddGroup.class,EditGroup.class})
    private String superStatus;

    /**
     * ssh起始端口
     */
    @NotNull(message = "ssh起始端口不能为空", groups = {AddGroup.class,EditGroup.class})
    private Long sshPortStart;

    /**
     * 内存限制，默认256MB
     */
    @NotNull(message = "内存限制不能为空", groups = {AddGroup.class,EditGroup.class})
    private Long memoryLimit;

    /**
     * 硬盘限制，默认1G
     */
    @NotNull(message = "硬盘限制不能为空", groups = {AddGroup.class,EditGroup.class})
    private Long diskLimit;

    /**
     * 最大可开小鸡数量
     */
    @NotNull(message = "最大可开小鸡数量不能为空", groups = {AddGroup.class,EditGroup.class})
    private Long maxQuantity;

    /**
     * 到期标志（0代表未到期 1代表到期）
     */
    @NotBlank(message = "到期标志（0代表未到期 1代表到期）不能为空", groups = {AddGroup.class,EditGroup.class})
    private String expireFlag;

    /**
     * 小鸡名称前缀
     */
    @NotBlank(message = "小鸡名称前缀不能为空", groups = {AddGroup.class,EditGroup.class})
    private String prefix;

    /**
     * 权限Ids
     */
    @NotNull(message = "权限不能为空", groups = {AddGroup.class,EditGroup.class})
    private List<Long> roleIds;

    /**
     * 外网起始端口
     */
    @NotNull(message = "外网起始端口不能为空", groups = {AddGroup.class,EditGroup.class})
    private Long netStartPort;

    /**
     * 外网分配端口数
     */
    @NotNull(message = "外网分配端口数不能为空", groups = {AddGroup.class,EditGroup.class})
    private Long netStep;

    /**
     * 上行速度限制,单位Mbps
     */
    @NotNull(message = "上行速度限制不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long upLimit;

    /**
     * 下行速度限制,单位Mbps
     */
    @NotNull(message = "下行速度限制不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long downLimit;

    /**
     * 是否启用IPV6(Y or N)
     */
    @NotBlank(message = "是否启用IPV6不能为空", groups = {AddGroup.class, EditGroup.class})
    private String ipv6Flag;


    /**
     * 磁盘限制
     */
    @IsJson(message = "磁盘限制模板json不合法", groups = {AddGroup.class, EditGroup.class})
    private String rootLimits;

    /**
     * instanceConfig org.relxd.lxd.builders.InstanceConfigBuilder
     */
    @IsJson(message = "实例模板json不合法", groups = {AddGroup.class, EditGroup.class})
    private String instanceConfig;


    /**
     * 镜像配置
     */
    @IsJson(message = "镜像配置模板json不合法", groups = {AddGroup.class, EditGroup.class})
    private String sourceConfig;
}
