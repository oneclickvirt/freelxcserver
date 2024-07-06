package com.lxc.common.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.*;
import com.lxc.common.constant.LxcConstants;
import com.lxc.common.core.domain.dto.ShellDto;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
/**
 * @decription 执行远程shell命令，并获取结果
 *
 */
@Slf4j
public class ShellUtils {

/* 想用iptables来控制容器流量进出，但是容器重建后重设的步骤比较麻烦；另外，如下命令在宿主机上运行了貌似没有在容器上得到正确的反映
    // 流量控制 %s表示的内容
    // $INTERFACE
    // $INTERFACE
    // $INTERFACE $DOWNLOAD_LIMIT
    // $INTERFACE $UPLOAD_LIMIT $DOWNLOAD_LIMIT
    // $INTERFACE
    // $INTERFACE $QUOTA
    // $INTERFACE
    public static final String TRAFFIC_CONTROL = " tc qdisc del dev %s root &&\n" +
        " tc qdisc add dev %s root handle 1: htb default 11 &&\n" +
        " tc class add dev %s parent 1: classid 1:1 htb rate %s &&\n" +
        " tc class add dev %s parent 1:1 classid 1:11 htb rate %s ceil %s &&\n" +
        " tc filter add dev %s protocol ip parent 1:0 prio 1 u32 match ip dst 0.0.0.0/0 flowid 1:1 &&" +
        " iptables -A INPUT -i %s -m quota --quota %s -j ACCEPT &&\n" +
        " iptables -A INPUT -i %s -j DROP";
    // 小鸡流量限额设置 $INTERFACE $QUOTA $INTERFACE
    public static final String QUOTA_SETTING = " iptables -A INPUT -i %s -m quota --quota %s -j ACCEPT &&\n" +
        " iptables -A INPUT -i %s -j DROP";
    // 防止和允许ip规则修改
    public static final String PROTECT_IPTABLES = " chattr +i /etc/iptables/rules.v4 &&"+
        " chattr +i /etc/iptables/rules.v6";
    public static final String RELEASE_IPTABLES = " chattr -i /etc/iptables/rules.v4 &&"+
        " chattr -i /etc/iptables/rules.v6";
*/

    // 获取小鸡IP
    public static final String GET_LXC_IP = " lxc info %s | grep eth0 | head -n 1 | awk '{print $3}'";
    public static final String NET_USED = " lxc info %s | awk '/eth0:/ {flag=1;next} /lo:/ {flag=0} flag {print}'| awk '/(Bytes received:)|(Bytes sent:)/ {print $NF}'";
    //    private static final String NET_USED = "sudo lxc info %s | grep -A 3 'Network usage:' |awk '{print $NF}' | tail -n +3 | head -n 2";
    public static final String GET_LXC_STATUS = " lxc info %s |grep -i -w status |awk '{print $NF}'";
    // 查询网卡
//    public static final String GET_LXC_INTERFACE = " lxc info %s |grep -i -w \"Host interface\" |awk '{print $NF}'";
    public static final String GET_LXC_INTERFACE = " lxc info %s |grep -i -w \"Host Interface\" |awk '{print $NF}'";
    public static final String GET_LXC_INTERFACE_BACKUP = " lxc info %s |grep -i -w \"eth0\" |awk '{print $NF}'";

    // vnstat添加网卡
    public static final String VNSTAT_ADD_INTERFACE = "vnstat --add  %s";

    public static final String CHANGE_STATUS = " lxc %s %s >/dev/null 2>&1 ; " + GET_LXC_STATUS;

    // ./buildone.sh 小鸡名称 内存大小 硬盘大小 SSH端口 外网起端口 外网止端口 下载速度 上传速度 是否启用IPV6(Y or N)
    public static final String CREATE_LXC = "nohup /root/buildone.sh %s %d %d %d %d %d %d %d %s >/dev/null 2>&1 &";

