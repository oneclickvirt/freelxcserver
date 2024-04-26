package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 小鸡流量统计对象 lxc_traffic
 *
 * @author lxc
 * @date 2022-11-28
 */
@Data
@TableName("lxc_traffic")
public class LxcTraffic {

    private static final long serialVersionUID = 1L;

    /**
     * lxc_lxc小鸡表id
     */
    @TableId(value = "lxc_id")
    private Long lxcId;

    /**
     * 小鸡接收流量
     */
    private String received;

    /**
     * 小鸡发送流量
     */
    private String sent;

    /**
     * 创建时间
     */
    private Date createTime;

}
