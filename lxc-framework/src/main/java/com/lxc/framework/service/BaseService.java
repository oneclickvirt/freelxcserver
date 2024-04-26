package com.lxc.framework.service;

import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public class BaseService<M extends BaseMapper<T>,T> extends ServiceImpl<M, T>  {


    @Autowired
    protected M myMapper;

    /**
     * 对象不为空才新增修改 如果为空则直接返回true
     *
     * @param obj
     * @param predicate
     * @param <T>
     * @return
     */
    public <T> Boolean ifPresentObj(T obj,Predicate<T> predicate) {
        if (obj != null) {
            return predicate.test(obj);
        }
        return true;
    }

    /**
     * 对象不为空才新增修改
     * 如果为空则直接返回true
     *
     * @param list
     * @param predicate
     * @param <T>
     * @return
     */
    public <T> Boolean ifPresentList(List<T> list,Predicate<T> predicate) {
        if (list != null && list.size() > 0) {
            for (T t : list) {
                if (!predicate.test(t)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 查询单纪录
     *
     * @param id
     * @return
     */
    public T getById(Long id) {
        return myMapper.selectById(id);
    }


    /**
     * 删除数据
     *
     * @param id
     */
    public Boolean deleteById(Long id) {
        return SqlHelper.retBool(myMapper.deleteById(id));
    }


    /**
     * 新增数据
     *
     * @param t
     * @return
     */
    public Boolean insert(T t) {
        return SqlHelper.retBool(myMapper.insert(t));
    }

    /**
     * 新增数据
     *
     * @param t
     * @return
     */
    public <T extends Model<T>> Boolean insertOrUpdate(T t) {
        return t.insertOrUpdate();
    }

    /**
     * 新增数据
     *
     * @param list
     * @return
     */
    public <T extends Model<T>> Boolean insertOrUpdate(List<T> list) {
        for (T t : list) {
            if (!insertOrUpdate(t)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 新增数据
     *
     * @param suppliers
     * @return
     */
    public Boolean insertOrUpdate(Supplier<Boolean>... suppliers) {
        for (Supplier<Boolean> supplier : suppliers) {
            try {
                if (!supplier.get()) {
                    return false;
                }
            } catch (Exception e) {
                throw e;
            }
        }
        return true;
    }


    /**
     * 修改数据
     *
     * @param t
     */
    public boolean updateById(T t) {
        return SqlHelper.retBool(myMapper.updateById(t));
    }

    /**
     * 判断字段重复
     *
     * @param id
     * @param columnName
     * @param value
     * @return
     */
    public Boolean hasRepeat(Long id, String columnName, Object value) {
        Long oldId = (Long) Optional.ofNullable(myMapper.selectMaps(new QueryWrapper<T>().select("id").eq(columnName,value).last("limit 1")))
//                .map(ls -> ls.stream().findFirst().orElse(new HashMap<>()).get("id")).orElse(null);
                                    .map(ls -> MapUtil.get(ls.stream().findFirst().orElse(null), "id", Long.class)).orElse(null);
        //数据库中没有，返回true表示可以新增或修改
        if (oldId == null) {
            return true;
        }
        //数据库中已经有了，但是与当前id相同也可以新增或修改
        if (id != null && id.equals(oldId)) {
            return true;
        }
        return false;
    }


    public T selectOne(String fieldValues, LambdaQueryWrapper<T> queryWrapper,Class<T> clazz) {
        if (StringUtils.isNotBlank(fieldValues)) {
            Predicate<TableFieldInfo> predicate = null;
            String[] fieldValuesArray = fieldValues.split(",");
            for (String field : fieldValuesArray) {
                predicate = predicate == null ? p -> p.getColumn().equals(field) : predicate.or(p -> p.getColumn().equals(field));
            }
            queryWrapper.select(clazz, predicate);
        }
        return myMapper.selectOne(queryWrapper);
    }

    /**
     * 删除数据
     *
     * @param ids
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int deleteBatch(Integer[] ids) {
        return myMapper.deleteBatchIds(Arrays.asList(ids));
    }

    public int delete(@Param("ew") Wrapper<T> queryWrapper) {
        return myMapper.delete(queryWrapper);
    }
}
