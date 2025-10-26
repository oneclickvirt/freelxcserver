package com.lxc.machine.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lxc.common.annotation.ExcelDictFormat;
import com.lxc.common.convert.ExcelDictConvert;
import com.lxc.machine.domain.LxcQueue;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

//import com.lxc.machine.domain.LxcQueue;


/**
 * 小鸡信息视图对象 lxc_lxc
 *
 * @author lxc
 * @date 2022-10-11
 */
@Data
@ExcelIgnoreUnannotated
public class LxcLxcVo {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 小鸡名称
     */
    @ExcelProperty(value = "小鸡名称")
    private String name;

    /**
     * 小鸡标签
     */
    @ExcelProperty(value = "小鸡标签")
    private String tag;

    /**
     * 母鸡IP
     */
    @ExcelProperty(value = "母鸡IP")
    private String superIp;

    /**
     * 容器所用镜像
     */
    private String mirrorImage;

    /**
     * 子级标签
     */
    @ExcelProperty(value = "子级标签")
    private String childName;

    /**
     * ssh端口
     */
    @ExcelProperty(value = "ssh端口")
    private String sshPort;

    /**
     * 小鸡密码
     */
    @ExcelProperty(value = "小鸡密码")
    private String password;

    /**
     * 开始端口
     */
    @ExcelProperty(value = "开始端口")
    private String portStart;

    /**
     * 结束端口
     */
    @ExcelProperty(value = "结束端口")
    private String portEnd;

    /**
     *
     */
    @ExcelProperty(value = "")
    private String superDate;

    /**
     *
     */
    @ExcelProperty(value = "")
    private String superDead;

    /**
     * 小鸡使用状态 1是占用 0是空闲
     */
    @ExcelProperty(value = "小鸡使用状态 1是占用 0是空闲", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "lxc_state")
    private Integer state;

    /**
     * 小鸡状态
     */
    @ExcelProperty(value = "小鸡状态 1是占用 0是空闲", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "lxc_status")
    private String lxcStatus;

    /**
     * 小鸡最近更新的状态
     */
    @ExcelProperty(value = "小鸡最近更新的状态")
    @ExcelDictFormat(dictType = "last_updated_status")
    private String lastUpdatedStatus;

    /**
     * 小鸡累积使用流量
     */
    @ExcelProperty(value = "流量使用量")
    @ExcelDictFormat(dictType = "lxc_traffic")
    private BigDecimal lxcTraffic;

    /**
     * 网卡
     */
    @ExcelProperty(value = "网卡名称")
    private String hostInterface;

    /**
     * IPV6
     */
    @ExcelProperty(value = "IPV6")
    private String ipv6Addr;


    private Integer count;

    private LxcQueue lxcQueue;

    private LxcQueueVo lxcQueueVo;

