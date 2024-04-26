package com.lxc.common.utils.ip;


import cn.hutool.core.net.NetUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 获取地址类
 *
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressUtils {

    // IP地址查询
    public static final String IP_URL = "https://ip125.com/api/%s?lang=zh-CN";

    // 未知地址
    public static final String UNKNOWN = "XX XX";

    public static String getRealAddressByIP(String ip) {
        String address = UNKNOWN;
        if (StringUtils.isBlank(ip)) {
            return address;
        }
//        return ip;
        // 内网不查询
//        ip = "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : HtmlUtil.cleanHtmlTag(ip);
        try {
            if (NetUtil.isInnerIP(ip)) {
                return "内网IP";
            }
            String rspStr = HttpUtil.createGet(String.format(IP_URL,ip)).execute().body();
            if (StringUtils.isEmpty(rspStr)) {
                log.error("获取地理位置异常 {}", ip);
                return UNKNOWN;
            }
            JSONObject obj = JSON.parseObject(rspStr);
            String country = obj.getString("country");
            String city = obj.getString("city");
            if (StringUtils.equalsIgnoreCase(country,city)){
                city = "";
            }
            String org = obj.getString("org");
            String isp = obj.getString("isp");
            return String.format("%s %s %s ISP:%s", country, city, org, isp);
        } catch (Exception e) {
            log.error("获取地理位置异常 {}", ip);
        }
        return UNKNOWN;
    }

    public static void main(String[] args) {
        String rspStr = HttpUtil.createGet(String.format(IP_URL, "2400:6180:0:d0::fe5:1")).execute().body();
        String json = "{\"status\":\"success\",\"country\":\"新加坡\",\"countryCode\":\"SG\",\"region\":\"05\",\"regionName\":\"South West\",\"city\":\"新加坡\",\"zip\":\"627753\",\"lat\":1.32123,\"lon\":103.695,\"timezone\":\"Asia/Singapore\",\"isp\":\"DigitalOcean, LLC\",\"org\":\"Digital Ocean\",\"as\":\"AS14061 DigitalOcean, LLC\",\"query\":\"2400:6180:0:d0::fe5:1\"}";
        JSONObject obj = JSON.parseObject(rspStr);
        String country = obj.getString("country");
        String city = obj.getString("city");
        if (StringUtils.equalsIgnoreCase(country,city)){
            city = "";
        }
        String org = obj.getString("org");
        String isp = obj.getString("isp");
        System.out.println(String.format("%s %s %s ISP:%s", country, city, org, isp));
    }
}
