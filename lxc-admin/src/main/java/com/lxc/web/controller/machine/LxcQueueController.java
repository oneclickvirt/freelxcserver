package com.lxc.web.controller.machine;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.http.HttpStatus;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lxc.common.annotation.Log;
import com.lxc.common.annotation.RateLimiter;
import com.lxc.common.annotation.RepeatSubmit;
import com.lxc.common.constant.CacheNames;
import com.lxc.common.constant.LxcConstants;
import com.lxc.common.core.controller.BaseController;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.R;
import com.lxc.common.core.domain.dto.ShellDto;
import com.lxc.common.core.domain.entity.LxcUser;
import com.lxc.common.core.domain.model.LoginUser;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import com.lxc.common.core.validate.lxc.LxcAddGroup;
import com.lxc.common.core.validate.lxc.LxcEditGroup;
import com.lxc.common.enums.BusinessType;
import com.lxc.common.enums.LimitType;
import com.lxc.common.utils.BotUtils;
import com.lxc.common.utils.DateUtils;
import com.lxc.common.utils.ShellUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.poi.ExcelUtil;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcQueue;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcApplyBo;
import com.lxc.machine.domain.bo.LxcQueueBo;
import com.lxc.machine.domain.bo.SysSuperRoleBo;
import com.lxc.machine.domain.vo.LxcQueueVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.domain.vo.SysSuperRoleVo;
import com.lxc.machine.mapper.LxcLxcMapper;
import com.lxc.machine.mapper.LxcQueueMapper;
import com.lxc.machine.mapper.LxcSuperLxcMapper;
import com.lxc.machine.mapper.LxcUserMapper;
import com.lxc.machine.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.relxd.lxd.model.BackgroundOperationResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 小鸡和用户关联表
 *
 * @author lxc
 * @date 2022-10-11
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/lxc/queue")
@Slf4j
public class LxcQueueController extends BaseController {

    private final ILxcQueueService lxcQueueService;
    private final ILxcUserService lxcUserService;
    private final ILxcSuperLxcService lxcSuperLxcService;
    private final ILxcLxcService lxcLxcService;

    private final LxcQueueMapper queueMapper;
    private final LxcLxcMapper lxcMapper;

    private final LxcUserMapper userMapper;

    private final LxcSuperLxcMapper superLxcMapper;

    private final LockTemplate lockTemplate;

    private final ISysSuperRoleService sysSuperRoleService;

    @Data
    public static class QueueDto {

        @NotNull Long lxcId;

        String modifyType;

    }

    @SaCheckPermission("lxc:lxc:all")
    @Log(title = "领取小鸡", businessType = BusinessType.INSERT)
    @PostMapping("/addone")
    @RepeatSubmit()
    public R<Object> addUserLxc(@RequestBody @Validated(LxcAddGroup.class) LxcQueueBo lxcQueue) {
        final LockInfo lockInfo = lockTemplate.lock(CacheNames.LXC_ADD + lxcQueue.getLxcId() + "", 30000L, 5000L, RedissonLockExecutor.class);
        if (null == lockInfo) {
            throw new RuntimeException("小鸡被抢了😤");
        }

        // 获取锁成功，处理业务
        try {
            LoginUser loginUser = this.getLoginUser();
            String tgId = loginUser.getTgId();
            //账号验证
            if (!BotUtils.isMember(LxcConstants.CHAT_ID, Long.valueOf(tgId))) {
//                return R.fail(1403, "");  TODO 改了验证
//                return R.success("申请成功");
            }
            long count = lxcUserService.count(new QueryWrapper<LxcUser>().eq("tg_id", tgId));
            if (count <= 0) {
                return R.fail("用户信息不存在");
            }
            R<Object> exception = lxcQueueService.addUserLxc(lxcQueue, loginUser);
            if (exception != null) {
                return exception;
            }
            RedisUtils.deleteKeys(CacheNames.FREE_LXC_LIST + "*");
            RedisUtils.deleteKeys(CacheNames.STATISTICS_LXC_LIST);
            return R.success("申请成功");
        } finally {
            //释放锁
            lockTemplate.releaseLock(lockInfo);
        }
    }


