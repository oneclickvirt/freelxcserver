package com.lxc.machine.domain.vo;

import lombok.Data;

@Data
public class LxcContainerMetricsDailyVo {
    private Long id;
    private String tgId;
    private String containerId;
    private String containerName;
    private int hourstamp;
    private Long netIn;
    private Long netOut;
    private String DateTag;
}
