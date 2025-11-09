package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lxc.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小鸡和用户关联表对象 lxc_queue
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lxc_queue")
public class LxcQueue extends BaseEntity {
    private static final long serialVersionUID=1L;
    @TableId(value = "id")
    private Long id;
    /**
     * 小鸡ID
     */
    private Long lxcId;
    /**
     * 母鸡名称
     */
    private String superName;
    /**
     * 用户TG的ID
     */
    private String tgId;
    /**
     * 小鸡IP地址
     */
    private String ip;
    /**
     * ssh端口
     */
    private String sshPort;
    /**
     * 结束时间
     */
    private String endTime;

    @TableField(exist = false)
    private String superTag;
    /**
     * 小鸡名称
     */
    @TableField(exist = false)
    private String childName;
    /**
     * 小鸡状态
     */
    @TableField(exist = false)
    private String lxcStatus;
    /**
     * 逻辑删除（0代表存在 1代表删除）
     */
     private int isDeleted;
}
