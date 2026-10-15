package com.lxc.machine.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lxc.common.core.domain.dto.ShellDto;
import com.lxc.common.utils.JsonUtils;
import com.lxc.common.utils.ShellUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.spring.SpringUtils;
import com.lxc.machine.domain.LxcContainerState;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcApplyBo;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.impl.LxcSuperLxcServiceImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.relxd.lxd.ApiException;
import org.relxd.lxd.ApiResponse;
import org.relxd.lxd.RelxdApiClient;
import org.relxd.lxd.api.InstancesApi;
import org.relxd.lxd.api.OperationsApi;
import org.relxd.lxd.builders.InstanceConfigBuilder;
import org.relxd.lxd.builders.SourceConfigBuilder;
import org.relxd.lxd.model.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstancesUtils {
    //    private final InstancesApi API = SpringUtils.getBean("lxcApi", InstancesApi.class);
    private InstancesApi instancesApi;
    private OperationsApi operationsApi;
    public static InstancesUtils target(String ip,String port) {
        assert ip != null;
        // port : 8443-lxd 9969-incus
//        String baseUrl = "https://" + ip + ":9969";
        String baseUrl = "https://" + ip + ":" + port;
        return new InstancesUtils(new InstancesApi(new RelxdApiClient(baseUrl)), new OperationsApi(new RelxdApiClient(baseUrl)));
    }

    private InstancesUtils(InstancesApi instancesApi, OperationsApi operationsApi) {
        this.instancesApi = instancesApi;
        this.operationsApi = operationsApi;
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

        try {
            BackgroundOperationResponse actualCreateInstancesResponse = this.instancesApi.postInstances(target, createInstancesRequest);
            log.info("Create Instance Response >>>>>>>>>> " + actualCreateInstancesResponse);
            String operation = actualCreateInstancesResponse.getOperation();
            if (!StringUtils.isEmpty(operation)) {
                String operationId = operation.split("/")[operation.split("/").length - 1];
                BackgroundOperationResponse operationsUUID = this.operationsApi.getOperationsUUID(operationId, null, null);
                log.info("Create Instance Operation Response >>>>>>>>>> " + operationsUUID);
            }
        } catch (ApiException e) {
            log.error("LXD API Exception: " + e.getMessage());
            log.error("HTTP Status Code: " + e.getCode());
            log.error("Response Body: " + e.getResponseBody());
            throw new RuntimeException(e);
        }
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
            .setAlias("debian/11/cloud")
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
//        rootLimit.setSize(superLxc.getDiskLimit() + "GB");
//        rootLimit.setLimitsMax(superLxc.getDiskLimit() + "GB");
        rootLimit.setSize(superLxc.getDiskLimit());
        rootLimit.setLimitsMax(superLxc.getDiskLimit());
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
        if (!StringUtils.isEmpty(superLxc.getMemoryLimit())) {
            // 将内存数值+单位正则分割开来
            String memoryLimitStr = superLxc.getMemoryLimit();
            Pattern pattern = Pattern.compile("(\\d+)(\\D+)"); // 捕获数字(\\d+)和单位(\\D+)
            Matcher matcher = pattern.matcher(memoryLimitStr);
            if (!matcher.matches()) {
                throw new RuntimeException("内存限制格式错误: " + memoryLimitStr + "（正确格式如：2GB、512MB）");
            }
            String memoryValue = matcher.group(1); // 提取数字部分（第一组）
            String memoryUnit = matcher.group(2); // 提取单位部分（第二组）

            if (memoryUnit.equals("TB")) {
                instanceConfigBuilder.setLimitsMemory(memoryValue + "TiB");
            } else if (memoryUnit.equals("MB")) {
                instanceConfigBuilder.setLimitsMemory(memoryValue + "MiB");
            } else if (memoryUnit.equals("KB")) {
                instanceConfigBuilder.setLimitsMemory(memoryValue + "KiB");
            } else if (memoryUnit.equals("GB")) {
                instanceConfigBuilder.setLimitsMemory(memoryValue + "GiB");
            }
        } else {
            // 默认分配256MB
            instanceConfigBuilder.setLimitsMemory("256MiB");
        }
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
     * 默认execRequest配置在容器内执行命令，只设置必要参数
     * @param containerName
     * @param command
     */
    public String postInstancesByNameExec(String containerName, String... command) {
        // Create the request object (CreateInstancesByNameExecRequest)，只设置必要参数
        CreateInstancesByNameExecRequest execRequest = new CreateInstancesByNameExecRequest();
        // 执行命令
        execRequest.setCommand(Arrays.asList(command));
        execRequest.setSignal(10);
        execRequest.setInteractive(false);
        execRequest.setUser(0); // 1000表示普通用户，0表示root用户
        execRequest.setGroup(0);
        execRequest.setWaitForWebsocket(false);
        System.out.println("execRequest:" + execRequest);
        try {
            BackgroundOperationResponse response = this.instancesApi.postInstancesByNameExec(containerName, execRequest);
//            System.out.println("response:" + response);
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
//         这个参数目前貌似不支持了，加上的话会使得lxc exec失败
        if (execRequestMap.containsKey("recordOutput")) {
            Boolean recordOutput = (Boolean) execRequestMap.get("recordOutput");
        }
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
//        Long index = Long.parseLong(sshDto.getChildName().split(superLxc.getPrefix())[1]);
        String childName = sshDto.getChildName();
        Long downLimit = sshDto.getDownLimit();
        Long upLimit = sshDto.getUpLimit();
        Long sshPort = sshDto.getChildSshPort(); // 宿主机创建容器起始端口+容器序号=容器SSH端口
        log.info("creating children sshPort:{}", sshPort);
        String ipv6Flag = sshDto.getIpv6Flag();
        Integer portStart = sshDto.getPortStart();
        Integer portEnd = sshDto.getPortEnd();
        try {
            // 每段命令如果有返回，将返回信息记录到日志文件中childName_init.log和返回到控制台
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
            Thread.sleep(3000);

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
//            if (system.toLowerCase().contains("alpine")) {
//                Thread.sleep(3000);
//                List<String> res12 = restartContainer(way, sshDto, childName);
//                addLog("特殊处理Alpine系统", resLog, res12);
//            }
            List<String> res12 = restartContainer(way, sshDto, childName);
            addLog("重启容器等待脚本", resLog, res12);
            // 强制暂停一段时间，等待容器内脚本准备完毕
            Thread.sleep(30000);
            List<String> res13 = configAndSSH(way, sshDto, childName, system, password);
            addLog("执行ssh和config脚本", resLog, res13);
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
        log.info("设置容器配置: {}", command);
        log.info("设置容器配置返回结果：{}", res);
        return res;
    }

    // 2. 生成随机密码，替换掉-
    private String generateRandomPassword() {
        String ori = UUID.randomUUID().toString();
        return ori.substring(2, 11).replace("-", "0");
    }

    // 3. 启动容器
    private List<String> startContainer(String way, ShellDto sshDto, String containerName) throws Exception {
        String cmdPrefix = "incus".equals(way) ? "incus" : "lxc";
        String command = StrUtil.format(cmdPrefix + " start {}", containerName);
        List<String> res = ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
//        log.info("启动容器返回结果：{}", res);
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
        String result = postInstancesByNameExec(containerName, "cat /etc/os-release");
        log.info("检测系统类型返回结果：{}", result);
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
                res.add("sudo yum update -y\n" + postInstancesByNameExec(containerName, "sudo", "yum", "update ", "-y"));
                res.add("sudo yum install -y curl dos2unix\n" + postInstancesByNameExec(containerName, "sudo yum install", "-y", "curl dos2unix"));
                break;
            case "alpine":
                res.add("apk update\n" + postInstancesByNameExec(containerName, "apk", "update"));
                res.add("apk add --no-cache curl\n" + postInstancesByNameExec(containerName, "apk add", "--no-cache curl"));
                break;
            case "openwrt":
                res.add("opkg update\n" + postInstancesByNameExec(containerName, "opkg update"));
                break;
            default:
                res.add("sudo apt-get update -y\n" + postInstancesByNameExec(containerName, "sudo", "apt-get", "update", "-y"));
                res.add("sudo apt-get install curl -y --fix-missing\n" + postInstancesByNameExec(containerName, "sudo apt-get install curl", "-y", "--fix-missing"));
                res.add("sudo apt-get install dos2unix -y --fix-missing\n" + postInstancesByNameExec(containerName, "sudo apt-get install dos2unix", "-y", "--fix-missing"));
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
            res.add("chmod 777 /root/ssh_sh.sh\n" + postInstancesByNameExec(containerName, "chmod", "777", "/root/ssh_sh.sh"));
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
//        sleep(10000);
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
        log.info("添加端口转发: {}，执行结果：{}", command, res);
        return res;
    }

    // 11. 添加端口区间映射
    private List<String> addPortRangeForwarding(ShellDto sshDto, String way, String containerName, String deviceName, String protocol, int startPort, int endPort) throws Exception {
        String cmdPrefix = "incus".equals(way) ? "incus" : "lxc";
        String command = StrUtil.format(cmdPrefix + " config device add {} {} proxy listen={}:0.0.0.0:{}-{} connect={}:127.0.0.1:{}-{}",
            containerName, deviceName, protocol, startPort, endPort, protocol, startPort, endPort);
        List<String> res = ShellUtils.execCmdOutList(command, ShellUtils.getSession(sshDto));
        log.info("添加端口区间映射: {}，执行结果：{}", command, res);
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

    public void getInstancesByNameConsoleWithHttpInfo(String instanceName) {
        try {
            ApiResponse<BackgroundOperationResponse> response = instancesApi.getInstancesByNameConsoleWithHttpInfo(instanceName, 0, null);
            if (response.getStatusCode() == 200) {
                log.info("容器[{}]命令执行成功，响应: {}", instanceName, response.getData());
            }
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> configAndSSH(String way, ShellDto sshDto, String containerName, String system, String password) {
        List<String> res = new ArrayList<>();
        List<String> res1 = new ArrayList<>();
        if (system.toLowerCase().contains("alpine") || system.toLowerCase().contains("openwrt")) {
            res.add("chmod 777 /root/ssh_sh.sh\n" + postInstancesByNameExec(containerName, "chmod", "777", "/root/ssh_sh.sh"));
        } else {
            res.add("chmod 777 /root/ssh_bash.sh\n" + postInstancesByNameExec(containerName,  "chmod", "777", "/root/ssh_bash.sh"));
            res.add("dos2unix /root/ssh_bash.sh\n" + postInstancesByNameExec(containerName,  "dos2unix", "/root/ssh_bash.sh"));

            res.add("bash /root/ssh_bash.sh\n" + postInstancesByNameExec(containerName,  "bash", "/root/ssh_bash.sh", password));
            res.add("chmod +x /root/config.sh\n" + postInstancesByNameExec(containerName,  "chmod", "+x", "/root/config.sh"));
            res.add("dos2unix /root/config.sh\n" + postInstancesByNameExec(containerName,  "dos2unix", "/root/config.sh"));
            res.add("/root/config.sh\n" + postInstancesByNameExec(containerName,  "/root/config.sh"));

            // 清空历史命令
            res.add("history -c\n" + postInstancesByNameExec(containerName, "history -c"));
        }
        return res;
    }

    public LxcContainerState getInstancesByNameState(String instancename) {
        try {
            BackgroundOperationResponse response = instancesApi.getInstancesByNameState(instancename, null, null);
//            log.info("容器[{}]状态: {}\n", instancename, response);
            LxcContainerState lxcContainerState = getLxcContainerState(instancename, response);
            // network=send+revc
            lxcContainerState.setNetwork(lxcContainerState.getSend() + lxcContainerState.getRecv());
            return lxcContainerState;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static LxcContainerState getLxcContainerState(String instancename, BackgroundOperationResponse response) {
        Object metadata = response.getMetadata();
        JSONObject entries = JSONUtil.parseObj(metadata);
        LxcContainerState lxcContainerState = new LxcContainerState();
        lxcContainerState.setTimestamp(System.currentTimeMillis() * 1000); // 记录时间戳，单位(ns)；统一采用获取到数据后的建立对象后的该事件点
        lxcContainerState.setInstancename(instancename);
        for (Map.Entry<String, Object> entry : entries) {
            if (entry.getKey().equals("cpu")) {
                JSONObject cpuJson = JSONUtil.parseObj(entry.getValue().toString());
//                System.out.println("oldCpu:" + Double.parseDouble(cpuJson.get("usage").toString()));
                lxcContainerState.setCpu(Double.parseDouble(cpuJson.get("usage").toString()));
            }
            if (entry.getKey().equals("memory")) {
                JSONObject memoryJson = JSONUtil.parseObj(entry.getValue().toString());
                lxcContainerState.setMemory(Double.parseDouble(memoryJson.get("usage").toString()));
            }
            if (entry.getKey().equals("disk")) {
                JSONObject diskJson = JSONUtil.parseObj(entry.getValue().toString());
                JSONObject rootJson = JSONUtil.parseObj(diskJson.get("root").toString());
                lxcContainerState.setDisk(Double.parseDouble(rootJson.get("usage").toString()));
            }
            if (entry.getKey().equals("network")) {
                JSONObject networkJson = JSONUtil.parseObj(entry.getValue().toString());
                String eth0 = networkJson.get("eth0").toString();
                String counters = JSONUtil.parseObj(eth0).get("counters").toString();
                lxcContainerState.setRecv(Double.parseDouble(JSONUtil.parseObj(counters).get("bytes_received").toString()));
                lxcContainerState.setSend(Double.parseDouble(JSONUtil.parseObj(counters).get("bytes_sent").toString()));
            }
        }
        return lxcContainerState;
    }

    public String getOperationLog(String uuid) {
        BackgroundOperationResponse operationsUUID = null;
        try {
            operationsUUID = this.operationsApi.getOperationsUUID(uuid, null, null);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        log.info("Operation Log: {}", operationsUUID);
        return operationsUUID.toString();
    }
}
