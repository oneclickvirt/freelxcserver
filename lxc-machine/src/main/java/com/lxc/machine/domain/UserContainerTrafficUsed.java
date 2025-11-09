package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_container_traffic_used")
public class UserContainerTrafficUsed {
    private Long id;
    //    用户TG ID
    private String tgId;
    //    宿主机ID
    private Long superId;
    //    容器ID
    private Long containerId;
    //    容器名称
    private String containerName;
    //    接收流量（字节）
    private Long trafficIn;
    //    发送流量（字节）
    private Long trafficOut;
    //    累计流量（进+出，字节）
    private Long totalUsed;
    //    上次采样时间
    private Date lastSampled;

}
