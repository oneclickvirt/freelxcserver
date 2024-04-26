package com.lxc.machine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.constant.CacheNames;
import com.lxc.common.constant.LxcConstants;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.enums.LxcStatus;
import com.lxc.common.utils.BeanCopyUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.framework.service.BaseService;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcQueue;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.SysSuperRole;
import com.lxc.machine.domain.bo.LxcSuperLxcBo;
import com.lxc.machine.domain.vo.LxcCartVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.mapper.LxcSuperLxcMapper;
import com.lxc.machine.service.*;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 母鸡信息Service业务层处理
 *
 * @author lxc
 * @date 2022-10-11
 */
@RequiredArgsConstructor
@Service
public class LxcSuperLxcServiceImpl extends BaseService<LxcSuperLxcMapper, LxcSuperLxc> implements ILxcSuperLxcService {

    private final LxcSuperLxcMapper baseMapper;
    private final ILxcLxcService lxcLxcService;

    @Autowired
    @Lazy
    ILxcQueueService lxcQueueService;
    private final ISysSuperRoleService sysSuperRoleService;
    private final ISysLxcRoleService sysLxcRoleService;

    /**
     * 查询母鸡信息
     */
    @Override
    public LxcSuperLxcVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询母鸡信息列表
     */
    @Override
    public TableDataInfo<LxcSuperLxcVo> queryPageList(LxcSuperLxcBo bo,PageQuery pageQuery) {
        LambdaQueryWrapper<LxcSuperLxc> lqw = buildQueryWrapper(bo);
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setColumn("expire_flag");
        orderItem1.setAsc(false);
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setColumn("id");
        orderItem2.setAsc(false);
        Page<LxcSuperLxc> build = pageQuery.build();
        build.addOrder(orderItem1,orderItem2);
        Page<LxcSuperLxcVo> result = baseMapper.selectVoPage(build,lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询母鸡信息列表
     */
    @Override
    public List<LxcSuperLxcVo> queryList(LxcSuperLxcBo bo) {
        LambdaQueryWrapper<LxcSuperLxc> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<LxcSuperLxc> buildQueryWrapper(LxcSuperLxcBo bo) {
        Map<String,Object> params = bo.getParams();
        LambdaQueryWrapper<LxcSuperLxc> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()),LxcSuperLxc::getName,bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getTag()),LxcSuperLxc::getTag,bo.getTag());
        lqw.eq(StringUtils.isNotBlank(bo.getIp()),LxcSuperLxc::getIp,bo.getIp());
        lqw.eq(StringUtils.isNotBlank(bo.getPassword()),LxcSuperLxc::getPassword,bo.getPassword());
        lqw.eq(StringUtils.isNotBlank(bo.getSshPort()),LxcSuperLxc::getSshPort,bo.getSshPort());
        lqw.eq(StringUtils.isNotBlank(bo.getDate()),LxcSuperLxc::getDate,bo.getDate());
        lqw.eq(StringUtils.isNotBlank(bo.getDead()),LxcSuperLxc::getDead,bo.getDead());
        lqw.eq(StringUtils.isNotBlank(bo.getSuperStatus()), LxcSuperLxc::getSuperStatus, bo.getSuperStatus());
        lqw.eq(bo.getSshPortStart() != null, LxcSuperLxc::getSshPortStart, bo.getSshPortStart());
        lqw.eq(bo.getMemoryLimit() != null, LxcSuperLxc::getMemoryLimit, bo.getMemoryLimit());
        lqw.eq(bo.getDiskLimit() != null, LxcSuperLxc::getDiskLimit, bo.getDiskLimit());
        lqw.eq(bo.getMaxQuantity() != null, LxcSuperLxc::getMaxQuantity, bo.getMaxQuantity());
        lqw.eq(bo.getNetStartPort() != null, LxcSuperLxc::getNetStartPort, bo.getNetStartPort());
        lqw.eq(bo.getNetStep() != null, LxcSuperLxc::getNetStep, bo.getNetStep());
        lqw.eq(bo.getUpLimit() != null, LxcSuperLxc::getUpLimit, bo.getUpLimit());
        lqw.eq(bo.getDownLimit() != null, LxcSuperLxc::getDownLimit, bo.getDownLimit());
        lqw.eq(StringUtils.isNotBlank(bo.getExpireFlag()), LxcSuperLxc::getExpireFlag, bo.getExpireFlag());
        lqw.eq(StringUtils.isNotBlank(bo.getPrefix()), LxcSuperLxc::getPrefix, bo.getPrefix());
        lqw.eq(StringUtils.isNotBlank(bo.getIpv6Flag()), LxcSuperLxc::getIpv6Flag, bo.getIpv6Flag());
        lqw.eq(StringUtils.isNotBlank(bo.getRootLimits()), LxcSuperLxc::getRootLimits, bo.getRootLimits());
        lqw.eq(StringUtils.isNotBlank(bo.getInstanceConfig()), LxcSuperLxc::getInstanceConfig, bo.getInstanceConfig());
        lqw.eq(StringUtils.isNotBlank(bo.getSourceConfig()), LxcSuperLxc::getSourceConfig, bo.getSourceConfig());
        return lqw;
    }


