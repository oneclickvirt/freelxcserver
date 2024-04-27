package com.lxc.machine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.ssh.JschUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jcraft.jsch.Session;
import com.lxc.common.constant.CacheNames;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.utils.ShellUtils;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.framework.service.BaseService;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcLxcBo;
import com.lxc.machine.domain.vo.LxcLxcVo;
import com.lxc.machine.mapper.LxcLxcMapper;
import com.lxc.machine.mapper.SysLxcRoleMapper;
import com.lxc.machine.service.ILxcLxcService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 小鸡信息Service业务层处理
 *
 * @author lxc
 * @date 2022-10-11
 */
@RequiredArgsConstructor
@Service
public class LxcLxcServiceImpl extends BaseService<LxcLxcMapper, LxcLxc> implements ILxcLxcService {

    private final SysLxcRoleMapper sysLxcRoleMapper;
    private final LxcLxcMapper baseMapper;

    @Lazy
    @Autowired
    private LxcQueueServiceImpl lxcQueueService;

    @Lazy
    @Autowired
    private LxcSuperLxcServiceImpl lxcSuperLxcService;


    /**
     * 查询小鸡信息
     */
    @Override
    public LxcLxcVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询小鸡信息列表
     */
    @Override
    public TableDataInfo<LxcLxcVo> queryPageList(LxcLxcBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LxcLxc> lqw = buildQueryWrapper(bo);
        lqw.last("order by lxc_status,state,super_ip,id desc");
        Page<LxcLxcVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询小鸡信息列表
     */
    @Override
    public List<LxcLxcVo> queryList(LxcLxcBo bo) {
        LambdaQueryWrapper<LxcLxc> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<LxcLxcVo> getLxcList(QueryWrapper<LxcLxc> queryWrapper) {
        return baseMapper.selectVoList(queryWrapper);
    }

    /**
     * 根据角色查询小鸡信息列表
     */
    @Override
    public List<LxcLxcVo> getLxcListWithRoles(List<RoleDTO> sysRoles) {
        return BeanUtil.copyToList(baseMapper.getLxcListWithRoles(sysRoles), LxcLxcVo.class);
    }

    /**
     * 对LxcLxcBo中每个成员变量在数据库查找的匹配条件进行配置
     * @param bo
     * @return
     */
    private LambdaQueryWrapper<LxcLxc> buildQueryWrapper(LxcLxcBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LxcLxc> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), LxcLxc::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getTag()), LxcLxc::getTag, bo.getTag());
        lqw.eq(StringUtils.isNotBlank(bo.getSuperIp()), LxcLxc::getSuperIp, bo.getSuperIp());
        lqw.like(StringUtils.isNotBlank(bo.getChildName()), LxcLxc::getChildName, bo.getChildName());
        lqw.eq(StringUtils.isNotBlank(bo.getSshPort()), LxcLxc::getSshPort, bo.getSshPort());
        lqw.eq(StringUtils.isNotBlank(bo.getPassword()), LxcLxc::getPassword, bo.getPassword());
        lqw.eq(StringUtils.isNotBlank(bo.getPortStart()), LxcLxc::getPortStart, bo.getPortStart());
        lqw.eq(StringUtils.isNotBlank(bo.getPortEnd()), LxcLxc::getPortEnd, bo.getPortEnd());
        lqw.eq(StringUtils.isNotBlank(bo.getSuperDate()), LxcLxc::getSuperDate, bo.getSuperDate());
        lqw.eq(StringUtils.isNotBlank(bo.getSuperDead()), LxcLxc::getSuperDead, bo.getSuperDead());
        lqw.eq(StringUtils.isNotBlank(bo.getLxcStatus()), LxcLxc::getLxcStatus, bo.getLxcStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getHostInterface()), LxcLxc::getHostInterface, bo.getHostInterface());
        lqw.eq(StringUtils.isNotBlank(bo.getIpv6Addr()), LxcLxc::getIpv6Addr, bo.getIpv6Addr());
        lqw.eq(bo.getState() != null, LxcLxc::getState, bo.getState());
        return lqw;
    }

    /**
     * 新增小鸡信息
     */
    @Override
    public Boolean insertByBo(LxcLxcBo bo) {
        LxcLxc add = BeanUtil.toBean(bo, LxcLxc.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改小鸡信息
     */
    @Override
    public Boolean updateByBo(LxcLxcBo bo) {
        LxcLxc update = BeanUtil.toBean(bo, LxcLxc.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 修改小鸡信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Boolean updateByBoForce(LxcLxcBo bo) {
        LxcLxc update = BeanUtil.toBean(bo, LxcLxc.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(LxcLxc entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除小鸡信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        Map<String, LxcSuperLxc> superMap = new HashMap<>();
        ids.forEach(id -> {
            LxcLxc lxcLxc = baseMapper.selectById(id);
            lxcQueueService.delForceByLxcId(id);
            String superIp = lxcLxc.getSuperIp();
            if (!superMap.containsKey(superIp)) {
                LambdaQueryWrapper<LxcSuperLxc> eq = Wrappers.lambdaQuery();
                eq.eq(LxcSuperLxc::getIp, superIp);
                LxcSuperLxc lxcSuperLxc = lxcSuperLxcService.getOne(eq);
                superMap.put(lxcSuperLxc.getIp(), lxcSuperLxc);
            }
            LxcSuperLxc lxcSuperLxc = superMap.get(superIp);
            try {
                ShellUtils.execCmdOutList(String.format("lxc stop %1$s && lxc delete %1$s", lxcLxc.getChildName())
                    , "root", lxcSuperLxc.getPassword(), lxcSuperLxc.getIp(), 22);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        int i = baseMapper.deleteBatchIds(ids);
        superMap.forEach((k, v) -> {
            lxcSuperLxcService.setFreshCache(v);
        });
        return i > 0;
    }

    @Override
    public Map<String, Object> webIndexStatisticsLxc() {
        Map<String, Object> re = Maps.newHashMap();
        // 空闲
        re.put("lxcOnLineList", getLxcStatisticsListByState(0));
        // 已用
        re.put("lxcLxcUsedList", getLxcStatisticsListByState(1));
        return re;
    }

    /**
     * 根据状态查询小鸡所有统计
     */
    public List<Map<String, Object>> getLxcStatisticsListByState(int state) {
        if (state == 0) {//空闲
            List<Map<String, Object>> freeList = Lists.newArrayList();
            lxcSuperLxcService.getAliveLxcSuperLxcList().forEach(lxcSuperLxc -> {
                String keyOfFree = String.format(CacheNames.LXC_SUPER_APPLY_FREE, lxcSuperLxc.getId());
                long freeNum = RedisUtils.getAtomicValue(keyOfFree);
                if (freeNum > 0) {
                    //空闲的小鸡名称和数量
                    Map<String, Object> freeMap = Maps.newHashMap();
                    freeMap.put("name", lxcSuperLxc.getName());
                    freeMap.put("value", freeNum);
                    freeList.add(freeMap);
                }
            });
            return freeList;
        } else {//忙碌
            QueryWrapper<LxcLxc> lxcQueryWrapper = new QueryWrapper<>();
            lxcQueryWrapper.groupBy("tag");
            lxcQueryWrapper.select("tag,name,count(*) count");
            lxcQueryWrapper.groupBy("tag,name");
            List<LxcLxcVo> lxcLxcVos = baseMapper.selectVoList(lxcQueryWrapper);
            return lxcLxcVos.stream().map(obj -> {
                Map<String, Object> map = Maps.newHashMap();
                map.put("name", obj.getName());
                map.put("value", obj.getCount());
                return map;
            }).collect(Collectors.toList());
        }
    }

    /**
     * 统计已使用数量
     */
    @Override
    public int countUsedByTgId(String tgId) {
        return baseMapper.countUsedByTgId(tgId);
    }

    /**
     * 根据lxcId获取session
     *
     * @param lxcId
     * @param mapper
     */
    @Override
    public <T> T getSessionByLxcId(Long lxcId, BiFunction<Session, LxcLxc, T> mapper) {
        LxcLxc lxc = this.getById(lxcId);
        assert lxc != null;
        return lxcSuperLxcService.lambdaQuery()
            .eq(LxcSuperLxc::getIp, lxc.getSuperIp())
            .last("limit 1")
            .oneOpt()
            .map(superLxc -> {
                //配置IP、ssh、权限、密码获取会话Sessino
                Session session = JschUtil.getSession(superLxc.getIp(), 22, "root", superLxc.getPassword());
                return mapper.apply(session, lxc);
            })
            .orElse(null);
    }

}
