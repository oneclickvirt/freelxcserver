package com.lxc.common.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * @author IMCYS
 * @description TODO 标准JSON返回草案模板
 * @since 1.0
 **/
@JsonIgnoreProperties(ignoreUnknown = true)   //忽略传参时其他无用字段
@JsonInclude(JsonInclude.Include.NON_NULL)   // 忽略反参时值为null的字段
public class R {

    @JsonIgnore
    private boolean dataType = false;
    private Integer code;
    private String msg;
    private Object data;


    public Object getData() {

        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public R(Integer code,String msg) {
        this.code = code;
        this.msg = msg;
        this.dataType = true;
    }

    public R(Integer code,String msg,Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


    @Override
    public String toString() {
        return "R{" + "code=" + code + ", msg='" + msg + '\'' + ", data=" + data + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        R r = (R) o;
        return code.equals(r.code) && msg.equals(r.msg) && data.equals(r.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, msg, data);
    }

    /**
     * @param code 状态码 暂无无标准
     * @param msg  提示内容
     * @return JSON对象
     */
    public static R success(Integer code, String msg) {
        return new R(code, msg);
    }

    public static R success(Integer code, String msg, Object data) {
        return new R(code, msg, data);
    }
}
