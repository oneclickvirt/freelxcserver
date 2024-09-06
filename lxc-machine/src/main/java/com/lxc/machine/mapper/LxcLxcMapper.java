package com.lxc.machine.mapper;

import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.vo.LxcLxcVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

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

    /**
     * 根据tgId和小鸡名称查询小鸡信息
     * @param tgId
     * @param childName
     * @return
     */
    @Select("select * from lxc_lxc_and_traffic lxc left join lxc_queue queue on lxc.id = queue.lxc_id where queue.tg_id=#{tgId} and lxc.child_name=#{childName}")
    LxcLxcVo selectVoLxc(@Param("tgId") String tgId,@Param("childName") String childName);

    @Select("select * from lxc_lxc_and_traffic lxc left join lxc_queue queue on lxc.id = queue.lxc_id where lxc_status=#{lxcStatus}")
    List<LxcLxcVo> selectVoLxcByStatus(@Param("lxcStatus") String lxcStatus);

    /**
     * 根据status查找小鸡列表
     * @param lxcStatus
     * @return
     */
    @Select("select * from lxc_lxc_and_traffic lxc where lxc_status=#{lxcStatus}")
    List<LxcLxc> selectLxcListByStatus(@Param("lxcStatus") String lxcStatus);

    /**
     * 更新小鸡状态
     * @param lxcStatus
     */
    @Update("update lxc_lxc_and_traffic set lxc_status=#{lxcStatus} where id=#{id}")
    void updateStatus(@Param("lxcStatus") String lxcStatus,@Param("id") Long id);

    /**
     *
     * @param id
     * @param lxcTraffic
     */
    @Update("update lxc_lxc_and_traffic set lxc_traffic=#{lxcTraffic} where id=#{id}")
    void updateTraffic(Long id, BigDecimal lxcTraffic);
}
