package com.lxc.machine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.constant.CacheNames;
import com.lxc.common.constant.LxcConstants;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.R;
import com.lxc.common.core.domain.dto.ShellDto;
import com.lxc.common.core.domain.entity.LxcUser;
import com.lxc.common.core.domain.model.LoginUser;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.enums.LxcStatus;
import com.lxc.common.helper.LoginHelper;
import com.lxc.common.utils.ShellUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.framework.service.BaseService;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcQueue;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.SysLxcRole;
import com.lxc.machine.domain.bo.LxcApplyBo;
import com.lxc.machine.domain.bo.LxcQueueBo;
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.domain.vo.LxcQueueVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.domain.vo.SysLxcRoleVo;
import com.lxc.machine.mapper.*;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcQueueService;
import com.lxc.machine.service.ILxcSuperLxcService;
import com.lxc.machine.utils.InstancesUtils;
import lombok.RequiredArgsConstructor;
import org.relxd.lxd.ApiException;
import org.relxd.lxd.api.InstancesApi;
import org.relxd.lxd.model.BackgroundOperationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 小鸡和用户关联表Service业务层处理
 *
 * @author lxc
 * @date 2022-10-11
 */
@RequiredArgsConstructor
@Service
public class LxcQueueServiceImpl extends BaseService<LxcQueueMapper, LxcQueue> implements ILxcQueueService {

    private final InstancesApi lxcApi;

    private final LxcQueueMapper baseMapper;

    private final LxcLxcMapper lxcMapper;

    private final LxcQueueMapper queueMapper;

    private final LxcSuperLxcMapper lxcSuperLxcMapper;

    private final SysLxcRoleMapper sysLxcRoleMapper;

    private final SysSuperRoleMapper sysSuperRoleMapper;

    private final ILxcLxcService lxcService;
    private final ILxcSuperLxcService lxcSuperLxcService;

