package com.lxc.machine.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {
    /**
     * 流量单位换算 -> MB
     * @param numberStr
     * @param unitStr
     * @return
     */
    public static BigDecimal unitUnify(String numberStr,String unitStr) {
        // 将kB、B、GB的单位化成MB
        BigDecimal flowNum = new BigDecimal(numberStr);
        switch (unitStr.toLowerCase()) {
            case "kb":
                flowNum = flowNum.divide(new BigDecimal(1024),2, RoundingMode.HALF_UP);
                break;
            case "b":
                flowNum = flowNum.divide(new BigDecimal(1024*1024),2,RoundingMode.HALF_UP);
                break;
            case "gb":
                flowNum = flowNum.multiply(new BigDecimal(1024));
                break;
        }
        return flowNum;
    }

    public static BigDecimal unitUnifyToBytes(String numberStr,String unitStr) {
        // 将kB、MB、GB的单位化成字节
        BigDecimal flowNum = new BigDecimal(numberStr);
        switch (unitStr.toLowerCase()) {
            case "kb":
                flowNum = flowNum.multiply(new BigDecimal(1024));
                break;
            case "mb":
                flowNum = flowNum.multiply(new BigDecimal(1024*1024));
                break;
            case "gb":
                flowNum = flowNum.multiply(new BigDecimal(1024*1024*1024));
                break;
            default:
                throw new IllegalArgumentException("不支持的单位: " + unitStr);
        }
        return flowNum;
    }

    public static BigDecimal numUnit2Decimal(String trafficTotalLimit) {
        String tTLNumberStr = extractNumber(trafficTotalLimit);
        String tTLUnitStr = extractUnit(trafficTotalLimit);
        BigDecimal tTL2MB = unitUnify(tTLNumberStr,tTLUnitStr);
        return tTL2MB;
    }
    /**
     * 正则表达式提取浮点数
     * @param input
     * @return
     */
    public static String extractNumber(String input) {
        // 定义一个正则表达式来匹配浮点数部分
        String numberRegex = "[+-]?([0-9]*[.])?[0-9]+";
        Pattern pattern = Pattern.compile(numberRegex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        } else {
            return null; // 或者返回一个默认值，取决于你的需求
        }
    }

    /**
     * 正则表达式提取字符串
     * @param input
     * @return
     */
    public static String extractUnit(String input) {
        // 定义一个正则表达式来匹配单位部分
        String unitRegex = "[a-zA-Z]+";
        Pattern pattern = Pattern.compile(unitRegex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group();
        } else {
            return null; // 或者返回一个默认值，取决于你的需求
        }
    }
}
