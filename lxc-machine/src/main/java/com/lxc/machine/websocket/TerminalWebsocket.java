package com.lxc.machine.websocket;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lxc.common.core.domain.model.LoginUser;
import com.lxc.common.helper.LoginHelper;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.spring.SpringUtils;
import com.lxc.machine.websocket.pojo.Terminal;
import com.lxc.machine.websocket.service.impl.WebSSHServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * terminal
 */
@Slf4j
@ServerEndpoint(value = "/terminal/{type}/{lxcId}/{token}")
@Component
public class TerminalWebsocket {

    private final WebSSHServiceImpl webSSHService = SpringUtils.getBean("webSSHServiceImpl");
    private ConcurrentHashMap<String,Terminal> terminalSessionMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String,String> uidMap = new ConcurrentHashMap<>();


    /**
     * 固定前缀
     */
    private static final String USER_ID = "user_id_";

    private Session session;
    private String uid;
    public static CopyOnWriteArraySet<TerminalWebsocket> webSockets = new CopyOnWriteArraySet<>();
    private static Map<String,Session> sessionPool = new HashMap<String,Session>();
    //session集合
    private static List<Map<String,Object>> sessionList = new ArrayList<>();
    //信息集合
    private static List<Map<String,String>> MessageList = new ArrayList<>();


    @OnOpen
    public void onOpen(Session session,@PathParam(value = "type") String type,@PathParam(value = "lxcId") Long lxcId,@PathParam(value = "token") String token) throws IOException {
        if (StringUtils.isEmpty(token) || StringUtils.isEmpty(type) || lxcId == null) {
            session.close();
            return;
        }
        // 根据 token 获取对应的 userId
        String loginId = (String) StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            session.close();
            throw new SaTokenException("连接失败，无效Token：" + token);
        }
        // put到集合，方便后续操作
        long userId = Long.parseLong(loginId.split(LoginHelper.JOIN_CODE)[1]);
        LoginUser loginUser = (LoginUser) StpUtil.getTokenSessionByToken(token).get(LoginHelper.LOGIN_USER_KEY);
        if (loginUser == null) {
            session.close();
            return;
        }
        Terminal terminal = Terminal.builder()
                                    .userId(userId)
                                    .lxcId(lxcId)
                                    .socketSession(session)
                                    .isToSuperLxc(StringUtils.equals(type,"super"))
                                    .loginUser(loginUser)
                                    .build();
        boolean connected = connect(session, terminal);
        if (!connected) {
            return;
        }
        session.setMaxIdleTimeout(TimeUnit.MINUTES.toMillis(10));
//        session.setMaxIdleTimeout(TimeUnit.SECONDS.toMillis(3));
        uidMap.put(session.getId(),USER_ID + userId + ":" + lxcId);
        terminalSessionMap.put(USER_ID + userId + ":" + lxcId,terminal);
//        webSockets.add(this);
        log.info("【websocket消息】有新的terminal连接，总数为:" + uidMap.size());
    }

    private boolean connect(Session session, Terminal terminal) throws IOException {
        String res = webSSHService.initConnection(terminal);
        if (!StringUtils.equals(res,"success") && !StringUtils.equals(res,"JSchException: Auth fail")) {
//            session.getAsyncRemote().sendText(res);
//            session.getAsyncRemote().sendText("连接异常！");
            session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "exception"));
            return false;
        }
        terminal.setIsAuthed(false);
        session.getAsyncRemote().sendText(res);
        try {
            if (StringUtils.equals(res,"success")) {
                terminal.setIsAuthed(true);
                Thread.sleep(10);
                webSSHService.openShellOutStream(terminal);
            }
        } catch (Exception e) {
            session.close();
            return false;
        }
        return true;
    }

