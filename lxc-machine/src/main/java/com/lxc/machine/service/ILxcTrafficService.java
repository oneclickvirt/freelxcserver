package com.lxc.machine.service;

import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.machine.domain.bo.LxcTrafficBo;
import com.lxc.machine.domain.vo.LxcTrafficVo;
import java.util.Collection;
import java.util.List;

/**
 * 小鸡流量统计Service接口
 *
 * @author lxc
 * @date 2022-11-28
 */
public interface ILxcTrafficService {

    /**
     * 查询小鸡流量统计
     */
    LxcTrafficVo queryById(Long lxcId);

    /**
     * 查询小鸡流量统计列表
     */
    TableDataInfo<LxcTrafficVo> queryPageList(LxcTrafficBo bo,PageQuery pageQuery);

    /**
     * 查询小鸡流量统计列表
     */
    List<LxcTrafficVo> queryList(LxcTrafficBo bo);

    /**
     * 新增小鸡流量统计
     */
    Boolean insertByBo(LxcTrafficBo bo);

    /**
     * 修改小鸡流量统计
     */
    Boolean updateByBo(LxcTrafficBo bo);

    /**
     * 校验并批量删除小鸡流量统计信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids,Boolean isValid);
}
