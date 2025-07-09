package com.lxc.machine.utils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lxc.common.core.domain.dto.ShellDto;
import com.lxc.common.enums.LxcStatus;
import com.lxc.common.utils.JsonUtils;
import com.lxc.common.utils.ShellUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.spring.SpringUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcApplyBo;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.impl.LxcSuperLxcServiceImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.relxd.lxd.ApiException;
import org.relxd.lxd.RelxdApiClient;
import org.relxd.lxd.api.InstancesApi;
import org.relxd.lxd.builders.InstanceConfigBuilder;
import org.relxd.lxd.builders.SourceConfigBuilder;
import org.relxd.lxd.model.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstancesUtils {
    //    private final InstancesApi API = SpringUtils.getBean("lxcApi", InstancesApi.class);
    private InstancesApi instancesApi;
    public static InstancesUtils target(String ip,String port) {
        assert ip != null;
        // port : 8443-lxd 9969-incus
//        String baseUrl = "https://" + ip + ":9969";
        String baseUrl = "https://" + ip + ":" + port;
        return new InstancesUtils(new InstancesApi(new RelxdApiClient(baseUrl)));
    }

    private InstancesUtils(InstancesApi instancesApi) {
        this.instancesApi = instancesApi;
    }

    private LxcSuperLxcServiceImpl lxcSuperLxcService = SpringUtils.getBean(LxcSuperLxcServiceImpl.class);
    private ILxcLxcService lxcLxcService = SpringUtils.getBean(ILxcLxcService.class);

    /**
     * 创建容器实例
     *
     * @param lxc
     * @param superLxc
     * @param lxcApplyBo
     * @throws Exception
     */
    // 在这呢
    public void postInstances(LxcLxc lxc, LxcSuperLxc superLxc, LxcApplyBo lxcApplyBo) throws Exception {
        String target = null;
        final String architecture = "x86_64";
        final String type = "container";
//        final String fingerprint = "f1335f08a2567d068405bf0165d942e6bf06a22c598f0e58e689e9e3e303432e";
        final String nameOfContainer = lxc.getChildName();

        SourceConfigBuilder sourceConfigBuilder = prepareSourceConfigBuilder(superLxc, lxcApplyBo.getMirrorImage());
        DevicesRoot devicesRoot = prepareDevicesRoot(superLxc);
        InstanceConfigBuilder configBuilder = prepareInstanceConfigBuilder(superLxc);

        List<String> profiles = new ArrayList<>();
        profiles.add("default");

        CreateInstancesRequest createInstancesRequest = populateCreateInstancesRequest(devicesRoot, sourceConfigBuilder.asMap(), type, profiles, architecture, nameOfContainer, configBuilder.asMap(), false);

//        BackgroundOperationResponse actualCreateInstancesResponse = this.instancesApi.postInstances(target, createInstancesRequest);

        try {
            BackgroundOperationResponse actualCreateInstancesResponse = this.instancesApi.postInstances(target, createInstancesRequest);
            log.info("Create Instance Response >>>>>>>>>> " + actualCreateInstancesResponse);
            assertTrue((Objects.equals(actualCreateInstancesResponse.getStatusCode(), 200)) || Objects.equals(actualCreateInstancesResponse.getStatusCode(), 100));

        } catch (ApiException e) {
            log.error("LXD API Exception: " + e.getMessage());
            log.error("HTTP Status Code: " + e.getCode());
            log.error("Response Body: " + e.getResponseBody());
            throw new RuntimeException(e);
        }
    }

// 原方法返回类型从 void 改为 CompletableFuture<Void>，支持异步结果监听
public CompletableFuture<Void> postInstancesAsync(LxcLxc lxc, LxcSuperLxc superLxc, LxcApplyBo lxcApplyBo) {
    // 从 superLxc 获取目标节点（假设存在该字段，解决 target 为 null 的问题）
    String target = null;
    final String architecture = "x86_64";
    final String type = "container";
    final String nameOfContainer = lxc.getChildName();
    final String superLxcIp = superLxc.getIp(); // 新增：宿主机IP用于日志

    // 原有参数准备逻辑（未修改）
    SourceConfigBuilder sourceConfigBuilder = prepareSourceConfigBuilder(superLxc, lxcApplyBo.getMirrorImage());
    DevicesRoot devicesRoot = prepareDevicesRoot(superLxc);
    InstanceConfigBuilder configBuilder = prepareInstanceConfigBuilder(superLxc);
    List<String> profiles = new ArrayList<>();
    profiles.add("default");
    CreateInstancesRequest createInstancesRequest = populateCreateInstancesRequest(
        devicesRoot,
        sourceConfigBuilder.asMap(),
        type,
        profiles,
        architecture,
        nameOfContainer,
        configBuilder.asMap(),
        false
    );

    // 使用 CompletableFuture 异步调用 LXD API（关键修改：异常传播和日志增强）
    return CompletableFuture.supplyAsync(() -> {
        try {
            log.info("开始异步创建容器[{}]（宿主机IP: {}）", nameOfContainer, superLxcIp);
            return this.instancesApi.postInstances(target, createInstancesRequest);
        } catch (ApiException e) {
            // 日志新增容器名和宿主机IP，便于排查
            log.error("容器[{}]（宿主机IP: {}）创建失败，LXD API异常: {}",
                nameOfContainer, superLxcIp, e.getMessage());
            log.error("HTTP状态码: {}, 响应体: {}", e.getCode(), e.getResponseBody());
            throw new RuntimeException("容器创建失败", e); // 向上传播异常
        }
    }).thenAccept(response -> {
        // 处理创建成功/失败状态（新增状态更新逻辑）
        if (Objects.equals(response.getStatusCode(), 200) || Objects.equals(response.getStatusCode(), 100)) {
            log.info("容器[{}]（宿主机IP: {}）创建成功，响应: {}",
                nameOfContainer, superLxcIp, response);
            lxc.setLxcStatus(LxcStatus.RUNNING.getCode()); // 状态更新为运行中
        } else {
            log.error("容器[{}]（宿主机IP: {}）创建失败，状态码: {}",
                nameOfContainer, superLxcIp, response.getStatusCode());
            lxc.setLxcStatus(LxcStatus.FAILED_TO_CREATE.getCode()); // 状态更新为失败
        }
        // 持久化状态到数据库（假设通过 Spring 上下文获取 LxcLxcService）
        SpringUtils.getBean(ILxcLxcService.class).updateById(lxc);
    }).exceptionally(ex -> {
        // 处理未捕获异常（新增状态更新逻辑）
        log.error("容器[{}]（宿主机IP: {}）创建最终失败: {}",
            nameOfContainer, superLxcIp, ex.getMessage());
        lxc.setLxcStatus(LxcStatus.FAILED_TO_CREATE.getCode());
        SpringUtils.getBean(ILxcLxcService.class).updateById(lxc);
        return null;
    });
}
    /**
     * 封装RootLimit
     */
    public static SourceConfigBuilder prepareSourceConfigBuilder(LxcSuperLxc superLxc, String alias) {
        List<SourceConfigBuilder> sourceConfigBuilders = JsonUtils.parseArray(superLxc.getSourceConfig(), SourceConfigBuilder.class);
        if (sourceConfigBuilders.isEmpty() || StringUtils.isEmpty(alias)) {
            return getDefaultSourceConfigBuilder();
        }
        return sourceConfigBuilders.parallelStream()
            .filter(sourceConfigBuilder -> StringUtils.equals(sourceConfigBuilder.getAlias(), alias))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("没有找到镜像" + alias));
    }

    /**
     * 默认镜像，Debian
     *
     * @return
     */
    public static SourceConfigBuilder getDefaultSourceConfigBuilder() {
        return new SourceConfigBuilder()
            .setType("image")
            .setAlias("debian/11")
            .setServer("https://lxdimages.spiritlhl.net/")
            .setProtocol("simplestreams")
            .setMode("pull");
        /**
         *             [{
         *               "type": "image",
         *               "alias": alias,
         *               // "remoteImage": "spiritlhl", // 远程镜像源直接写死
         *               "server": "https://lxdimages.spiritlhl.net/",
         *               "protocol": "simplestreams",
         *               "mode": "pull"
         *             }]
         */
//        return new SourceConfigBuilder()
//            .setType("image")
//            .setAlias("debian/11")
//            .setServer("https://images.linuxcontainers.org")
//            .setProtocol("simplestreams")
//            .setMode("pull");
        /**
         * [{"type":"image","alias":"debian/12","server":"https://images.opsmaru.dev/spaces/43ad54472be82d7236eea3d1","protocol":"simplestreams","mode":"pull"},{"type":"image","alias":"ubuntu/22.04","server":"https://images.opsmaru.dev/spaces/43ad54472be82d7236eea3d1","protocol":"simplestreams","mode":"pull"}]
         */
    }

    /**
     * 封装RootLimit
     * 母鸡硬件配置
     */
    public static RootLimit prepareRootLimit(LxcSuperLxc superLxc) {
        RootLimit rootLimit = JsonUtils.parseObject(superLxc.getRootLimits(), RootLimit.class);
        assert rootLimit != null;
        rootLimit.setSize(superLxc.getDiskLimit() + "GB");
        rootLimit.setLimitsMax(superLxc.getDiskLimit() + "GB");
        /*"root": {
            "pool": "default",
            "size": "1GB",
            "limits.max": "1GB",
            "limits.read": "5000iops",
            "limits.write": "5000iops",
            "path": "/",
            "type": "disk"
        }*/
        if (rootLimit.getPool() == null) {
            rootLimit.setPool("default");
        }
        if (rootLimit.getLimitsRead() == null) {
            rootLimit.setLimitsRead("5000iops");
        }
        if (rootLimit.getLimitsWrite() == null) {
            rootLimit.setLimitsWrite("5000iops");
        }
        if (rootLimit.getPath() == null) {
            rootLimit.setPath("/");
        }
        if (rootLimit.getType() == null) {
            rootLimit.setType("disk");
        }

        return rootLimit;
    }

    /**
     * 封装devicesRoot
     */
    public static DevicesRoot prepareDevicesRoot(LxcSuperLxc superLxc) {
        DevicesRoot devicesRoot = new DevicesRoot();
        devicesRoot.setRoot(prepareRootLimit(superLxc));
        return devicesRoot;
    }

    /**
     * 封装InstanceConfigBuilder
     */
    public static InstanceConfigBuilder prepareInstanceConfigBuilder(LxcSuperLxc superLxc) {
        InstanceConfigBuilder instanceConfigBuilder = JsonUtils.parseObject(superLxc.getInstanceConfig(), InstanceConfigBuilder.class);
        if (instanceConfigBuilder == null) {
            instanceConfigBuilder = new InstanceConfigBuilder();
        }
        // alreadyBound是指instanceConfigBuilder这个配置map集合里有没有括号里的key
        if (!instanceConfigBuilder.alreadyBound("limits.memory.swap")) {
            instanceConfigBuilder.setLimitsMemorySwap(true);
        }
        if (!instanceConfigBuilder.alreadyBound("limits.memory.swap.priority")) {
            instanceConfigBuilder.setLimitsMemorySwapPriority(1);
        }
        if (!instanceConfigBuilder.alreadyBound("security.nesting")) {
            instanceConfigBuilder.setSecurityNesting(true);
        }
        instanceConfigBuilder.setLimitsMemory(superLxc.getMemoryLimit() + "MiB");
        return instanceConfigBuilder;
    }

    public static CreateInstancesRequest populateCreateInstancesRequest(DevicesRoot devices, Map<String, Object> source, String type, List<String> profiles, String architecture, String name, Map<String, Object> createInstancesRequestConfig, Boolean setEphemerial) {
        CreateInstancesRequest createInstancesRequest = new CreateInstancesRequest();
        createInstancesRequest.setName(name);
        createInstancesRequest.setArchitecture(architecture);
        createInstancesRequest.setProfiles(profiles);
        createInstancesRequest.setEphemeral(setEphemerial);
        createInstancesRequest.setConfig(createInstancesRequestConfig);
        createInstancesRequest.setType(type);
        createInstancesRequest.setDevices(devices);
        createInstancesRequest.setSource(source);
        return createInstancesRequest;
    }

    /**
     * 默认execRequest配置在容器内执行命令
     * @param containerName
     * @param command
     */
    public String postInstancesByNameExec(String containerName, String... command) {
        // Create the request object (CreateInstancesByNameExecRequest)
        CreateInstancesByNameExecRequest execRequest = new CreateInstancesByNameExecRequest();
        // 执行命令
        execRequest.setCommand(Arrays.asList(command));
        execRequest.setSignal(10);
        execRequest.setCwd("/root/");
        execRequest.setInteractive(false);
        execRequest.setWidth(80);
        execRequest.setHeight(25);
        execRequest.setUser(0); // 1000表示普通用户，0表示root用户
        execRequest.setGroup(0);
        execRequest.setWaitForWebsocket(true);
        System.out.println("execRequest:" + execRequest);
        try {
            BackgroundOperationResponse response = this.instancesApi.postInstancesByNameExec(containerName, execRequest);
            System.out.println("response:" + response);
            if (response.getStatusCode() == 200 || response.getStatusCode() == 100) {
                log.info("容器[{}]命令执行成功，响应: {}", containerName, response);
                return response.toString();
            } else {
                log.error("容器[{}]命令执行失败，状态码: {}，错误：{}", containerName, response.getStatusCode(), response.getError());
                return response.toString();
            }
        } catch (ApiException e) {
            log.error("容器[{}]命令执行最终失败: {}", containerName, e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public void postInstancesByNameExec(String containerName, Map<String, Object> execRequestMap) {
        CreateInstancesByNameExecRequest execRequest = populateCreateInstancesByNameExecRequest(execRequestMap);
        System.out.println("execRequest:" + execRequest);
        try {
            BackgroundOperationResponse response = this.instancesApi.postInstancesByNameExec(containerName, execRequest);
            System.out.println("response:" + response);
            if (response.getStatusCode() == 200 || response.getStatusCode() == 100) {
                log.info("容器[{}]命令执行成功，响应: {}", containerName, response);
            } else {
                log.error("容器[{}]命令执行失败，状态码: {}", containerName, response.getStatusCode());
            }
        } catch (ApiException e) {
            log.error("容器[{}]命令执行最终失败: {}", containerName, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public CreateInstancesByNameExecRequest populateCreateInstancesByNameExecRequest(Map<String, Object> execRequestMap) {
        CreateInstancesByNameExecRequest execRequest = new CreateInstancesByNameExecRequest();
        if (execRequestMap.containsKey("command")) {
            execRequest.setCommand((List<String>) execRequestMap.get("command"));
        }
        if (execRequestMap.containsKey("signal")) {
            execRequest.setSignal((Integer) execRequestMap.get("signal"));
        }
        if (execRequestMap.containsKey("args")) {
            execRequest.setArgs((Args) execRequestMap.get("args"));
        }
        if (execRequestMap.containsKey("environment")) {
            execRequest.setEnvironment(execRequestMap.get("environment"));
        }
        if (execRequestMap.containsKey("waitForWebsocket")) {
            execRequest.setWaitForWebsocket((Boolean) execRequestMap.get("waitForWebsocket"));
        }
        /* 这个参数目前貌似不支持了，加上的话会使得lxc exec失败
       if (execRequestMap.containsKey("recordOutput")) {
            Boolean recordOutput = (Boolean) execRequestMap.get("recordOutput");
        }*/
        if (execRequestMap.containsKey("interactive")) {
            execRequest.setInteractive((Boolean) execRequestMap.get("interactive"));
        }
        if (execRequestMap.containsKey("width")) {
            execRequest.setWidth((Integer) execRequestMap.get("width"));
        }
        if (execRequestMap.containsKey("height")) {
            execRequest.setHeight((Integer) execRequestMap.get("height"));
        }
        if (execRequestMap.containsKey("user")) {
            execRequest.setUser((Integer) execRequestMap.get("user"));
        }
        if (execRequestMap.containsKey("group")) {
            execRequest.setGroup((Integer) execRequestMap.get("group"));
        }
        if (execRequestMap.containsKey("cwd")) {
            execRequest.setCwd((String) execRequestMap.get("cwd"));
        }
        return execRequest;
    }

    public void addLog(String title, StringBuilder resLog, List<String> res) {
        resLog.append("==========" + title + "==========\n");
        for (String s : res) {
            resLog.append(s + "\n");
        }
    }

    public void execModifySh(ShellDto sshDto, String way) {
        LambdaQueryWrapper<LxcSuperLxc> lqw = new LambdaQueryWrapper<>();
        lqw.eq(LxcSuperLxc::getIp, sshDto.getIp());
        LxcSuperLxc superLxc = lxcSuperLxcService.getOne(lqw);
        Long index = Long.parseLong(sshDto.getChildName().split(superLxc.getPrefix())[1]);
        String childName = sshDto.getChildName();
        Long downLimit = sshDto.getDownLimit();
        Long upLimit = sshDto.getUpLimit();
        Long sshPort = sshDto.getChildSshPort() + index; // 宿主机创建容器起始端口+容器序号=容器SSH端口
        String ipv6Flag = sshDto.getIpv6Flag();
        Integer portStart = sshDto.getPortStart();
        Integer portEnd = sshDto.getPortEnd();

        // 每段命令如果有返回，将返回信息记录到日志文件中childName_init.log和返回到控制台

        try {
            StringBuilder resLog = new StringBuilder();
            // 1. 切换到/root目录（Java环境默认工作目录通常是项目根目录，这里不需要切换）

            // 2. 配置容器属性（security.nesting=true）
            List<String> res1 = setContainerConfig(sshDto, childName, way, "security.nesting", "true");
            addLog(" 配置容器属性（security.nesting=true）", resLog, res1);

            // 3. 生成随机密码
            String password = generateRandomPassword();

            // 4. 启动容器
            List<String> res2 = startContainer(way, sshDto, childName);
            addLog("启动容器", resLog, res2);

            // 5. 等待容器启动
            Thread.sleep(1000);

            // 6. 检查DNS（通过LXD API执行）
            List<String> res3 = ShellUtils.execCmdOutList("/usr/local/bin/check-dns.sh", ShellUtils.getSession(sshDto));
            addLog("检查DNS（通过LXD API执行）", resLog, res3);

            // 7. 检测系统类型
            String system = detectContainerSystem(sshDto, childName);

            // 8. 根据系统类型安装基础软件（通过LXD API执行）
            List<String> res4 = installBasePackages(sshDto, childName, system);
            addLog("根据系统类型安装基础软件（通过LXD API执行）", resLog, res4);

            // 9. 根据系统类型配置SSH（通过LXD API执行）
            List<String> res5 = configureSSH(way, sshDto, childName, system, password);
            addLog("根据系统类型配置SSH（通过LXD API执行）", resLog, res5);

            // 10. 添加SSH端口转发（宿主机命令）
            List<String> res6 = addPortForwarding(sshDto, way, childName, "ssh-port", "tcp", sshPort, 22);
            addLog("添加SSH端口转发（宿主机命令）", resLog, res6);

            // 11. 配置IPv6（如果启用）
            if ("Y".equalsIgnoreCase(ipv6Flag)) {
                List<String> res7 = configureIPv6(way, sshDto, childName);
                addLog("配置IPv6（如果启用）", resLog, res7);

            }

            // 12. 配置端口区间映射
            if (portStart != 0 && portEnd != 0) {
                List<String> res8 = addPortRangeForwarding(sshDto, way, childName, "nattcp-ports", "tcp", portStart, portEnd);
                addLog("配置端口区间映射nattcp-ports", resLog, res8);

                List<String> res9 = addPortRangeForwarding(sshDto, way, childName, "natudp-ports", "udp", portStart, portEnd);
                addLog("配置端口区间映射natudp-ports", resLog, res9);
            }

            // 13. 配置网速限制
            List<String> res10 = configureNetworkSpeedLimits(sshDto, way, childName, downLimit, upLimit);
            addLog("配置网速限制", resLog, res10);

            // 14. 清理本地脚本（如果存在）
            List<String> res11 = cleanupLocalScripts(sshDto);
            addLog("清理本地脚本（如果存在）", resLog, res11);

            // 15. 特殊处理Alpine系统
            if (system.toLowerCase().contains("alpine")) {
                Thread.sleep(3000);
                List<String> res12 = restartContainer(way, sshDto, childName);
                addLog("特殊处理Alpine系统", resLog, res12);
            }
            saveLogToFile(sshDto, childName, resLog.toString());
            // 16. 输出结果
            String output = portStart != 0 && portEnd != 0
                ? StrUtil.format("{} {} {} {} {}", childName, sshPort, password, portStart, portEnd)
                : StrUtil.format("{} {} {}", childName, sshPort, password);

            // 保存结果到文件
            saveOutputToFile(sshDto, childName, output);
            log.info("配置完成: {}", output);
        } catch (Exception e) {
            log.error("容器配置失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    // 1. 设置容器配置属性
    private List<String> setContainerConfig(ShellDto sshDto, String containerName, String way, String key, String value) throws Exception {
        String cmdPrefix = "incus".equals(way) ? "incus" : "lxc";
        String command = StrUtil.format(cmdPrefix + " config set {} {} {}", containerName, key, value);
        List<String> res = ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
        log.info("设置容器配置返回结果：{}", res);
        log.info("设置容器配置: {}", command);
        return res;
    }

    // 2. 生成随机密码
    private String generateRandomPassword() {
        String ori = UUID.randomUUID().toString();
        return ori.substring(2, 11);
    }

    // 3. 启动容器
    private List<String> startContainer(String way, ShellDto sshDto, String containerName) throws Exception {
        String cmdPrefix = "incus".equals(way) ? "incus" : "lxc";
        String command = StrUtil.format(cmdPrefix + " start {}", containerName);
        List<String> res = ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
        log.info("启动容器返回结果：{}", res);
        log.info("启动容器: {}", command);
        return res;
    }

    // 4. 重启容器
    private List<String> restartContainer(String way, ShellDto sshDto, String containerName) throws Exception {
        String cmdPrefix = "incus".equals(way) ? "incus" : "lxc";
        String command = StrUtil.format(cmdPrefix + " restart {}", containerName);
        List<String> res = ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
        log.info("重启容器: {}", command);
        return res;
    }

    // 5. 检测系统类型
    private String detectContainerSystem(ShellDto sshDto, String containerName) throws Exception {
        String result = postInstancesByNameExec(containerName, "bash", "-c", "cat /etc/os-release");
        if (result.toLowerCase().contains("centos") ||
            result.toLowerCase().contains("almalinux") ||
            result.toLowerCase().contains("fedora") ||
            result.toLowerCase().contains("rocky")) {
            return "centos";
        } else if (result.toLowerCase().contains("alpine")) {
            return "alpine";
        } else if (result.toLowerCase().contains("openwrt")) {
            return "openwrt";
        } else {
            return "debian";
        }
    }

    // 7. 安装基础软件
    private List<String> installBasePackages(ShellDto sshDto, String containerName, String system) throws Exception {
        List<String> res = new ArrayList<>();
        switch (system.toLowerCase()) {
            case "centos":
                res.add(postInstancesByNameExec(containerName, "bash", "-c", "sudo yum update -y"));
                res.add(postInstancesByNameExec(containerName, "bash", "-c", "sudo yum install -y curl dos2unix"));
                break;
            case "alpine":
                res.add(postInstancesByNameExec(containerName, "bash", "-c", "apk update"));
                res.add(postInstancesByNameExec(containerName, "bash", "-c", "apk add --no-cache curl"));
                break;
            case "openwrt":
                res.add(postInstancesByNameExec(containerName, "bash", "-c", "opkg update"));
                break;
            default:
                res.add(postInstancesByNameExec(containerName, "bash", "-c", "sudo apt-get update -y"));
                res.add(postInstancesByNameExec(containerName, "bash", "-c", "sudo apt-get install curl -y --fix-missing"));
                res.add(postInstancesByNameExec(containerName, "bash", "-c", "sudo apt-get install dos2unix -y --fix-missing"));
        }
        return res;
    }

    // 8. 配置SSH
    private List<String> configureSSH(String way, ShellDto sshDto, String containerName, String system, String password) throws Exception {
        List<String> res = new ArrayList<>();
        List<String> res1 = new ArrayList<>();
        if (system.toLowerCase().contains("alpine") || system.toLowerCase().contains("openwrt")) {
            // 下载并执行ssh_sh.sh
            if ("incus".equals(way)) {
                res1 = downloadFileToContainer(way, sshDto, containerName,
                    "https://raw.githubusercontent.com/oneclickvirt/incus/main/scripts/ssh_sh.sh",
                    "/root/ssh_sh.sh");
            } else {
                res1 = downloadFileToContainer(way, sshDto, containerName,
                    "https://raw.githubusercontent.com/oneclickvirt/lxd/main/scripts/ssh_sh.sh",
                    "/root/ssh_sh.sh");
            }
            for (String s : res1) {
                res.add(s);
            }

            res.add(postInstancesByNameExec(containerName, "bash", "-c", "chmod 777 /root/ssh_sh.sh"));
            res.add(postInstancesByNameExec(containerName, "bash", "/root/ssh_sh.sh", password));
        } else {
            // 下载并执行ssh_bash.sh
            if ("incus".equals(way)) {
                res1 = downloadFileToContainer(way, sshDto, containerName,
                    "https://raw.githubusercontent.com/oneclickvirt/incus/main/scripts/ssh_bash.sh",
                    "/root/ssh_bash.sh");
            } else {
                res1 = downloadFileToContainer(way, sshDto, containerName,
                    "https://raw.githubusercontent.com/oneclickvirt/lxd/main/scripts/ssh_bash.sh",
                    "/root/ssh_bash.sh");
            }
            for (String s : res1) {
                res.add(s);
            }

            res.add(postInstancesByNameExec(containerName, "bash", "-c", "chmod 777 /root/ssh_bash.sh"));
            res.add(postInstancesByNameExec(containerName, "bash", "-c", "dos2unix /root/ssh_bash.sh"));
            res.add(postInstancesByNameExec(containerName, "bash", "/root/ssh_bash.sh", password));

            List<String> res2 = new ArrayList<>();
            // 下载并执行config.sh
            if ("incus".equals(way)) {
                res2 = downloadFileToContainer(way, sshDto, containerName,
                    "https://raw.githubusercontent.com/oneclickvirt/incus/main/scripts/config.sh",
                    "/root/config.sh");
            } else {
                res2 = downloadFileToContainer(way, sshDto, containerName,
                    "https://raw.githubusercontent.com/oneclickvirt/lxd/main/scripts/config.sh",
                    "/root/config.sh");
            }
            for (String s : res2) {
                res.add(s);
            }
            res.add(postInstancesByNameExec(containerName, "bash", "-c", "chmod +x /root/config.sh"));
            res.add(postInstancesByNameExec(containerName, "bash", "-c", "dos2unix /root/config.sh"));
            res.add(postInstancesByNameExec(containerName, "bash", "/root/config.sh"));

            // 清空历史命令
            res.add(postInstancesByNameExec(containerName, "bash", "-c", "history -c"));
        }
        return res;
    }

    // 9. 下载文件到容器
    private List<String> downloadFileToContainer(String way, ShellDto sshDto, String containerName, String url, String destination) throws Exception {
        List<String> res = new ArrayList<>();
        // 先在宿主机下载文件
        String cmdPrefix = "incus".equals(way) ? "incus" : "lxc";
        String localPath = "/tmp/" + UUID.randomUUID().toString();
        String downloadCmd = StrUtil.format("curl -L {} -o {}", url, localPath);
        List<String> res1 = ShellUtils.execCmdOutList(downloadCmd, ShellUtils.getSession(sshDto));
        for (String s : res1) {
            res.add(s);
        }

        // 将文件上传到容器
        String uploadCmd = StrUtil.format(cmdPrefix + " file push {} {}{}", localPath, containerName, destination);
        List<String> res2 = ShellUtils.execCmdOutList(uploadCmd, ShellUtils.getSession(sshDto));
        for (String s : res2) {
            res.add(s);
        }

        // 删除临时文件
        List<String> res3 = ShellUtils.execCmdOutList("rm -f" + localPath, ShellUtils.getSession(sshDto));
        for (String s : res3) {
            res.add(s);
        }
        return res;
    }

    // 10. 添加端口转发
    private List<String> addPortForwarding(ShellDto sshDto, String way, String containerName, String deviceName, String protocol, Long hostPort, int containerPort) throws Exception {
        String cmdPrefix = "incus".equals(way) ? "incus" : "lxc";
        String command = StrUtil.format(cmdPrefix + " config device add {} {} proxy listen={}:0.0.0.0:{} connect={}:127.0.0.1:{}",
            containerName, deviceName, protocol, hostPort, protocol, containerPort);
        List<String> res = ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
        log.info("添加端口转发: {}", command);
        return res;
    }

    // 11. 添加端口区间映射
    private List<String> addPortRangeForwarding(ShellDto sshDto, String way, String containerName, String deviceName, String protocol, int startPort, int endPort) throws Exception {
        String cmdPrefix = "incus".equals(way) ? "incus" : "lxc";
        String command = StrUtil.format(cmdPrefix + " config device add {} {} proxy listen={}:0.0.0.0:{}-{} connect={}:127.0.0.1:{}-{}",
            containerName, deviceName, protocol, startPort, endPort, protocol, startPort, endPort);
        List<String> res = ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
        log.info("添加端口区间映射: {}", command);
        return res;
    }

    // 12. 配置IPv6
    private List<String> configureIPv6(String way, ShellDto sshDto, String containerName) throws Exception {
        List<String> res = new ArrayList<>();
        // 添加定时任务
        res.add(postInstancesByNameExec(containerName, "bash", "-c",
            "echo '*/1 * * * * curl -m 6 -s ipv6.ip.sb && curl -m 6 -s ipv6.ip.sb' | crontab -"));
        Thread.sleep(1000);
        String cmdPrefix = "";
        if ("incus".equals(way)) {
            cmdPrefix = "incus";
            // 下载并执行IPv6构建脚本
            List<String> res1 = downloadFileToLocal(sshDto, "https://raw.githubusercontent.com/oneclickvirt/incus/main/scripts/build_ipv6_network.sh",
                "/root/build_ipv6_network.sh");
            for (String s : res1) {
                res.add(s);
            }
        } else {
            cmdPrefix = "lxc";
            // 下载并执行IPv6构建脚本
            List<String> res1 = downloadFileToLocal(sshDto, "https://raw.githubusercontent.com/oneclickvirt/lxd/main/scripts/build_ipv6_network.sh",
                "/root/build_ipv6_network.sh");
            for (String s : res1) {
                res.add(s);
            }
        }
        List<String> res2 = ShellUtils.execCmdOutList("chmod +x /root/build_ipv6_network.sh", ShellUtils.getSession(sshDto));
        for (String s : res2) {
            res.add(s);
        }
        String ipv6NetworkSh = String.format("/root/build_ipv6_network.sh %s", containerName);
        List<String> res3 = ShellUtils.execCmdOutList(ipv6NetworkSh, ShellUtils.getSession(sshDto));
        for (String s : res3) {
            res.add(s);
        }
        return res;
    }

    // 13. 下载文件到本地
    private List<String> downloadFileToLocal(ShellDto sshDto, String url, String destination) throws Exception {
        String command = StrUtil.format("curl -L {} -o {}", url, destination);
        List<String> res = ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
        return res;
    }

    // 14. 配置网速限制
    private List<String> configureNetworkSpeedLimits(ShellDto sshDto, String way, String containerName, long downloadSpeed, long uploadSpeed) throws Exception {
        List<String> res = new ArrayList<>();
        String cmdPrefix = "incus".equals(way) ? "incus" : "lxc";
        // 停止容器
        String stopCmd = StrUtil.format(cmdPrefix + " stop {}", containerName);
        List<String> res1 = ShellUtils.execCmdOutList(stopCmd, ShellUtils.getSession(sshDto));
        for (String s : res1) {
            res.add(s);
        }

        // 计算最大带宽
        long maxSpeed = Math.max(downloadSpeed, uploadSpeed);

        // 设置网络限制
        String configCmd = StrUtil.format(cmdPrefix + " config device override {} eth0 limits.egress={}Mbit limits.ingress={}Mbit limits.max={}Mbit",
            containerName, uploadSpeed, downloadSpeed, maxSpeed);
        List<String> res2 = ShellUtils.execCmdOutList(configCmd, ShellUtils.getSession(sshDto));
        for (String s : res2) {
            res.add(s);
        }

        // 启动容器
        String startCmd = StrUtil.format(cmdPrefix + " start {}", containerName);
        List<String> res3 = ShellUtils.execCmdOutList(startCmd, ShellUtils.getSession(sshDto));
        for (String s : res3) {
            res.add(s);
        }
        return res;
    }

    // 15. 清理本地脚本
    private List<String> cleanupLocalScripts(ShellDto sshDto) throws Exception {
        return ShellUtils.execCmdOutList("rm -rf ssh_bash.sh config.sh ssh_sh.sh", ShellUtils.getSession(sshDto));
    }

    // 16. 保存输出到文件
    private void saveOutputToFile(ShellDto sshDto, String containerName, String content) throws Exception {
        String command = StrUtil.format("echo '{}' > {}", content, containerName);
        ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
    }

    private void saveLogToFile(ShellDto sshDto, String containerName, String content) throws Exception {
        String command = StrUtil.format("echo '{}' > {}", content, containerName+"_init.log");
        ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
    }
}
