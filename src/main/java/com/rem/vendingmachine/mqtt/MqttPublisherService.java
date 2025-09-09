package com.rem.vendingmachine.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MqttPublisherService {

    @Autowired
    private MqttClient mqttClient; // 注入配置的服务端 MQTT 客户端

    /**
     * 基于指定主题发布消息（自定义 QoS）
     *
     * @param topic   发布的主题
     * @param payload 消息内容
     * @param qos     服务质量等级（0, 1, 2）
     */
    public void publish(String topic, String payload, int qos) {
        System.out.println("开始发布MQTT消息 - 主题: " + topic);
        System.out.println("开始发布MQTT消息 - 内容: " + payload);
        System.out.println("开始发布MQTT消息 - QoS: " + qos);

        try {
            // 将消息内容封装为 MQTT 消息对象
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos); // 设置 QoS 等级
            mqttClient.publish(topic, message); // 发布消息到指定主题

            // 成功日志
            System.out.println("消息发布成功 - 主题: " + topic + ", 内容: " + payload);
        } catch (MqttException e) {
            // 异常日志
            System.err.println("消息发布失败 - 主题: " + topic + ", 错误: " + e.getMessage());
            e.printStackTrace(); // 添加堆栈跟踪
            throw new RuntimeException("MQTT 消息发布失败", e); // 向上抛出异常（可选，根据业务需求）
        } catch (Exception e) {
            System.err.println("未知异常 - 主题: " + topic + ", 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 基于指定主题发布消息（默认 QoS = 1）
     *
     * @param topic   发布的主题
     * @param payload 消息内容
     */
    public void publish(String topic, String payload) {
        publish(topic, payload, 1); // 调用主方法，默认 QoS 为 1（至少一次传送）
    }
}
