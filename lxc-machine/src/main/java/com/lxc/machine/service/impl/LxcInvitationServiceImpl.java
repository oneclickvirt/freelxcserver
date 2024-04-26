package com.lxc.machine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.utils.InvitationCodeGenerator;
import com.lxc.common.utils.StringUtils;
import com.lxc.machine.domain.LxcInvitation;
import com.lxc.machine.domain.bo.LxcInvitationBo;
import com.lxc.machine.domain.vo.LxcInvitationVo;
import com.lxc.machine.mapper.LxcInvitationMapper;
import com.lxc.machine.service.ILxcInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 邀请码Service业务层处理
 *
 * @author lxc
 * @date 2023-10-16
 */
@RequiredArgsConstructor
@Service
public class LxcInvitationServiceImpl implements ILxcInvitationService {

    private final LxcInvitationMapper baseMapper;

    /**
     * 查询邀请码
     */
    @Override
    public LxcInvitationVo queryById(String tgId) {
        return baseMapper.selectVoById(tgId);
    }

    /**
     * 查询邀请码列表
     */
    @Override
    public TableDataInfo<LxcInvitationVo> queryPageList(LxcInvitationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LxcInvitation> lqw = buildQueryWrapper(bo);
        String used = bo.getUsed();
        if (StringUtils.isNotBlank(used)) {
            if (StringUtils.equals(used, "Y")) {
                lqw.isNotNull(LxcInvitation::getTgId);
            } else {
                lqw.isNull(LxcInvitation::getTgId);
            }
        }
        // tgId为空时的排在前面
        lqw.orderByAsc(LxcInvitation::getTgId);
        Page<LxcInvitationVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询邀请码列表
     */
    @Override
    public List<LxcInvitationVo> queryList(LxcInvitationBo bo) {
        LambdaQueryWrapper<LxcInvitation> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<LxcInvitation> buildQueryWrapper(LxcInvitationBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<LxcInvitation> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTgId()), LxcInvitation::getTgId, bo.getTgId());
        lqw.eq(StringUtils.isNotBlank(bo.getInvitationCode()), LxcInvitation::getInvitationCode, bo.getInvitationCode());
        return lqw;
    }

    /**
     * 新增邀请码
     */
    @Override
    public Boolean insertByBo(LxcInvitationBo bo) {
        Integer count = bo.getCount();
        for (int i = 0; i < count; i++) {
            LxcInvitation add = new LxcInvitation();
            // 生成邀请码
            add.setInvitationCode(InvitationCodeGenerator.generateInvitationCode(16));
            validEntityBeforeSave(add);
            baseMapper.insert(add);
        }
        return true;
    }

    /**
     * 修改邀请码
     */
    @Override
    public Boolean updateByBo(LxcInvitationBo bo) {
        LxcInvitation update = BeanUtil.toBean(bo, LxcInvitation.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(LxcInvitation entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除邀请码
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * @param invitationCode
     * @return
     */
    @Override
    public Boolean checkInvitationCode(String invitationCode) {

        LambdaQueryWrapper<LxcInvitation> lqw = Wrappers.lambdaQuery();
        lqw.eq(LxcInvitation::getInvitationCode, invitationCode);
        lqw.isNull(LxcInvitation::getTgId);
        return baseMapper.selectCount(lqw) > 0;
    }

    /**
     * 关联邀请码
     *
     * @param invitationCode
     * @param tgId
     * @return
     */
    @Override
    public int relateInvitationCode(String invitationCode, String tgId) {
        LambdaQueryWrapper<LxcInvitation> lqw = Wrappers.lambdaQuery();
        lqw.eq(LxcInvitation::getInvitationCode, invitationCode);
        LxcInvitation lxcInvitation = baseMapper.selectOne(lqw);
        lxcInvitation.setTgId(tgId);
        return baseMapper.updateById(lxcInvitation);
    }
}
