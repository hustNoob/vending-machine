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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MqttSubscriberService {

    @Autowired
    private MqttClient mqttClient;  // 服务端 MQTT 客户端

    @Autowired
    private VendingMachineService vendingMachineService;

    @Autowired
    private OrderService orderService;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 内存中的 MQTT 消息存储
    private final Map<String, List<MqttLog>> messageLogs = new ConcurrentHashMap<>();

    // 构造函数初始化存储结构
    public MqttSubscriberService() {
        messageLogs.put("heartbeat", new ArrayList<>());
        messageLogs.put("state", new ArrayList<>());
        messageLogs.put("order", new ArrayList<>());
    }

    /**
     * 订阅指定主题
     *
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
     * 处理收到的 MQTT 消息，根据主题分类存储并更新后端状态
     *
     * @param topic   消息的主题
     * @param message 消息对象
     */
    private void handleMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload()); // 解码消息内容
            System.out.println("收到消息 - 主题: " + topic + ", 内容: " + payload);

            // 创建日志对象
            MqttLog log = new MqttLog(topic, payload, System.currentTimeMillis());

            // 根据主题分类处理
            if (topic.startsWith("vendingmachine/heartbeat")) {
                handleHeartbeat(topic, log);
            } else if (topic.startsWith("vendingmachine/state")) {
                handleState(topic, payload, log);
            } else if (topic.startsWith("vendingmachine/order")) {
                handleOrder(topic, payload, log);
            } else {
                System.err.println("未知主题消息：" + topic);
            }
        } catch (Exception e) {
            System.err.println("处理消息时发生错误：" + e.getMessage());
        }
    }

    /**
     * 处理心跳消息并存储到日志
     */
    private void handleHeartbeat(String topic, MqttLog log) {
        try {
            // 从主题中提取 machineId
            String[] topicParts = topic.split("/");
            int machineId = Integer.parseInt(topicParts[2]);

            // 更新数据库中的 lastUpdateTime
            vendingMachineService.updateLastUpdateTime(machineId, LocalDateTime.now());
            System.out.println("心跳已更新 - 设备ID: " + machineId);

            // 存储日志
            synchronized (messageLogs) {
                messageLogs.get("heartbeat").add(log);
            }
        } catch (Exception e) {
            System.err.println("处理心跳消息失败：" + e.getMessage());
        }
    }

    /**
     * 处理状态消息
     */
    private void handleState(String topic, String payload, MqttLog log) {
        try {
            // 解析 JSON 消息
            JsonNode root = objectMapper.readTree(payload);

            int machineId = root.get("machineId").asInt();
            double temperature = root.get("temperature").asDouble();
            int status = root.get("status").asInt();

            // 更新售货机状态到数据库
            vendingMachineService.updateVendingMachineStatus(machineId, temperature, status);
            System.out.println("状态已更新 - 设备ID: " + machineId + ", 温度: " + temperature + ", 状态: " + status);

            // 存储日志
            synchronized (messageLogs) {
                messageLogs.get("state").add(log);
            }
        } catch (Exception e) {
            System.err.println("处理状态消息失败：" + e.getMessage());
        }
    }

    /**
     * 处理订单消息
     */
    private void handleOrder(String topic, String payload, MqttLog log) {
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

            // 存储日志
            synchronized (messageLogs) {
                messageLogs.get("order").add(log);
            }
        } catch (Exception e) {
            System.err.println("处理订单消息失败：" + e.getMessage());
        }
    }

    /**
     * 获取分类的 MQTT 消息日志
     *
     * @param type 消息类型（heartbeat, state, order）
     * @return 消息日志列表
     */
    public List<MqttLog> getLogs(String type) {
        synchronized (messageLogs) {
            return new ArrayList<>(messageLogs.getOrDefault(type, new ArrayList<>()));
        }
    }

    /**
     * 日志数据类
     */
    public static class MqttLog {
        private final String topic;
        private final String payload;
        private final long timestamp;

        public MqttLog(String topic, String payload, long timestamp) {
            this.topic = topic;
            this.payload = payload;
            this.timestamp = timestamp;
        }

        public String getTopic() {
            return topic;
        }

        public String getPayload() {
            return payload;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}