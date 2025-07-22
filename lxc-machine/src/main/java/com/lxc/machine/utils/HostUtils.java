package com.lxc.machine.utils;

import com.lxc.common.utils.ShellUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;

public class HostUtils {
    public HostUtils() {}

    /**
     * 删除对应容器及日志
     * @param lxcLxc
     * @param lxcSuperLxc
     * @param operWay
     * @throws Exception
     */
    public static void deleteContainer(LxcLxc lxcLxc, LxcSuperLxc lxcSuperLxc, String operWay) throws Exception {
        // 停机
        ShellUtils.execCmdOutList(String.format(operWay + " stop %1$s && " + operWay + " delete %1$s", lxcLxc.getChildName()), "root", lxcSuperLxc.getPassword(), lxcSuperLxc.getIp(), 22);
        // 删容器
//        ShellUtils.execCmdOutList(String.format(operWay + " delete %1$s", lxcLxc.getChildName())
//            , "root", lxcSuperLxc.getPassword(), lxcSuperLxc.getIp(), 22);
        // 移除初始化日志文件
        ShellUtils.execCmdOutList(String.format("rm -rf %1$s %1$s_init.log", lxcLxc.getChildName()), "root", lxcSuperLxc.getPassword(), lxcSuperLxc.getIp(), 22);
    }
}
