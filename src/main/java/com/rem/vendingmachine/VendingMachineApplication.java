package com.rem.vendingmachine;

import com.rem.vendingmachine.mqtt.MqttSubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VendingMachineApplication implements CommandLineRunner {

    @Autowired
    private MqttSubscriberService mqttSubscriberService;

    public static void main(String[] args) {
        SpringApplication.run(VendingMachineApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // 服务端开机时，自动订阅相关 MQTT 主题
        mqttSubscriberService.subscribe("vendingmachine/heartbeat/#");
        mqttSubscriberService.subscribe("vendingmachine/state/#");
        mqttSubscriberService.subscribe("vendingmachine/order/#");
        System.out.println("服务端完成了所有 MQTT 主题的订阅");
    }
}
