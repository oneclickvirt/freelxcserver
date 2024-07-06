package com.lxc.common.constant;

/**
 * 缓存组名称常量
 * <p>
 * key 格式为 cacheNames#ttl#maxIdleTime#maxSize
 * <p>
 * ttl 过期时间 如果设置为0则不过期 默认为0 maxIdleTime 最大空闲时间 根据LRU算法清理空闲数据 如果设置为0则不检测 默认为0 maxSize 组最大长度 根据LRU算法清理溢出数据 如果设置为0则无限长 默认为0
 * <p>
 * 例子: test#60s、test#0#60s、test#0#1m#1000、test#1h#0#500
 *
 * @author Lion Li
 */
public interface CacheNames {

    /**
     * 演示案例
     */
    String DEMO_CACHE = "demo:cache#60s#10m#20";

    /**
     * 系统配置
     */
    String SYS_CONFIG = "sys_config";

    /**
     * 数据字典
     */
    String SYS_DICT = "sys_dict";

    /**
     * OSS内容
     */
    String SYS_OSS = "sys_oss#30d";

    /**
     * OSS配置
     */
    String SYS_OSS_CONFIG = "sys_oss_config";

    /**
     * 在线用户
     */
    String ONLINE_TOKEN = "online_tokens";

    /**
     * 空闲主机列表
     */
    String FREE_LXC_LIST = "lxc:free_lxc_list";

    /**
     * 主页统计
     */
    String STATISTICS_LXC_LIST = "lxc:statistics_lxc_list";

    /**
     * 领取小鸡
     */
    String LXC_ADD = "lxc:add";

    /**
     * 小鸡申请的宿主机和镜像信息
     */
    String LXC_APPLY_VO = "lxc:apply:%d:applyVO";

    /**
     * 重建时的队列信息
     */
    String LXC_QUEUE_RESET = "lxc:reset:%d:queue";

    /**
     * 重建时的小鸡信息
     */
    String LXC_LXC_RESET = "lxc:reset:%d:lxclxc";

    /**
     *
     */
    String LXC_SUPER_LXC_RESET = "lxc:reset:%d:superLxc";

    /**
     * 自助开小鸡
     */
    String LXC_SUPER_APPLY_ALL = "lxc:super:apply:%d:all";


    /**
     * 自助开小鸡
     */
    String LXC_SUPER_APPLY_HEAD = "lxc:super:apply:%d";

    /**
     * 自助开小鸡（空闲）
     */
    String LXC_SUPER_APPLY_FREE = LXC_SUPER_APPLY_HEAD + ":free";

    /**
     * 自助开小鸡（已使用）
     */
    String LXC_SUPER_APPLY_USED = LXC_SUPER_APPLY_HEAD + ":used";

    /**
     * 自助开小鸡（使用中）
     */
    String LXC_SUPER_APPLY_LOCK = "lxc:super:apply:lock";

    /**
     * 重建前小鸡的状态
     */
    String LXC_ORIGINAL_STATUS = "lxc:beforeReset:%s:lxcStatus";

}
