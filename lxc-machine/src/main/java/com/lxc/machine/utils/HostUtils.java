package com.lxc.machine.utils;

import com.lxc.common.utils.ShellUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;

import java.util.List;

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
        List<String> stopAndDelContainer = ShellUtils.execCmdOutList(String.format(operWay + " stop %1$s && " + operWay + " delete %1$s", lxcLxc.getChildName()), "root", lxcSuperLxc.getPassword(), lxcSuperLxc.getIp(), 22);
        System.out.println("stopAndDelContainer: " + stopAndDelContainer);
        if (stopAndDelContainer.contains("false")) {
            throw new Exception("删除容器失败");
        }
        // 删容器
//        ShellUtils.execCmdOutList(String.format(operWay + " delete %1$s", lxcLxc.getChildName())
//            , "root", lxcSuperLxc.getPassword(), lxcSuperLxc.getIp(), 22);
        // 移除初始化日志文件
        List<String> delFileAndLog = ShellUtils.execCmdOutList(String.format("rm -rf %1$s %1$s_init.log", lxcLxc.getChildName()), "root", lxcSuperLxc.getPassword(), lxcSuperLxc.getIp(), 22);
        System.out.println("delFileAndLog: " + delFileAndLog);
        if (delFileAndLog.contains("false")) {
            throw new Exception("删除日志文件失败");
        }
    }
}
