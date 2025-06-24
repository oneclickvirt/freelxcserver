package com.lxc.machine.utils;

import com.lxc.common.enums.LxcStatus;
import com.lxc.common.utils.JsonUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.spring.SpringUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcApplyBo;
import com.lxc.machine.service.ILxcLxcService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.relxd.lxd.ApiException;
import org.relxd.lxd.RelxdApiClient;
import org.relxd.lxd.api.InstancesApi;
import org.relxd.lxd.builders.InstanceConfigBuilder;
import org.relxd.lxd.builders.SourceConfigBuilder;
import org.relxd.lxd.model.BackgroundOperationResponse;
import org.relxd.lxd.model.CreateInstancesRequest;
import org.relxd.lxd.model.DevicesRoot;
import org.relxd.lxd.model.RootLimit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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


}
