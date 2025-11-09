package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("traffic_super_lxc_record")
public class TrafficSuperLxcRecord {
    private Long id;
    // 关联的用户TG ID
    private String tgId;
    // 宿主机id
    private Long superId;
    //    累计流量（进+出，字节）
    private Long usedTraffic;
    //    限额（字节）
    private Long maxLimit;
    //    是否超出限额
    private Integer isTrafficLimited;
}
