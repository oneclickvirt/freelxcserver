package com.lxc.machine.utils;

import com.lxc.common.utils.JsonUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcApplyBo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static org.wildfly.common.Assert.assertTrue;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstancesUtils {
    //    private final InstancesApi API = SpringUtils.getBean("lxcApi", InstancesApi.class);
    private InstancesApi instancesApi;

    public static InstancesUtils target(String ip) {
        assert ip != null;
        String baseUrl = "https://" + ip + ":9969";
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

        BackgroundOperationResponse actualCreateInstancesResponse = this.instancesApi.postInstances(target, createInstancesRequest);
        log.info("Create Instance Response >>>>>>>>>> " + actualCreateInstancesResponse);
        assertTrue((Objects.equals(actualCreateInstancesResponse.getStatusCode(), 200)) || Objects.equals(actualCreateInstancesResponse.getStatusCode(), 100));
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
     * 默认镜像
     *
     * @return
     */
    public static SourceConfigBuilder getDefaultSourceConfigBuilder() {
        return new SourceConfigBuilder()
            .setType("image")
            .setAlias("debian/11")
            .setServer("https://images.linuxcontainers.org")
            .setProtocol("simplestreams")
            .setMode("pull");
    }

    /**
     * 封装RootLimit
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
