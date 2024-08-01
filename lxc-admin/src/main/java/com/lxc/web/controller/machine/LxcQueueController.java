package com.lxc.web.controller.machine;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.date.DateUtil;
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
import com.lxc.common.enums.LxcStatus;
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
import com.lxc.machine.domain.bo.LxcLxcBo;
import com.lxc.machine.domain.bo.LxcQueueBo;
import com.lxc.machine.domain.bo.SysSuperRoleBo;
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.domain.vo.LxcQueueVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.domain.vo.SysSuperRoleVo;
import com.lxc.machine.mapper.LxcLxcMapper;
import com.lxc.machine.mapper.LxcQueueMapper;
import com.lxc.machine.mapper.LxcSuperLxcMapper;
import com.lxc.machine.mapper.LxcUserMapper;
import com.lxc.machine.service.*;
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
    private final LxcLxcController lxcLxcController;

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

        // 查询母鸡-角色表中特定母鸡ip的记录列表，是有权限在该母鸡领取小鸡的角色的记录
        SysSuperRoleBo sysSuperRoleBo = new SysSuperRoleBo();
        sysSuperRoleBo.setSuperId(superLxc.getId());
        List<SysSuperRoleVo> sysSuperRoleVos = sysSuperRoleService.queryList(sysSuperRoleBo);

        // 判断是否有角色权限领取小鸡
        if (!sysSuperRoleVos.isEmpty()) {
            AtomicBoolean flag = new AtomicBoolean(false);
            sysSuperRoleVos.parallelStream()
                // 从登录的用户信息里取出角色id，筛选对应角色id的记录
                .filter(sysSuperRoleVo -> loginUser.hasRole(sysSuperRoleVo.getRoleId()))
                .findFirst()
                // 如果用户有角色权限，则把标志位设为true
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
        // 获取一个对象的值(CacheNames.LXC_SUPER_APPLY_LOCK)作为读写锁
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

    @SaCheckPermission("lxc:lxc:all")
    @PostMapping("/resetLxc/{childName}")
    public R resetUserLxcByName(@PathVariable String childName) {
        log.info("用户{}重建小鸡",childName);
        LoginUser loginUser = this.getLoginUser();
        String tgId = loginUser.getTgId();

        if (!BotUtils.isMember(LxcConstants.CHAT_ID, Long.valueOf(tgId))) {
//            return R.fail(1403);  TODO 改了验证
        }

        long count = lxcUserService.count(new QueryWrapper<LxcUser>().eq("tg_id", tgId));
        if (count <= 0) {
            return R.fail("用户信息不存在");
        }

//        QueryWrapper<LxcQueue> lxcQueueQueryWrapper = new QueryWrapper<>();
//        lxcQueueQueryWrapper.eq("tg_id",tgId);
//
//
//        QueryWrapper<LxcLxc> lxcQueryWrapper = new QueryWrapper<>();
//        lxcQueryWrapper.eq("child_name",childName);
//        LxcLxcVo lxcLxcVo = lxcMapper.selectVoOne(lxcQueryWrapper);

        LxcLxcVo lxcLxcVo = lxcMapper.selectVoLxc(tgId,childName);

        LxcLxcBo lxcLxcBo = new LxcLxcBo();
        BeanUtils.copyProperties(lxcLxcVo,lxcLxcBo);
        if(lxcLxcBo != null) {
            resetUserLxcData(lxcLxcBo);
            return R.ok("重建小鸡成功");
        }else{
            String boNull = String.format("获取用户%s的小鸡%s为空", tgId, childName);
            return R.fail(boNull);
        }
    }

    /**
     * 重建小鸡
     * @param lxcDto
     * @return
     */
    // TODO
    @SaCheckPermission("lxc:lxc:all")
    @Transactional
    public R resetUserLxcData(@RequestBody @Valid LxcLxcBo lxcDto) {
        //先查找出选定的小鸡的信息(/lxc/userLxcData)，先存为变量VO，然后删除原小鸡(LxcLxcService有删除指定id的小鸡方法)，再用原有信息重新创建
        //getLxcData
        LoginUser loginUser = this.getLoginUser();
        String tgId = loginUser.getTgId();

        //TODO 添加校验重建的用户是否为创建的用户


        QueryWrapper<LxcQueue> query = new QueryWrapper<>();
        query.eq("lxc_id", lxcDto.getId());
        //查询小鸡队列,queueVo只查找一个VO对象
        LxcQueueVo queueVo = queueMapper.selectVoOne(query);
        if (queueVo == null) {
            return R.fail(1005);
        }
        //校验了tgid是否为创建小鸡的用户的id
        if (!tgId.equals(queueVo.getTgId())) {
            return R.fail(1004);
        }
        //根据队列id查询小鸡信息
        //LxcLxcVo是为了不重写queryById函数
        LxcLxcVo lxcVo = lxcLxcService.queryById(queueVo.getLxcId());
        LxcLxc lxc = new LxcLxc();
        BeanUtils.copyProperties(lxcVo,lxc);

        //******获取小鸡重建前的lxcStatus，若流量超限，在重建后运行时设置回frozen******
        String lxcStatusOrigin = lxc.getLxcStatus();
        System.out.println("小鸡重建前状态为：" + lxcStatusOrigin);
        /** TODO
         * 暂时先用redis来存小鸡重建前状态
         */
        String keyOfLxcStatus = String.format(CacheNames.LXC_ORIGINAL_STATUS,lxc.getId());
        RedisUtils.setCacheObject(keyOfLxcStatus,lxcStatusOrigin);

        lxc.setLxcStatus(LxcStatus.WAITING_FOR_PROCESS.getCode());

//        String keyOfLxc = String.format(CacheNames.LXC_LXC_RESET, lxc.getId());
//        RedisUtils.setCacheObject(keyOfLxc,lxc);

        queueVo.setPortStart(lxc.getPortStart());
        queueVo.setPortEnd(lxc.getPortEnd());
        queueVo.setPassword(lxc.getPassword());
        queueVo.setIpv6Addr(lxc.getIpv6Addr());
//        String keyOfQueue = String.format(CacheNames.LXC_QUEUE_RESET, lxc.getId());
//        RedisUtils.setCacheObject(keyOfQueue,queueVo);
        log.info(">>>>>>>>>>>>>>>>>lxc:{}",lxc);
        log.info(">>>>>>>>>>>>>>>>>queue:{}",queueVo);

        String keyOfSuperLxc = String.format(CacheNames.LXC_SUPER_LXC_RESET, lxc.getId());
        LxcSuperLxc superLxc = RedisUtils.getCacheObject(keyOfSuperLxc);
        log.info("小鸡重建前信息获取成功:{}",queueVo);


//        lxcQueueService.deleteWithValidByIds(Arrays.asList(queue.getId()),true);
        Boolean aBoolean = lxcLxcService.deleteWithValidByIdsInReset(Arrays.asList(lxc.getId()), true);
        if(aBoolean)
            log.info("已删除小鸡信息:{}",lxc.getId());
        else
            log.info("删除小鸡信息失败:{}",lxc.getId());

        String keyOfApplyVo = String.format(CacheNames.LXC_APPLY_VO,lxc.getId());
        LxcApplyBo applyBo = (LxcApplyBo)RedisUtils.getCacheObject(keyOfApplyVo);
        //重新创建lxc小鸡
//        lxcQueueService.applyLxc(superLxc,loginUser,applyBo,true);
        log.info(">>>>>>>>>>>>>>>>>superLxc:{}",superLxc);
        String superLxcIp = superLxc.getIp();
//        String lxcPort = checkWhichPortForLxd(superLxcIp);
        // 从数据库取访问lxd还是incus端口
        String way = superLxc.getWay();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + way);
//        String lxcPort = "lxd".equals(way) ? LxcConstants.LXD_PORT : LxcConstants.INCUS_PORT;
        String lxcPort = LxcConstants.LXD_PORT;
        try {
            InstancesUtils.target(superLxcIp,lxcPort).postInstances(lxc, superLxc, applyBo);
            lxc.setUpdateTime(DateUtil.parseDate(DateUtil.now()));
            lxcMapper.insert(lxc);
            LxcQueue lxcQueue = new LxcQueue();
            BeanUtils.copyProperties(queueVo,lxcQueue);
            lxcQueue.setUpdateTime(DateUtil.parseDate(DateUtil.now()));
            lxcQueueService.insert(lxcQueue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /**
         * 创建小鸡是后台任务，这里状态修改是前台任务，修改得太早了
         */
        // 检查小鸡重建前是否流量超限，若超限，则小鸡状态设为frozen TODO
/*
        if(lxc.getLxcStatus().equals(LxcStatus.RUNNING.getCode()) && lxcStatusOrigin.equalsIgnoreCase(LxcStatus.TRAFFIC_LIMIT.getCode())){
            try {
                ShellUtils.execCmdAndReturnStatus(ShellDto.builder()
                    .ip(superLxcIp)
                    .password(superLxc.getPassword())
                    .modifyType("traffic_limit")
                    .childName(lxc.getChildName())
                    .build());
                lxcLxcService.updateStatus(LxcStatus.TRAFFIC_LIMIT.getCode(),lxc.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
*/


        //删除lxc和queue缓存
//        RedisUtils.deleteObject(keyOfApplyVo);
//        RedisUtils.deleteObject(keyOfSuperLxc);
//        RedisUtils.deleteObject(keyOfLxc);
//        RedisUtils.deleteObject(keyOfQueue);
        return R.ok();
    }

    /**
     * 获取lxc监听的端口
     * @param superIp
     * @return
     */
    public String checkWhichPortForLxd(String superIp){
        List<String> list = new ArrayList<>();
        LxcSuperLxcVo lxcSuperLxcVo = lxcSuperLxcService.queryByIp(superIp);
        String command = String.format("lsof -i:8443");
        try {
            list = ShellUtils.execCmdOutList(command,"root",lxcSuperLxcVo.getPassword(),lxcSuperLxcVo.getIp(),Integer.valueOf(lxcSuperLxcVo.getSshPort()));
            System.out.println(list.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(list != null || !list.isEmpty()){
            return "8443";
        }else{
            return "9969";
        }
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
    // 查找容器创建日志
    @SaIgnore
    @GetMapping("/getInitLog/{lxcId}")
    public R<List<String>> getInitLog(@PathVariable Long lxcId) {
        return lxcLxcService.getSessionByLxcId(lxcId, (session, lxc) -> R.ok(ShellUtils.execCmdOutList(String.format(ShellUtils.INIT_LOG, lxc.getChildName()), session)));
    }
}
