package com.lxc.web.controller.machine;

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
import com.lxc.machine.domain.bo.LxcTrafficBo;
import com.lxc.machine.domain.vo.LxcTrafficVo;
import com.lxc.machine.service.ILxcTrafficService;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小鸡流量统计
 *
 * @author lxc
 * @date 2022-11-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/traffic")
public class LxcTrafficController extends BaseController {

    private final ILxcTrafficService iLxcTrafficService;

    /**
     * 查询小鸡流量统计列表
     */
    @SaCheckPermission("system:traffic:list")
    @GetMapping("/list")
    public TableDataInfo<LxcTrafficVo> list(LxcTrafficBo bo,PageQuery pageQuery) {
        return iLxcTrafficService.queryPageList(bo,pageQuery);
    }

    /**
     * 导出小鸡流量统计列表
     */
    @SaCheckPermission("system:traffic:export")
    @Log(title = "小鸡流量统计", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LxcTrafficBo bo,HttpServletResponse response) {
        List<LxcTrafficVo> list = iLxcTrafficService.queryList(bo);
        ExcelUtil.exportExcel(list,"小鸡流量统计",LxcTrafficVo.class,response);
    }

    /**
     * 获取小鸡流量统计详细信息
     *
     * @param lxcId 主键
     */
    @SaCheckPermission("system:traffic:query")
    @GetMapping("/{lxcId}")
    public R<LxcTrafficVo> getInfo(
        @NotNull(message = "主键不能为空")
        @PathVariable Long lxcId
    ) {
        return R.ok(iLxcTrafficService.queryById(lxcId));
    }

    /**
     * 新增小鸡流量统计
     */
    @SaCheckPermission("system:traffic:add")
    @Log(title = "小鸡流量统计", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LxcTrafficBo bo) {
        return toAjax(iLxcTrafficService.insertByBo(bo) ? 1 : 0);
    }

    /**
     * 修改小鸡流量统计
     */
    @SaCheckPermission("system:traffic:edit")
    @Log(title = "小鸡流量统计", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LxcTrafficBo bo) {
        return toAjax(iLxcTrafficService.updateByBo(bo) ? 1 : 0);
    }

    /**
     * 删除小鸡流量统计
     *
     * @param lxcIds 主键串
     */
    @SaCheckPermission("system:traffic:remove")
    @Log(title = "小鸡流量统计", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lxcIds}")
    public R<Void> remove(
        @NotEmpty(message = "主键不能为空")
        @PathVariable Long[] lxcIds
    ) {
        return toAjax(iLxcTrafficService.deleteWithValidByIds(Arrays.asList(lxcIds),true) ? 1 : 0);
    }
}
