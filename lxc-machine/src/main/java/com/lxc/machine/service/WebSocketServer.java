package com.lxc.machine.service;

import com.lxc.common.core.domain.model.LoginUser;
import com.lxc.common.helper.LoginHelper;

import javax.websocket.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//@Component
//@ServerEndpoint("/ws/container-data")
public class WebSocketServer {
    public final static Map<String, Session> sessionPools = new ConcurrentHashMap<>();
    @OnMessage
    public void onMessage(Session session, String message) {

    }

    @OnOpen
    public void onOpen(Session session) {
        // 建立连接的对象只有管理员账号，使用tgid来作为session的key可行，但传入时需要当前用户登录信息处获取，不是从数据库lxc_queue中获取
        System.out.println("websocket连接成功");
        LoginUser loginUser = LoginHelper.getLoginUser();
        String tgId = loginUser.getTgId();
        sessionPools.remove(tgId);
        sessionPools.put(tgId, session);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("websocket连接关闭");
        LoginUser loginUser = LoginHelper.getLoginUser();
        String tgId = loginUser.getTgId();
        sessionPools.remove(tgId);
    }

    @OnError
    public void onError(Session session, Throwable exception) {
        System.out.println("websocket发生错误");
        exception.printStackTrace();
    }
}
