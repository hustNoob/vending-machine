package com.rem.vendingmachine.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {

    //MQTT broker的地址也就是云服务器ip
    private static final String BROKER_URL = "tcp://8.148.64.50:1883";

    //客户端id，无严格要求
    private static final String CLIENT_ID = "VendingMachineApp";

    //MQTT连接配置,一些基本配置
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true); //每次连接过就清除session
        options.setConnectionTimeout(10); //10秒超时时间，基本设置
        options.setKeepAliveInterval(60); //心跳检测保持连接
        return options;
    }

    //MQTT客户端,连上了可以发布和订阅消息了
    @Bean
    public MqttClient mqttClient(MqttConnectOptions options) throws MqttException {
        MqttClient client = new MqttClient(BROKER_URL, CLIENT_ID); //配置连接的信息
        client.connect(options);
        return client;
    }
}
