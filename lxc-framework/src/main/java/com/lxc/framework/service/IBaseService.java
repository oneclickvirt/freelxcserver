package com.lxc.framework.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.ibatis.annotations.Param;

public interface IBaseService<T> extends IService<T> {

    /**
     * 对象不为空才新增修改 如果为空则直接返回true
     *
     * @param obj
     * @param predicate
     * @param <T>
     * @return
     */
    <T> Boolean ifPresentObj(T obj,Predicate<T> predicate);

    /**
     * 对象不为空才新增修改 如果为空则直接返回true
     *
     * @param list
     * @param predicate
     * @param <T>
     * @return
     */
    public <T> Boolean ifPresentList(List<T> list,Predicate<T> predicate);

    /**
     * 查询单纪录
     *
     * @param id
     * @return
     */
    public T getById(Long id);

    /**
     * 删除数据
     *
     * @param id
     */
    public Boolean deleteById(Long id);

    /**
     * 新增数据
     *
     * @param t
     * @return
     */
    public Boolean insert(T t);

    /**
     * 新增数据
     *
     * @param t
     * @return
     */
    public <T extends Model<T>> Boolean insertOrUpdate(T t);


    /**
     * 新增数据
     *
     * @param list
     * @return
     */
    public <T extends Model<T>> Boolean insertOrUpdate(List<T> list);

    /**
     * 新增数据
     *
     * @param suppliers
     * @return
     */
    public Boolean insertOrUpdate(Supplier<Boolean>... suppliers);

    /**
     * 修改数据
     *
     * @param t
     */
    public boolean updateById(T t);

    /**
     * 判断字段重复
     *
     * @param id
     * @param columnName
     * @param value
     * @return
     */
    public Boolean hasRepeat(Long id,String columnName,Object value);

    public T selectOne(String fieldValues,LambdaQueryWrapper<T> queryWrapper,Class<T> clazz);

    /**
     * 删除数据
     *
     * @param ids
     */
    public int deleteBatch(Integer[] ids);

    int delete(@Param("ew") Wrapper<T> queryWrapper);
}