    /**
     * 新增母鸡信息
     */
    @Override
    public Boolean insertByBo(LxcSuperLxcBo bo) {
        LxcSuperLxc add = BeanUtil.toBean(bo,LxcSuperLxc.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            setFreshCache(add);
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改母鸡信息
     */
    @Override
    public Boolean updateByBo(LxcSuperLxcBo bo) {
        Long[] roleIdsArr = bo.getRoleIds().toArray(new Long[0]);
        sysSuperRoleService.delete(new QueryWrapper<SysSuperRole>().eq("super_id",bo.getId()));
        sysSuperRoleService.insertLxcAuth(bo.getId(),roleIdsArr);
        LxcSuperLxc update = BeanUtil.toBean(bo,LxcSuperLxc.class);
        validEntityBeforeSave(update);
        setFreshCache(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(LxcSuperLxc entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除母鸡信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids,Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        ids.forEach(id -> {
            LxcSuperLxc lxcSuperLxc = baseMapper.selectById(id);
            String superLxcIp = lxcSuperLxc.getIp();
            sysLxcRoleService.deleteBySuperIp(superLxcIp);
            lxcQueueService.delete(new QueryWrapper<LxcQueue>().eq("ip",superLxcIp));
            lxcLxcService.delete(new QueryWrapper<LxcLxc>().eq("super_ip",superLxcIp));
            sysSuperRoleService.delete(new QueryWrapper<SysSuperRole>().eq("super_id",id));
            RedisUtils.deleteKeys(String.format(CacheNames.LXC_SUPER_APPLY_HEAD,lxcSuperLxc.getId()) + "*");
        });
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 缓存母鸡可开小鸡数量
     */
    @Override
    public void loadingStockCache() {
        List<LxcSuperLxc> list = getAliveLxcSuperLxcList();
        for (LxcSuperLxc lxcSuperLxc : list) {
            setFreshCache(lxcSuperLxc);
        }
    }

    /**
     * 获取可用状态的母鸡列表
     */
    @Override
    public List<LxcSuperLxc> getAliveLxcSuperLxcList() {
        LambdaQueryWrapper<LxcSuperLxc> lqw = Wrappers.lambdaQuery();
        lqw.ne(LxcSuperLxc::getSuperStatus,LxcConstants.LXC_SUPER_DISABLED)
           .ne(LxcSuperLxc::getExpireFlag,LxcConstants.LXC_SUPER_EXPIRED);
        List<LxcSuperLxc> list = baseMapper.selectList(lqw);
        return list;
    }

    /**
     * 设置母鸡可开小鸡缓存
     */
    @Override
    public void setFreshCache(LxcSuperLxc lxcSuperLxc) {
        Long superLxcId = lxcSuperLxc.getId();
        RReadWriteLock readWriteLock = RedisUtils.getClient().getReadWriteLock(CacheNames.LXC_SUPER_APPLY_LOCK + superLxcId);
        RLock rLock = readWriteLock.writeLock();
        try {
            rLock.lock();
            String keyOfAll = String.format(CacheNames.LXC_SUPER_APPLY_ALL,superLxcId);
            String keyOfFree = String.format(CacheNames.LXC_SUPER_APPLY_FREE,superLxcId);
            String keyOfUsed = String.format(CacheNames.LXC_SUPER_APPLY_USED,superLxcId);
            long countUsed = lxcLxcService.count(
                    Wrappers.lambdaQuery(LxcLxc.class)
                            .eq(LxcLxc::getSuperIp,lxcSuperLxc.getIp())
                            .ne(LxcLxc::getLxcStatus,LxcStatus.DESTROYED.getCode())
                            .ne(LxcLxc::getLxcStatus,LxcStatus.EXPIRED.getCode())
            );
            RedisUtils.setAtomicValue(keyOfAll,lxcSuperLxc.getMaxQuantity());
            RedisUtils.setAtomicValue(keyOfFree,lxcSuperLxc.getMaxQuantity() - countUsed);
            RedisUtils.setAtomicValue(keyOfUsed,countUsed);
        } finally {
            rLock.unlock();
        }
    }

    /**
     * 根据角色查询小鸡信息列表
     */
    @Override
    public List<LxcCartVo> getSuperListWithRoles(List<RoleDTO> sysRoles) {
        List<LxcSuperLxc> superListWithRoles = baseMapper.getSuperListWithRoles(sysRoles);
        return BeanCopyUtils.copyList(superListWithRoles,LxcCartVo.class);
    }

}
