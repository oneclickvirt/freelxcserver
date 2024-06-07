package com.lxc.machine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.utils.StringUtils;
import com.lxc.framework.service.BaseService;
import com.lxc.machine.domain.LxcTraffic;
import com.lxc.machine.domain.bo.LxcTrafficBo;
import com.lxc.machine.domain.vo.LxcTrafficVo;
import com.lxc.machine.mapper.LxcTrafficMapper;
import com.lxc.machine.service.ILxcTrafficService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 小鸡流量统计Service业务层处理
 *
 * @author lxc
 * @date 2022-11-28
 */
@RequiredArgsConstructor
@Service
public class LxcTrafficServiceImpl extends BaseService<LxcTrafficMapper,LxcTraffic> implements ILxcTrafficService {

    private final LxcTrafficMapper baseMapper;

    /**
     * 查询小鸡流量统计
     */
    @Override
    public LxcTrafficVo queryById(Long lxcId) {
        return baseMapper.selectVoById(lxcId);
    }

    /**
     * 查询小鸡流量统计列表
     */
    @Override
    public TableDataInfo<LxcTrafficVo> queryPageList(LxcTrafficBo bo,PageQuery pageQuery) {
        LambdaQueryWrapper<LxcTraffic> lqw = buildQueryWrapper(bo);
        Page<LxcTrafficVo> result = baseMapper.selectVoPage(pageQuery.build(),lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询小鸡流量统计列表
     */
    @Override
    public List<LxcTrafficVo> queryList(LxcTrafficBo bo) {
        LambdaQueryWrapper<LxcTraffic> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<LxcTraffic> buildQueryWrapper(LxcTrafficBo bo) {
        Map<String,Object> params = bo.getParams();
        LambdaQueryWrapper<LxcTraffic> lqw = Wrappers.lambdaQuery();
        //非空判断+字段eq对象值
        lqw.eq(StringUtils.isNotBlank(bo.getReceived()),LxcTraffic::getReceived,bo.getReceived());
        lqw.eq(StringUtils.isNotBlank(bo.getSent()),LxcTraffic::getSent,bo.getSent());
        return lqw;
    }

    /**
     * 新增小鸡流量统计
     */
    @Override
    public Boolean insertByBo(LxcTrafficBo bo) {
        LxcTraffic add = BeanUtil.toBean(bo,LxcTraffic.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {

            bo.setLxcId(add.getLxcId());
        }
        return flag;
    }

    /**
     * 修改小鸡流量统计
     */
    @Override
    public Boolean updateByBo(LxcTrafficBo bo) {
        LxcTraffic update = BeanUtil.toBean(bo,LxcTraffic.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(LxcTraffic entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除小鸡流量统计
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids,Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
