package com.lxc.web.controller.machine;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.io.IoUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lxc.common.annotation.Log;
import com.lxc.common.annotation.RateLimiter;
import com.lxc.common.annotation.RepeatSubmit;
import com.lxc.common.constant.CacheNames;
import com.lxc.common.core.controller.BaseController;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.R;
import com.lxc.common.core.domain.dto.ShellDto;
import com.lxc.common.core.domain.model.LoginUser;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import com.lxc.common.enums.BusinessType;
import com.lxc.common.enums.LimitType;
import com.lxc.common.utils.ShellUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.file.FileUtils;
import com.lxc.common.utils.poi.ExcelUtil;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcQueue;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcLxcBo;
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.domain.vo.LxcQueueVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.mapper.LxcQueueMapper;
import com.lxc.machine.mapper.LxcSuperLxcMapper;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcQueueService;
import com.lxc.machine.service.ILxcSuperLxcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 小鸡信息
 *
 * @author lxc
 * @date 2022-10-11
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/lxc")
@Slf4j
public class LxcLxcController extends BaseController {
    //    final修饰的都是@Autowired注入的变量，这是@RequiredArgsConstructor的效果
    private final ILxcLxcService lxcLxcService;
    private final ILxcQueueService lxcQueueService;
    private final ILxcSuperLxcService lxcSuperLxcService;

    private final LxcQueueMapper queueMapper;
    private final LxcSuperLxcMapper superLxcMapper;

