package com.lxc.machine.websocket.service;

import com.jcraft.jsch.JSchException;
import com.lxc.machine.websocket.pojo.Terminal;
import java.io.IOException;
import javax.websocket.Session;

/**
 * WebSSH
 */
public interface WebSSHService {

    /**
     * 初始化连接
     *
     * @param terminal
     */
    public String initConnection(Terminal terminal);

    /**
     * 开启Chanel读取数据
     *
     * @param terminal
     */
    public void openShellOutStream(Terminal terminal) throws JSchException;

    /**
     * @Description: 处理客户段发的数据
     * @Param:
     * @return:
     */
    public void recvHandle(String buffer,Terminal terminal);

    /**
     * @Description: 数据写回前端 for websocket
     * @Param:
     * @return:
     */
    public void sendMessage(Session session,byte[] buffer) throws IOException;

    /**
     * @Description: 关闭连接
     * @Param:
     * @return:
     */
    public void close(Session session);
}
