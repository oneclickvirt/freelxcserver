package com.lxc.machine.mapper;

import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.machine.domain.LxcQueue;
import com.lxc.machine.domain.vo.LxcQueueVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 小鸡和用户关联表Mapper接口
 *
 * @author lxc
 * @date 2022-10-11
 */
public interface LxcQueueMapper extends BaseMapperPlus<LxcQueueMapper, LxcQueue, LxcQueueVo> {
    List<LxcQueueVo> queryAllLxc(@Param("tgId") String tgId,@Param("nowDateTime") String nowDateTime);

    /**
     * 根据小鸡lxcId查找指定小鸡队列
     * @param id
     * @return
     */
    @Select("select * from lxc_queue queue where lxc_id=#{id}")
    LxcQueue selectQueueByLxcId(Long id);
}