    /**
     * 根据lxcId查找lxc状态【lxc->superLxc->shell】
     * @param lxcId
     * @return
     */
    @RateLimiter(count = 60, time = 60, limitType = LimitType.IP)
    @SaCheckPermission("lxc:lxc:all") //权限判断
    @RequestMapping("/pingLxc")
    public Boolean ping(Long lxcId) {
        //        InetAddresss address = new InetAddresss(ip, port);
        // ping
        /*log.info("/ping {}  {}",superIp,sshPort);
        Socket socket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(superIp, Integer.parseInt(sshPort));
        boolean connected = false;
        try {
            socket.connect(socketAddress);
             connected = socket.isConnected();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return connected||ShellUtils.pingHost(superIp, Integer.parseInt(sshPort));*/
        LxcLxcVo lxcLxcVo = lxcLxcService.queryById(lxcId);
        if (lxcLxcVo == null) {
            return false;
        }
        QueryWrapper<LxcSuperLxc> queryWrapper = new QueryWrapper<>();
//        queryWrapper.select("password","way")
//            .eq("ip", lxcLxcVo.getSuperIp())
//            .last("limit 1");
        queryWrapper.eq("ip", lxcLxcVo.getSuperIp())
            .last("limit 1");
        LxcSuperLxcVo superLxc = superLxcMapper.selectVoOne(queryWrapper);
        if (superLxc == null) {
            return false;
        }
        try {
            String way = superLxc.getWay();
            log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + way);
            String operWay = "";
            if("lxd".equals(way)){
                operWay = "lxc";
            }else{
                operWay = "incus";
            }
            //对终端进行操作的命令 使用NF来表示列数 $NF 表示打印出等于总列数的那一列的数据
            String res = ShellUtils.execCmdWithStatus(operWay + " info " + lxcLxcVo.getChildName() + " |grep -i -w status |awk '{print $NF}'", "root", superLxc.getPassword(), lxcLxcVo.getSuperIp(), 22);
            return StringUtils.equalsIgnoreCase(res, "Running");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return
     */
    @SaCheckPermission("lxc:lxc:all")//@SaCheckPermission是权限认证
    @GetMapping("/getLxcList")
    @Log(title = "查询未领取小鸡列表", businessType = BusinessType.OTHER, isSaveResponseData = false)
    public R getLxcList() {
        // 当前登录用户
        LoginUser loginUser = this.getLoginUser();
        // 转成字符串，内容是RoleId，用逗号分隔
        String roleIds = loginUser.getRoles().stream()
            .map((obj) -> obj.getRoleId().toString())
            .collect(Collectors.joining(", "));
        // 能获取到的角色列表即为空闲小鸡列表
        String cacheKey = CacheNames.FREE_LXC_LIST + ":" + roleIds;
        List<LxcLxcVo> lxcLxcVos = null;
        // 同样用了旁路缓存模式
        if (RedisUtils.hasKey(cacheKey)) {
            // 若之前缓存到redis中，直接取出列表
            lxcLxcVos = RedisUtils.getCacheList(cacheKey);
        } else {
            // 有空闲的Role才能查找出空闲小鸡列表
            lxcLxcVos = lxcLxcService.getLxcListWithRoles(loginUser.getRoles());
            // 写入redis缓存
            RedisUtils.setCacheList(cacheKey, lxcLxcVos);
        }
        return R.ok("获取成功", lxcLxcVos);
    }

    /**
     * 分页查询小鸡信息列表
     * lxc和incus的小鸡放同一张lxc表和queue表
     */
    @SaCheckPermission("lxc:lxc:list")
    @GetMapping("/list")
    public TableDataInfo<LxcLxcVo> list(LxcLxcBo bo, PageQuery pageQuery) {
        TableDataInfo<LxcLxcVo> pageList = lxcLxcService.queryPageList(bo, pageQuery);
        pageList.getRows().parallelStream().forEach(lxcLxcVo -> {
            QueryWrapper<LxcQueue> queryWrapper = new QueryWrapper<>();
//            queryWrapper.select("tg_id")
//                .eq("lxc_id", lxcLxcVo.getId())
//                .last("limit 1");
            queryWrapper.eq("lxc_id", lxcLxcVo.getId())
                .last("limit 1");
            LxcQueueVo queue = queueMapper.selectVoOne(queryWrapper);
            lxcLxcVo.setLxcQueueVo(queue);
        });
        return pageList;
    }

    // 用户页面的小鸡展示
    @SaCheckPermission("lxc:lxc:all")
    @PostMapping("/userLxcData")
    public R getLxcData(@RequestBody @Valid LxcLxcBo lxcDto) {
        LoginUser loginUser = this.getLoginUser();
        String tgId = loginUser.getTgId();
        QueryWrapper<LxcQueue> query = new QueryWrapper<>();
        query.eq("lxc_id", lxcDto.getId());
        //查询小鸡队列,queue只查找一个VO对象
        LxcQueueVo queue = queueMapper.selectVoOne(query);
        if (queue == null) {
            return R.fail(1005);
        }
        if (!tgId.equals(queue.getTgId())) {
            return R.fail(1004);
        }
        //根据队列id查询小鸡信息
        LxcLxcVo lxc = lxcLxcService.queryById(queue.getLxcId());
        queue.setPortStart(lxc.getPortStart());
        queue.setPortEnd(lxc.getPortEnd());
        queue.setPassword(lxc.getPassword());
        queue.setIpv6Addr(lxc.getIpv6Addr());
        queue.setChildName(lxc.getChildName());
        QueryWrapper<LxcSuperLxc> queryWrapper = new QueryWrapper<>();
        //.select里的是查询后显示控制台能看到的内容
//        queryWrapper.select("password","way").
//            eq("ip", lxc.getSuperIp())
//            .last("limit 1");
        queryWrapper.eq("ip", lxc.getSuperIp())
            .last("limit 1");
        //查询的表由lxc_super_lxc改成lxc_and_incus_super_lxc
        LxcSuperLxcVo superLxc = superLxcMapper.selectVoOne(queryWrapper);
        String way = superLxc.getWay();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + way);
        //获取小鸡流量接收和发送情况
        queue.setNetUsage(ShellUtils.getNetUsedInfo(
            ShellDto.builder()
                .ip(lxc.getSuperIp())
                .password(superLxc.getPassword())
                .way(superLxc.getWay())
                .childName(lxc.getChildName())
                .build()
        ));
        return R.ok("查询成功", queue);
    }



    /**
     * 导出小鸡信息列表
     */
    @SaCheckPermission("lxc:lxc:export")
    @Log(title = "小鸡信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LxcLxcBo bo, HttpServletResponse response) {
        List<LxcLxcVo> list = lxcLxcService.queryList(bo);
        ExcelUtil.exportExcel(list, "小鸡信息", LxcLxcVo.class, response);
    }

    /**
     * 统计在线数据
     * @return
     */
    @SaIgnore
    @GetMapping("/onLineLxc")
    @Cacheable(cacheNames = CacheNames.STATISTICS_LXC_LIST)
    public R getOnLineLxcList() {
        Map<String, Object> statistics = lxcLxcService.webIndexStatisticsLxc();
        return R.ok("获取成功", statistics);
    }

