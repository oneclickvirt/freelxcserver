package com.lxc.machine.websocket.pojo;

import com.jcraft.jsch.ChannelShell;
import com.lxc.common.core.domain.model.LoginUser;
import java.io.OutputStream;
import java.io.Serializable;
import javax.websocket.Session;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Terminal implements Serializable {

    private static final long serialVersionUID = 1L;

    private Session socketSession;

    private com.jcraft.jsch.Session sshSession;

    private ChannelShell channel;

    private OutputStream out;

    private String host;

    private String port;

    private String username;

    private String password;

    private String command;

    private Long lxcId;

    private Long userId;

    private boolean isToSuperLxc;

    private Boolean isAuthed;

    private LoginUser loginUser;

}
