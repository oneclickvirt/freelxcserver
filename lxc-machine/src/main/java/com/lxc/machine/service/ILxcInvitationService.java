package com.lxc.machine.service;


import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.machine.domain.bo.LxcInvitationBo;
import com.lxc.machine.domain.vo.LxcInvitationVo;

import java.util.Collection;
import java.util.List;

/**
 * 邀请码Service接口
 *
 * @author lxc
 * @date 2023-10-16
 */
public interface ILxcInvitationService {

    /**
     * 查询邀请码
     */
    LxcInvitationVo queryById(String tgId);

    /**
     * 查询邀请码列表
     */
    TableDataInfo<LxcInvitationVo> queryPageList(LxcInvitationBo bo, PageQuery pageQuery);

    /**
     * 查询邀请码列表
     */
    List<LxcInvitationVo> queryList(LxcInvitationBo bo);

    /**
     * 新增邀请码
     */
    Boolean insertByBo(LxcInvitationBo bo);

    /**
     * 修改邀请码
     */
    Boolean updateByBo(LxcInvitationBo bo);

    /**
     * 校验并批量删除邀请码信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);

    /**
     * 校验邀请码是否存在
     *
     * @param invitationCode
     * @return
     */
    Boolean checkInvitationCode(String invitationCode);

    /**
     * 关联邀请码
     *
     * @param invitationCode
     * @param tgId
     * @return
     */
    int relateInvitationCode(String invitationCode, String tgId);
}