    /**
     * 获取小鸡信息详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("lxc:lxc:query")
    @GetMapping("/{id}")
    public R<LxcLxcVo> getInfo(
        @NotNull(message = "主键不能为空")
        @PathVariable Long id
    ) {
        return R.ok(lxcLxcService.queryById(id));
    }

    /**
     * 新增小鸡信息
     */
    @SaCheckPermission("lxc:lxc:add")
    @Log(title = "小鸡信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LxcLxcBo bo) {
        //响应操作数据库影响的行数
        return toAjax(lxcLxcService.insertByBo(bo) ? 1 : 0);
    }

    /**
     * 修改小鸡信息
     */
    @SaCheckPermission("lxc:lxc:edit")
    @Log(title = "小鸡信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LxcLxcBo bo) {
        return toAjax(lxcLxcService.updateByBo(bo) ? 1 : 0);
    }

    /**
     * 修改小鸡信息
     */
  /*  @SaCheckPermission("lxc:lxc:edit")
    @Log(title = "手动执行创建小鸡命令", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> execCreatShell(@RequestBody LxcLxcBo bo) {
        String superIp = bo.getSuperIp();
        LxcSuperLxc superLxc = lxcSuperLxcService.getOne(new QueryWrapper<LxcSuperLxc>().eq("ip",superIp).last("LIMIT 1"));
        //连接母鸡服务器
        ShellDto shellDto = ShellDto.builder()
                                    .ip(superLxc.getIp())
                                    .sshPort(Integer.valueOf(superLxc.getSshPort()))
                                    .username("root")
                                    .password(superLxc.getPassword())
                                    .build();
        ShellUtils.execCmdAndOut(String.format(ShellUtils.IF_PASSWD_FILE_EXIST,bo.getChildName()),ShellUtils.getSession(shellDto));
        LxcLxcBo temp = new LxcLxcBo();
        //手动将未创建小鸡变为等待中
        temp.setLxcStatus(LxcStatus.PENDING.getCode());
        temp.setId(bo.getId());
        lxcLxcService.updateByBoForce(temp);
        return toAjax(true);
    }
*/

    /**
     * 删除小鸡信息
     *
     * @param ids 主键串
     */
    @SaCheckPermission("lxc:lxc:remove")
    @Log(title = "小鸡信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {

        return toAjax(lxcLxcService.deleteWithValidByIds(Arrays.asList(ids), true) ? 1 : 0);
    }


    /**
     * 获取实例的网络流量图
     *
     * @param lxcId
     * @param type
     * @param response
     */
    @GetMapping("/getNetImage/{lxcId}/{type}")
    @SaIgnore
    public void getNetImage(@PathVariable Long lxcId, @PathVariable String type, HttpServletResponse response) {
        boolean userLxc = lxcQueueService.isUserLxc(lxcId, this.getLoginUser().getTgId());
        if (!userLxc) {
            throw new RuntimeException("这不是你的小鸡");
        }
        LxcLxc lxclxc = lxcLxcService.getById(lxcId);
        QueryWrapper<LxcSuperLxc> queryWrapper = new QueryWrapper<>();
//        queryWrapper.select("password","way").
//            eq("ip", lxclxc.getSuperIp())
//            .last("limit 1");
        queryWrapper.eq("ip", lxclxc.getSuperIp())
            .last("limit 1");
        //查询的表由lxc_super_lxc改成lxc_and_incus_super_lxc
        LxcSuperLxcVo superLxc = superLxcMapper.selectVoOne(queryWrapper);
        String way = superLxc.getWay();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + way);
        // TODO Shell
        String command = "";
        String streamCommand = "";
        if("incus".equals(way)){
            streamCommand = ShellUtils.INCUS_VNSTATI_SUMMARY_STREAM;
        }
        if("lxd".equals(way)){
            streamCommand = ShellUtils.VNSTATI_SUMMARY_STREAM;
        }
        switch (type) {
            case "summary":
            default:
                command = streamCommand;
                break;
        }
        String finalCommand = command;
        lxcLxcService.getSessionByLxcId(lxcId, (session, lxc) -> {
            String childName = lxc.getChildName();
            InputStream fileStream = ShellUtils.getCommandFileStream(String.format(finalCommand, childName), session);
            try {
                FileUtils.setAttachmentResponseHeader(response, childName + ".png");
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment;filename=" + "fi1.png");
                response.setContentLength(fileStream.available());
                IoUtil.copy(fileStream, response.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }



}
