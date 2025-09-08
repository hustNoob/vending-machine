package com.rem.vendingmachine.device;

public class SimulatedDeviceMain {

    public static void main(String[] args) throws InterruptedException {
        // 模拟设备 1
        String machineId = "1";
        SimulatedDeviceClient client = new SimulatedDeviceClient(machineId);

        // 启动心跳和状态上报
        SimulatedDeviceTask task = new SimulatedDeviceTask(client);
        task.start();

        // 模拟订单上报
        Thread.sleep(3000); // 等待 5 秒后上报订单
        client.reportOrder("12345", 1, 15.75);

        // 订阅命令主题
        client.subscribe("vendingmachine/command/" + machineId);
    }
}
