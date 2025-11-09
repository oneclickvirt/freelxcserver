package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("traffic_stats_by_time")
public class TrafficStatsByTime {
    private Long id;
    // 宿主机id
    private Long superId;
    //    容器ID
    private Long containerId;
    //    容器名称
    private String containerName;
    //    接收字节数
    private Long rxBytes;
    //    发送字节数
    private Long txBytes;
    //    年份
    private Integer year;
    //    月份
    private Integer month;
    //    日期
    private Integer day;
    //    小时
    private Integer hour;

}
