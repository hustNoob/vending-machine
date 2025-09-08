package com.rem.vendingmachine.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rem.vendingmachine.dao.VendingMachineMapper;
import com.rem.vendingmachine.model.VendingMachine;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MqttSubscriberService {

    @Autowired
    private MqttClient mqttClient;

    @Autowired
    private VendingMachineMapper vendingMachineMapper;

    /**
     * 订阅指定主题并处理消息
     * @param topic 主题
     */
    public void subscribe(String topic) {
        try {
            mqttClient.subscribe(topic, (receivedTopic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("收到来自主题[" + receivedTopic + "]的消息：" + payload);

                // 模拟处理收到的命令
                if (receivedTopic.startsWith("vendingmachine/command")) {
                    String machineId = getMachineIdFromTopic(receivedTopic);
                    handleDeviceCommand(Integer.parseInt(machineId), payload);
                }
            });
            System.out.println("已订阅主题：" + topic);
        } catch (Exception e) {
            System.err.println("订阅失败：" + e.getMessage());
        }
    }

    private String getMachineIdFromTopic(String topic) {
        String[] parts = topic.split("/");
        return parts[parts.length - 1]; // 假定格式为 vendingmachine/command/{machineId}
    }

    private void handleDeviceCommand(int machineId, String messagePayload) {
        try {
            // 解析 JSON 消息内容
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(messagePayload);
            String command = jsonNode.get("command").asText(); // 提取命令
            long timestamp = jsonNode.get("timestamp").asLong(); // 提取时间戳（如果需要）

            // 查询目标售货机
            VendingMachine machine = vendingMachineMapper.selectVendingMachineById(machineId);

            if (machine == null) {
                System.out.println("设备ID：" + machineId + " 未找到！");
                return;
            }

            // 执行设备命令
            System.out.println("执行命令：" + command);
            machine.applyCommand(command); // 假设 applyCommand 能正确处理命令

            // 更新数据库（如设备状态/温度变化等）
            vendingMachineMapper.updateVendingMachine(machine);

            // 模拟设备状态上报
            String stateTopic = "vendingmachine/state/" + machineId;
            String statePayload = String.format(
                    "{\"status\":%d,\"temperature\":%.1f,\"location\":\"%s\"}",
                    machine.getStatus(),
                    machine.getTemperature(),
                    machine.getLocationDesc()
            );
            publish(stateTopic, statePayload);
        } catch (Exception e) {
            System.err.println("处理设备命令时发生错误：" + e.getMessage());
        }
    }


    // 发布消息（如设备模拟状态上报）
    public void publish(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1); // 至少一次
            mqttClient.publish(topic, message);
            System.out.println("消息发布成功，主题：" + topic + ", 内容：" + payload);
        } catch (MqttException e) {
            System.err.println("消息发布失败：" + e.getMessage());
        }
    }
}
