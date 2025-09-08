package com.rem.vendingmachine.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rem.vendingmachine.service.OrderService;
import com.rem.vendingmachine.service.VendingMachineService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MqttSubscriberService {

    @Autowired
    private MqttClient mqttClient;  // 服务端 MQTT 客户端

    @Autowired
    private VendingMachineService vendingMachineService;

    @Autowired
    private OrderService orderService;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 订阅指定主题
     * @param topic 待订阅的主题
     */
    public void subscribe(String topic) {
        try {
            mqttClient.subscribe(topic, (receivedTopic, message) -> handleMessage(receivedTopic, message));
            System.out.println("成功订阅主题：" + topic);
        } catch (Exception e) {
            System.err.println("订阅主题失败：" + topic + "，错误：" + e.getMessage());
        }
    }

    /**
     * 处理收到的消息，根据主题路由到不同方法
     * @param topic   消息的主题
     * @param message 消息对象
     */
    private void handleMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload()); // 解码消息内容
            System.out.println("收到消息 - 主题: " + topic + ", 内容: " + payload);

            // 根据主题类型处理
            if (topic.startsWith("vendingmachine/heartbeat")) {
                handleHeartbeat(topic);
            } else if (topic.startsWith("vendingmachine/state")) {
                handleState(payload);
            } else if (topic.startsWith("vendingmachine/order")) {
                handleOrder(payload);
            } else {
                System.err.println("未知主题消息：" + topic);
            }
        } catch (Exception e) {
            System.err.println("处理消息时发生错误：" + e.getMessage());
        }
    }

    /**
     * 处理心跳消息，更新设备状态
     */
    private void handleHeartbeat(String topic) {
        try {
            // 从主题中提取 machineId
            String[] topicParts = topic.split("/");
            int machineId = Integer.parseInt(topicParts[2]);

            // 更新数据库中的 lastUpdateTime
            vendingMachineService.updateLastUpdateTime(machineId, LocalDateTime.now());
            System.out.println("更新心跳 - 设备ID: " + machineId + ", 时间: " + LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("处理心跳消息失败：" + e.getMessage());
        }
    }

    /**
     * 处理状态消息，更新设备运行状态
     */
    private void handleState(String payload) {
        try {
            // 解析 JSON 消息
            JsonNode root = objectMapper.readTree(payload);

            int machineId = root.get("machineId").asInt();
            double temperature = root.get("temperature").asDouble();
            int status = root.get("status").asInt();

            // 更新售货机状态到数据库
            vendingMachineService.updateVendingMachineStatus(machineId, temperature, status);
            System.out.println("更新状态 - 设备ID: " + machineId + ", 温度: " + temperature + ", 状态: " + status);
        } catch (Exception e) {
            System.err.println("处理状态消息失败：" + e.getMessage());
        }
    }

    /**
     * 处理订单消息，创建订单并更新库存
     */
    private void handleOrder(String payload) {
        try {
            // 解析 JSON 消息
            JsonNode root = objectMapper.readTree(payload);

            String orderId = root.get("orderId").asText();
            int userId = root.get("userId").asInt();
            int vendingMachineId = root.get("machineId").asInt();
            double totalPrice = root.get("totalPrice").asDouble();

            // 调用服务创建订单
            orderService.createOrderFromMachine(vendingMachineId, userId, orderId, totalPrice);
            System.out.println("订单已成功处理 - 订单ID: " + orderId);
        } catch (Exception e) {
            System.err.println("处理订单消息失败：" + e.getMessage());
        }
    }
}
