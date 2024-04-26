package com.lxc.machine.mapper;

import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.vo.LxcLxcVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 小鸡信息Mapper接口
 *
 * @author lxc
 * @date 2022-10-11
 */
public interface LxcLxcMapper extends BaseMapperPlus<LxcLxcMapper,LxcLxc,LxcLxcVo> {

    /**
     * 根据角色查询小鸡信息列表
     */
    List<LxcLxcVo> getLxcListWithRoles(@Param("sysRoles") List<RoleDTO> sysRoles);

    /**
     * 统计已使用数量
     */
    int countUsedByTgId(@Param("tgId") String tgId);
}
