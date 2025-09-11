package com.rem.vendingmachine.device;

import java.util.Timer;
import java.util.TimerTask;

public class SimulatedDeviceTask {

    private SimulatedDeviceClient client; // 引入 SimulatedDeviceClient 实例

    public SimulatedDeviceTask(SimulatedDeviceClient client) {
        this.client = client;
    }

    // 启动定时任务
    public void start() {
        Timer timer = new Timer();

//        // --- 模拟用户购买任务 ---
//        //===============================================================
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                // 模拟一个用户ID，例如 1
//                // 在真实场景中，这可能由某种方式确定（例如，设备端记录了最近使用的用户ID）
//                int simulatedUserId = 1;
//                System.out.println("[定时任务] 设备 " + client.getMachineId() + " 尝试模拟一次购买...");
//                client.simulatePurchaseAndReportOrder(simulatedUserId);
//            }
//        }, 10000, 30000); // 延迟10秒启动，每30秒尝试一次模拟购买
//        //================================================================

        // 心跳上报任务，每 10 秒一次
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String topic = "vendingmachine/heartbeat/" + client.getMachineId();
                String payload = "{}";
                client.publish(topic, payload);
            }
        }, 0, 5000); // 每10秒

        // --- 修改：状态上报任务，使用客户端内部状态 ---
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 从 client 中获取最新的状态
                double temperature = client.getCurrentTemperature();
                int status = client.getCurrentStatus();

                String topic = "vendingmachine/state/" + client.getMachineId();

                // 重要：在上报前先确保库存是最新的，或至少触发一次更新请求
                // 方式一：直接请求一次库存 (简单有效，增加一次请求)
                client.requestInventory(); // 触发请求，但不等待响应（异步）
                // （或者可以考虑在 getPayloadForStateReport 内部调用，并等待一小段时间，但这更复杂）

                //重要，集成了状态上报
                String payload = client.getPayloadForStateReport();
                client.publish(topic, payload);
                // System.out.println("（定时任务）状态已上报: " + payload); // 可以移到 getPayloadForStateReport 里
            }
        }, 0, 5000); // 每10秒
        // --- 修改结束 ---

        // --- 库存同步任务 ---
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("[定时同步] 设备 " + client.getMachineId() + " 正在请求最新库存...");
                client.requestInventory(); // 触发请求库存
            }
        }, 5000, 10000); // 延迟5秒启动，每30秒执行一次
        // --- 新增结束 ---
    }
}