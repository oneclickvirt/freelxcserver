package com.lxc.web.controller.machine;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lxc.common.annotation.Log;
import com.lxc.common.annotation.RepeatSubmit;
import com.lxc.common.constant.CacheNames;
import com.lxc.common.core.controller.BaseController;
import com.lxc.common.core.domain.PageQuery;
import com.lxc.common.core.domain.R;
import com.lxc.common.core.domain.model.LoginUser;
import com.lxc.common.core.page.TableDataInfo;
import com.lxc.common.core.validate.AddGroup;
import com.lxc.common.core.validate.EditGroup;
import com.lxc.common.enums.BusinessType;
import com.lxc.common.utils.StringUtils;
import com.lxc.common.utils.poi.ExcelUtil;
import com.lxc.common.utils.redis.RedisUtils;
import com.lxc.machine.domain.LxcLxc;
import com.lxc.machine.domain.LxcSuperLxc;
import com.lxc.machine.domain.SysSuperRole;
import com.lxc.machine.domain.bo.LxcSuperLxcBo;
import com.lxc.machine.domain.dto.LxcTextDto;
import com.lxc.machine.domain.vo.LxcCartVo;
import com.lxc.machine.domain.vo.LxcSuperLxcVo;
import com.lxc.machine.mapper.LxcLxcMapper;
import com.lxc.machine.mapper.LxcSuperLxcMapper;
import com.lxc.machine.service.ILxcLxcService;
import com.lxc.machine.service.ILxcSuperLxcService;
import com.lxc.machine.service.ISysLxcRoleService;
import com.lxc.machine.service.ISysSuperRoleService;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 母鸡信息
 *
 * @author lxc
 * @date 2022-10-11
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/lxc/superLxc")
public class LxcSuperLxcController extends BaseController {

    private final ILxcSuperLxcService lxcSuperLxcService;

    private final ILxcLxcService lxcLxcService;

    private final LxcSuperLxcMapper superLxcMapper;

    private final LxcLxcMapper lxcMapper;

    private final ISysLxcRoleService sysLxcRoleService;

    private final ISysSuperRoleService sysSuperRoleService;



    /**
     * 查询母鸡信息列表
     */
    @SaCheckPermission("lxc:superLxc:list")
    @GetMapping("/list")
    public TableDataInfo<LxcSuperLxcVo> list(LxcSuperLxcBo bo,PageQuery pageQuery) {
        TableDataInfo<LxcSuperLxcVo> pageList = lxcSuperLxcService.queryPageList(bo,pageQuery);
        pageList.getRows().parallelStream().forEach(superLxcVo -> {
            long free = RedisUtils.getAtomicValue(String.format(CacheNames.LXC_SUPER_APPLY_FREE,superLxcVo.getId()));
            superLxcVo.setFreeQuantity(free < 0 ? 0 : free);
            /*long countStartUp = lxcLxcService.count(new QueryWrapper<LxcLxc>().eq("super_ip",superLxcVo.getIp()));
            superLxcVo.setUsedQuantity(countStartUp);*/
            superLxcVo.setUsedQuantity(RedisUtils.getAtomicValue(String.format(CacheNames.LXC_SUPER_APPLY_USED,superLxcVo.getId())));
        });
        return pageList;
    }

    /**
     * 查询母鸡基本信息
     *
     * @param search
     * @date 2022/11/24
     * @returns List<LxcSuperLxcVo>
     * @version v1.0
     */
    @SaCheckPermission("lxc:lxc:all")
    @GetMapping("/listBaseInfoAll")
    public List<LxcSuperLxcVo> listBaseInfoAll(String search) {
        QueryWrapper<LxcSuperLxc> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","ip","tag","name");
        // likeRight == search%，即%在匹配项右边
        if (StringUtils.isNotEmpty(search)) {
            queryWrapper.likeRight("ip",search).or()
                        .likeRight("name",search).or()
                        .eq("id",search);
        }

        return superLxcMapper.selectVoList(queryWrapper);
    }


    /**
     * 查询可申请的母鸡列表
     *
     * @param
     * @date 2022/11/26
     * @returns List<LxcSuperLxcVo>
     * @version v1.0
     */
    @SaCheckPermission("lxc:lxc:all")
    @GetMapping("/listSuperCart")
    public List<LxcCartVo> listSuperCart() {
        LoginUser loginUser = this.getLoginUser();

        return lxcSuperLxcService.getSuperListWithRoles(loginUser.getRoles())
            .stream()
            .filter(superLxcVo -> {
                    String keyOfFree = String.format(CacheNames.LXC_SUPER_APPLY_FREE, superLxcVo.getId());
                    return RedisUtils.getAtomicValue(keyOfFree) > 0;
                }
            ).collect(Collectors.toList());

    }

    /**
     * 导出母鸡信息列表
     */
    @SaCheckPermission("lxc:superLxc:export")
    @Log(title = "母鸡信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(LxcSuperLxcBo bo,HttpServletResponse response) {
        List<LxcSuperLxcVo> list = lxcSuperLxcService.queryList(bo);
        ExcelUtil.exportExcel(list,"母鸡信息",LxcSuperLxcVo.class,response);
    }

