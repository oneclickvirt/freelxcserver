package com.lxc.machine.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.lxc.common.annotation.Log;
import com.lxc.common.annotation.RepeatSubmit;
import com.lxc.common.core.controller.BaseController;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.R;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import com.lxc.common.enums.BusinessType;
import com.lxc.common.utils.poi.ExcelUtil;
import com.lxc.machine.domain.bo.LxcInvitationBo;
import com.lxc.machine.domain.vo.LxcInvitationVo;
import com.lxc.machine.service.ILxcInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;


/**
 * 邀请码
 *
 * @author lxc
 * @date 2023-10-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/lxc/invitation")
public class LxcInvitationController extends BaseController {

    private final ILxcInvitationService iLxcInvitationService;

    /**
     * 查询邀请码列表
     */
    @SaCheckPermission("system:invitation:list")
    @GetMapping("/list")
    public TableDataInfo<LxcInvitationVo> list(LxcInvitationBo bo, PageQuery pageQuery) {
        return iLxcInvitationService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出邀请码列表
     */
    @SaCheckPermission("system:invitation:export")
    @Log(title = "邀请码", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LxcInvitationBo bo, HttpServletResponse response) {
        List<LxcInvitationVo> list = iLxcInvitationService.queryList(bo);
        ExcelUtil.exportExcel(list, "邀请码", LxcInvitationVo.class, response);
    }

    /**
     * 获取邀请码详细信息
     *
     * @param tgId 主键
     */
    @SaCheckPermission("system:invitation:query")
    @GetMapping("/{tgId}")
    public R<LxcInvitationVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable String tgId) {
        return R.ok(iLxcInvitationService.queryById(tgId));
    }

    /**
     * 新增邀请码
     */
    @SaCheckPermission("system:invitation:add")
    @Log(title = "邀请码", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LxcInvitationBo bo) {
        return toAjax(iLxcInvitationService.insertByBo(bo) ? 1 : 0);
    }

    /**
     * 修改邀请码
     */
    @SaCheckPermission("system:invitation:edit")
    @Log(title = "邀请码", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LxcInvitationBo bo) {
        return toAjax(iLxcInvitationService.updateByBo(bo) ? 1 : 0);
    }

    /**
     * 删除邀请码
     *
     * @param invitationCodes 主键串
     */
    @SaCheckPermission("system:invitation:remove")
    @Log(title = "邀请码", businessType = BusinessType.DELETE)
    @DeleteMapping("/{invitationCodes}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable String[] invitationCodes) {
        return toAjax(iLxcInvitationService.deleteWithValidByIds(Arrays.asList(invitationCodes), true) ? 1 : 0);
    }
}
