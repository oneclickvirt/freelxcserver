package com.lxc.common.CustomValidators;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.io.IOException;

/**
 * description: 自定义注解校验器，需指定注解和校验类型，校验是否是 JSON 格式
 */
public class IsJsonConstraintValidator implements ConstraintValidator<IsJson, String> {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * description: 校验逻辑，是否校验成功，空值配合 @Null 或 @NotNull、@NotEmpty 校验
     *
     * @param value   被校验的值
     * @param context 上下文环境信息
     * @return boolean 校验结果
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return StringUtils.isEmpty(value) || isJsonValid(value);
    }

    public static boolean isJsonValid(String jsonInString) {
        try {
            OBJECT_MAPPER.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
