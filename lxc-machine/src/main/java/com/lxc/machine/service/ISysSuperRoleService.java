package com.lxc.machine.service;

import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.framework.service.IBaseService;
import com.lxc.machine.domain.SysSuperRole;
import com.lxc.machine.domain.bo.SysSuperRoleBo;
import com.lxc.machine.domain.vo.SysSuperRoleVo;
import java.util.Collection;
import java.util.List;

/**
 * 母鸡和角色关联Service接口
 *
 * @author lxc
 * @date 2022-11-16
 */
public interface ISysSuperRoleService extends IBaseService<SysSuperRole> {

    /**
     * 查询母鸡和角色关联
     */
    SysSuperRoleVo queryById(Long lxcId);

    /**
     * 查询母鸡和角色关联列表
     */
    TableDataInfo<SysSuperRoleVo> queryPageList(SysSuperRoleBo bo,PageQuery pageQuery);

    /**
     * 查询母鸡和角色关联列表
     */
    List<SysSuperRoleVo> queryList(SysSuperRoleBo bo);

    /**
     * 新增母鸡和角色关联
     */
    Boolean insertByBo(SysSuperRoleBo bo);

    /**
     * 修改母鸡和角色关联
     */
    Boolean updateByBo(SysSuperRoleBo bo);

    /**
     * 校验并批量删除母鸡和角色关联信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids,Boolean isValid);

    /**
     * 关联lxc角色
     *
     * @param superId 用户ID
     * @param roleIds 角色组
     */
    void insertLxcAuth(Long superId,Long[] roleIds);
}
