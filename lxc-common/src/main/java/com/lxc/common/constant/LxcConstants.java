package com.lxc.common.constant;

public interface LxcConstants {

    String BOOT_NAME = "lxc_testing_bot";
    String BOOT_TOKEN = "5381858832:AAG_M5zpe4qYAV0PDnPuatypmVa7Wsd4qpQ";

    String REG_BOOT_TOKEN = "5381858832:AAG_M5zpe4qYAV0PDnPuatypmVa7Wsd4qpQ";

    String CHAT_ID = "-1001474554890";

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
