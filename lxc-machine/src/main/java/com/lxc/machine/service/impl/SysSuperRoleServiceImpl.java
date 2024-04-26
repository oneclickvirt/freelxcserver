package com.lxc.machine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.framework.service.BaseService;
import com.lxc.machine.domain.SysSuperRole;
import com.lxc.machine.domain.bo.SysSuperRoleBo;
import com.lxc.machine.domain.vo.SysSuperRoleVo;
import com.lxc.machine.mapper.SysSuperRoleMapper;
import com.lxc.machine.service.ISysSuperRoleService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 母鸡和角色关联Service业务层处理
 *
 * @author lxc
 * @date 2022-11-16
 */
@RequiredArgsConstructor
@Service
public class SysSuperRoleServiceImpl extends BaseService<SysSuperRoleMapper,SysSuperRole> implements ISysSuperRoleService {

    private final SysSuperRoleMapper baseMapper;

    /**
     * 查询母鸡和角色关联
     */
    @Override
    public SysSuperRoleVo queryById(Long lxcId) {
        return baseMapper.selectVoById(lxcId);
    }

    /**
     * 查询母鸡和角色关联列表
     */
    @Override
    public TableDataInfo<SysSuperRoleVo> queryPageList(SysSuperRoleBo bo,PageQuery pageQuery) {
        LambdaQueryWrapper<SysSuperRole> lqw = buildQueryWrapper(bo);
        Page<SysSuperRoleVo> result = baseMapper.selectVoPage(pageQuery.build(),lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询母鸡和角色关联列表
     */
    @Override
    public List<SysSuperRoleVo> queryList(SysSuperRoleBo bo) {
        LambdaQueryWrapper<SysSuperRole> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysSuperRole> buildQueryWrapper(SysSuperRoleBo bo) {
        Map<String,Object> params = bo.getParams();
        LambdaQueryWrapper<SysSuperRole> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getRoleId() != null,SysSuperRole::getRoleId,bo.getRoleId());
        lqw.eq(bo.getSuperId() != null,SysSuperRole::getSuperId,bo.getSuperId());
        return lqw;
    }

    /**
     * 新增母鸡和角色关联
     */
    @Override
    public Boolean insertByBo(SysSuperRoleBo bo) {
        SysSuperRole add = BeanUtil.toBean(bo,SysSuperRole.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setSuperId(add.getSuperId());
        }
        return flag;
    }

    /**
     * 修改母鸡和角色关联
     */
    @Override
    public Boolean updateByBo(SysSuperRoleBo bo) {
        SysSuperRole update = BeanUtil.toBean(bo,SysSuperRole.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysSuperRole entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除母鸡和角色关联
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids,Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 关联lxc角色
     *
     * @param superId 用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertLxcAuth(Long superId,Long[] roleIds) {
        insertLxcRole(superId,roleIds);
    }

    /**
     * 新增lxc角色信息
     *
     * @param superId   用户ID
     * @param roleIds 角色组
     */
    public void insertLxcRole(Long superId,Long[] roleIds) {
        if (ArrayUtil.isNotEmpty(roleIds)) {
            // 新增用户与角色管理
            List<SysSuperRole> list = new ArrayList<>(roleIds.length);
            for (Long roleId : roleIds) {
                SysSuperRole lr = new SysSuperRole();
                lr.setSuperId(superId);
                lr.setRoleId(roleId);
                list.add(lr);
            }
            baseMapper.insertBatch(list);
        }
    }
}
