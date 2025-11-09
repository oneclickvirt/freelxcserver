package com.lxc.machine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.machine.domain.SysLxcRole;
import com.lxc.machine.domain.bo.SysLxcRoleBo;
import com.lxc.machine.domain.vo.SysLxcRoleVo;
import com.lxc.machine.mapper.SysLxcRoleMapper;
import com.lxc.machine.service.ISysLxcRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 小鸡和角色关联Service业务层处理
 *
 * @author lxc
 * @date 2022-11-16
 */
@RequiredArgsConstructor
@Service
public class SysLxcRoleServiceImpl implements ISysLxcRoleService {

    private final SysLxcRoleMapper baseMapper;
    private final SysLxcRoleMapper sysLxcRoleMapper;

    /**
     * 查询小鸡和角色关联
     */
    @Override
    public SysLxcRoleVo queryById(Long lxcId){
        return baseMapper.selectVoById(lxcId);
    }

    /**
     * 查询小鸡和角色关联列表
     */
    @Override
    public TableDataInfo<SysLxcRoleVo> queryPageList(SysLxcRoleBo bo,PageQuery pageQuery) {
        LambdaQueryWrapper<SysLxcRole> lqw = buildQueryWrapper(bo);
        Page<SysLxcRoleVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询小鸡和角色关联列表
     */
    @Override
    public List<SysLxcRoleVo> queryList(SysLxcRoleBo bo) {
        LambdaQueryWrapper<SysLxcRole> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysLxcRole> buildQueryWrapper(SysLxcRoleBo bo) {
        Map<String,Object> params = bo.getParams();
        LambdaQueryWrapper<SysLxcRole> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getRoleId() != null,SysLxcRole::getRoleId,bo.getRoleId());
        lqw.eq(bo.getLxcId() != null,SysLxcRole::getLxcId,bo.getLxcId());
        return lqw;
    }

    /**
     * 新增小鸡和角色关联
     */
    @Override
    public Boolean insertByBo(SysLxcRoleBo bo) {
        SysLxcRole add = BeanUtil.toBean(bo, SysLxcRole.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setLxcId(add.getLxcId());
        }
        return flag;
    }

    /**
     * 修改小鸡和角色关联
     */
    @Override
    public Boolean updateByBo(SysLxcRoleBo bo) {
        SysLxcRole update = BeanUtil.toBean(bo, SysLxcRole.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysLxcRole entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除小鸡和角色关联
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 关联lxc角色
     *
     * @param lxcId  用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertLxcAuth(Long lxcId,Long[] roleIds) {
        insertLxcRole(lxcId,roleIds);
    }


    /**
     * 根据lxcId删除
     *
     * @param lxcId
     * @return
     */
    @Override
    public void deleteByLxcId(Long lxcId) {
        SysLxcRole sysLxcRole = new SysLxcRole();
        sysLxcRole.setLxcId(lxcId);
        sysLxcRole.setIsDeleted(1);
        baseMapper.updateById(sysLxcRole);
    }

    /**
     * 根据superIp删除
     *
     * @param superIp
     * @return
     */
    @Override
    public void deleteBySuperIp(String superIp) {
        baseMapper.deleteBySuperIp(superIp);
    }

    /**
     * 新增lxc角色信息
     *
     * @param lxcId   用户ID
     * @param roleIds 角色组
     */
    public void insertLxcRole(Long lxcId,Long[] roleIds) {
        if (ArrayUtil.isNotEmpty(roleIds)) {
            // 新增用户与角色管理
            List<SysLxcRole> list = new ArrayList<>(roleIds.length);
            for (Long roleId : roleIds) {
                SysLxcRole lr = new SysLxcRole();
                lr.setLxcId(lxcId);
                lr.setRoleId(roleId);
                list.add(lr);
            }
            sysLxcRoleMapper.insertBatch(list);
        }
    }
}
