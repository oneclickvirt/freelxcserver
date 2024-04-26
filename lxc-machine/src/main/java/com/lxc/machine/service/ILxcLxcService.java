package com.lxc.machine.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jcraft.jsch.Session;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.framework.service.IBaseService;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.bo.LxcLxcBo;
import com.lxc.machine.domain.vo.LxcLxcVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 小鸡信息Service接口
 *
 * @author lxc
 * @date 2022-10-11
 */
public interface ILxcLxcService extends IBaseService<LxcLxc> {

    /**
     * 查询小鸡信息
     */
    LxcLxcVo queryById(Long id);

    /**
     * 查询小鸡信息列表
     */
    TableDataInfo<LxcLxcVo> queryPageList(LxcLxcBo bo, PageQuery pageQuery);

    /**
     * 查询小鸡信息列表
     */
    List<LxcLxcVo> queryList(LxcLxcBo bo);

    /**
     * 查询小鸡信息列表
     */
    List<LxcLxcVo> getLxcList(QueryWrapper<LxcLxc> queryWrapper);

    /**
     * 根据角色查询小鸡信息列表
     */
    List<LxcLxcVo> getLxcListWithRoles(List<RoleDTO> sysRoles);

    /**
     * 新增小鸡信息
     */
    Boolean insertByBo(LxcLxcBo bo);

    /**
     * 修改小鸡信息
     */
    Boolean updateByBo(LxcLxcBo bo);

    /**
     * 修改小鸡信息
     */
    Boolean updateByBoForce(LxcLxcBo bo);


    /**
     * 校验并批量删除小鸡信息信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验并批量删除小鸡信息信息
     */
    Map<String, Object> webIndexStatisticsLxc();

    /**
     * 根据状态查询小鸡所有统计
     */
    List<Map<String, Object>> getLxcStatisticsListByState(int state);

    /**
     * 统计已使用数量
     */
    int countUsedByTgId(String tgId);

    /**
     * 根据lxcId获取session
     */
    <T> T getSessionByLxcId(Long lxcId, BiFunction<Session, LxcLxc, T> mapper);
}