    /**
     * 创建时间
     */
    private Date createTime;


//    public LxcLxcVo() {
//    }
//
//    public LxcLxcVo(Long id, String name, String tag, String superIp, String childName, String sshPort, String password, String portStart, String portEnd, String superDate, String superDead, Integer state, String lxcStatus, String hostInterface, String ipv6Addr, Integer count, LxcQueueVo lxcQueueVo, Date createTime) {
//        this.id = id;
//        this.name = name;
//        this.tag = tag;
//        this.superIp = superIp;
//        this.childName = childName;
//        this.sshPort = sshPort;
//        this.password = password;
//        this.portStart = portStart;
//        this.portEnd = portEnd;
//        this.superDate = superDate;
//        this.superDead = superDead;
//        this.state = state;
//        this.lxcStatus = lxcStatus;
//        this.hostInterface = hostInterface;
//        this.ipv6Addr = ipv6Addr;
//        this.count = count;
//        this.lxcQueueVo = lxcQueueVo;
//        this.createTime = createTime;
//    }
//
//    public static long getSerialVersionUID() {
//        return serialVersionUID;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getTag() {
//        return tag;
//    }
//
//    public void setTag(String tag) {
//        this.tag = tag;
//    }
//
//    public String getSuperIp() {
//        return superIp;
//    }
//
//    public void setSuperIp(String superIp) {
//        this.superIp = superIp;
//    }
//
//    public String getChildName() {
//        return childName;
//    }
//
//    public void setChildName(String childName) {
//        this.childName = childName;
//    }
//
//    public String getSshPort() {
//        return sshPort;
//    }
//
//    public void setSshPort(String sshPort) {
//        this.sshPort = sshPort;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
//
//    public String getPortStart() {
//        return portStart;
//    }
//
//    public void setPortStart(String portStart) {
//        this.portStart = portStart;
//    }
//
//    public String getPortEnd() {
//        return portEnd;
//    }
//
//    public void setPortEnd(String portEnd) {
//        this.portEnd = portEnd;
//    }
//
//    public String getSuperDate() {
//        return superDate;
//    }
//
//    public void setSuperDate(String superDate) {
//        this.superDate = superDate;
//    }
//
//    public String getSuperDead() {
//        return superDead;
//    }
//
//    public void setSuperDead(String superDead) {
//        this.superDead = superDead;
//    }
//
//    public Integer getState() {
//        return state;
//    }
//
//    public void setState(Integer state) {
//        this.state = state;
//    }
//
//    public String getLxcStatus() {
//        return lxcStatus;
//    }
//
//    public void setLxcStatus(String lxcStatus) {
//        this.lxcStatus = lxcStatus;
//    }
//
//    public String getHostInterface() {
//        return hostInterface;
//    }
//
//    public void setHostInterface(String hostInterface) {
//        this.hostInterface = hostInterface;
//    }
//
//    public String getIpv6Addr() {
//        return ipv6Addr;
//    }
//
//    public void setIpv6Addr(String ipv6Addr) {
//        this.ipv6Addr = ipv6Addr;
//    }
//
//    public Integer getCount() {
//        return count;
//    }
//
//    public void setCount(Integer count) {
//        this.count = count;
//    }
//
//    public LxcQueueVo getLxcQueueVo() {
//        return lxcQueueVo;
//    }
//
//    public void setLxcQueueVo(LxcQueueVo lxcQueueVo) {
//        this.lxcQueueVo = lxcQueueVo;
//    }
//
//    public Date getCreateTime() {
//        return createTime;
//    }
//
//    public void setCreateTime(Date createTime) {
//        this.createTime = createTime;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        LxcLxcVo lxcLxcVo = (LxcLxcVo) o;
//        return Objects.equals(id, lxcLxcVo.id) && Objects.equals(name, lxcLxcVo.name) && Objects.equals(tag, lxcLxcVo.tag) && Objects.equals(superIp, lxcLxcVo.superIp) && Objects.equals(childName, lxcLxcVo.childName) && Objects.equals(sshPort, lxcLxcVo.sshPort) && Objects.equals(password, lxcLxcVo.password) && Objects.equals(portStart, lxcLxcVo.portStart) && Objects.equals(portEnd, lxcLxcVo.portEnd) && Objects.equals(superDate, lxcLxcVo.superDate) && Objects.equals(superDead, lxcLxcVo.superDead) && Objects.equals(state, lxcLxcVo.state) && Objects.equals(lxcStatus, lxcLxcVo.lxcStatus) && Objects.equals(hostInterface, lxcLxcVo.hostInterface) && Objects.equals(ipv6Addr, lxcLxcVo.ipv6Addr) && Objects.equals(count, lxcLxcVo.count) && Objects.equals(lxcQueueVo, lxcLxcVo.lxcQueueVo) && Objects.equals(createTime, lxcLxcVo.createTime);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id, name, tag, superIp, childName, sshPort, password, portStart, portEnd, superDate, superDead, state, lxcStatus, hostInterface, ipv6Addr, count, lxcQueueVo, createTime);
//    }
}
