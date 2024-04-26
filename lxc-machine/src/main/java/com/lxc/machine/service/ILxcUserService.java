package com.lxc.machine.service;

import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.entity.LxcUser;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.framework.service.IBaseService;
import com.lxc.machine.domain.bo.LxcUserBo;
import com.lxc.machine.domain.vo.LxcUserVo;
import java.util.Collection;
import java.util.List;

/**
 * lxc用户信息Service接口
 *
 * @author lxc
 * @date 2022-10-11
 */
public interface ILxcUserService extends IBaseService<LxcUser> {

    /**
     * 查询lxc用户信息
     */
    LxcUserVo queryById(Long id);


    /**
     * 查询lxc用户信息
     */
    LxcUser queryByTgId(String tgId);

    /**
     * 查询lxc用户信息列表
     */
    TableDataInfo<LxcUserVo> queryPageList(LxcUserBo bo, PageQuery pageQuery);

    /**
     * 查询lxc用户信息列表
     */
    List<LxcUserVo> queryList(LxcUserBo bo);

    /**
     * 新增lxc用户信息
     */
    Boolean insertByBo(LxcUserBo bo);

    /**
     * 修改lxc用户信息
     */
    Boolean updateByBo(LxcUserBo bo);

    /**
     * 校验并批量删除lxc用户信息信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 注册用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    boolean registerLxcUser(LxcUser user);

}
