package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.mqtt.MqttSubscriberService.MqttLog;
import com.rem.vendingmachine.mqtt.MqttSubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    @Autowired
    private MqttSubscriberService mqttSubscriberService;

    // 获取 MQTT 消息记录
    @GetMapping("/data")
    public List<MqttLog> getLogs(@RequestParam String type) {
        return mqttSubscriberService.getLogs(type);
    }
}