//package org.relxd.lxd.incus;
//
//import com.squareup.okhttp.*;
//
//import java.io.IOException;
//
//public class IncusApi {
//    private final String baseUrl;
//    private final OkHttpClient client;
//
//    public IncusApi(String baseUrl) {
//        this.baseUrl = baseUrl;
//        this.client = new OkHttpClient();
//    }
//
//    public String listContainers() throws IOException {
//        Request request = new Request.Builder()
//            .url(baseUrl + "/1.0/containers")
//            .get()
//            .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            return response.body().string();
//        }
//    }
//
//    public String getContainerState(String containerName) throws IOException {
//        Request request = new Request.Builder()
//            .url(baseUrl + "/1.0/containers/" + containerName + "/state")
//            .get()
//            .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            return response.body().string();
//        }
//    }
//
//    public String createContainer(String containerName, String image) throws IOException {
//        MediaType JSON = MediaType.get("application/json; charset=utf-8");
//        String json = "{\"name\":\"" + containerName + "\",\"source\":{\"type\":\"image\",\"alias\":\"" + image + "\"}}";
//
//        RequestBody body = RequestBody.create(json, JSON);
//        Request request = new Request.Builder()
//            .url(baseUrl + "/1.0/containers")
//            .post(body)
//            .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            return response.body().string();
//        }
//    }
//
//    public String startContainer(String containerName) throws IOException {
//        MediaType JSON = MediaType.get("application/json; charset=utf-8");
//        String json = "{\"action\":\"start\",\"timeout\":30}";
//
//        RequestBody body = RequestBody.create(json, JSON);
//        Request request = new Request.Builder()
//            .url(baseUrl + "/1.0/containers/" + containerName + "/state")
//            .put(body)
//            .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            return response.body().string();
//        }
//    }
//
//    public String stopContainer(String containerName) throws IOException {
//        MediaType JSON = MediaType.get("application/json; charset=utf-8");
//        String json = "{\"action\":\"stop\",\"timeout\":30,\"force\":true}";
//
//        RequestBody body = RequestBody.create(json, JSON);
//        Request request = new Request.Builder()
//            .url(baseUrl + "/1.0/containers/" + containerName + "/state")
//            .put(body)
//            .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            return response.body().string();
//        }
//    }
//
//    public String freezeContainer(String containerName) throws IOException {
//        MediaType JSON = MediaType.get("application/json; charset=utf-8");
//        String json = "{\"action\":\"freeze\",\"timeout\":30}";
//
//        RequestBody body = RequestBody.create(json, JSON);
//        Request request = new Request.Builder()
//            .url(baseUrl + "/1.0/containers/" + containerName + "/state")
//            .put(body)
//            .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            return response.body().string();
//        }
//    }
//
//    public String unfreezeContainer(String containerName) throws IOException {
//        MediaType JSON = MediaType.get("application/json; charset=utf-8");
//        String json = "{\"action\":\"unfreeze\",\"timeout\":30}";
//
//        RequestBody body = RequestBody.create(json, JSON);
//        Request request = new Request.Builder()
//            .url(baseUrl + "/1.0/containers/" + containerName + "/state")
//            .put(body)
//            .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            return response.body().string();
//        }
//    }
//
//    public String deleteContainer(String containerName) throws IOException {
//        Request request = new Request.Builder()
//            .url(baseUrl + "/1.0/containers/" + containerName)
//            .delete()
//            .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            return response.body().string();
//        }
//    }
//
//    public static void main(String[] args) {
//        try {
////            String baseUrl = "https://47.237.120.189:8443"; // 这个 URL 可以根据实际情况动态传递
//            String baseUrl = "https://47.237.120.189:9969"; // 这个 URL 可以根据实际情况动态传递
//            IncusApi client = new IncusApi(baseUrl);
//
//            // 创建一个容器
//            String newContainer = client.createContainer("YanTest2", "ubuntu:22.04");
//            System.out.println("Container Created: " + newContainer);
//
////            // 启动容器
////            String start = client.startContainer("new-container");
////            System.out.println("Container Started: " + start);
////
////            // 冻结容器
////            String freeze = client.freezeContainer("new-container");
////            System.out.println("Container Frozen: " + freeze);
////
////            // 解冻容器
////            String unfreeze = client.unfreezeContainer("new-container");
////            System.out.println("Container Unfrozen: " + unfreeze);
////
////            // 停止容器
////            String stop = client.stopContainer("new-container");
////            System.out.println("Container Stopped: " + stop);
////
////            // 删除容器
////            String delete = client.deleteContainer("new-container");
////            System.out.println("Container Deleted: " + delete);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
