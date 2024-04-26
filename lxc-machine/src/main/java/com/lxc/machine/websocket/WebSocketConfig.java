package com.lxc.machine.websocket;

import cn.dev33.satoken.stp.StpUtil;
import com.lxc.common.core.domain.model.LoginUser;
import com.lxc.common.helper.LoginHelper;
import java.util.List;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class WebSocketConfig extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config,HandshakeRequest request,HandshakeResponse response) {
        System.out.println(config.getUserProperties());
        Class<?> endpointClass = config.getEndpointClass();
        List<String> token = request.getHeaders().get("Sec-WebSocket-Protocol");
        if (token == null || token.size() == 0) {
            return;
        }
        StpUtil.setTokenValue(token.get(0));
        LoginUser loginUser = LoginHelper.getLoginUser();
//        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//        requestAttributes.setAttribute("loginUser", loginUser, 0);
        //第二种方案（基于Spring环境条件下）
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpSession session = attributes.getRequest().getSession(true);
        session.setAttribute("loginUser",loginUser);
        config.getUserProperties().put("session",session);
        // Get HttpServletRequest
//        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        // Get client IP address
//        String clientIp = httpServletRequest.getRemoteAddr();

        // Set client IP as a user property
//        config.getUserProperties().put("loginUser", loginUser);
        response.getHeaders().put("Sec-WebSocket-Protocol",token);

        super.modifyHandshake(config,request,response);
    }
}
