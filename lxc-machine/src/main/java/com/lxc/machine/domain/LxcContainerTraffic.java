package com.lxc.machine.domain;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lxc.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

@Data
@TableName("lxc_container_traffic")
public class LxcContainerTraffic extends BaseEntity {
    private static final long serialVersionUID=1L;

    @TableId
    private Long id;

    private String containerName;

    private Long sent;

    private Long recv;

    private DateTime timestamp;

    private Date yearMonth;

    private enum Type {
        HOURLY,
        DAILY,
        MONTHLY
    }
}
