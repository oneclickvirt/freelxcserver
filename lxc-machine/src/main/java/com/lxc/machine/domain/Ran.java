package com.lxc.machine.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "ran")
@Validated
public class Ran {
//    ran-value: ${random.value}
//    ran-int: ${random.int}
//    ran-long: ${random.long}
//    ran-int-num: ${random.int(10)}
//    ran-int-range: ${random.int[10,20]}
//    ran-placeholder: placeholder_${ran.ran-value:}
    private Integer ranValue;
    private Integer ranInt;
    private Long ranLong;
    private Integer ranIntNum;
    private Integer ranIntRange;
    private String ranPlaceholder;
}
