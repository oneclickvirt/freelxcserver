package com.incus.api;

import okhttp3.*;

import java.io.IOException;

public class IncusApi {
    private static final String BASE_URL = "https://127.0.0.1:8443";
    private final OkHttpClient client;

    public IncusApi() {
        this.client = new OkHttpClient();
    }
    // 列出所有容器
    public String listContainers() throws IOException {
        Request request = new Request.Builder()
            .url(BASE_URL + "/1.0/containers")
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
    // 获取指定containerName容器状态
    public String getContainerState(String containerName) throws IOException {
        Request request = new Request.Builder()
            .url(BASE_URL + "/1.0/containers/" + containerName + "/state")
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
    // 创建指定containerName、指定镜像(linux系统)容器
    public String createContainer(String containerName, String image) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"name\":\"" + containerName + "\",\"source\":{\"type\":\"image\",\"alias\":\"" + image + "\"}}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
            .url(BASE_URL + "/1.0/containers")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
    // 将指定containerName容器开机
    public String startContainer(String containerName) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"action\":\"start\",\"timeout\":30}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
            .url(BASE_URL + "/1.0/containers/" + containerName + "/state")
            .put(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
    // 将指定containerName容器关机
    public String stopContainer(String containerName) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"action\":\"stop\",\"timeout\":30,\"force\":true}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
            .url(BASE_URL + "/1.0/containers/" + containerName + "/state")
            .put(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
    // 删除指定containerName的容器
    public String deleteContainer(String containerName) throws IOException {
        Request request = new Request.Builder()
            .url(BASE_URL + "/1.0/containers/" + containerName)
            .delete()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
}
