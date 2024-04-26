package com.lxc.demo.mapper;

import com.lxc.common.annotation.DataColumn;
import com.lxc.common.annotation.DataPermission;
import com.lxc.common.core.mapper.BaseMapperPlus;
import com.lxc.demo.domain.TestTree;
import com.lxc.demo.domain.vo.TestTreeVo;

/**
 * 测试树表Mapper接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public interface TestTreeMapper extends BaseMapperPlus<TestTreeMapper, TestTree, TestTreeVo> {

}
