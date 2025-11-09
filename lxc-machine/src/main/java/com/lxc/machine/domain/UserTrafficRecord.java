package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_traffic_record")
public class UserTrafficRecord {
    private Long id;
    //    用户TG ID
    private String tgId;
    //    累计流量（进+出，字节）
    private Long usedTraffic;
    //    限额（字节）
    private Long limitTraffic;
    //    是否超出限额
    private Boolean isTrafficLimited;
    //    重置时间，默认下一个月1号0点
    private Date resetTime;
}
