package com.lxc.web.controller.machine;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpStatus;
import com.baomidou.lock.LockTemplate;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.lxc.common.enums.LxcStatus;
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
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.domain.vo.LxcQueueVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.domain.vo.SysSuperRoleVo;
import com.lxc.machine.mapper.LxcLxcMapper;
import com.lxc.machine.mapper.LxcQueueMapper;
import com.lxc.machine.mapper.LxcSuperLxcMapper;
import com.lxc.machine.service.*;
import com.lxc.machine.utils.BotUtils;
import com.lxc.machine.utils.HostUtils;
import com.lxc.machine.utils.InstancesUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.relxd.lxd.model.BackgroundOperationResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    private final LxcSuperLxcMapper superLxcMapper;

    private final LockTemplate lockTemplate;

    private final ISysSuperRoleService sysSuperRoleService;

    @Data
    public static class QueueDto {

        @NotNull Long lxcId;

        String modifyType;

    }

   /* @SaCheckPermission("lxc:lxc:all")
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
            if (!BotUtils.isMember(BotUtils.getChatId(LxcConstants.TG_CHANNEL_ID), Long.valueOf(tgId))) {
                return R.fail(1403, "");  // TODO 改了验证
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
    }*/

    @SaCheckPermission("lxc:lxc:all")
    @RateLimiter(count = 3, time = 20, limitType = LimitType.IP)
    @Log(title = "自助领取小鸡", businessType = BusinessType.INSERT)
    @PostMapping("/applyLxc")
    @RepeatSubmit()
    public R<Object> applyLxc(@RequestBody @Validated(LxcAddGroup.class) LxcApplyBo lxcApplyBo, @RequestParam(required = false, defaultValue = "false") Boolean isReset) {
        LoginUser loginUser = this.getLoginUser();
        String tgId = loginUser.getTgId();
        // 缓存小鸡空余数和使用数，防止超额领取
        Long lxcSuperId = lxcApplyBo.getLxcSuperId();
        // redis缓存操作
        String keyOfFree = String.format(CacheNames.LXC_SUPER_APPLY_FREE, lxcSuperId);
        String keyOfUsed = String.format(CacheNames.LXC_SUPER_APPLY_USED, lxcSuperId);
        long preFree = RedisUtils.getAtomicValue(keyOfFree);
        log.info("{}剩余可开小鸡数量:{}", lxcSuperId, preFree);
        if (preFree <= 0) {
            return R.fail("来晚啦，小鸡已经被抢光了");
        }
        LxcUser tgUser = loginUser.getLxcUser();
        if (!BotUtils.isMember(BotUtils.getChatId(LxcConstants.TG_CHANNEL_ID), Long.valueOf(tgId))) {
            return R.fail(1403);
        }
        if (lxcUserService.count(new QueryWrapper<LxcUser>().eq("tg_id", tgId)) <= 0) {
            return R.fail("用户信息不存在");
        }
        // 母鸡状态校验（是否可领取、是否过期）
        LxcSuperLxc superLxc = lxcSuperLxcService.getById(lxcSuperId);
        if (StringUtils.equals(superLxc.getSuperStatus(), LxcConstants.LXC_SUPER_DISABLED)) {
            return R.fail("该母鸡已禁止自助领取");
        }
        Date dead = DateUtils.dateTime(DateUtils.YYYY_MM_DD, superLxc.getDead());
        Date now = new Date();
        if (now.compareTo(dead) > 0 || superLxc.getExpireFlag().equals(LxcConstants.LXC_SUPER_EXPIRED)) {
            RedisUtils.setAtomicValue(keyOfFree, 0);
            return R.fail("该母鸡已过期，请联系站长");
        }
        // 角色权限校验（用户是否有权限领取该母鸡的小鸡）
        SysSuperRoleBo sysSuperRoleBo = new SysSuperRoleBo();
        sysSuperRoleBo.setSuperId(superLxc.getId());
        List<SysSuperRoleVo> sysSuperRoleVos = sysSuperRoleService.queryList(sysSuperRoleBo);
        if (sysSuperRoleVos.isEmpty()) {
            return R.fail("该母鸡没有设置领取权限,请联系站长");
        }
        AtomicBoolean hasRole = new AtomicBoolean(false);
        sysSuperRoleVos.parallelStream()
            .filter(vo -> loginUser.hasRole(vo.getRoleId()))
            .findFirst()
            .ifPresent(vo -> hasRole.set(true));
        if (!hasRole.get()) {
            return R.fail("你没有领取权限");
        }
        // 配额校验（用户剩余配额是否足够）
        Long lxcQuota = tgUser.getLxcQuota();
        long countUserUsed = lxcLxcService.countUsedByTgId(tgId);
        if (lxcQuota - countUserUsed <= 0) {
            return R.fail(HttpStatus.HTTP_NOT_ACCEPTABLE, "配额不足");
        }
        // 分布式锁控制（防止超额领取）
        RReadWriteLock readWriteLock = RedisUtils.getClient().getReadWriteLock(CacheNames.LXC_SUPER_APPLY_LOCK + lxcSuperId);
        RLock rLock = readWriteLock.writeLock();
        rLock.lock();
        try {
            R<Object> exception = lxcQueueService.applyLxc(tgId, DateUtils.getDate(),  superLxc, loginUser, lxcApplyBo, isReset);
            if (exception != null) {
                return exception;
            }
        } catch (Exception e) {
            log.error("领取小鸡异常", e);
            return R.fail("创建异常，请联系站长");
        } finally {
            rLock.unlock();
        }
        return R.success("申请成功，请等待小鸡创建。若长时间未创建，请联系站长");
    }

    @SaCheckPermission("lxc:lxc:all")
    @GetMapping("/testExec/{instancename}")
    @Log(title = "在小鸡里执行指令", businessType = BusinessType.OTHER)
    public R testExec(@PathVariable String instancename) {
//        InstancesUtils.target("8.216.37.221", LxcConstants.LXD_PORT).postInstancesByNameExec(instancename,  "touch", "/root/testfile.txt");
        InstancesUtils.target("8.216.37.221", LxcConstants.LXD_PORT).postInstancesByNameExec(instancename,  "chmod", "777", "/root/ssh_bash.sh");
        InstancesUtils.target("8.216.37.221", LxcConstants.LXD_PORT).postInstancesByNameExec(instancename,  "dos2unix", "/root/ssh_bash.sh");
        LambdaQueryWrapper<LxcLxc> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(LxcLxc::getChildName, instancename);
        LxcLxc lxc = lxcLxcService.getOne(lambdaQueryWrapper);
        InstancesUtils.target("8.216.37.221", LxcConstants.LXD_PORT).postInstancesByNameExec(instancename,  "bash", "/root/ssh_bash.sh", lxc.getPassword());
        InstancesUtils.target("8.216.37.221", LxcConstants.LXD_PORT).postInstancesByNameExec(instancename,  "chmod", "+x", "/root/config.sh");
        InstancesUtils.target("8.216.37.221", LxcConstants.LXD_PORT).postInstancesByNameExec(instancename,  "dos2unix", "/root/config.sh");
        InstancesUtils.target("8.216.37.221", LxcConstants.LXD_PORT).postInstancesByNameExec(instancename,  "/root/config.sh");
        return R.ok("执行成功");
    }

    @SaCheckPermission("lxc:lxc:all")
    @GetMapping("/testInstanceLog/{uuid}")
    @Log(title = "获取小鸡执行日志", businessType = BusinessType.OTHER)
    public R testInstanceLog(@PathVariable String uuid) {
        String res = InstancesUtils.target("66.70.153.71", LxcConstants.LXD_PORT).getOperationLog(uuid);
        return R.ok("执行成功", res);
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

    /**
     * 手动重建容器——删掉旧的，创建新的
     * @param childName
     * @return
     */
    @SaCheckPermission("lxc:lxc:all")
    @PostMapping("/resetLxc/{childName}")
    public R<String> resetUserLxcByName(@PathVariable String childName) {
        log.info("{}重建小鸡", childName);
        LoginUser loginUser = this.getLoginUser();
        String tgId = loginUser.getTgId();

        if (!BotUtils.isMember(BotUtils.getChatId(LxcConstants.TG_CHANNEL_ID), Long.valueOf(tgId))) {
            return R.fail(1403);  // TODO 改了验证
        }

        long count = lxcUserService.count(new QueryWrapper<LxcUser>().eq("tg_id", tgId));
        if (count <= 0) {
            return R.fail("用户信息不存在");
        }

        LxcLxcVo lxcLxcVo = lxcMapper.selectVoLxc(tgId, childName);
        if (lxcLxcVo == null) {
            String voNull = String.format("获取用户%s的小鸡的名称传递错误，为：%s", tgId, childName);
            log.info("重建的小鸡信息为：{}", lxcLxcVo);
            return R.fail(voNull);
        }

//        LxcLxcBo lxcLxcBo = new LxcLxcBo();
        LxcApplyBo lxcApplyBo = new LxcApplyBo();
        BeanUtils.copyProperties(lxcLxcVo, lxcApplyBo);

        // 调用重建方法
        R<String> result = resetUserLxcData(lxcApplyBo);
        if (result.isSuccess()) {
            return R.ok("重建小鸡成功");
        } else {
            return R.fail(result.getMsg());
        }
    }

    /**
     * 重建小鸡
     * @param lxcApplyBo 小鸡信息
     * @return 重建结果
     */
    @SaCheckPermission("lxc:lxc:all")
    @Transactional
    public R<String> resetUserLxcData(@RequestBody @Valid LxcApplyBo lxcApplyBo) {
        LoginUser loginUser = this.getLoginUser();
        String tgId = loginUser.getTgId();

        QueryWrapper<LxcQueue> query = new QueryWrapper<>();
        query.eq("lxc_id", lxcApplyBo.getId());
        LxcQueueVo queueVo = queueMapper.selectVoOne(query);
        if (queueVo == null) {
            return R.fail(1005, "未找到小鸡队列信息");
        }

        if (!tgId.equals(queueVo.getTgId())) {
            return R.fail(1004, "非创建用户，无重建权限");
        }

        LxcLxcVo lxcVo = lxcLxcService.queryById(queueVo.getLxcId());
        LxcLxc lxc = new LxcLxc();
        BeanUtils.copyProperties(lxcVo, lxc);

        // 保存重建前状态
        String lxcStatusOrigin = lxc.getLxcStatus();
        System.out.println("小鸡重建前状态为：" + lxcStatusOrigin);

        // 检查是否为 frozen 状态
        if (LxcStatus.FROZEN.getCode().equals(lxcStatusOrigin) || LxcStatus.TRAFFIC_LIMIT.getCode().equals(lxcStatusOrigin)) {
            return R.fail("冻结或流量超限的小鸡不能被重建");
        }
        lxc.setLxcStatus(LxcStatus.RESETING.getCode());

        String keyOfSuperLxc = String.format(CacheNames.LXC_SUPER_LXC_RESET, lxc.getId());
        LxcSuperLxc superLxc = RedisUtils.getCacheObject(keyOfSuperLxc);
        if (superLxc == null) {
            // 数据库获取母鸡信息
            LambdaQueryWrapper<LxcSuperLxc> lqw = new LambdaQueryWrapper<>();
            lqw.eq(LxcSuperLxc::getIp, lxcApplyBo.getSuperIp());
            superLxc = lxcSuperLxcService.getOne(lqw);
        }
        log.info("小鸡重建前信息获取成功:{}", lxc);
        log.info("重建前信息获取成功:{}", superLxc);
        String ip = superLxc.getIp();
        String password = superLxc.getPassword();
        String sshPort = superLxc.getSshPort();
        String childName = lxc.getChildName();
        /**
         * 不需要lxcApplyBo，在lxc表中多存了一个字段
         */
        try {
            // 删除原来的lxcApplyBo缓存
            //删掉缓存中创建小鸡时的applyVo信息
            String keyOfApplyVo = String.format(CacheNames.LXC_APPLY_VO, lxc.getId());
            if((RedisUtils.getCacheObject(keyOfApplyVo)) != null)
                RedisUtils.deleteObject(keyOfApplyVo);
            // 删除宿主机上对应的容器和日志文件（不删也可以，重建会覆盖）
            HostUtils.deleteContainer(lxc, superLxc, superLxc.getWay());
            lxcApplyBo.setLxcSuperId(superLxc.getId());
            log.info("重建前lxcApplyBo:{}", lxcApplyBo);
            applyLxc(lxcApplyBo, true);
        } catch (Exception e) {
            log.error("执行重建命令失败", e);
            return R.fail("执行重建命令失败，请联系管理员");
        }

        // 更新小鸡状态 由于小鸡重建使用applyLxc函数，是异步创建，小鸡没有重建完后面的代码也会执行
        // 这里用循环判断小鸡是否重建完成 RUNNING，如果是则更新为原始状态， todo：这样会导致用户卡住在这个函数里吗？这个要测试！！！
//        while (!LxcStatus.RUNNING.equals(lxc.getLxcStatus())) {
//            // 未创建完成，线程暂停1s，再重新判断状态
//            sleep(1000);
//        }

        lxc.setLxcStatus(lxcStatusOrigin);
        lxc.setUpdateTime(DateUtil.parseDate(DateUtil.now()));
        lxcMapper.updateById(lxc);

        // 更新队列信息
        LxcQueue lxcQueue = new LxcQueue();
        BeanUtils.copyProperties(queueVo, lxcQueue);
        lxcQueue.setUpdateTime(DateUtil.parseDate(DateUtil.now()));
        lxcQueueService.updateById(lxcQueue);
        // 查一下现在宿主机有哪几个容器，如果重建的容器不在其中，则说明比重建前容器序号小的机器被释放了，默认创建容器会补上小的序号。
        // 宿主机没删掉旧的容器
        return R.ok("重建成功");
    }

    /**
     * 从容器信息输出中提取镜像指纹
     * @param infoOutput 容器信息输出内容
     * @return 镜像指纹，如果未找到则返回 null
     */
    private String extractImageFingerprint(String infoOutput) {
        String[] lines = infoOutput.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith("Fingerprint:")) {
                return lines[i].split(":")[1].trim();
            }
        }
        return null;
    }

    @RateLimiter(count = 3, time = 20, limitType = LimitType.IP)
    @Log(title = "小鸡操作", businessType = BusinessType.OTHER)
    @SaCheckPermission("lxc:lxc:all")
    @PostMapping("/modifyLxc")
    public R modifyLxc(@RequestBody @Validated(LxcEditGroup.class) QueueDto queueDto) {

        LoginUser loginUser = this.getLoginUser();

        String tgId = loginUser.getTgId();

        //账号验证
        if (!BotUtils.isMember(BotUtils.getChatId(LxcConstants.TG_CHANNEL_ID), Long.valueOf(tgId))) {
            return R.fail(1403, "");    // TODO 改了验证
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

        String way = superLxc.getWay();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + way);
        try {
            String res = ShellUtils.execCmdAndReturnStatus(
                ShellDto.builder()
                    .ip(ip)
                    .password(password)
                    .way(superLxc.getWay())
                    .modifyType(queueDto.modifyType)
                    .childName(name)
                    .build()
            );
            log.info(res);
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
//                    .select("lxc_id")
                    .eq("id", id)
            );
            if (lxcQueue != null) {
                LxcLxc lxcLxc = new LxcLxc();
                lxcLxc.setState(0);
                lxcLxc.setId(lxcQueue.getLxcId());
                lxcLxcList.add(lxcLxc);
            } else {
                // 表中找不到有效id的小鸡
                return R.fail(R.FAIL, "删除选项中存在无法定位的id，" + id);
            }
            //删掉缓存中创建小鸡时的applyVo信息
            String keyOfApplyVo = String.format(CacheNames.LXC_APPLY_VO, id);
            if((RedisUtils.getCacheObject(keyOfApplyVo)) != null)
                RedisUtils.deleteObject(keyOfApplyVo);
        }
        lxcMapper.updateBatchById(lxcLxcList);
        // 在前面对queueId进行判断，找不到对应id的记录直接返回错误，这里就不对id进行有效性判断了
        Boolean aBoolean = lxcQueueService.deleteWithValidByIds(Arrays.asList(ids), true);
        if (aBoolean) {
            RedisUtils.deleteKeys(CacheNames.FREE_LXC_LIST + "*");
            RedisUtils.deleteKeys(CacheNames.STATISTICS_LXC_LIST);
        }
        return toAjax(aBoolean ? 1 : 0);
    }

    public static void main(String[] args) {
//        Socket socket = new Socket();
//        SocketAddress socketAddress = new InetSocketAddress("192.168.1.102", 22);
//        try {
//            socket.connect(socketAddress);
//            System.out.println(socket.isConnected());
//            System.out.println(ShellUtils.pingHost("45.8.22.53", 22));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        // 过期标志 防止过期标志没有被及时更改，获取过期日期与当前日期进行对比
//        Date dead = DateUtils.dateTime(DateUtils.YYYY_MM_DD, "2025-05-31");
//        Date now = new Date();
//        System.out.println("dead:"+dead);
//        System.out.println("now:"+now);
//        System.out.println(dead.compareTo(now) > 0);
    }

    @SaCheckPermission("lxc:lxc:list")
//    @SaIgnore
    @RequestMapping("/getAllInstancesBase")
    public R<BackgroundOperationResponse> getAllInstancesBase(String ip) {
        // 直接从数据库读取
        QueryWrapper<LxcSuperLxc> queryWrapper = new QueryWrapper<>();
        queryWrapper
            .eq("ip",ip);
        LxcSuperLxc lxcSuperLxc = lxcSuperLxcService.getOne(queryWrapper);
        if (lxcSuperLxc == null) {
            return R.fail("找不到对应ip的宿主机");
        }
        return R.ok(lxcQueueService.getAllInstancesBase(ip));
    }

    //    @SaCheckPermission("lxc:lxc:list")
    // 查找容器创建日志
    @SaIgnore
    @GetMapping("/getInitLog/{lxcId}")
    public R<List<String>> getInitLog(@PathVariable Long lxcId) {
        return lxcLxcService.getSessionByLxcId(lxcId, (session, lxc) -> R.ok(ShellUtils.execCmdOutList(String.format(ShellUtils.INIT_LOG, lxc.getChildName()), session)));
    }
}
