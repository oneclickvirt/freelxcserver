package com.lxc.machine.domain.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LxcTextDto {

    @NotBlank
    String lxcText;

    @NotBlank
    String superIp;

    @NotBlank
    List<Long> roleIds;
}
