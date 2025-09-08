package com.rem.vendingmachine.device;

import java.util.Timer;
import java.util.TimerTask;

public class SimulatedDeviceTask {

    private SimulatedDeviceClient client;

    public SimulatedDeviceTask(SimulatedDeviceClient client) {
        this.client = client;
    }

    // 启动定时任务
    public void start() {
        Timer timer = new Timer();

        // 心跳上报任务，每 30 秒一次
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String topic = "vendingmachine/heartbeat/" + client.getMachineId();
                String payload = "{}"; // 心跳消息内容一般为空
                client.publish(topic, payload);
            }
        }, 0, 30000);

        // 状态上报任务，每 60 秒一次
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String topic = "vendingmachine/state/" + client.getMachineId();
                String payload = "{\"machineId\": \"" + client.getMachineId() + "\", \"temperature\": 25.5, \"status\": 1}";
                client.publish(topic, payload);
            }
        }, 0, 60000);
    }
}