    @SaCheckPermission("lxc:lxc:all")
    @RateLimiter(count = 3, time = 20, limitType = LimitType.IP)
    @Log(title = "自助领取小鸡", businessType = BusinessType.INSERT)
    @PostMapping("/applyLxc")
    @RepeatSubmit()
    public R<Object> applyLxc(@RequestBody @Validated(LxcAddGroup.class) LxcApplyBo lxcApplyBo) {
        LoginUser loginUser = this.getLoginUser();
        String tgId = loginUser.getTgId();

        Long lxcSuperId = lxcApplyBo.getLxcSuperId();
        String keyOfFree = String.format(CacheNames.LXC_SUPER_APPLY_FREE, lxcSuperId);
        String keyOfUsed = String.format(CacheNames.LXC_SUPER_APPLY_USED, lxcSuperId);
        long preFree = RedisUtils.getAtomicValue(keyOfFree);
        log.info("{}剩余可开小鸡数量:{}", lxcSuperId, preFree);
        if (preFree <= 0) {
            return R.fail("来晚啦，小鸡已经被抢光了");
        }

        LxcUser tgUser = loginUser.getLxcUser();

        if (!BotUtils.isMember(LxcConstants.CHAT_ID, Long.valueOf(tgId))) {
//            return R.fail(1403);  TODO 改了验证
//            return R.success("申请成功");
        }

        long count = lxcUserService.count(new QueryWrapper<LxcUser>().eq("tg_id", tgId));
        if (count <= 0) {
            return R.fail("用户信息不存在");
        }

        LxcSuperLxc superLxc = lxcSuperLxcService.getById(lxcSuperId);
        String superStatus = superLxc.getSuperStatus();
        if (StringUtils.equals(superStatus, LxcConstants.LXC_SUPER_DISABLED)) {
            return R.fail("该母鸡已禁止自助领取");
        }
        if (superLxc.getExpireFlag().equals(LxcConstants.LXC_SUPER_EXPIRED)) {
            RedisUtils.setAtomicValue(keyOfFree, 0);
            return R.fail("该母鸡已过期，请联系站长");
        }

        SysSuperRoleBo sysSuperRoleBo = new SysSuperRoleBo();
        sysSuperRoleBo.setSuperId(superLxc.getId());
        List<SysSuperRoleVo> sysSuperRoleVos = sysSuperRoleService.queryList(sysSuperRoleBo);

        // 判断是否有角色权限领取小鸡
        if (!sysSuperRoleVos.isEmpty()) {
            AtomicBoolean flag = new AtomicBoolean(false);
            sysSuperRoleVos.parallelStream()
                .filter(sysSuperRoleVo -> loginUser.hasRole(sysSuperRoleVo.getRoleId()))
                .findFirst()
                .ifPresent(sysSuperRoleVo -> flag.set(true));
            if (!flag.get()) {
                return R.fail("你没有领取权限");
            }
        } else {
            return R.fail("该母鸡没有设置领取权限,请联系站长");
        }

        Long lxcQuota = tgUser.getLxcQuota();
        long countUserUsed = lxcLxcService.countUsedByTgId(tgId);
        if (lxcQuota - countUserUsed <= 0) {
            return R.fail(HttpStatus.HTTP_NOT_ACCEPTABLE, "配额不足");
        }

        RReadWriteLock readWriteLock = RedisUtils.getClient().getReadWriteLock(CacheNames.LXC_SUPER_APPLY_LOCK + lxcSuperId);
        RLock rLock = readWriteLock.writeLock();
        rLock.lock();
        try {
            long free = RedisUtils.decrAtomicValue(keyOfFree) + 1;
            log.info("{}剩余可开小鸡数量:{}", lxcSuperId, free);
            if (free <= 0) {
                return R.fail("来晚啦，小鸡已领完");
            }
            long used = RedisUtils.incrAtomicValue(keyOfUsed) - 1;
            log.info("{}已领取小鸡数量:{}", lxcSuperId, used);
            R<Object> exception = lxcQueueService.applyLxc(superLxc, loginUser, lxcApplyBo);
            if (exception != null) {
                return exception;
            }
            RedisUtils.deleteKeys(CacheNames.FREE_LXC_LIST + "*");
            RedisUtils.deleteKeys(CacheNames.STATISTICS_LXC_LIST);
        } catch (Exception e) {
            RedisUtils.incrAtomicValue(keyOfFree);
            RedisUtils.decrAtomicValue(keyOfUsed);
            log.error("领取小鸡异常", e);
            return R.fail("创建异常，请联系站长");
        } finally {
            rLock.unlock();
        }
        return R.success("申请成功，请等待小鸡创建。若长时间未创建，请联系站长");
    }

    @SaCheckPermission("lxc:lxc:all")
    @GetMapping("/listAll")
    @Log(title = "查询自己的小鸡列表 listAll", businessType = BusinessType.OTHER)
    public R getUserLxcData() {
        LoginUser loginUser = this.getLoginUser();
        String tgId = loginUser.getTgId();
        List<LxcQueueVo> queueList = lxcQueueService.queryAllLxc(tgId, DateUtils.getDate());
        return R.ok("获取成功", queueList);
    }

