package com.lxc.machine.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class LxcUserMonthlyTrafficVo {
    private String tgId;
    private String monthTag;
    private Long trafficUsed;
    private Long trafficLimit;
    private String status;
    private Date frozenAt;
    private Integer isFrozen;
}