    // ./modify.sh test2 30000 30001 30005 300 300 Y/N
    public static final String INIT_LXC = "nohup /root/modify.sh %1$s %2$d %3$d %4$d %5$d %6$d %7$s > %1$s_init.log 2>&1 &";
//    public static final String INIT_LXC = "nohup /root/modify.sh %1$s %2$d %3$d %4$d %5$d %6$d %7$s 2>&1 | tee -a %1$s_init.log &";
//    public static final String INIT_LXC = "nohup /root/modify.sh %1$s %2$d %3$d %4$d %5$d %6$d %7$s | tee -a  %1$s_init.log 2>&1 &";

    public static final String IF_PASSWD_FILE_EXIST = " if [ ! -f \"/root/%1$s\" ];then\n"
        + "  echo 'false' \n"
        + "  else\n"
        + "    echo \"true\"\n"
        + "    cat /root/%1$s | awk '{print $3}' \n"
        + " fi";

    //*************************************************************//
    // 跳过判断日志文件
    public static final String NOT_CHECK_FILE_EXIST = "";
    //*************************************************************//


    public static final String IF_IPV6_FILE_EXIST = " if [ ! -f \"/root/%1$s_v6\" ];then\n"
        + "  echo 'false' \n"
        + "  else\n"
        + "    cat /root/%1$s_v6\n"
        + " fi";

//    public static final String IF_LXD_WORK = " if [ ! -f \"ps aux | grep forkexec| grep grep -v\" ];then\n"
//        + "  echo \"false\"\n"
//        + "  else\n"
//        + "  echo \"true\"\n"
//        + " fi";

    public static final String IF_LXD_WORK = " if [ ! \"ps aux | grep forkexec| grep grep -v\" ];then\n"
        + "  echo \"false\"\n"
        + "  else\n"
        + "  echo \"true\"\n"
        + " fi";

    public static final String VNSTATI_SUMMARY = "vnstati -s -i $(lxc info %1$s |grep -i -w \"Host interface\" |awk '{print $NF}')  -o %2$s && base64 -w 0 %2$s  && rm -rf %2$s";
    public static final String VNSTATI_SUMMARY_STREAM = "vnstati  -vs 0 --headertext %1$s  -L -ne --scale 150 --style 3 -ru 0 -i $(lxc info %1$s |grep -i -w \"Host interface\" |awk '{print $NF}') -o -";

    public static final String INIT_LOG = " if [ ! -f \"/root/%1$s_init.log\" ];then\n"
        + "  echo '创建日志不存在' \n"
        + "  else\n"
        + "    cat /root/%1$s_init.log \n"
        + " fi";

    @Data
    public static class Shell {
        String cmd;
        List<String> result;
    }

    /**
     * @param sshDto shell 命令
     * @throws Exception
     * @description 执行shell命令
     */
    public static String execCmdAndReturnStatus(ShellDto sshDto) throws Exception {
        String ip = sshDto.getIp();
        Integer sshPort = sshDto.getSshPort();
        String username = sshDto.getUsername();
        String password = sshDto.getPassword();
        String modifyType = sshDto.getModifyType();
        String childName = sshDto.getChildName();
        String result = "";
        String command = "";
        String nowStatus = ShellUtils.execCmdWithStatus(String.format(GET_LXC_STATUS,childName),Optional.ofNullable(username).orElse("root"),password,ip,22);
        switch (modifyType) {
            case "stop":
                if (nowStatus.equalsIgnoreCase("RUNNING")) {
                    command = "stop";
                    result = "关机成功";
                } else {
                    return "小鸡目前已经是关机状态";
                }
                break;
            case "start":
                if (nowStatus.equalsIgnoreCase("Stopped")) {
                    command = "start";
                    result = "开机成功";
                } else {
                    return "小鸡目前已经是开机状态或已流量超限";
                }
                break;
            case "restart":
                if (nowStatus.equalsIgnoreCase("RUNNING")) {
                    command = "restart";
                    result = "重启成功";
                } else {
                    return "小鸡处于关机状态，请执行开机操作";
                }
                break;
            /*case "delete":
                int deleteCode = queueMapper.deleteById(queueDto.getLxcId());
                lxc.setState(0);
                int updateCode = lxcMapper.updateById(lxc);
                if (deleteCode == 1 & updateCode == 1) {
                    return R.success("删除成功");
                } else {
                    return R.fail("服务器内部错误，请联系管理员");
                }*/
            case "traffic_limit":
                if(nowStatus.equalsIgnoreCase("RUNNING") || nowStatus.equalsIgnoreCase("UNFROZEN")){
                    command = "pause";
                    result = "流量使用已达该小鸡上限";
                }else{
                    return "小鸡目前已是冻结状态";
                }
                break;
            case "release_limit":
                if(nowStatus.equalsIgnoreCase("FROZEN")){
                    command = "start";
                    result = "小鸡解除冻结状态";
                    //iptables -A INPUT -i <container-interface> -j DROP
                    //iptables -A OUTPUT -o <container-interface> -j DROP

                }
                else{
                    return "小鸡已解冻";
                }
                break;
            default:
                return "无效操作";
        }
        return result + ",当前状态：" + ShellUtils.execCmdWithStatus(String.format(CHANGE_STATUS,command,childName,childName),
                                                                    Optional.ofNullable(username).orElse("root"),
                                                                    password,ip,
                                                                    Optional.ofNullable(sshPort).orElse(22)
        );
    }

