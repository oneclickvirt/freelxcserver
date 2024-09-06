package com.lxc.machine.mapper;

import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 母鸡信息Mapper接口
 *
 * @author lxc
 * @date 2022-10-11
 */
public interface LxcSuperLxcMapper extends BaseMapperPlus<LxcSuperLxcMapper,LxcSuperLxc,LxcSuperLxcVo> {

    /**
     * 根据角色查询小鸡信息列表
     */
    List<LxcSuperLxc> getSuperListWithRoles(@Param("sysRoles") List<RoleDTO> sysRoles);

    /**
     * 根据母鸡ip查找密码
     * @return
     */
    @Select("select * from lxc_and_incus_super_lxc where ip = #{superIp}")
    LxcSuperLxcVo queryByIp(String superIp);
}