    /**
     * 获取母鸡信息详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("lxc:superLxc:query")
    @GetMapping("/{id}")
    public R<LxcSuperLxcVo> getInfo(
            @NotNull(message = "主键不能为空")
            @PathVariable Long id
    ) {
        LxcSuperLxcVo superLxcVo = lxcSuperLxcService.queryById(id);
        List<SysSuperRole> superRoleList = sysSuperRoleService.list(new QueryWrapper<SysSuperRole>().eq("super_id",id));
        // 转为id数组[]
        superLxcVo.setRoleIds(superRoleList.stream().map(SysSuperRole::getRoleId).toArray(Long[]::new));
        return R.ok(superLxcVo);
    }


    @PostMapping("/addLxcMulti")
    @SaCheckPermission("lxc:superLxc:add")
    @Log(title = "批量添加小鸡", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    public R<String> addLxcMulti(@RequestBody LxcTextDto lxcTextDto) {
        String contents = lxcTextDto.getLxcText();
        RedisUtils.deleteKeys(CacheNames.STATISTICS_LXC_LIST);
        RedisUtils.deleteKeys(CacheNames.FREE_LXC_LIST + "*");
        String pattern = "([a-z]+\\d*) (\\d*) ([\\da-z]*) ?(\\d*) ?(\\d*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(contents);

        String superIp = lxcTextDto.getSuperIp();
        LxcSuperLxc lxcSuperLxc = superLxcMapper.selectOne(new QueryWrapper<LxcSuperLxc>().eq("ip",superIp).last("limit 1"));
        if (lxcSuperLxc == null) {
            return R.fail("母鸡不存在");
        }
        while (m.find()) {
            //信息独立截取
            String[] arr = m.group(0).split(" ");
            List<String> lxcList = Arrays.asList(arr);
            String lxcName = lxcList.get(0);
            try {
                String myLxcName = lxcSuperLxc.getName();
                String lxcDate = lxcSuperLxc.getDate();
                String lxcDead = lxcSuperLxc.getDead();
                String lxcTag = lxcSuperLxc.getTag();
                String lxcIp = lxcSuperLxc.getIp();
                LxcLxc lxc = new LxcLxc()
                        .setChildName(lxcName)
                        .setName(myLxcName)
                        .setTag(lxcTag)
                        .setPassword(lxcList.get(2))
                        .setSuperIp(lxcIp)
                        .setSshPort(lxcList.get(1))
                        .setSuperDate(lxcDate)
                        .setSuperDead(lxcDead);
                if (lxcList.size() > 3) {
                    lxc.setPortStart(lxcList.get(3))
                       .setPortEnd(lxcList.get(4));
                }
                lxcMapper.insertOrUpdate(lxc);
                List<Long> roleIds = lxcTextDto.getRoleIds();
                // list 转数组
                Long[] roleIdsArr = roleIds.toArray(new Long[0]);
                sysLxcRoleService.insertLxcAuth(lxc.getId(),roleIdsArr);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return R.success("添加完毕");

    }


    /**
     * 新增母鸡信息
     */
    @SaCheckPermission("lxc:superLxc:add")
    @Log(title = "母鸡信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody LxcSuperLxcBo bo) {
        List<Long> roleIds = bo.getRoleIds();
        // list 转数组
        Long[] roleIdsArr = roleIds.toArray(new Long[0]);
        Boolean aBoolean = lxcSuperLxcService.insertByBo(bo);
        if (aBoolean) {
            sysSuperRoleService.insertLxcAuth(bo.getId(),roleIdsArr);
        }
        return toAjax(aBoolean ? 1 : 0);
    }

    /**
     * 修改母鸡信息
     */
    @SaCheckPermission("lxc:superLxc:edit")
    @Log(title = "母鸡信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LxcSuperLxcBo bo) {
        Boolean aBoolean = lxcSuperLxcService.updateByBo(bo);
        return toAjax(aBoolean ? 1 : 0);
    }

    /**
     * 删除母鸡信息
     *
     * @param ids 主键串
     */
    @SaCheckPermission("lxc:superLxc:remove")
    @Log(title = "母鸡信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(
            @NotEmpty(message = "主键不能为空")
            @PathVariable Long[] ids
    ) {
        return toAjax(lxcSuperLxcService.deleteWithValidByIds(Arrays.asList(ids),true) ? 1 : 0);
    }

    /**
     * 状态修改
     */
    @SaCheckPermission("lxc:superLxc:edit")
    @Log(title = "母鸡信息", businessType = BusinessType.UPDATE)
    @PutMapping("/changeSuperStatus")
    public R<Void> changeSuperStatus(@RequestBody LxcSuperLxc lxcSuperLxc) {
        return toAjax(lxcSuperLxcService.updateById(lxcSuperLxc));
    }
}
