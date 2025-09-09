package com.rem.vendingmachine.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {

    // MQTT Broker 地址
    private static final String BROKER_URL = "tcp://8.148.64.50:1883";

    // 默认客户端 ID 前缀（确保唯一）
    private static final String CLIENT_ID_PREFIX = "VendingMachineApp_";

    // 连接选项配置
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true); // 清除会话数据
        options.setConnectionTimeout(10); // 连接超时时间
        options.setKeepAliveInterval(60); // 心跳包发送间隔（秒）
        options.setAutomaticReconnect(true); // 自动重连
        return options;
    }

    // 服务端 MQTT 客户端（用于发布和订阅消息）
    @Bean
    public MqttClient mqttServerClient(MqttConnectOptions options) throws MqttException {
        String clientId = CLIENT_ID_PREFIX + "Server";
        System.out.println("正在连接MQTT Broker: " + BROKER_URL + ", ClientID: " + clientId);
        MqttClient client = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());
        try {
            client.connect(options);
            System.out.println("服务端 MQTT 客户端连接成功，ClientID: " + clientId);
        } catch (MqttException e) {
            System.err.println("服务端 MQTT 客户端连接失败，ClientID: " + clientId);
            e.printStackTrace();
            throw e;
        }
        return client;
    }
}
