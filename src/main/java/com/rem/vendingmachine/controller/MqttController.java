package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.mqtt.MqttSubscriberService.MqttLog;
import com.rem.vendingmachine.mqtt.MqttSubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    @Autowired
    private MqttSubscriberService mqttSubscriberService;

    // 获取 MQTT 消息记录
    @GetMapping("/data")
    public List<MqttLog> getLogs(@RequestParam String type) {
        if ("order".equals(type)) {
            return mqttSubscriberService.getAllOrderLogs(); // 返回所有订单消息
        }
        return mqttSubscriberService.getLogs(type);
    }


    // --- 获取所有设备快照 ---
    @GetMapping("/devices")
    public Collection<MqttSubscriberService.DeviceSnapshot> getDeviceSnapshots() {
        return mqttSubscriberService.getAllDeviceSnapshotsComprehensive(); // 新方法
    }

    // 在 MqttController 中添加新方法
    @GetMapping("/processed-orders")
    public List<MqttLog> getProcessedOrders() {
        return mqttSubscriberService.getProcessedOrders();
    }


}