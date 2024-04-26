package com.lxc.web.controller.machine;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.lxc.common.annotation.Log;
import com.lxc.common.annotation.RepeatSubmit;
import com.lxc.common.constant.Constants;
import com.lxc.common.core.controller.BaseController;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.R;
import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.domain.entity.LxcUser;
import com.lxc.common.core.domain.model.LoginBody;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import com.lxc.common.enums.BusinessType;
import com.lxc.common.utils.poi.ExcelUtil;
import com.lxc.machine.domain.bo.LxcUserBo;
import com.lxc.machine.domain.vo.LxcUserVo;
import com.lxc.machine.mapper.LxcQueueMapper;
import com.lxc.machine.mapper.LxcUserMapper;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcUserService;
import com.lxc.system.service.ISysMenuService;
import com.lxc.system.service.ISysUserService;
import com.lxc.system.service.SysLoginService;
import com.lxc.system.service.SysPermissionService;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * lxc用户信息
 *
 * @author lxc
 * @date 2022-10-11
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/lxc/user")
@Slf4j
public class LxcUserController extends BaseController {

    private final ILxcUserService iLxcUserService;
    private final ILxcLxcService lxcLxcService;
    private final LxcUserMapper lxcUserMapper;
    private final LxcQueueMapper lxcQueueMapper;

    private final SysLoginService loginService;
    private final ISysMenuService menuService;
    private final ISysUserService userService;
    private final SysPermissionService permissionService;


    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @SaIgnore
    @PostMapping("/login")
    public R<Map<String,Object>> login(@Validated @RequestBody LoginBody loginBody) {
        Map<String,Object> ajax = new HashMap<>();
        // 生成令牌
        String token = loginService.lxcLogin(loginBody.getUsername(),loginBody.getPassword(),loginBody.getCode(),
                                             loginBody.getUuid());
        ajax.put(Constants.TOKEN,token);
        return R.ok(ajax);
    }


    @SaCheckPermission("lxc:lxc:all")
    @RequestMapping("/nav")
    public R navData() {
        LxcUser lxcUser = this.getLoginUser().getLxcUser();
        int used = lxcLxcService.countUsedByTgId(lxcUser.getTgId());
        // 筛选配额最大的数量
        this.getLoginUser()
            .getRoles()
            .stream()
            .max(Comparator.comparingInt(RoleDTO::getRoleQuota))
            .ifPresent(role -> lxcUser.setRoleName(role.getRoleName()));
        lxcUser.setUsingQuota(used);
        return R.ok(lxcUser);
    }


    /**
     * 查询lxc用户信息列表
     */
    @SaCheckPermission("lxc:user:list")
    @GetMapping("/list")
    public TableDataInfo<LxcUserVo> list(LxcUserBo bo,PageQuery pageQuery) {
        return iLxcUserService.queryPageList(bo,pageQuery);
    }

    /**
     * 导出lxc用户信息列表
     */
    @SaCheckPermission("lxc:user:export")
    @Log(title = "lxc用户信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LxcUserBo bo,HttpServletResponse response) {
        List<LxcUserVo> list = iLxcUserService.queryList(bo);
        ExcelUtil.exportExcel(list,"lxc用户信息",LxcUserVo.class,response);
    }

    /**
     * 获取lxc用户信息详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("lxc:user:query")
    @GetMapping("/{id}")
    public R<LxcUserVo> getInfo(
        @NotNull(message = "主键不能为空")
        @PathVariable Long id
    ) {
        return R.ok(iLxcUserService.queryById(id));
    }

    /**
     * 新增lxc用户信息
     */
    @SaCheckPermission("lxc:user:add")
    @Log(title = "lxc用户信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LxcUserBo bo) {
        return toAjax(iLxcUserService.insertByBo(bo) ? 1 : 0);
    }

    /**
     * 修改lxc用户信息
     */

    @SaCheckPermission("lxc:user:edit")
    @Log(title = "lxc用户信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LxcUserBo bo) {
        return toAjax(iLxcUserService.updateByBo(bo) ? 1 : 0);
    }

    /**
     * 删除lxc用户信息
     *
     * @param ids 主键串
     */
    @SaCheckPermission("lxc:user:remove")
    @Log(title = "lxc用户信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(
        @NotEmpty(message = "主键不能为空")
        @PathVariable Long[] ids
    ) {
        return toAjax(iLxcUserService.deleteWithValidByIds(Arrays.asList(ids),true) ? 1 : 0);
    }
}
