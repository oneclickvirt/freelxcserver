package com.lxc.machine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.entity.LxcUser;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.utils.StringUtils;
import com.lxc.framework.service.BaseService;
import com.lxc.machine.domain.bo.LxcUserBo;
import com.lxc.machine.domain.vo.LxcUserVo;
import com.lxc.machine.mapper.LxcUserMapper;
import com.lxc.machine.service.ILxcUserService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * lxc用户信息Service业务层处理
 *
 * @author lxc
 * @date 2022-10-11
 */
@RequiredArgsConstructor
@Service
public class LxcUserServiceImpl extends BaseService<LxcUserMapper,LxcUser> implements ILxcUserService {

    private final LxcUserMapper baseMapper;

    /**
     * 查询lxc用户信息
     */
    @Override
    public LxcUserVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询小鸡信息
     */
    @Override
    public LxcUser queryByTgId(String tgId){
        QueryWrapper<LxcUser> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("tg_id", tgId);
        return baseMapper.selectOne(userQueryWrapper);
    }


    /**
     * 查询lxc用户信息列表
     */
    @Override
    public TableDataInfo<LxcUserVo> queryPageList(LxcUserBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LxcUser> lqw = buildQueryWrapper(bo);
        Page<LxcUserVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询lxc用户信息列表
     */
    @Override
    public List<LxcUserVo> queryList(LxcUserBo bo) {
        LambdaQueryWrapper<LxcUser> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<LxcUser> buildQueryWrapper(LxcUserBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LxcUser> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTgId()), LxcUser::getTgId, bo.getTgId());
        lqw.eq(StringUtils.isNotBlank(bo.getPassword()), LxcUser::getPassword, bo.getPassword());
        lqw.like(StringUtils.isNotBlank(bo.getName()), LxcUser::getName, bo.getName());
        lqw.eq(bo.getLxcQuota() != null, LxcUser::getLxcQuota, bo.getLxcQuota());
        lqw.eq(bo.getVipType() != null, LxcUser::getVipType, bo.getVipType());
        return lqw;
    }

    /**
     * 新增lxc用户信息
     */
    @Override
    public Boolean insertByBo(LxcUserBo bo) {
        LxcUser add = BeanUtil.toBean(bo, LxcUser.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改lxc用户信息
     */
    @Override
    public Boolean updateByBo(LxcUserBo bo) {
        LxcUser update = BeanUtil.toBean(bo, LxcUser.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(LxcUser entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除lxc用户信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerLxcUser(LxcUser user) {
        return baseMapper.insert(user) > 0;
    }
}
