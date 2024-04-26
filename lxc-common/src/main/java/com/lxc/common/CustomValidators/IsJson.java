package com.lxc.common.CustomValidators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * description: 自定义校验注解，需指定校验器，可以指定多个，校验字符串是否是json
 */
@Documented
@Constraint(validatedBy = {IsJsonConstraintValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsJson {

    String message() default "不是一个合法的JSON字符串";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

