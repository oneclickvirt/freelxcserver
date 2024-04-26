package com.lxc.machine.websocket.service.impl;

import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.JschUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.lxc.common.core.domain.model.LoginUser;
import com.lxc.common.utils.ShellUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcQueue;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcQueueService;
import com.lxc.machine.service.ILxcSuperLxcService;
import com.lxc.machine.websocket.pojo.Terminal;
import com.lxc.machine.websocket.service.WebSSHService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class WebSSHServiceImpl implements WebSSHService {

    public final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private final ILxcSuperLxcService lxcSuperLxcService;

    private final ILxcLxcService lxcLxcService;

    private final ILxcQueueService lxcQueueService;

    @Override
    public String initConnection(Terminal terminal) {
        try {
            ChannelShell shell;
            Long lxcId = terminal.getLxcId();
            LoginUser loginUser = terminal.getLoginUser();
            com.jcraft.jsch.Session sshSession;
            int timeout = (int) TimeUnit.SECONDS.toMillis(10);
            try {
                boolean authed = terminal.getIsAuthed() == null || terminal.getIsAuthed();
                if (terminal.isToSuperLxc()) {
                    if (!loginUser.getIsAdmin()) {
                        return "无权限";
                    }
                    LxcSuperLxc superLxc = lxcSuperLxcService.getById(lxcId);
                    sshSession = JschUtil.openSession(superLxc.getIp(),Integer.parseInt(superLxc.getSshPort()),"root",authed ? superLxc.getPassword() : terminal.getPassword(),timeout);
                } else {
                    LxcLxc lxc = lxcLxcService.getById(lxcId);
                    LxcQueue queue = lxcQueueService.getOne(new QueryWrapper<LxcQueue>().eq("lxc_id",lxcId));
                    String tgId = queue.getTgId();
                    if (!StringUtils.equals(tgId,loginUser.getTgId())) {
                        return "这不是你的小鸡";
                    }
                    sshSession = JschUtil.openSession(lxc.getSuperIp(),Integer.parseInt(lxc.getSshPort()),"root",authed ? lxc.getPassword() : terminal.getPassword(),timeout);
                }
            } catch (JschRuntimeException e) {
                return e.getMessage();
            }

            terminal.setSshSession(sshSession);
            shell = (ChannelShell) JschUtil.createChannel(sshSession,ChannelType.SHELL);
            shell.setPty(true);
            shell.setPtyType("xterm-256color");
            shell.setEnv("LANG","zh_CN.UTF-8");
//        channelShell.setEnv("force_color_prompt","yes");
            shell.setEnv("TERM","xterm-256color");
            terminal.setChannel(shell);
            terminal.setOut(shell.getOutputStream());
//        ChannelExec channel = (ChannelExec) ShellUtils.createChannel(sshSession,ChannelType.EXEC);
//        ShellUtils.execCmdNoEcho("echo 'force_color_prompt=yes' >> $HOME/.bashrc && source $HOME/.bashrc",channel);
        } catch (Exception e) {
            e.printStackTrace();
            return "发生异常，连接失败，请联系站长";
        }
        return "success";
    }

    @Override
    public void openShellOutStream(Terminal terminal) throws JSchException {
        ChannelShell shell = terminal.getChannel();
        shell.connect();
        threadPoolTaskExecutor.execute(() -> {
            Session socketSession = terminal.getSocketSession();
            try (InputStream inputStream = shell.getInputStream()) {

                //循环读取
                byte[] buffer = new byte[2048];
                int i;
                //如果没有数据来，线程会一直阻塞在这个地方等待数据。
                while (socketSession != null && socketSession.isOpen() && shell.isConnected() && (i = inputStream.read(buffer)) != -1) {
//                    String text = new String(buffer,0,i);
                    socketSession.getBasicRemote().sendBinary(ByteBuffer.wrap(buffer,0,i),true);
//                    terminal.getSocketSession().getBasicRemote().sendBinary(ByteBuffer.wrap(text.getBytes()),true);
//                    log.info("接收到的数据:{}",text);
                }
                int exitStatus = shell.getExitStatus();
                log.info("session关闭了:{}",exitStatus);
            } catch (Exception e) {
                try {
                    if (socketSession != null && socketSession.isOpen()) {
                        socketSession.close();
                    }
                } catch (IOException ignored) {
                }
            } finally {
                JschUtil.close(shell);
            }
        });
    }

    @Override
    public void recvHandle(String buffer,Terminal terminal) {
        try {
            ShellUtils.flushShellCmd(buffer,terminal.getChannel(),terminal.getOut());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(Session session,byte[] buffer) throws IOException {

    }

    @Override
    public void close(Session session) {

    }
}
