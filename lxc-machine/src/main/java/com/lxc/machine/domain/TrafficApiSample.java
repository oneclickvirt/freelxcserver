package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("traffic_api_sample")
public class TrafficApiSample {
    private Long id;
    // 宿主机id
    private Long superId;
    // 容器ID
    private Long containerId;
    // 容器名称
    private String containerName;
    // 流入流量
    private Long rxBytes;
    // 流出流量
    private Long txBytes;
    // 上次采样的网络流量（字节）
    private Long lastSampledNetwork;
    // 是否运行中
    private Boolean isRunning;
    // 更新时间
    private Date updateTime;
}
