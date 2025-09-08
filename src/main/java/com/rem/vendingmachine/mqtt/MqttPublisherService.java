package com.rem.vendingmachine.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MqttPublisherService {

    @Autowired
    private MqttClient mqttClient;

    /**
     * 发布消息到指定主题
     * @param topic 主题
     * @param payload 消息内容
     */
    public void publish(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());//消息内容
            message.setQos(1); //消息，1，至少一次
            mqttClient.publish(topic, message); //有异常抛出，可能发布不成功
            System.out.println("消息发布成功，topic为" + topic);
        } catch (MqttException e) {
            System.err.println("消息发布失败："+e.getMessage());
        }
    }

}
