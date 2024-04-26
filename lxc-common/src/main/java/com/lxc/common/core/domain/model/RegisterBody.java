package com.lxc.common.core.domain.model;

import com.lxc.common.constant.UserConstants;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 用户注册对象
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
public class RegisterBody {

//    private String userType;

    /**
     * 用户名
     */
    @NotBlank(message = "{user.username.not.blank}")
    @Length(min = UserConstants.USERNAME_MIN_LENGTH, max = UserConstants.USERNAME_MAX_LENGTH, message = "{user.username.length.valid}")
    @Pattern(regexp = UserConstants.USERNAME_PATTERN, message = "用户名只能以大小写字母开头,且只能包含字母、数字、下划线")
    private String username;

    /**
     * 用户密码
     */
    @NotBlank(message = "{user.password.not.blank}")
    @Length(min = UserConstants.PASSWORD_MIN_LENGTH, max = UserConstants.PASSWORD_MAX_LENGTH, message = "{user.password.length.valid}")
    @Pattern(regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9._!$&@]{5,20}$", message = "必须包含大小写字母和数字的组合，可以使用特殊字符. & _ ! $ @")
    private String password;
    /**
     * tgid
     */
    @NotBlank(message = "TGID不能为空")
    @Pattern(regexp = "^[0-9]+$", message = "TGID必须为数字")
    private String tgId;


    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;


    /**
     * 注册码
     */
    private String invitationCode;

    /**
     * 唯一标识
     */
    private String uuid;

}
