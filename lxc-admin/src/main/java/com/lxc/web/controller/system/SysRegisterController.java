package com.lxc.web.controller.system;

import cn.dev33.satoken.annotation.SaIgnore;
import com.lxc.common.annotation.RateLimiter;
import com.lxc.common.core.controller.BaseController;
import com.lxc.common.core.domain.R;
import com.lxc.common.core.domain.model.RegisterBody;
import com.lxc.common.utils.ServletUtils;
import com.lxc.machine.service.ILxcInvitationService;
import com.lxc.system.service.ISysConfigService;
import com.lxc.system.service.SysRegisterService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册验证
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
public class SysRegisterController extends BaseController {

    private final SysRegisterService registerService;
    private final ISysConfigService configService;
    private final ILxcInvitationService lxcInvitationService;

    /**
     * 用户注册
     */
    @SaIgnore
    @RateLimiter(time = 10, count = 3)
    @PostMapping("/register")
    public R<Void> register(@Validated @RequestBody RegisterBody user) {
        String invitationCode = user.getInvitationCode();
        boolean empty = StringUtils.isBlank(invitationCode);
        if (empty && StringUtils.length(invitationCode) < 15 && !("true".equals(configService.selectConfigByKey("sys.account.registerUser")))) {
            return R.fail("当前系统没有开启注册功能！");
        }
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        // 验证码开关
        if (captchaEnabled) {
            registerService.validateCaptcha(user.getUsername(), user.getCode(), user.getUuid(), ServletUtils.getRequest());
        }
        if (empty) {
            registerService.register(user);
        } else {
            synchronized (invitationCode) {
                Boolean aBoolean = lxcInvitationService.checkInvitationCode(invitationCode);
                if (!aBoolean) {
                    return R.fail("邀请码错误！");
                }
                registerService.register(user);
            }
        }
        return R.ok();
    }
}
