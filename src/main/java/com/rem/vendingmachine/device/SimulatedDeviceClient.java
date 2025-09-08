package com.rem.vendingmachine.device;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulatedDeviceClient {

    private static final String BROKER_URL = "tcp://8.148.64.50:1883"; // MQTT Broker 地址
    private MqttClient client;
    private String machineId;

    public SimulatedDeviceClient(String machineId) {
        try {
            this.machineId = machineId;
            String clientId = "SimulatedDevice_" + machineId + "_" + UUID.randomUUID(); // ClientID 保证唯一
            client = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());

            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // 每次连接清理会话，确保数据一致性
            options.setAutomaticReconnect(true); // 自动重连
            client.connect(options);

            System.out.println("模拟设备已连接，设备ID: " + machineId);
        } catch (MqttException e) {
            System.err.println("模拟设备连接失败：" + e.getMessage());
        }
    }

    /**
     * 发布消息到指定主题
     * @param topic   主题
     * @param payload 消息内容
     */
    public void publish(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1); // 至少一次传递
            client.publish(topic, message);
            System.out.println("消息已发布 - 主题: " + topic + ", 内容: " + payload);
        } catch (MqttException e) {
            System.err.println("消息发布失败：" + e.getMessage());
        }
    }

    /**
     * 订阅主题，并设置回调处理收到的消息
     * @param topic 订阅的主题
     */
    public void subscribe(String topic) {
        try {
            client.subscribe(topic, (receivedTopic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("收到命令消息 - 主题: " + receivedTopic + ", 内容: " + payload);
                processCommand(payload);
            });
            System.out.println("成功订阅主题：" + topic);
        } catch (MqttException e) {
            System.err.println("订阅主题失败：" + topic + "，错误：" + e.getMessage());
        }
    }

    /**
     * 处理命令消息
     * @param payload 命令内容
     */
    private void processCommand(String payload) {
        // 模拟执行命令，例如修改温度、更新状态等
        System.out.println("执行命令：" + payload);
    }

    /**
     * 模拟订单上报
     * @param orderId    订单编号
     * @param userId     用户 ID
     * @param totalPrice 订单总额
     */
    public void reportOrder(String orderId, int userId, double totalPrice) {
        String topic = "vendingmachine/order/" + orderId;
        String payload = String.format(
                "{\"orderId\": \"%s\", \"userId\": %d, \"machineId\": \"%s\", \"totalPrice\": %.2f}",
                orderId, userId, this.machineId, totalPrice
        );
        publish(topic, payload);
    }
}
