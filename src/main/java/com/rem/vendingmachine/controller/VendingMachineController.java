package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.model.Product;
import com.rem.vendingmachine.model.VendingMachine;
import com.rem.vendingmachine.mqtt.MqttPublisherService;
import com.rem.vendingmachine.service.ProductService;
import com.rem.vendingmachine.service.VendingMachineServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vending-machine")
public class VendingMachineController {

    @Autowired
    private VendingMachineServiceImpl vendingMachineService;

    @Autowired
    private MqttPublisherService mqttPublisherService;

    @Autowired
    private ProductService productService;

    /**
     * 添加无人售货机接口
     * @param vendingMachine 无人售货机
     * @return 添加无人售货机结果
     */
    @PostMapping("/add")
    public String addVendingMachine(@RequestBody VendingMachine vendingMachine) {
        if (vendingMachineService.addVendingMachine(vendingMachine)) {
            return "无人售货机添加成功！";
        }
        return "无人售货机添加失败！";
    }

    /**
     * 更新无人售货机接口
     * @param vendingMachine 无人售货机
     * @return 更新无人售货机结果
     */
    @PutMapping("/update")
    public String updateVendingMachine(@RequestBody VendingMachine vendingMachine) {
        if (vendingMachineService.updateVendingMachine(vendingMachine)) {
            return "无人售货机更新成功！";
        }
        return "无人售货机更新失败！";
    }

    /**
     * 删除无人售货机接口
     * @param id 删除无人售货机的id
     * @return 删除无人售货机结果
     */
    @DeleteMapping("/delete/{id}")
    public String deleteVendingMachine(@PathVariable int id) {
        if (vendingMachineService.deleteVendingMachineById(id)) {
            return "无人售货机删除成功！";
        }
        return "无人售货机删除失败！";
    }

    /**
     * 查询所有无人售货机接口
     * @return 查询所有无人售货机结果
     */
    @GetMapping("/all")
    public List<VendingMachine> getAllVendingMachines() {
        return vendingMachineService.getAllVendingMachines();
    }

    /**
     * 查询某无人售货机接口
     * @param id 查询无人售货机的id
     * @return 查询某无人售货机结果
     */
    @GetMapping("/{id}")
    public VendingMachine getVendingMachineById(@PathVariable int id) {
        VendingMachine vendingMachine = vendingMachineService.getVendingMachineById(id);
        if (vendingMachine == null) {
            throw new RuntimeException("id为"+id+"的无人售货机未找到");
        }
        return vendingMachine;
    }

    /**
     * 查询某无人售货机接口
     * @param status 查询某无人售货机的状态
     * @return 查询某无人售货机结果
     */
    // 根据状态查询售货机
    @GetMapping("/status/{status}")
    public List<VendingMachine> getVendingMachinesByStatus(@PathVariable int status) {
        return vendingMachineService.getVendingMachinesByStatus(status);
    }

    /**
     * mqtt发布命令接口
     * @param machineCode 发布命令所需topic部分
     * @param command 发布的命令
     * @return 发布命令结果
     */
    //发命令
    @PostMapping("/mqtt/command/{machineCode}")
    public String sendCommandToVendingMachine(@PathVariable String machineCode, @RequestParam String command) {
        String topic = "vendingmachine/command/" + machineCode;
        String payload = String.format("{\"command\":\"%s\",\"timestamp\":\"%d\"}", command, System.currentTimeMillis());
        mqttPublisherService.publish(topic, payload);
        return "命令已发送！主题：" + topic + "，命令：" + command;
    }

    @GetMapping("/{id}/products")
    public List<Product> getProductsByVendingMachineId(@PathVariable int id) {
        return productService.listAllProducts(); // 返回所有商品
    }

    // 临时测试接口
    @GetMapping("/debug/all-machine-ids")
    public List<Integer> getAllMachineIds() {
        List<VendingMachine> machines = vendingMachineService.getAllVendingMachines();
        return machines.stream().map(VendingMachine::getId).collect(Collectors.toList());
    }

    @PostMapping("/mqtt/order")
    public String sendOrderToMQTT(@RequestBody Map<String, Object> orderData) {
        try {
            // 提取订单信息
            String orderId = (String) orderData.get("orderId");
            int userId = (Integer) orderData.get("userId");
            String machineId = (String) orderData.get("machineId");
            double totalPrice = ((Number) orderData.get("totalPrice")).doubleValue();

            // 构造MQTT消息负载
            String payload = String.format(
                    "{\"orderId\": \"%s\", \"userId\": %d, \"machineId\": \"%s\", \"totalPrice\": %.2f}",
                    orderId, userId, machineId, totalPrice
            );

            // 发布到MQTT主题
            String topic = "vendingmachine/order/" + orderId;
            mqttPublisherService.publish(topic, payload);

            return "订单已通过MQTT上报: " + orderId;
        } catch (Exception e) {
            return "订单上报失败: " + e.getMessage();
        }
    }
}
