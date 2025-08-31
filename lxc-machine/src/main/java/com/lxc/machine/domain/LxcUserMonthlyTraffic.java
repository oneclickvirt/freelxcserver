package com.lxc.machine.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("lxc_user_monthly_traffic")
public class LxcUserMonthlyTraffic {
    //    用户TG ID
    private String tgId;
    //    月份标记，如 2025-07
    private String monthTag;
    //    累计流量（进+出，字节）
    private Long trafficUsed;
    //    限额（字节）
    private Long trafficLimit;
    //    normal / exceeded
    private String status;
    //    首次被冻结时间（避免重复冻结）
    private Date frozenAt;
    //    是否已冻结容器
    private Integer isFrozen;
}
