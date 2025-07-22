package com.lxc.machine.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.R;
import com.lxc.common.core.domain.model.LoginUser;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.framework.service.IBaseService;
import com.lxc.machine.domain.LxcQueue;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.bo.LxcApplyBo;
import com.lxc.machine.domain.bo.LxcQueueBo;
import com.lxc.machine.domain.vo.LxcQueueVo;
import org.relxd.lxd.model.BackgroundOperationResponse;

import java.util.Collection;
import java.util.List;

/**
 * 小鸡和用户关联表Service接口
 *
 * @author lxc
 * @date 2022-10-11
 */
public interface ILxcQueueService extends IBaseService<LxcQueue> {

    /**
     * 查询小鸡和用户关联表
     */
    LxcQueueVo queryById(Long id);

    /**
     * 查询小鸡和用户关联表列表
     */
    TableDataInfo<LxcQueueVo> queryPageList(LxcQueueBo bo,PageQuery pageQuery);

    /**
     * 查询小鸡和用户关联表列表
     */
    List<LxcQueueVo> queryList(LxcQueueBo bo);

    /**
     * 新增小鸡和用户关联表
     */
    Boolean insertByBo(LxcQueueBo bo);

    /**
     * 修改小鸡和用户关联表
     */
    Boolean updateByBo(LxcQueueBo bo);

    /**
     * 校验并批量删除小鸡和用户关联表信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids,Boolean isValid);

    R<Object> addUserLxc(LxcQueueBo queueDto,LoginUser loginUser);

//    R<Object> applyLxc(LxcSuperLxc superLxc, LoginUser loginUser, LxcApplyBo lxcApplyBo);

    R<Object> applyLxc(String tgId, String date, LxcSuperLxc superLxc, LoginUser loginUser, LxcApplyBo lxcApplyBo, Boolean isReset);

    /**
     * 查询用户全部小鸡
     */
    List<LxcQueueVo> queryAllLxc(String tgId,String nowDateTime);

    /**
     * 删除和小鸡关联信息
     *
     * @param queryWrapper
     * @return
     */
    int delForce(QueryWrapper<LxcQueue> queryWrapper);

    /**
     * 通过LxcId删除小鸡关联信息
     *
     * @param lxcId
     * @return
     */
    int delForceByLxcId(Long lxcId);


    /**
     * 查询所有的实例
     *
     * @param ip
     * @return
     */
    BackgroundOperationResponse getAllInstancesBase(String ip);


    /**
     * 根据lxcId判断是否是用户的小鸡
     *
     * @param lxcId
     * @param tgId
     */
    boolean isUserLxc(Long lxcId, String tgId);
}
