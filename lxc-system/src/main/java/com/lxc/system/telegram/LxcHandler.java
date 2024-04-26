package com.lxc.system.telegram;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.lxc.common.constant.CacheConstants;
import com.lxc.common.enums.CaptchaType;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.common.utils.reflect.ReflectUtils;
import com.lxc.common.utils.spring.SpringUtils;
import com.lxc.framework.config.properties.CaptchaProperties;
import com.lxc.system.service.ISysUserService;
import java.time.Duration;
import java.time.LocalTime;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class LxcHandler {
    private final ISysUserService sysUserService;
    private final CaptchaProperties captchaProperties;

    public final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final LockTemplate lockTemplate;

    @SneakyThrows
    public String handle(Long chatId, String text) {
        log.info("chatId: {}, text: {}", chatId, text);
        String cacheKey = CacheConstants.TG_BOT_RE_PASSWD + chatId;
        String delayCacheKey = CacheConstants.TG_BOT_RE_PASSWD_DELAY + cacheKey;
        switch (text) {
            case "/start":
                text = "start";
                break;
            case "/myid":
                text = chatId.toString();
                break;
            case "/repasswd":
                text = doRePasswd(chatId, cacheKey);
                break;
            default:
                if (RedisUtils.hasKey(cacheKey)) {
                    Long id = RedisUtils.getCacheObject(cacheKey);
                    // 加锁防止并发
                    synchronized (id) {
                        if (RedisUtils.hasKey(cacheKey)) {
                            final LockInfo lockDo = lockTemplate.lock(CacheConstants.TG_BOT_WAITE_PASSWD + cacheKey, 30000L, 5000L, RedissonLockExecutor.class);
                            if (null == lockDo) {
                                return "重置密码中，请不要重复发送密码";
                            }
                            // 获取锁成功，处理业务
                            try {
                                RedisUtils.deleteObject(cacheKey);
                                RedisUtils.deleteObject(delayCacheKey);
                                // 密码校验
                                String reg = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9._!$&@]{5,20}$";
                                Pattern pattern = Pattern.compile(reg);
                                if (!pattern.matcher(text).matches()) {
                                    text = "密码长度5到20为且必须包含大小写字母和数字的组合，可以使用特殊字符. & _ ! $ @";
                                } else {
                                    String passwd = BCrypt.hashpw(text);
                                    if (sysUserService.resetUserPwdByTgId(chatId, passwd) > 0) {
                                        text = "密码重置成功";
                                    } else {
                                        text = "修改密码异常，请联系管理员";
                                    }
                                }
                            } finally {
                                //释放锁
                                lockTemplate.releaseLock(lockDo);
                            }
                        }
                    }
                } else if (RedisUtils.hasKey(delayCacheKey)) {
                    text = "已超时，请重新发送/repasswd重置密码";
                    RedisUtils.deleteObject(delayCacheKey);
                } else {
                    text = "不要调皮😝";
                }
                break;
        }
        return text;
    }

    @NotNull
    private String doRePasswd(Long chatId, String cacheKey) {
        String text;
        log.info("start:{},time:{}", chatId, LocalTime.now().toString());
        final LockInfo lockInfo = lockTemplate.lock(CacheConstants.TG_BOT_DO_REPASSWD_LOCK + cacheKey, 30000L, 5000L, RedissonLockExecutor.class);
        if (null == lockInfo) {
            return "不要频繁发送重复指令";
        }
        // 获取锁成功，处理业务
        try {
            text = handleRePasswd(cacheKey, chatId);
        } finally {
            //释放锁
            lockTemplate.releaseLock(lockInfo);
        }
        return text;
    }

    private String handleRePasswd(String cacheKey, Long chatId) {
        if (RedisUtils.hasKey(cacheKey)) {
            return "请输入新密码";
        } else {
            Long tgId = sysUserService.selectIdByTgId(chatId);
            if (tgId == null) {
                return "请先在官方网站注册账号 https://lxc.spiritysdx.top";
            } else {
                RedisUtils.setCacheObject(cacheKey, chatId, Duration.ofMinutes(1));
//                RedisUtils.setCacheObject(cacheKey, tgId, Duration.ofMinutes(1));
                RedisUtils.setCacheObject(CacheConstants.TG_BOT_RE_PASSWD_DELAY + cacheKey, tgId, Duration.ofMinutes(3));
                return "请在一分钟内输入新密码";
            }
        }
    }

    public String getTgCode() {
        // 生成验证码
        CaptchaType captchaType = CaptchaType.CHAR;
        Integer length = captchaProperties.getTgRePasswdLength();
        CodeGenerator codeGenerator = ReflectUtils.newInstance(captchaType.getClazz(), length);
        AbstractCaptcha captcha = SpringUtils.getBean(captchaProperties.getCategory().getClazz());
        captcha.setGenerator(codeGenerator);
        captcha.createCode();
        return captcha.getCode();
    }

}
