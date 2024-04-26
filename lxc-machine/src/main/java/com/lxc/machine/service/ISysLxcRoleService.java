package com.lxc.machine.service;

import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.machine.domain.bo.SysLxcRoleBo;
import com.lxc.machine.domain.vo.SysLxcRoleVo;
import java.util.Collection;
import java.util.List;

/**
 * 小鸡和角色关联Service接口
 *
 * @author lxc
 * @date 2022-11-16
 */
public interface ISysLxcRoleService {

    /**
     * 查询小鸡和角色关联
     */
    SysLxcRoleVo queryById(Long lxcId);

    /**
     * 查询小鸡和角色关联列表
     */
    TableDataInfo<SysLxcRoleVo> queryPageList(SysLxcRoleBo bo,PageQuery pageQuery);

    /**
     * 查询小鸡和角色关联列表
     */
    List<SysLxcRoleVo> queryList(SysLxcRoleBo bo);

    /**
     * 新增小鸡和角色关联
     */
    Boolean insertByBo(SysLxcRoleBo bo);

    /**
     * 修改小鸡和角色关联
     */
    Boolean updateByBo(SysLxcRoleBo bo);

    /**
     * 校验并批量删除小鸡和角色关联信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids,Boolean isValid);

    /**
     * 关联lxc角色
     *
     * @param lxcId   用户ID
     * @param roleIds 角色组
     */
    void insertLxcAuth(Long lxcId,Long[] roleIds);


    /**
     * 根据lxcId删除
     *
     * @param lxcId
     * @return
     */
    void deleteByLxcId(Long lxcId);

    /**
     * 根据superIp删除
     *
     * @param superIp
     * @return
     */
    void deleteBySuperIp(String superIp);
}
