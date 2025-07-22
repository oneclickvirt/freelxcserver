package com.lxc.common.constant;

public interface LxcConstants {

//    String BASE_URL = "https://%s:8443";
    String LXD_PORT = "8443";

    String BOOT_NAME = "lxc_testing_bot";

    String BOOT_TOKEN = "5381858832:AAG_M5zpe4qYAV0PDnPuatypmVa7Wsd4qpQ";
//    String BOOT_NAME = "hiyaa2044_bot";
//    String REG_BOOT_TOKEN = "5381858832:AAG_M5zpe4qYAV0PDnPuatypmVa7Wsd4qpQ";
//    String BOOT_TOKEN = "7267680854:AAGeTeoHg6QfBd0bxhabuNj5hsoaTsI1Q9o";

    String CHAT_ID = "-1001474554890";

    /**
     * 小鸡流量限制GB
     */
    Integer LXC_NETSTREAM_LIMIT = 100;

    /**
     * 小鸡采样时间间隔(s)
     */
    Integer LXC_SAMPLE_INTERVAL = 5;

    /**
     * 可开小鸡禁用标识
     */
    String LXC_SUPER_DISABLED = "0";

    /**
     * 母鸡过期标识
     */
    String LXC_SUPER_EXPIRED = "Y";


    /**
     * 是否启用IPV6
     */
    String LXC_IPV6_FLAG = "Y";

    /**
     * 随机生成uuid的key名
     */
    String USER_UUID_KEY = "UID";
    /**
     * 发送指令：连接
     */
    String WEBSSH_OPERATE_CONNECT = "connect";
    /**
     * 发送指令：命令
     */
    String WEBSSH_OPERATE_COMMAND = "command";
}