    /**
     * 获取服务器使用流量情况
     * @param sshDto shell 命令
     * @throws Exception
     * @description 执行shell命令
     */
    public static List<String> getNetUsedInfo(ShellDto sshDto) {
        List<String> list = null;
        try {
            String ip = sshDto.getIp();
            Integer sshPort = sshDto.getSshPort();
            String username = sshDto.getUsername();
            String password = sshDto.getPassword();
            String modifyType = sshDto.getModifyType();
            String childName = sshDto.getChildName();
            String result = "";
            String command = String.format(NET_USED,childName);
            list = ShellUtils.execCmdOutList(
                command,
                Optional.ofNullable(username).orElse("root"),
                password, ip,
                Optional.ofNullable(sshPort).orElse(22)
            );
        } catch (Exception e) {
            return null;
        }
        return list;
    }

    /**
     * 配置连接
     *
     * @param user
     * @param passwd
     * @param host
     * @param post
     * @throws Exception
     */
    public static Session connect(String user,String passwd,String host,int post) throws Exception {

        /*session.setPassword(passwd);
        java.util.Properties config = new java.util.Properties();
        //第一次登陆
        config.put("StrictHostKeyChecking","no");
        session.setConfig(config);*/
        Session session = null;
        try {
            session = JschUtil.getSession(host,post,user,passwd);
//            session.connect(50000);
        } catch (Exception e) {
            throw new Exception("连接远程端口无效或用户名密码错误");
        }
        return session;
    }