    /**
     * 查询小鸡和用户关联表
     */
    @Override
    public LxcQueueVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询小鸡和用户关联表列表
     */
    @Override
    public TableDataInfo<LxcQueueVo> queryPageList(LxcQueueBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LxcQueue> lqw = buildQueryWrapper(bo);
        Page<LxcQueueVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询小鸡和用户关联表列表
     */
    @Override
    public List<LxcQueueVo> queryList(LxcQueueBo bo) {
        LambdaQueryWrapper<LxcQueue> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<LxcQueue> buildQueryWrapper(LxcQueueBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LxcQueue> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getLxcId() != null, LxcQueue::getLxcId, bo.getLxcId());
        lqw.like(StringUtils.isNotBlank(bo.getSuperName()), LxcQueue::getSuperName, bo.getSuperName());
        lqw.eq(StringUtils.isNotBlank(bo.getTgId()), LxcQueue::getTgId, bo.getTgId());
        lqw.eq(StringUtils.isNotBlank(bo.getIp()), LxcQueue::getIp, bo.getIp());
        lqw.eq(StringUtils.isNotBlank(bo.getSshPort()), LxcQueue::getSshPort, bo.getSshPort());
        lqw.eq(StringUtils.isNotBlank(bo.getEndTime()), LxcQueue::getEndTime, bo.getEndTime());
        return lqw;
    }

    /**
     * 新增小鸡和用户关联表
     */
    @Override
    public Boolean insertByBo(LxcQueueBo bo) {
        RedisUtils.deleteKeys(CacheNames.FREE_LXC_LIST + "*");
        RedisUtils.deleteKeys(CacheNames.STATISTICS_LXC_LIST);
        LxcQueue add = BeanUtil.toBean(bo, LxcQueue.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改小鸡和用户关联表
     */
    @Override
    public Boolean updateByBo(LxcQueueBo bo) {
        LxcQueue update = BeanUtil.toBean(bo, LxcQueue.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(LxcQueue entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除小鸡和用户关联表
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Object> addUserLxc(LxcQueueBo queueBo, LoginUser loginUser) {
        LxcUser tgUser = loginUser.getLxcUser();
        LxcLxc lxc = lxcMapper.selectById(queueBo.getLxcId());
        if (lxc == null) {
            return R.fail("没有这样的鸡，不要调皮啦");
        }

        if (lxc.getState() == 1) {
            return R.fail("来晚啦，该小鸡已经被领取");
        }

        LambdaQueryWrapper<SysLxcRole> lxcRoleLambdaQueryWrapper = Wrappers.lambdaQuery(SysLxcRole.class)
            .eq(SysLxcRole::getLxcId, lxc.getId());
        List<SysLxcRoleVo> sysLxcRoleVos = sysLxcRoleMapper.selectVoList(lxcRoleLambdaQueryWrapper);
        // 判断是否有角色权限领取该小鸡
        if (sysLxcRoleVos.size() > 0) {
            boolean flag = false;
            for (SysLxcRoleVo sysLxcRoleVo : sysLxcRoleVos) {
                if (loginUser.hasRole(sysLxcRoleVo.getRoleId())) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                return R.fail("你没有领取这个鸡的权限");
            }
        } else {
            return R.fail("该小鸡没有设置领取权限,请联系管理员");
        }

        String tgId = loginUser.getTgId();

        QueryWrapper<LxcQueue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tg_id", tgId);
        List<LxcQueue> queueList = baseMapper.selectList(queryWrapper);

        Long lxcQuota = tgUser.getLxcQuota();
        if (lxcQuota - queueList.size() <= 0) {
            return R.fail(HttpStatus.HTTP_NOT_ACCEPTABLE, "配额不足");
        }

        LxcQueue queue = new LxcQueue();
        queue.setLxcId(queueBo.getLxcId());
        queue.setEndTime(lxc.getSuperDead());
        queue.setSuperName(lxc.getName());
        queue.setTgId(tgId);
        queue.setIp(lxc.getSuperIp());
        queue.setSshPort(lxc.getSshPort());

        lxc.setState(1);
        lxcMapper.updateById(lxc);
        baseMapper.insert(queue);

        tgUser.setSurplusQuota(tgUser.getSurplusQuota() - 1);
        LoginHelper.setLoginUser(loginUser);
        return null;
    }


    public static int findFirstDiscontinuous(List<Integer> numbers) {

        if (numbers.isEmpty()) {
            return 1;
        }

        int size = numbers.size();
        if (size == 1) {
            if (numbers.get(0) == 1) {
                return 2;
            } else {
                return 1;
            }
        }
        int index = numbers.get(size - 1);
        for (int i = 0; i < size; i++) {
            int j = i + 1;
            if (numbers.get(i) != j) {
                index = i;
                break;
            }
        }
        return index + 1;
    }

    /**
     * 原applyLxc
     * @param superLxc
     * @param loginUser
     * @param lxcApplyBo
     * @return
     */
//    @Override
//    public R<Object> applyLxc(LxcSuperLxc superLxc, LoginUser loginUser, LxcApplyBo lxcApplyBo) {
//        String superLxcIp = superLxc.getIp();
//        BackgroundOperationResponse allInstancesBase = getAllInstancesBase(superLxcIp);
//        Object metadata = allInstancesBase.getMetadata();
//        // 转为string list
//        List<String> instancesList = JSONUtil.parseArray(JSONUtil.toJsonStr(metadata)).toList(String.class);
//        //获取去掉前缀的小鸡名称序号列表
//        List<Integer> collect = instancesList.stream()
//            .map(o -> {
//                try {
//                    //小鸡名称分割出前缀和序号，如te1分为te 和 1
//                    return Integer.parseInt(o.split(superLxc.getPrefix())[1]);
//                } catch (Exception e) {
//                    return 0;
//                }
//            })
//            .filter(o -> o != 0)
//            .distinct()
//            .sorted()
//            .collect(Collectors.toList());
//        // 找出没有连续的数字，比如1,2,5,6返回3 ，1,2,3,4返回5
//        int portIndex = findFirstDiscontinuous(collect);
//
//        LxcUser tgUser = loginUser.getLxcUser();
//        tgUser.setSurplusQuota(tgUser.getSurplusQuota() - 1);
//        LoginHelper.setLoginUser(loginUser);
//        //从开始端口偏移当前小鸡索引
//        long sshPort = superLxc.getSshPortStart() + portIndex;
//
//        long netStartPort = superLxc.getNetStartPort() + (portIndex * superLxc.getNetStep());
//        LxcLxc lxc = new LxcLxc().setChildName(superLxc.getPrefix() + portIndex)
//            .setName(superLxc.getName())
//            .setTag(superLxc.getTag())
//            .setSuperIp(superLxcIp)
//            .setSshPort(String.valueOf(sshPort))
//            .setPortStart(netStartPort + "")
//            .setPortEnd(netStartPort + (superLxc.getNetStep() - 1) + "")
//            .setState(1)
//            .setLxcStatus(LxcStatus.WAITING_FOR_PROCESS.getCode());
//        try {
//            InstancesUtils.target(superLxcIp).postInstances(lxc, superLxc, lxcApplyBo);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        lxcMapper.insert(lxc);
//
//        LxcQueue queue = new LxcQueue();
//        queue.setLxcId(lxc.getId());
//        queue.setEndTime(superLxc.getDead());
//        queue.setSuperName(lxc.getName());
//        queue.setTgId(loginUser.getTgId());
//        queue.setIp(lxc.getSuperIp());
//        queue.setSshPort(lxc.getSshPort());
//
//        baseMapper.insert(queue);
//        return null;
//    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Object> applyLxc(LxcSuperLxc superLxc, LoginUser loginUser, LxcApplyBo lxcApplyBo) {
        LxcLxc lxc = new LxcLxc();
        String superLxcIp = superLxc.getIp();
//        if(isReset) {
//            String keyOfLxc = String.format(CacheNames.LXC_LXC_RESET,lxc.getId());
//            lxc = RedisUtils.getCacheObject(CacheNames.LXC_LXC_RESET);
//            lxc.setLxcStatus(LxcStatus.WAITING_FOR_PROCESS.getCode());
//        }else {
//            String superLxcIp = superLxc.getIp();

        //****************************************************//
        // 手动执行创建容器日志文件
//        ShellUtils.execCmd(ShellUtils.INIT_LXC,"root",superLxc.getPassword(),);

        //****************************************************//

        BackgroundOperationResponse allInstancesBase = getAllInstancesBase(superLxcIp);
        Object metadata = allInstancesBase.getMetadata();
        // 将lxc list中容器(前缀+序号)中的容器名列表转为string list
        // 删除小鸡时要注意确保把对应容器删掉，否则会影响重新创建前lxc list读取已创建小鸡列表
        List<String> instancesList = JSONUtil.parseArray(JSONUtil.toJsonStr(metadata)).toList(String.class);
        //获取去掉前缀的小鸡名称序号列表
        List<Integer> collect = instancesList.stream()
            .map(o -> {
                try {
                    //小鸡名称分割出前缀和序号，如te1分为te 和 1
                    return Integer.parseInt(o.split(superLxc.getPrefix())[1]);
                } catch (Exception e) {
                    return 0;
                }
            })
            .filter(o -> o != 0)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        // 找出没有连续的数字，比如1,2,5,6返回3 ，1,2,3,4返回5
        int portIndex = findFirstDiscontinuous(collect);

        LxcUser tgUser = loginUser.getLxcUser();
        tgUser.setSurplusQuota(tgUser.getSurplusQuota() - 1);
        LoginHelper.setLoginUser(loginUser);
        //从开始端口偏移当前小鸡索引
        long sshPort = superLxc.getSshPortStart() + portIndex;

        long netStartPort = superLxc.getNetStartPort() + (portIndex * superLxc.getNetStep());
        // 如果前面之前的小鸡没删干净(因某些原因只删了一部分数据)，在删除方法里加了个@Transactional注解，这里set入lxc中的childName就会跟实际不符，特别是重建时，前后序号会不一致
        lxc = new LxcLxc().setChildName(superLxc.getPrefix() + portIndex)
            .setName(superLxc.getName())
            .setTag(superLxc.getTag())
            .setSuperIp(superLxcIp)
            .setSshPort(String.valueOf(sshPort))
            .setPortStart(netStartPort + "")
            .setPortEnd(netStartPort + (superLxc.getNetStep() - 1) + "")
            .setState(1)
            .setLxcStatus(LxcStatus.WAITING_FOR_PROCESS.getCode());
        try {
//            String lxcPort = checkWhichPortForLxd(superLxcIp);
            String way = superLxc.getWay();
            String lxcPort = "lxd".equals(way) ? LxcConstants.LXD_PORT : LxcConstants.INCUS_PORT;
            // 调用REST API执行lxd命令创建实例，重构成incus时改成okHttp来请求API
            InstancesUtils.target(superLxcIp,lxcPort).postInstances(lxc, superLxc, lxcApplyBo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        }
//            try {
//                InstancesUtils.target(superLxcIp).postInstances(lxc, superLxc, lxcApplyBo);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
        lxcMapper.insert(lxc);

        LxcQueue queue = new LxcQueue();
        queue.setLxcId(lxc.getId());
        queue.setEndTime(superLxc.getDead());
        queue.setSuperName(lxc.getName());
        queue.setTgId(loginUser.getTgId());
        queue.setIp(lxc.getSuperIp());
        queue.setSshPort(lxc.getSshPort());

        baseMapper.insert(queue);
        //将lxcApply存到redis，备用于重建，主要还是存小鸡的镜像信息
        String keyOfApplyVo = String.format(CacheNames.LXC_APPLY_VO,lxc.getId());
        if(RedisUtils.getCacheObject(keyOfApplyVo) == null) {//如果LXC_APPLY_VO为空
//            String keyOfApplyVo = String.format(CacheNames.LXC_APPLY_VO,lxc.getId());
            RedisUtils.setCacheObject(keyOfApplyVo, lxcApplyBo);
        }
        String keyOfSuperLxc = String.format(CacheNames.LXC_SUPER_LXC_RESET,lxc.getId());
        RedisUtils.setCacheObject(keyOfSuperLxc,superLxc);
        return null;
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
        if(list.size() == 0){
            return "9969";
        }else{
            return "8443";
        }
    }


    public static ShellDto prepareCreateShellDto(LxcSuperLxcVo lxcSuperLxcVo, LxcLxcVo lxc) {
        return ShellDto.builder()
            .ip(lxcSuperLxcVo.getIp())
            .sshPort(Integer.valueOf(lxcSuperLxcVo.getSshPort()))
            .username("root")
            .password(lxcSuperLxcVo.getPassword())
            .way(lxcSuperLxcVo.getWay()) //
            .childName(lxc.getChildName())
            .memoryLimit(lxcSuperLxcVo.getMemoryLimit())
            .diskLimit(lxcSuperLxcVo.getDiskLimit())
            .childSshPort(Long.valueOf(lxc.getSshPort()))
            .portStart(Integer.valueOf(lxc.getPortStart()))
            .portEnd(Integer.valueOf(lxc.getPortEnd()))
            .upLimit(lxcSuperLxcVo.getUpLimit())
            .downLimit(lxcSuperLxcVo.getDownLimit())
            .ipv6Flag(lxcSuperLxcVo.getIpv6Flag())
            .build();
    }

    @Override
    public List<LxcQueueVo> queryAllLxc(String tgId, String nowDateTime) {
        return baseMapper.queryAllLxc(tgId, nowDateTime);
    }


    public static void main(String[] args) {
        /*LxcLxc lxc = new LxcLxc().setChildName("test")
            .setName("test")
            .setTag("test")
            .setSuperIp("test")
            .setSshPort("test")
            .setPortStart("test")
            .setPortEnd("test")
            .setState(1)
            .setLxcStatus(LxcStatus.PENDING.getCode());
        ShellDto copy = BeanCopyUtils.copy(lxc, ShellDto.class);
        LxcSuperLxcVo lxcSuperLxcVo = new LxcSuperLxcVo();
        lxcSuperLxcVo.setIp("test");
        lxcSuperLxcVo.setPassword("test");
        lxcSuperLxcVo.setSshPort("test");
        BeanCopyUtils.copy(lxcSuperLxcVo, copy);
        System.out.println(copy);*/
//        System.out.println(findFirstDiscontinuous(Arrays.asList(2, 3)));
//*************************************************************************************
//        LxcSuperLxc superLxc = new LxcSuperLxc();
//        String superLxcIp = superLxc.getIp();
//        BackgroundOperationResponse allInstancesBase = getAllInstancesBase(superLxcIp);
//        Object metadata = allInstancesBase.getMetadata();
//        // 转为string list
//        List<String> instancesList = JSONUtil.parseArray(JSONUtil.toJsonStr(metadata)).toList(String.class);
//        //获取去掉前缀的小鸡名称序号列表
//        List<Integer> collect = instancesList.stream()
//            .map(o -> {
//                try {
//                    //小鸡名称分割出前缀和序号，如te1分为te 和 1
//                    return Integer.parseInt(o.split(superLxc.getPrefix())[1]);
//                } catch (Exception e) {
//                    return 0;
//                }
//            })
//            .filter(o -> o != 0)
//            .distinct()
//            .sorted()
//            .collect(Collectors.toList());
    }

    /**
     * 删除和小鸡关联信息
     */
    @Override
    public int delForce(QueryWrapper<LxcQueue> queryWrapper) {
        return baseMapper.delete(queryWrapper);
    }


    /**
     * 通过LxcId删除小鸡关联信息
     */
    @Override
    public int delForceByLxcId(Long lxcId) {
        sysLxcRoleMapper.delete(new QueryWrapper<SysLxcRole>().eq("lxc_id", lxcId));
        return baseMapper.delete(new QueryWrapper<LxcQueue>().eq("lxc_id", lxcId));
    }

    @Override
    public BackgroundOperationResponse getAllInstancesBase(String ip) {
//        String lxcPort = checkWhichPortForLxd(ip);
        // 直接从数据库读取
        QueryWrapper<LxcSuperLxc> queryWrapper = new QueryWrapper<>();
        queryWrapper
//            .select("way")
            .eq("ip",ip);
        LxcSuperLxc lxcSuperLxc = lxcSuperLxcService.getOne(queryWrapper);
        String way = lxcSuperLxc.getWay();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  " + way);
//        LxcSuperLxcVo lxcSuperLxcVo = lxcSuperLxcService.queryByIp(ip);

        String lxcPort = "lxd".equals(way) ? LxcConstants.LXD_PORT : LxcConstants.INCUS_PORT;
        try {
            // 目的是调用LXC的REST API创建实例
            return lxcApi.of(ip,lxcPort).getInstances(null, null);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据lxcId判断是否是用户的小鸡
     *
     * @param lxcId
     * @param tgId
     */
    @Override
    public boolean isUserLxc(Long lxcId, String tgId) {
        LambdaQueryWrapper<LxcQueue> eq = Wrappers.lambdaQuery();
        eq.eq(LxcQueue::getLxcId, lxcId);
        eq.eq(LxcQueue::getTgId, tgId);
        return baseMapper.selectCount(eq) > 0;
    }
}
