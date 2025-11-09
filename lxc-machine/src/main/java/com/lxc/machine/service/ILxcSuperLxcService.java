package com.lxc.machine.service;

import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.framework.service.IBaseService;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcSuperLxcBo;
import com.lxc.machine.domain.vo.LxcCartVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 母鸡信息Service接口
 *
 * @author lxc
 * @date 2022-10-11
 */
public interface ILxcSuperLxcService extends IBaseService<LxcSuperLxc> {

    /**
     * 查询母鸡信息
     */
    LxcSuperLxcVo queryById(Long id);

    /**
     * 查询母鸡信息列表
     */
    TableDataInfo<LxcSuperLxcVo> queryPageList(LxcSuperLxcBo bo,PageQuery pageQuery);

    /**
     * 查询母鸡信息列表
     */
    List<LxcSuperLxcVo> queryList(LxcSuperLxcBo bo);

    /**
     * 新增母鸡信息
     */
    Integer insertByBo(LxcSuperLxcBo bo);

    /**
     * 修改母鸡信息
     */
    Boolean updateByBo(LxcSuperLxcBo bo);

    /**
     * 校验并批量删除母鸡信息信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids,Boolean isValid);

    /**
     * 缓存母鸡可开小鸡数量
     */
    void loadingStockCache();

    /**
     * 获取可用状态的母鸡列表
     */
    List<LxcSuperLxc> getAliveLxcSuperLxcList();

    /**
     * 设置母鸡可开小鸡缓存
     */
    void setFreshCache(LxcSuperLxc lxcSuperLxc);

    /**
     * 根据角色查询小鸡信息列表
     */
    List<LxcCartVo> getSuperListWithRoles(@Param("sysRoles") List<RoleDTO> sysRoles);

    /**
     * 根据母鸡ip查询密码
     * @param superIp
     * @return
     */
    LxcSuperLxcVo queryByIp(String superIp);
}