//    @OnOpen
//    public void onOpen(Session session,@PathParam(value = "lxcId") String lxcId,@PathParam(value = "token") String token) throws IOException {
//        // 根据 token 获取对应的 userId
//        String loginId = (String) StpUtil.getLoginIdByToken(token);
//        if (loginId == null) {
//            session.close();
//            throw new SaTokenException("连接失败，无效Token：" + token);
//        }
//        // put到集合，方便后续操作
//        long userId = Long.parseLong(loginId.split(LoginHelper.JOIN_CODE)[1]);
//        WebSSH webSSH = new WebSSH();
//        webSSH.setSession(session);
//        com.jcraft.jsch.Session sshSession = JschUtil.getSession("192.168.1.102",22,"root","982968053");
//        Channel channel = ShellUtils.createChannel(sshSession,ChannelType.SHELL);
//        channel.setOutputStream(System.out);
//        ChannelShell channelShell = (ChannelShell) channel;
//        channelShell.setPty(true);
//        channelShell.setPtyType("xterm-256color");
//        channelShell.setEnv("LANG","zh_CN.UTF-8");
////        channelShell.setEnv("force_color_prompt","yes");
//        channelShell.setEnv("TERM","xterm-256color");
//
//        webSSH.setChannel(channel);
//        OutputStream outputStream = channel.getOutputStream();
//        webSSH.setOut(outputStream);
//        sessionMap.put(USER_ID + userId,webSSH);
//        try {
//            channel.connect();
//        } catch (JSchException e) {
//            session.close();
//            return;
//        }
//        ChannelExec channelExec = (ChannelExec) ShellUtils.createChannel(sshSession,ChannelType.EXEC);
//        ShellUtils.execCmdNoEcho("echo 'force_color_prompt=yes' >> $HOME/.bashrc && source $HOME/.bashrc",channelExec);
////        ShellUtils.flushShellCmd("echo 'force_color_prompt=yes' >> $HOME/.bashrc && source $HOME/.bashrc \r",(ChannelShell)webSSH.getChannel(),webSSH.getOut());
//        threadPoolTaskExecutor.execute(() -> {
//
//            try (InputStream inputStream = channel.getInputStream()) {
////                ShellUtils.execSocketCmd("echo 'force_color_prompt=yes' >> $HOME/.bashrc && source $HOME/.bashrc",webSSH.getChannel(),session,webSSH.getOut());
//                /*String cmd = "ls \n\r";
//                IoUtil.readUtf8Lines(inputStream,(LineHandler) line -> {
//                    try {
//                        log.info("line:{}",line);
//                        session.getAsyncRemote().sendText(line);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                });*/
//
//                //循环读取
//                byte[] buffer = new byte[256];
//                int i = 0;
//                //如果没有数据来，线程会一直阻塞在这个地方等待数据。
//                log.info("exitStatus:{}","线程会一直阻塞在这个地方等待数据");
//                while (channel.isConnected() && (i = inputStream.read(buffer)) != -1) {
//                    session.getBasicRemote().sendText(new String(buffer,0,i));
//                }
//                int exitStatus = channel.getExitStatus();
//                log.info("session关闭了:{}",exitStatus);
//            } catch (Exception e) {
//                try {
//                    session.close();
//                } catch (IOException ex) {
//                    throw new RuntimeException(ex);
//                }
//                throw new RuntimeException(e);
//            } finally {
//                try {
//                    session.close();
//                } catch (IOException e) {
//
//                }
//                try {
//                    outputStream.close();
//                } catch (IOException e) {
//
//                }
//                JschUtil.close(channel);
////            JschUtil.close(session);
//            }
//        });
//
//        Map<String,Object> map = new HashMap<>();
//        this.session = session;
//        this.uid = String.valueOf(userId);
//        webSockets.add(this);
//        sessionPool.put(this.uid,session);
//        map.put("uid",this.uid);
//        map.put("session",session);
//        sessionList.add(map);
//        // Constants.WEBSOCKET = true;//定义常量  是否开启websocket连接
//        System.out.println("【websocket消息】有新的连接，总数为:" + webSockets.size());
////        session.getAsyncRemote().sendText(String.valueOf(userId));
//    }

    @OnError
    public void onError(Session session,Throwable error) {
        log.error("【websocket消息】发生错误",error);
        try {
            doClose(session);
        } catch (Exception ignored) {

        }
    }

    @OnClose
    public void onClose(Session session) throws IOException {
//        webSockets.remove(this);
        if (session == null) {
            return;
        }
        doClose(session);
        log.info("【websocket消息】连接断开，总数为:{}",uidMap.size());
    }

    private void doClose(Session session) throws IOException {
        String uid = uidMap.get(session.getId());
        if (StringUtils.isBlank(uid)) {
            return;
        }
        Terminal terminal = getTerminal(session);
        uidMap.remove(session.getId());
        terminalSessionMap.remove(uid);
        terminal.getSocketSession().close();
        try {
            JschUtil.close(terminal.getChannel());
            JschUtil.close(terminal.getSshSession());
        } catch (Exception e) {
            log.error("关闭连接异常",e);
        }
        try {
            if (terminal.getOut() != null) {
                terminal.getOut().close();
            }
        } catch (Exception e) {
            log.error("关闭输出流异常",e);
        }
    }

    @OnMessage
    public void onMessage(Session session,String message) throws IOException {
        Terminal terminal = getTerminal(session);
        log.info("用户:{},发送命令:{}",terminal.getUserId(),message);
        if (terminal.getIsAuthed() == null) {
            return;
        }
        if (terminal.getIsAuthed()) {
//            {"op":"resize","cols":81,"rows":35,"height":814,"width":746}
            if (JSONUtil.isTypeJSON(message)) {
                JSONObject jsonObject = JSON.parseObject(message);
                String op = jsonObject.getString("op");
                if ("resize".equals(op)) {
                    Integer cols = jsonObject.getInteger("cols");
                    Integer rows = jsonObject.getInteger("rows");
                    Integer height = jsonObject.getInteger("height");
                    Integer width = jsonObject.getInteger("width");
                    log.info("cols:{},rows:{},height:{},width:{}",cols,rows,height,width);
                    try {
                        terminal.getChannel().setPtySize(cols,rows,height,width);
                    } catch (Exception e) {
                        log.error("设置终端大小异常",e);
                    }
                }
            } else {
                webSSHService.recvHandle(message,terminal);
            }
        } else {
            if (JSONUtil.isTypeJSON(message)) {
                JSONObject jsonObject = JSON.parseObject(message);
                terminal.setPassword(jsonObject.getString("passwd"));
                log.info("自己输入密码连接");
                connect(session,terminal);
            }
        }
    }

    private Terminal getTerminal(Session session) {
        String uid = uidMap.get(session.getId());
        if (StringUtils.isBlank(uid)) {
            return null;
        }
        return terminalSessionMap.get(uid);
    }

    // 此为广播消息
    public void sendAllMessage(String message) {
        for (TerminalWebsocket webSocket : webSockets) {
            try {
                webSocket.session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 此为单点消息
    public void sendOneMessage(String code,String message) {
        Session session = sessionPool.get(code);
        /*在发送数据之前先确认 session是否已经打开 使用session.isOpen() 为true 则发送消息
         * 不然会报错:The WebSocket session [0] has been closed and no method (apart from close()) may be called on a closed session */
        if (session != null && session.isOpen()) {
            try {
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 此为对应的广播消息
    public void sendAllMessageBySelf() {
        if (MessageList != null && MessageList.size() > 0) {
            for (Map<String,String> messageMap : MessageList) {
                if (sessionList != null && sessionList.size() > 0) {
                    for (Map<String,Object> sessionMap : sessionList) {
                        if (String.valueOf(sessionMap.get("uid")).equals(messageMap.get("uid"))) {
                            Map<String,Object> message = (Map<String,Object>) JSONArray.parse(messageMap.get("message"));
                            int employeeNum = Integer.parseInt(String.valueOf(message.get("employeeNum")));
                            //返回的数据
                            Map<String,Object> returnDate = new HashMap<>();
                            String returnStr = JSONArray.toJSONString(returnDate);
                            //向指定客户端推送信息
                            Session session = (Session) sessionMap.get("session");
                            session.getAsyncRemote().sendText(returnStr);
                        }
                    }
                }
            }
        }
    }
}