    /**
     * @param command shell 命令
     * @param user    用户名
     * @param passwd  密码
     * @param host    ip地址
     * @param port    端口号
     * @throws Exception
     * @description 执行shell命令
     */
    public static void execCmd(String command,String user,String passwd,String host,int port) throws Exception {
        System.out.println(command);
        Session session = connect(user,passwd,host,port);
        BufferedReader reader = null;
        Channel channel = null;
        try {
            channel = JschUtil.createChannel(session,ChannelType.EXEC);
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            channel.connect();
            InputStream in = channel.getInputStream();
            reader = new BufferedReader(new InputStreamReader(in));
            String buf = null;
            //返回数据
            while ((buf = reader.readLine()) != null) {
                System.out.println(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSchException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            JschUtil.close(channel);
//            JschUtil.close(session);

        }
    }

    /**
     * @param command shell 命令
     * @param user    用户名
     * @param passwd  密码
     * @param host    ip地址
     * @param port    端口号
     * @throws Exception
     * @description 执行shell命令
     */
    public static String execCmdWithStatus(String command,String user,String passwd,String host,int port) throws Exception {
        System.out.println(command);
        Session session = connect(user,passwd,host,port);
        Channel channel = null;
        AtomicReference<String> res = new AtomicReference<>("");
        BufferedReader reader = null;
        try {
            channel = JschUtil.createChannel(session,ChannelType.EXEC);
            ((ChannelExec) channel).setCommand("source /etc/profile >/dev/null 2>&1 ; source ~/.bashrc >/dev/null 2>&1 ; " + command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();
            channel.connect();
            reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            String buf = null;
            //返回数据
            while ((buf = reader.readLine()) != null) {
                System.out.println(buf);
                res.set(buf);
            }
            /*IoUtil.readLines(in,CharsetUtil.CHARSET_UTF_8,(LineHandler) line -> {
                System.out.println(line);
                res.set(line);
            });*/

        } catch (IOException | JSchException e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                JschUtil.close(channel);
            }
//            JschUtil.close(session);
        }
        return res.get();
    }

    public static List<String> execCmdOutList(String command,String user,String passwd,String host,int port) throws Exception {
        Session session = connect(user,passwd,host,port);
        BufferedReader reader = null;
        Channel channel = null;
        try {
            channel = JschUtil.createChannel(session,ChannelType.EXEC);
            ((ChannelExec) channel).setCommand("source /etc/profile >/dev/null 2>&1 ; source ~/.bashrc >/dev/null 2>&1 ; " + command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            channel.connect();
            InputStream in = channel.getInputStream();
            reader = new BufferedReader(new InputStreamReader(in));
            List<String> output = new ArrayList<String>();
            String buf = null;
            //返回数据
            while ((buf = reader.readLine()) != null) {
                output.add(buf);
            }
            return output;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSchException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            JschUtil.close(channel);
//            JschUtil.close(session);
        }
        return null;
    }

    /**
     * @param logPath
     * @param userIDSet
     * @param user
     * @param passwd
     * @param host
     * @param port
     * @return
     * @throws Exception
     */
    public static Set<String> execCmdAndgetOlderUserIDs(Set<String> userIDSet,String logPath,List<String> fileNames,String user,String passwd,String host,int port) throws Exception {
        Session session = connect(user,passwd,host,port);
        try {
            for (String fileName : fileNames) {
                String command = "cat " + fileName + "|grep '/login'";
                Channel channel = null;
                BufferedReader reader = null;
                try {
                    channel = JschUtil.createChannel(session,ChannelType.EXEC);
                    ((ChannelExec) channel).setCommand(command);
                    channel.setInputStream(null);
                    ((ChannelExec) channel).setErrStream(System.err);
                    channel.connect();
                    InputStream in = channel.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
                    String buf = null;
                    //返回数据
                    while ((buf = reader.readLine()) != null) {
                        analyseLogOneLine(userIDSet,buf);
                    }
                } finally {
                    reader.close();
                    JschUtil.close(channel);
                }
            }
        } finally {
//            JschUtil.close(session);
        }
        return userIDSet;
    }

    /**
     * 解析一行日志，获取老用户uid存放到Set中并返回
     *
     * @param userIDSet
     * @param str
     * @return
     */
    private static Set<String> analyseLogOneLine(Set<String> userIDSet,String str) {
        String[] logLine = str.split("\\s+\\|\\s+");
        String userToken = null;
        String cacheResult = null;
        if (logLine != null && logLine.length == 12) {
            userToken = logLine[6].trim();
            cacheResult = logLine[10].trim();
        }
        if (StringUtils.isNullOrEmpty(cacheResult)) {
            JSONObject jsonObject = JSON.parseObject(cacheResult);
            JSONObject body = (JSONObject) jsonObject.get("body");
            if (body != null) {
                String userID = (String) body.get("userID");
                if (StringUtils.isNullOrEmpty(userID) && StringUtils.isNullOrEmpty(userToken) && userToken.contains("anony")) {
                    userToken = userToken.substring(0,userToken.length() - 6);
                    if (!userToken.equals(userID)) {
                        userIDSet.add(userID);
                    }
                }
            }
        }
        return userIDSet;
    }


    public static void main(String[] args) throws Exception {
        //        lxc info tr12 |grep -i -w status |awk '{print $NF}'
//        Session session = JschUtil.getSession("23.158.72.21", 22, "root", "ZTWHxeqAaa");
        Session session = JschUtil.getSession("38.45.66.95", 22, "root", "4Q3WxeyhcGggnpN");
        //        List<String> cmdOutList = execCommandToLines(String.format(VNSTATI_SUMMARY, "fi1", "template"), session);
//        InputStream commandInputStream = getCommandFileStream(String.format(VNSTATI_SUMMARY_STREAM, "fi1", "template"), session);
        System.out.println(1);
        //        ShellUtils.execCmd("pwd","root","jMs5N2Xmwk","213.142.134.50",22);
        //        ShellUtils.execCmd("sudo lxc info tr12 | grep -A 3 'Network usage:' |awk '{print $NF}' | tail -n +3 | head -n 2","root","jMs5N2Xmwk","213.142.134.50",22);
//        ShellUtils.execCmd(" lxc info sg8 | grep -A 3 'Network usage:' |awk '{print $NF}' | tail -n +3 | head -n 2","root","spiritlhl136@sp","51.79.165.201",22);
        //        ShellUtils.execCmd(String.format(GET_LXC_STATUS,"sg8"),"root","spiritlhl136@sp","51.79.165.201",22);
        //        ShellUtils.execCmd("lxc info  sg8 |grep -i -w status |awk '{print $NF}'","root","spiritlhl136@sp","51.79.165.201",22);
//        Session session = JschUtil.getSession("51.79.165.201",22,"root","spiritlhl136@sp");

//        execCmd(" lxc info sg8 | grep -A 3 'Network usage:' |awk '{print $NF}' | tail -n +3 | head -n 2",session);
//        ShellUtils.execCmd("sudo lxc list","root","bBanjnY6AM","208.115.109.10",22);
        ShellUtils.execCmd("ls","root","4Q3WxeyhcGggnpN","38.45.66.95",22);
//        ShellUtils.execCmdAndOutput("sudo lxc list","root","spiritlhl136@sp","51.79.165.201",22);
     /*   Session session2 = JschUtil.getSession("208.115.109.10",22,"root","bBanjnY6AM");
        List<String> out = execCmdAndOut(String.format(IF_PASSWD_FILE_EXIST,"test6"),session2);
        if (out.size() == 2 && StringUtils.equals(out.get(0),"true")) {
            System.out.println(out.get(1));
        }*/
        JschUtil.close(session);

        /*ShellDto shellDto = ShellDto.builder()
                                .ip("208.115.109.10")
                                .sshPort(22)
                                .username("root")
                                .password("bBanjnY6AM")
                                .childName("test6")
                                .memoryLimit(256L)
                                .diskLimit(1L)
                                .childSshPort(20006L)
                                .portStart(30060)
                                .portEnd(30069)
                                .build();
        createLxc(shellDto);
        execCmd("lxc list",session2);*/
//        String res = ShellUtils.execCmdWithStatus("sudo lxc info sg8 |grep -i -w status |awk '{print $NF}'","root","spiritlhl136@sp","51.79.165.201",22);
//        System.out.println(res);
//        System.out.println(String.format(ShellUtils.IF_IPV6_FILE_EXIST, "et1"));
    }

    /**
     * 根据服务器Host和端口号，检查服务器是否ping通
     *
     * @param host IP地址或者Host
     * @param port 端口号
     * @return true说明能通，false不通
     */
    public static boolean pingHost(String host,int port) {
        Socket pingSocket = null;
        PrintWriter out = null;
        BufferedReader br = null;
        String response = null;

        try {
            pingSocket = new Socket(host,port);
            pingSocket.setSoTimeout(3000);
            out = new PrintWriter(pingSocket.getOutputStream(),true);
            out.println("ping");
            br = new BufferedReader(new InputStreamReader(pingSocket.getInputStream(),StandardCharsets.UTF_8));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            response = sb.toString();
        } catch (IOException ignored) {
            log.error("ping出错",ignored);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ignored) {
            }
            try {
                if (pingSocket != null) {
                    pingSocket.close();
                }
            } catch (IOException ignored) {
            }
        }
        return response != null;
    }

    public static Shell initLxc(ShellDto sshDto) {
        //1、利用JschUtil获取会话连接
        Session session = JschUtil.getSession(sshDto.getIp(), sshDto.getSshPort(), sshDto.getUsername(), sshDto.getPassword());
        System.out.println("initLxcSession:" + session);
        // 小鸡名称 内存大小 硬盘大小 SSH端口 外网起端口 外网止端口
        //  /root/modify.sh test2 30000 30001 30005 300 300
        String format = String.format(
            INIT_LXC,
            sshDto.getChildName(),
            sshDto.getChildSshPort(),
            sshDto.getPortStart(),
            sshDto.getPortEnd(),
            sshDto.getUpLimit(),
            sshDto.getDownLimit(),
            StringUtils.equals(sshDto.getIpv6Flag(), LxcConstants.LXC_IPV6_FLAG) ? sshDto.getIpv6Flag() : " "
        );
        List<String> cmdOutList = execCmdOutList(
            format,
            session
        );
        Shell shell = new Shell();
        shell.setCmd(format);
        shell.setResult(cmdOutList);
        return shell;
    }


    public static String createLxc(ShellDto sshDto) {
        //1、利用JschUtil获取会话连接
        Session session = JschUtil.getSession(sshDto.getIp(),sshDto.getSshPort(),sshDto.getUsername(),sshDto.getPassword());
        // 小鸡名称 内存大小 硬盘大小 SSH端口 外网起端口 外网止端口
        System.out.println("createLxcSession:" + session);
        String format = String.format(
            CREATE_LXC,
            sshDto.getChildName(),
            sshDto.getMemoryLimit(),
            sshDto.getDiskLimit(),
            sshDto.getChildSshPort(),
            sshDto.getPortStart(),
            sshDto.getPortEnd(),
            sshDto.getUpLimit(),
            sshDto.getDownLimit(),
            StringUtils.equals(sshDto.getIpv6Flag(), LxcConstants.LXC_IPV6_FLAG) ? sshDto.getIpv6Flag() : " "
        );
        execCmdWithoutOut(
            format,
            session
        );
        return format;
    }


    /**
     * 执行一条命令,不返回结果
     */
    public static void execCmdWithoutOut(String command,Session session) {
        Channel channel = JschUtil.createChannel(session,ChannelType.EXEC);
        ((ChannelExec) channel).setCommand("source /etc/profile  >/dev/null 2>&1 ; source /root/.bashrc >/dev/null 2>&1 ; " + command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        ((ChannelExec) channel).setPty(false);
        log.info("执行命令:~# {}",command);
        try {
            channel.connect();
            int exitStatus = channel.getExitStatus();
            log.info("exitStatus={}",exitStatus);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            JschUtil.close(channel);
        }
    }

    /**
     * 执行一条命令
     */
    public static void execCmd(String command,Session session) {
        Channel channel = JschUtil.createChannel(session,ChannelType.EXEC);
        ((ChannelExec) channel).setCommand("source /etc/profile >/dev/null 2>&1 ; source ~/.bashrc >/dev/null 2>&1 ; " + command);
//        ((ChannelExec) channel).setCommand("source .bash_profile && "+command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
//        ((ChannelExec) channel).setInputStream(System.in);

        System.out.println("执行 " + command);
//        IoUtil.readUtf8Lines(channel.getExtInputStream(), new LineHandler() {
//            @Override
//            public void handle(String line) {
//                System.out.println(line);
//            }
//        });
        try (InputStream inputStream = channel.getInputStream()) {
            channel.connect();
            IoUtil.readLines(
                inputStream, CharsetUtil.CHARSET_UTF_8,
                (LineHandler) System.out::println
            );
            int exitStatus = channel.getExitStatus();
            System.out.println(exitStatus);
        } catch (IOException | JSchException e) {
            throw new RuntimeException(e);
        } finally {
            JschUtil.close(channel);
        }
    }

    public static Channel createChannel(Session session,ChannelType channelType) {
        try {
            return session.openChannel(channelType.getValue());
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取session
     */
    public static Session getSession(String host,int port,String username,String password) {
        return JschUtil.getSession(host,port,username,password);
    }

    /**
     * 获取session
     */
    public static Session getSession(ShellDto sshDto) {
        return JschUtil.getSession(sshDto.getIp(),sshDto.getSshPort(),sshDto.getUsername(),sshDto.getPassword());
    }

    public static List<String> execCommandToLines(String command, Session session) throws JSchException, IOException {
        List<String> outputLines = new ArrayList<>();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("source /etc/profile >/dev/null 2>&1 ; source ~/.bashrc >/dev/null 2>&1 ; " + command);
        channel.connect();

        try (InputStream in = channel.getInputStream()) {
            byte[] buffer = new byte[1024];
            while (true) {
                int readLength = in.read(buffer);
                if (readLength < 0) {
                    break;
                }
                String line = new String(buffer, 0, readLength);
                outputLines.add(line);
            }
        }

        channel.disconnect();

        return outputLines;
    }

    public static InputStream getCommandFileStream(String command, Session session) {
        ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
        channel.setCommand("source /etc/profile >/dev/null 2>&1 ; source ~/.bashrc >/dev/null 2>&1 ; " + command);
        channel.setInputStream(null);
        channel.setErrStream(System.err);
        try (InputStream inputStream = channel.getInputStream()) {
//            channel.connect();
            //设置一个长一点的连接超时时间，默认为20s
            channel.connect(60 * 1000);//连接超时时间为60s
            byte[] bytes = IoUtil.readBytes(inputStream);
            return new ByteArrayInputStream(bytes);
        } catch (IOException | JSchException e) {
            throw new RuntimeException(e);
        } finally {
            JschUtil.close(channel);
        }
    }

    /**
     * 执行一条命令
     *
     * @return
     */
    public static List<String> execCmdOutList(String command,Session session) {
        List<String> list = new ArrayList<>();

        ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
        channel.setCommand("source /etc/profile >/dev/null 2>&1 ; source ~/.bashrc >/dev/null 2>&1 ; " + command);
//        ((ChannelExec) channel).setCommand("source .bash_profile && "+command);
        channel.setInputStream(null);
        channel.setErrStream(System.err);
//        ((ChannelExec) channel).setInputStream(System.in);

        log.info("执行:~# {}",command);
        try (InputStream inputStream = channel.getInputStream()) {
            channel.connect();

            IoUtil.readLines(inputStream,CharsetUtil.CHARSET_UTF_8,(LineHandler) list::add);
            int exitStatus = channel.getExitStatus();
            System.out.println(exitStatus);
        } catch (IOException | JSchException e) {
            throw new RuntimeException(e);
        } finally {
            JschUtil.close(channel);
        }
        return list;
    }


    /**
     * shell命令执行
     *
     * @return
     */
    public static void flushShellCmd(String command,ChannelShell channel,OutputStream out) throws IOException {
        if (channel != null && channel.isConnected()) {
            out.write(command.getBytes());
            out.flush();
            log.info("执行:~# {}",command);
        } else {
            log.error("channel is not connected");
        }
    }


    /**
     * 执行一条命令
     */
    public static void execCmdNoEcho(String command,ChannelExec channel) {
        channel.setCommand("source /etc/profile  >/dev/null 2>&1 ; source ~/.bashrc >/dev/null 2>&1 ; " + command);
        channel.setInputStream(null);
        channel.setErrStream(System.err);
        channel.setPty(false);
        log.info("执行命令:~# {}",command);
        try {
            channel.connect();
            int exitStatus = channel.getExitStatus();
            log.info("exitStatus={}",exitStatus);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            JschUtil.close(channel);
        }
    }

}
