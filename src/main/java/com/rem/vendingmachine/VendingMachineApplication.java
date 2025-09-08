package com.rem.vendingmachine;

import com.rem.vendingmachine.model.VendingMachine;
import com.rem.vendingmachine.mqtt.MqttSubscriberService;
import com.rem.vendingmachine.service.VendingMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class VendingMachineApplication implements CommandLineRunner {

    @Autowired
    private MqttSubscriberService mqttSubscriberService; // 注入 MQTT 订阅服务

    @Autowired
    private VendingMachineService vendingMachineService;

    public static void main(String[] args) {
        SpringApplication.run(VendingMachineApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // 应用启动后自动订阅所有设备命令主题
        mqttSubscriberService.subscribe("vendingmachine/command/#");
        System.out.println("MQTT已订阅：vendingmachine/command/#");

        // 模拟售货机设备状态定期上报
        simulateDeviceState();
    }

    private void simulateDeviceState() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            try {
                List<VendingMachine> machines = vendingMachineService.getAllVendingMachines();
                for (VendingMachine machine : machines) {
                    String stateTopic = "vendingmachine/state/" + machine.getId();
                    String statePayload = String.format(
                            "{\"status\":%d,\"temperature\":%.1f,\"location\":\"%s\"}",
                            machine.getStatus(),
                            machine.getTemperature(),
                            machine.getLocationDesc()
                    );
                    mqttSubscriberService.publish(stateTopic, statePayload);
                }
            } catch (Exception e) {
                System.err.println("设备状态上报失败：" + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

}
