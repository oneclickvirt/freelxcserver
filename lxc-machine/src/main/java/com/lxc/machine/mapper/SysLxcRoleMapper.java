package com.lxc.machine.mapper;

import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.machine.domain.SysLxcRole;
import com.lxc.machine.domain.vo.SysLxcRoleVo;
import org.apache.ibatis.annotations.Param;

/**
 * 小鸡和角色关联Mapper接口
 *
 * @author lxc
 * @date 2022-11-16
 */
public interface SysLxcRoleMapper extends BaseMapperPlus<SysLxcRoleMapper,SysLxcRole,SysLxcRoleVo> {

    /**
     * 根据lxcId删除
     *
     * @param lxcId
     * @return
     */
    void deleteByLxcId(@Param("lxcId") Long lxcId);

    /**
     * 根据superIp删除
     *
     * @param superIp
     * @return
     */
    void deleteBySuperIp(@Param("superIp") String superIp);
}
