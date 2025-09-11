package com.rem.vendingmachine.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulatedDeviceClient {

    private static final String BROKER_URL = "tcp://8.148.64.50:1883";
    private MqttClient client;
    private String machineId;

    // --- 通过 MQTT 从服务端获取内部的商品库存 ---
    private final Map<Integer, Integer> inventory = new HashMap<>();

    // --- 内部状态变量 ---
    private double currentTemperature = 25.0; // 初始化一个默认温度
    private int currentStatus = 1;           // 初始化为在线状态
    private final ObjectMapper objectMapper = new ObjectMapper(); // 用于解析JSON命令

    // --- 新增：用于模拟订单的商品项 ---
    // 使用 Map<productId, quantity> 来模拟购物车
    private final Map<Integer, Integer> cart = new HashMap<>();
    // --- 新增结束 ---

    public SimulatedDeviceClient(String machineId) {
        try {
            this.machineId = machineId;
            String clientId = "SimulatedDevice_" + machineId + "_" + UUID.randomUUID();
            client = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            client.connect(options);

            System.out.println("模拟设备已连接，设备ID: " + machineId + ", 初始温度: " + currentTemperature + "℃, 初始状态: " + currentStatus);
        } catch (MqttException e) {
            System.err.println("模拟设备连接失败：" + e.getMessage());
        }
    }

    public void publish(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            client.publish(topic, message);
            System.out.println("消息已发布 - 主题: " + topic + ", 内容: " + payload);
        } catch (MqttException e) {
            System.err.println("消息发布失败：" + e.getMessage());
        }
    }

    public void subscribe(String topic) {
        try {
            // --- 修改：订阅回调函数 ---
            client.subscribe(topic, (receivedTopic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("收到命令消息 - 主题: " + receivedTopic + ", 内容: " + payload);
                // 调用处理命令的方法
                // --- 修改：根据不同的主题调用不同处理方法 ---
                if (receivedTopic.startsWith("vendingmachine/inventory/response/")) {
                    handleInventoryResponse(payload);
                } else if (receivedTopic.startsWith("vendingmachine/command/")) {
                    processCommand(payload);
                    // --- 新增：处理来自前端的购物车处理请求 ---
                } else if (receivedTopic.startsWith("vendingmachine/frontend/order/request/")) {
                    handleFrontendOrderRequest(payload);
                    // --- 新增结束 ---
                } else {
                    System.out.println("【警告】收到未知主题消息: " + receivedTopic);
                }
            });
            System.out.println("成功订阅主题：" + topic);
        } catch (MqttException e) {
            System.err.println("订阅主题失败：" + topic + "，错误：" + e.getMessage());
        }
    }

    /**
     * 向服务端请求当前设备的库存信息
     */
    public void requestInventory() {
        String topic = "vendingmachine/inventory/request/" + this.machineId;
        String payload = String.format("{\"machineId\": \"%s\", \"timestamp\": %d}", this.machineId, System.currentTimeMillis());
        this.publish(topic, payload);
        System.out.println("【请求】已向服务端请求库存信息...");
    }

    /**
     * 处理从服务端接收到的库存响应
     * @param payload JSON 格式的库存数据
     */
    private void handleInventoryResponse(String payload) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String respMachineId = rootNode.path("machineId").asText();
            if (!this.machineId.equals(respMachineId)) {
                System.out.println("【警告】收到的库存响应不属于本设备 (期待: " + this.machineId + ", 实际: " + respMachineId + ")");
                return;
            }

            JsonNode productsNode = rootNode.path("products");
            if (productsNode.isArray()) {
                // --- 关键修改：用服务端返回的数据更新本地 inventory ---
                Map<Integer, Integer> newInventory = new HashMap<>();
                for (JsonNode productNode : productsNode) {
                    int productId = productNode.path("productId").asInt();
                    int stock = productNode.path("stock").asInt();
                    newInventory.put(productId, stock);
                }
                this.inventory.clear();
                this.inventory.putAll(newInventory);
                System.out.println("【库存更新】已从服务端接收并更新库存: " + this.inventory);
                // --- 关键修改结束 ---
            } else {
                System.out.println("【警告】库存响应中 'products' 字段不是数组或不存在。");
            }
        } catch (Exception e) {
            System.err.println("【错误】处理库存响应时失败: " + e.getMessage() + ", 原始载荷: " + payload);
        }
    }

    /**
     * 处理从服务端接收到的命令
     * @param payload 命令内容 (JSON 格式)
     */
    private void processCommand(String payload) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String command = rootNode.path("command").asText();

            switch (command.toUpperCase()) {
                case "CHANGE_TEMPERATURE":
                    double newTemp = rootNode.path("value").asDouble(currentTemperature);
                    this.currentTemperature = newTemp;
                    System.out.println("【命令执行】温度已更新至: " + this.currentTemperature + "℃");
                    break;
                case "SET_STATUS":
                    int newStatus = rootNode.path("value").asInt(this.currentStatus);
                    // --- 根据状态做出反应 ---
                    if (newStatus == 0) {
                        System.out.println("【命令执行】设备状态设置为离线 (0)，将无法处理购买请求。");
                    } else if (newStatus == 1) {
                        System.out.println("【命令执行】设备状态设置为在线 (1)，恢复服务。");
                    }
                    this.currentStatus = newStatus;
                    System.out.println("【命令执行】状态已更新至: " + this.currentStatus);

                    break;
                case "DISPENSE_PRODUCT":
                    if (this.currentStatus == 0) {
                        System.out.println("【警告】设备处于离线状态 (0)，无法执行售货请求！");
                        // 可以选择上报一个特殊的 "SALE_DENIED" 事件？
                        return; // 阻止操作
                    }
                    JsonNode productNode = rootNode.path("productId");
                    JsonNode quantityNode = rootNode.path("quantity");
                    if (!productNode.isMissingNode() && !quantityNode.isMissingNode()) {
                        int productId = productNode.asInt();
                        int quantity = quantityNode.asInt();
                        int currentStock = inventory.getOrDefault(productId, 0);
                        if (currentStock >= quantity) {
                            int newStock = currentStock - quantity;
                            inventory.put(productId, newStock); // 关键修改：真正更新了库存！
                            System.out.println("【售货】商品 " + productId + " 库存扣减 " + quantity + ", 剩余: " + newStock);
                        } else {
                            System.out.println("【售货失败】商品 " + productId + " 库存不足！当前: " + currentStock + ", 需要: " + quantity);
                        }
                    } else {
                        System.out.println("【警告】DISPENSE_PRODUCT 命令缺少 productId 或 quantity 参数");
                    }
                    break;
                default:
                    System.out.println("【警告】收到未知命令: " + command);
                    break;
            }
        } catch (Exception e) {
            System.err.println("【命令执行】处理命令时出错: " + e.getMessage() + ", 原始命令内容: " + payload);
        }
    }

    // --- 新增方法：处理来自前端的购物车处理请求 ---
    /**
     * 处理来自前端的订单处理请求。
     * 前端会发送包含用户ID和购物车信息的指令。
     * 设备会模拟出货（更新内部库存），然后生成并上报包含完整商品明细的订单。
     * @param payload 包含 orderId, userId, cart (items) 的JSON字符串
     */
    private void handleFrontendOrderRequest(String payload) {
        try {
            System.out.println("[设备 " + this.machineId + "] 收到来自前端的订单请求: " + payload);
            JsonNode root = objectMapper.readTree(payload);

            String frontendOrderId = root.path("orderId").asText(); // 从前端获取的订单ID
            int userId = root.path("userId").asInt();
            JsonNode cartNode = root.path("cart");

            if (cartNode == null || !cartNode.isArray()) {
                System.err.println("[设备 " + this.machineId + "] 订单请求缺少有效的购物车信息。");
                // 可考虑通过MQTT返回错误信息给前端
                return;
            }

            // 将前端的 cartNode 转换为 items 列表，用于上报
            List<Map<String, Object>> itemsForReport = new ArrayList<>();
            boolean inventorySufficient = true; // 标记库存是否足够

            for (JsonNode itemNode : cartNode) {
                int productId = itemNode.path("productId").asInt();
                int quantity = itemNode.path("quantity").asInt();

                // 检查本地库存是否足够
                int currentStock = this.inventory.getOrDefault(productId, 0);
                if (currentStock < quantity) {
                    System.err.println("[设备 " + this.machineId + "] 商品 " + productId + " 库存不足 (需要: " + quantity + ", 现有: " + currentStock + ").");
                    inventorySufficient = false;
                    // 可考虑记录哪个商品不足，或返回给前端
                    // 为简化，我们选择库存不足时直接返回，不处理
                    break;
                }

                Map<String, Object> item = new HashMap<>();
                item.put("productId", productId);
                item.put("quantity", quantity);
                itemsForReport.add(item);
            }

            // 如果所有商品库存都足够
            if (inventorySufficient && !itemsForReport.isEmpty()) {
                // --- 关键模拟：模拟出货动作 ---
                // 更新设备端的库存
                System.out.println("[设备 " + this.machineId + "] 开始模拟出货...");
                for (Map<String, Object> item : itemsForReport) {
                    int productId = (Integer) item.get("productId");
                    int quantity = (Integer) item.get("quantity");
                    int currentStock = this.inventory.getOrDefault(productId, 0);
                    int newStock = currentStock - quantity;
                    this.inventory.put(productId, newStock); // 更新内部库存
                    System.out.println("[设备 " + this.machineId + "] 模拟出货: 商品 " + productId + " 数量 " + quantity + ", 剩余库存: " + newStock);
                }
                System.out.println("[设备 " + this.machineId + "] 模拟出货完成。");
                // --- 模拟出货结束 ---

                // --- 关键操作：上报包含完整商品明细的订单 ---
                // 调用修改后的 reportOrder 方法
                this.reportOrder(frontendOrderId, userId, itemsForReport);
                System.out.println("[设备 " + this.machineId + "] 已根据前端请求，模拟出货并上报完整订单。");
            } else {
                System.err.println("[设备 " + this.machineId + "] 因库存不足或购物车为空，未能处理前端订单请求。");
                // 可考虑通过MQTT返回错误信息给前端
            }

        } catch (Exception e) {
            System.err.println("[设备 " + this.machineId + "] 处理前端订单请求失败: " + e.getMessage());
            e.printStackTrace();
            // 可考虑通过MQTT返回错误信息给前端
        }
    }
    // --- 新增方法结束 ---

    /**
     * 模拟订单上报 (修改后版本)
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param items 商品列表 (List<Map<productId, quantity>>)
     */
    public void reportOrder(String orderId, int userId, List<Map<String, Object>> items) {
        String topic = "vendingmachine/order/" + orderId;

        // 构造包含商品明细的完整 payload
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("orderId", orderId);
        payloadMap.put("userId", userId);
        payloadMap.put("machineId", this.machineId);
        // 注意：不直接传递总价，让服务端根据商品明细重新计算，更安全
        // 如果必须传递，可以计算，但现在我们不传
        payloadMap.put("items", items); // 关键：传递购买商品明细列表

        try {
            String payload = objectMapper.writeValueAsString(payloadMap);
            publish(topic, payload);
            System.out.println("[设备 " + this.machineId + "] 订单已上报 - ID: " + orderId + ", 主题: " + topic);
        } catch (Exception e) {
            System.err.println("[设备 " + this.machineId + "] 构造订单Payload失败: " + e.getMessage());
        }
    }

    /**
     * 构造状态上报的 Payload。
     * 注意：告警信息依赖于本地 inventory 状态。
     * 建议在调用此方法前，通过 requestInventory() 触发库存同步，
     * 以确保告警信息的准确性。
     * 定时任务中已包含在状态上报前调用 requestInventory()。
     * @return JSON 格式的 payload 字符串
     */
    public String getPayloadForStateReport() {
        // --- 修改开始：添加日志，表明正在生成状态报告 ---
        System.out.println("[设备 " + this.machineId + "] 正在生成状态报告...");
        // --- 修改结束 ---

        StringBuilder alertMessage = new StringBuilder();
        // 遍历当前本地维护的库存
        for (Map.Entry<Integer, Integer> entry : inventory.entrySet()) {
            if (entry.getValue() == 0) { // 检查库存为 0 的商品
                if (!alertMessage.isEmpty()) {
                    alertMessage.append("; ");
                }
                // --- 修改：明确指出是根据本地库存判断 ---
                alertMessage.append("商品 ID ").append(entry.getKey()).append(" (本地缓存) 库存为 0");
                // --- 修改结束 ---
            }
        }

        // 构造 JSON Payload
        String alerts = !alertMessage.isEmpty() ? alertMessage.toString() : "无";

        // --- 修改：记录生成的告警内容 ---
        System.out.println("[设备 " + this.machineId + "] 生成的告警信息: " + alerts);
        // --- 修改结束 ---

        // 使用 Map 和 ObjectMapper 来构建更复杂的 JSON
        Map<String, Object> stateData = new HashMap<>();
        stateData.put("machineId", machineId);
        stateData.put("temperature", currentTemperature);
        stateData.put("status", currentStatus);
        stateData.put("alerts", alerts);

        try {
            return objectMapper.writeValueAsString(stateData);
        } catch (Exception e) {
            System.err.println("构建状态上报 JSON 失败: " + e.getMessage());
            // 失败时回退
            return String.format("{\"machineId\": \"%s\", \"temperature\": %.2f, \"status\": %d, \"alerts\": \"%s\"}",
                    machineId, currentTemperature, currentStatus, "构建JSON失败");
        }
    }

    /**
     * 模拟在售货机上进行一次购买。
     * 随机选择购物车中的1-3种商品，每个商品1-2件。
     * 基于 inventory 中的商品和库存进行选择（避免买没有的）。
     * 然后调用 reportOrder 完成上报。
     */
    public void simulatePurchaseAndReportOrder(int userId) {
        if (this.currentStatus == 0) {
            System.out.println("[设备 " + this.machineId + "] 处于离线状态，无法模拟购买。");
            return;
        }

        // 清空上次购物车
        this.cart.clear();

        // 检查是否有可购买的商品
        List<Integer> availableProducts = this.inventory.entrySet().stream()
                .filter(e -> e.getValue() > 0) // 库存大于0
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (availableProducts.isEmpty()) {
            System.out.println("[设备 " + this.machineId + "] 没有可购买的商品（库存为0）。");
            return;
        }

        // 随机选择 1 - min(3, availableProducts.size()) 种商品
        Random random = new Random();
        int numItemsToBuy = random.nextInt(Math.min(3, availableProducts.size())) + 1;
        Collections.shuffle(availableProducts);

        List<Map<String, Object>> itemsForPayload = new ArrayList<>();

        for (int i = 0; i < numItemsToBuy; i++) {
            int productId = availableProducts.get(i);
            int quantity = random.nextInt(2) + 1; // 1 or 2
            int currentStock = this.inventory.getOrDefault(productId, 0);
            if (quantity > currentStock) {
                quantity = currentStock; // 不买超过库存的
            }
            if (quantity > 0) {
                // 将商品添加到本次模拟购买的列表中
                Map<String, Object> item = new HashMap<>();
                item.put("productId", productId);
                item.put("quantity", quantity);
                itemsForPayload.add(item);
                System.out.println("[设备 " + this.machineId + "] 模拟购物车: 添加商品 " + productId + ", 数量 " + quantity);
            }
        }

        if (itemsForPayload.isEmpty()) {
            System.out.println("[设备 " + this.machineId + "] 未能成功添加任何商品到模拟购物车。");
            return;
        }

        // 生成一个简单的订单ID
        String orderId = "SIM_ORDER_" + this.machineId + "_" + System.currentTimeMillis();

        // 使用修改后的 reportOrder 方法
        this.reportOrder(orderId, userId, itemsForPayload);
        System.out.println("[设备 " + this.machineId + "] 模拟购买完成，已上报订单: " + orderId);
    }
    // --- 新增方法结束 ---
}
