package com.lxc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@SpringBootTest
public class Test_JsonUrl {


    @Test
    public void test01(){
        String url = "https://lxdimages.spiritlhl.net/streams/v1/spiritlhl.json";
        String jsonResponse = fetchJson(url);

        if (jsonResponse != null) {
            System.out.println("获取到的JSON数据：");
            System.out.println(jsonResponse);

            // 示例：使用Jackson解析JSON
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode jsonNode = objectMapper.readValue(jsonResponse, ObjectNode.class);

                // 打印content_id字段
                String contentId = jsonNode.get("content_id").asText();
                System.out.println("\n解析示例 - content_id: " + contentId);

                // 打印products中的第一个产品
                ObjectNode productsNode = (ObjectNode) jsonNode.get("products");
                if (productsNode != null && !productsNode.isEmpty()) {
                    String firstProductKey = productsNode.fieldNames().next();
                    System.out.println("第一个产品: " + firstProductKey);
                }
            } catch (IOException e) {
                System.err.println("解析JSON出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    /**
     * 从指定URL获取JSON数据
     */
    private static String fetchJson(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() == 200) { // OK
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                System.err.println("HTTP错误代码: " + connection.getResponseCode());
                return null;
            }
        } catch (IOException e) {
            System.err.println("获取JSON数据时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
