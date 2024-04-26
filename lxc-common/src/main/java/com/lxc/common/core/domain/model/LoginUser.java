package com.lxc.common.core.domain.model;

import com.lxc.common.core.domain.dto.RoleDTO;
import com.lxc.common.core.domain.entity.LxcUser;
import com.lxc.common.helper.LoginHelper;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 登录用户身份权限
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * isAdmin
     */
    private Boolean isAdmin;

    /**
     * tgId
     */
    private String tgId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名
     */
    private String deptName;

    /**
     * 用户唯一标识
     */
    private String token;

    /**
     * 用户类型
     */
    private String userType;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 过期时间
     */
    private Long expireTime;

    /**
     * 登录IP地址
     */
    private String ipaddr;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 菜单权限
     */
    private Set<String> menuPermission;

    /**
     * 角色权限
     */
    private Set<String> rolePermission;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色对象
     */
    private List<RoleDTO> roles;

    /**
     * 数据权限 当前角色ID
     */
    private Long roleId;

    /**
     * 原表用户信息
     */
    private LxcUser lxcUser;

    /**
     * 获取登录id
     */
    public String getLoginId() {
        if (userType == null) {
            throw new IllegalArgumentException("用户类型不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return userType + LoginHelper.JOIN_CODE + userId;
    }

    /**
     * 获取最大的配额
     */
    public Integer getMaxQuota() {
        if (roles == null || roles.isEmpty()) {
            return 0;
        }
        Integer maxQuota = 0;
        for (RoleDTO role : roles) {
            if (role.getRoleQuota() > maxQuota) {
                maxQuota = role.getRoleQuota();
            }
        }
        return maxQuota;
    }

    /**
     * 是否有角色
     */
    public boolean hasRole(Long roleKey) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (RoleDTO role : roles) {
            if (role.getRoleId().equals(roleKey)) {
                return true;
            }
        }
        return false;
    }

}
