package com.lxc.machine.mapper;

import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

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
}
