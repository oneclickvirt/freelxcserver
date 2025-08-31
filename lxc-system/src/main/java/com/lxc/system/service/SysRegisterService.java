package com.lxc.system.service;

import cn.dev33.satoken.secure.BCrypt;
import com.lxc.common.constant.CacheConstants;
import com.lxc.common.constant.Constants;
import com.lxc.common.constant.LxcConstants;
import com.lxc.common.constant.UserConstants;
import com.lxc.common.core.domain.entity.LxcUser;
import com.lxc.common.core.domain.entity.SysUser;
import com.lxc.common.core.domain.model.RegisterBody;
import com.lxc.common.core.service.LogininforService;
import com.lxc.common.enums.UserType;
import com.lxc.common.exception.user.CaptchaException;
import com.lxc.common.exception.user.CaptchaExpireException;
import com.lxc.common.exception.user.UserException;
import com.lxc.machine.utils.BotUtils;
import com.lxc.common.utils.MessageUtils;
import com.lxc.common.utils.ServletUtils;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.machine.service.ILxcInvitationService;
import com.lxc.machine.service.ILxcUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * 注册校验方法
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRegisterService {

    private final ISysUserService userService;

    private final ISysConfigService configService;

    private final LogininforService asyncService;

    private final ILxcUserService lxcUserService;

    private final ISysRoleService sysRoleService;

    private final ILxcInvitationService lxcInvitationService;

    /**
     * 注册
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterBody registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        String tgId = registerBody.getTgId();
        // 校验用户类型是否存在
//        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();

        //账号验证
        if (!BotUtils.isMember(BotUtils.getChatId(LxcConstants.TG_CHANNEL_ID), Long.valueOf(tgId))) {
            throw new UserException("user.tg.error");
//            TODO 跳过验证

        }


        if (UserConstants.NOT_UNIQUE.equals(userService.checkUserNameUnique(username, tgId))) {
            throw new UserException("user.register.save.error", username);
        }

//        String tgUserName = BotUtils.getUserName(LxcBotConstants.CHAT_ID, Long.valueOf(tgId));
        String tgUserName = BotUtils.getUserName(BotUtils.getChatId(LxcConstants.TG_CHANNEL_ID), Long.valueOf(tgId));
        if (tgUserName == null) throw new UserException("user.tg.noname");

        LxcUser user = new LxcUser();
        user.setName(username);
        user.setTgId(tgId);

        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setTgId(tgId);
        sysUser.setDeptId(UserConstants.COMMON_DEP_ID);
        sysUser.setPassword(BCrypt.hashpw(password));
        sysUser.setUserType(UserType.SYS_USER.getUserType());

        boolean lxcFlag = lxcUserService.registerLxcUser(user);
        boolean regFlag = userService.registerUser(sysUser);
        Long userId = sysUser.getUserId();
        int authUsers = sysRoleService.insertAuthUsers(UserConstants.COMMON_ROLE_ID, new Long[]{userId});
        if (!regFlag || !lxcFlag || authUsers <= 0) {
            throw new UserException("user.register.error");
        }
        // 关联邀请码
        try {
            lxcInvitationService.relateInvitationCode(registerBody.getInvitationCode(), tgId);
        } catch (Exception e) {
            e.printStackTrace();
            // 删除邀请码
            lxcInvitationService.deleteWithValidByIds(Collections.singleton(registerBody.getInvitationCode()), true);
        }
        HttpServletRequest request = ServletUtils.getRequest();
        asyncService.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success"), request);
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid, HttpServletRequest request) {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.defaultString(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            asyncService.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.jcaptcha.expire"), request);
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            asyncService.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.jcaptcha.error"), request);
            throw new CaptchaException();
        }
    }
}
