package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.mqtt.MqttPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test-publish")
public class TestPublisherController {

    @Autowired
    private MqttPublisherService mqttPublisherService;

    /**
     * 调用 MQTT 服务 - 支持 JSON 格式发布消息
     *
     * @param body 包含 topic 和 payload 的 JSON 格式
     * @return 发布结果
     */
    @PostMapping
    public String publishMessage(@RequestBody Map<String, String> body) {
        // 获取 topic 和 payload
        String topic = body.get("topic");
        String payload = body.get("payload");

        System.out.println("=== TestPublisherController ===");
        System.out.println("收到发布请求 - topic: " + topic);
        System.out.println("收到发布请求 - payload: " + payload);
        System.out.println("==============================");

        // 参数校验
        if (topic == null || topic.isEmpty()) {
            throw new RuntimeException("参数缺失：topic");
        }
        if (payload == null || payload.isEmpty()) {
            throw new RuntimeException("参数缺失：payload");
        }

        try {
            // 发布 MQTT 消息
            mqttPublisherService.publish(topic, payload); // 默认 QoS=1
            System.out.println("MQTT消息发布成功 - 主题: " + topic + ", 内容: " + payload);
            return "发布成功 - 主题: " + topic + ", 内容: " + payload;
        } catch (Exception e) {
            System.err.println("MQTT消息发布失败 - 主题: " + topic + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return "发布失败 - 主题: " + topic + ", 错误: " + e.getMessage();
        }
    }
}
