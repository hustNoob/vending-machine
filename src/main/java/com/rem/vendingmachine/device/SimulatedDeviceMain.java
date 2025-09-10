package com.rem.vendingmachine.device;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SimulatedDeviceMain {

    // 获取数据库中所有售货机ID的工具方法
    private static List<Integer> fetchAllMachineIdsFromBackend() {
        try {
            String apiUrl = "http://localhost:8080/api/vending-machine/allMachineId";
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed to connect to backend: " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            String jsonResponse = response.toString().trim();
            // 简单解析JSON数组（实际项目中建议使用Jackson等库）
            jsonResponse = jsonResponse.replaceAll("[\\[\\]]", ""); // 移除 []
            if (jsonResponse.isEmpty()) {
                return Arrays.asList(1, 4, 5); // 默认值
            }
            return Arrays.stream(jsonResponse.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("获取设备列表失败: " + e.getMessage());
            return Arrays.asList(1, 4, 5); // 返回默认值
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("启动模拟设备系统...");

        // 从后端API获取当前数据库中的所有售货机ID
        List<Integer> machineIds = fetchAllMachineIdsFromBackend();
        if (machineIds == null || machineIds.isEmpty()) {
            System.out.println("未获取到设备列表，使用默认设备1,4,5");
            machineIds = Arrays.asList(1, 4, 5);
        }

        System.out.println("从数据库获取的设备ID: " + machineIds);

        // 转换为String数组
        String[] machineIdStrings = machineIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        SimulatedDeviceClient[] clients = new SimulatedDeviceClient[machineIdStrings.length];
        SimulatedDeviceTask[] tasks = new SimulatedDeviceTask[machineIdStrings.length];

        for (int i = 0; i < machineIdStrings.length; i++) {
            String machineId = machineIdStrings[i];
            System.out.println("正在启动设备: " + machineId);

            // 1. 创建模拟设备客户端
            clients[i] = new SimulatedDeviceClient(machineId);

            // 2. 启动心跳和状态上报任务
            tasks[i] = new SimulatedDeviceTask(clients[i]);
            tasks[i].start();

            // 3. 订阅命令主题
            Thread.sleep(500); // 简单等待一下
            String commandTopic = "vendingmachine/command/" + machineId;
            clients[i].subscribe(commandTopic);
            System.out.println("设备 " + machineId + " 已订阅命令主题: " + commandTopic);

            // 订阅库存响应主题
            String inventoryResponseTopic = "vendingmachine/inventory/response/" + machineId;
            clients[i].subscribe(inventoryResponseTopic);
            System.out.println("设备 " + machineId + " 已订阅库存响应主题: " + inventoryResponseTopic);

            clients[i].requestInventory();
        }

        System.out.println("所有数据库中的模拟设备已启动并正在运行...按 Ctrl+C 退出。");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("模拟设备程序已结束。");
        }
    }
}
