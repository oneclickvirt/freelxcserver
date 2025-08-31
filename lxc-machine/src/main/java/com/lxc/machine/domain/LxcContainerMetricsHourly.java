package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lxc_container_metrics_hourly")
public class LxcContainerMetricsHourly {
    //    主键
    @TableId
    private Long id;
    //    所属用户
    private String tgId;
    //    容器唯一 ID（非容器名）
    private String containerId;
    //    容器名
    private String containerName;
    //    小时维度时间戳
    private int hourstamp;
    //    进流量增量（字节）
    private Long netIn;
    //    出流量增量（字节）
    private Long netOut;
    //    格式为 2025-07-01 等
    private String DateTag;
}