    @RateLimiter(count = 2, time = 20, limitType = LimitType.IP)
    @Log(title = "小鸡操作", businessType = BusinessType.OTHER)
    @SaCheckPermission("lxc:lxc:all")
    @PostMapping("/modifyLxc")
    public R modifyLxc(@RequestBody @Validated(LxcEditGroup.class) QueueDto queueDto) {

        LoginUser loginUser = this.getLoginUser();

        String tgId = loginUser.getTgId();

        //账号验证
        if (!BotUtils.isMember(LxcConstants.CHAT_ID, Long.valueOf(tgId))) {
//            return R.fail(1403, "");   TODO 改了验证
//            return R.success("跳过验证");
        }

        LxcLxc lxc = lxcMapper.selectById(queueDto.getLxcId());

        QueryWrapper<LxcSuperLxc> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ip", lxc.getSuperIp()).last("limit 1");

        LxcSuperLxcVo superLxc = superLxcMapper.selectVoOne(queryWrapper);

        if (superLxc == null) {
            return R.fail("没有这样的小鸡，不要调皮啦");
        }

        String ip = "";
        String password = "";
        String name = "";

        ip = lxc.getSuperIp();
        password = superLxc.getPassword();
        name = lxc.getChildName();

        try {
            String res = ShellUtils.execCmdAndReturnStatus(
                ShellDto.builder()
                    .ip(ip)
                    .password(password)
                    .modifyType(queueDto.modifyType)
                    .childName(name)
                    .build()
            );
            return R.ok(res);
        } catch (Exception e) {
            return R.fail("执行命令出错，请联系管理员,错误信息：" + e.getMessage());
        }
    }


    /**
     * 查询小鸡和用户关联表列表
     */
    @SaCheckPermission("lxc:queue:list")
    @GetMapping("/list")
    public TableDataInfo<LxcQueueVo> list(LxcQueueBo bo, PageQuery pageQuery) {
        return lxcQueueService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出小鸡和用户关联表列表
     */
    @SaCheckPermission("lxc:queue:export")
    @Log(title = "小鸡和用户关联表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LxcQueueBo bo, HttpServletResponse response) {
        List<LxcQueueVo> list = lxcQueueService.queryList(bo);
        ExcelUtil.exportExcel(list, "小鸡和用户关联表", LxcQueueVo.class, response);
    }

    /**
     * 获取小鸡和用户关联表详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("lxc:queue:query")
    @GetMapping("/{id}")
    public R<LxcQueueVo> getInfo(
        @NotNull(message = "主键不能为空")
        @PathVariable Long id
    ) {
        return R.ok(lxcQueueService.queryById(id));
    }

    /**
     * 新增小鸡和用户关联表
     */
    @SaCheckPermission("lxc:queue:add")
    @Log(title = "小鸡和用户关联表", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LxcQueueBo bo) {
        RedisUtils.deleteKeys(CacheNames.FREE_LXC_LIST + "*");
        RedisUtils.deleteKeys(CacheNames.STATISTICS_LXC_LIST);
        return toAjax(lxcQueueService.insertByBo(bo) ? 1 : 0);
    }

    /**
     * 修改小鸡和用户关联表
     */
    @SaCheckPermission("lxc:queue:edit")
    @Log(title = "小鸡和用户关联表", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LxcQueueBo bo) {
        RedisUtils.deleteKeys(CacheNames.FREE_LXC_LIST + "*");
        RedisUtils.deleteKeys(CacheNames.STATISTICS_LXC_LIST);
        return toAjax(lxcQueueService.updateByBo(bo) ? 1 : 0);
    }

    /**
     * 删除小鸡和用户关联表
     *
     * @param ids 主键串
     */
    @SaCheckPermission("lxc:queue:remove")
    @Log(title = "小鸡和用户关联表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(
        @NotEmpty(message = "主键不能为空")
        @PathVariable Long[] ids
    ) {
        List<LxcLxc> lxcLxcList = new ArrayList<>();
        for (Long id : ids) {
            LxcQueue lxcQueue = queueMapper.selectOne(
                new QueryWrapper<LxcQueue>()
                    .select("lxc_id")
                    .eq("id", id)
            );
            LxcLxc lxcLxc = new LxcLxc();
            lxcLxc.setState(0);
            lxcLxc.setId(lxcQueue.getLxcId());
            lxcLxcList.add(lxcLxc);
        }
        lxcMapper.updateBatchById(lxcLxcList);
        Boolean aBoolean = lxcQueueService.deleteWithValidByIds(Arrays.asList(ids), true);
        if (aBoolean) {
            RedisUtils.deleteKeys(CacheNames.FREE_LXC_LIST + "*");
            RedisUtils.deleteKeys(CacheNames.STATISTICS_LXC_LIST);
        }
        return toAjax(aBoolean ? 1 : 0);
    }

    public static void main(String[] args) {
        Socket socket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress("192.168.1.102", 22);
        try {
            socket.connect(socketAddress);
            System.out.println(socket.isConnected());
            System.out.println(ShellUtils.pingHost("45.8.22.53", 22));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SaCheckPermission("lxc:lxc:list")
//    @SaIgnore
    @RequestMapping("/getAllInstancesBase")
    public R<BackgroundOperationResponse> getAllInstancesBase(String ip) {
        return R.ok(lxcQueueService.getAllInstancesBase(ip));
    }

    //    @SaCheckPermission("lxc:lxc:list")
    @SaIgnore
    @GetMapping("/getInitLog/{lxcId}")
    public R<List<String>> getInitLog(@PathVariable Long lxcId) {
        return lxcLxcService.getSessionByLxcId(lxcId, (session, lxc) -> R.ok(ShellUtils.execCmdOutList(String.format(ShellUtils.INIT_LOG, lxc.getChildName()), session)));
    }
}
