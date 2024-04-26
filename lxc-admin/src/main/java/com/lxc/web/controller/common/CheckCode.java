package com.lxc.web.controller.common;

import cn.dev33.satoken.annotation.SaIgnore;
import com.lxc.common.utils.ValidateCodeUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class CheckCode {


    // 生成验证码图片
    @SaIgnore
    @GetMapping("/api/getCaptchaImage")
    @ResponseBody
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.setContentType("image/png");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Expire", "0");
            response.setHeader("Pragma", "no-cache");

            ValidateCodeUtils validateCode = new ValidateCodeUtils();

            // 直接返回图片
            validateCode.getRandomCodeImage(request, response);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }


   /* @GetMapping("/api/login")
    @SaIgnore
    public com.lxc.common.message.R userLogin(@RequestBody @Valid LxcUserBo userDto, HttpServletRequest request) {

        //session获取
        HttpSession session = request.getSession();
        Object sessionCode = session.getAttribute("checkCode");

        //验证码校验
        if (sessionCode == null || !Objects.equals(userDto.getCode(),sessionCode.toString().toLowerCase())) {
            return com.lxc.common.message.R.success(-1,"验证码错误");
            //虽然我很不喜欢你们这样子来做异常处理，但是这又是很多教程里喜欢的写法
        }

        return com.lxc.common.message.R.success(-1,"验证码错误");

    }*/


}
