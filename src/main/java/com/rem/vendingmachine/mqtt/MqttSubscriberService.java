package com.rem.vendingmachine.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rem.vendingmachine.dao.OrderItemMapper;
import com.rem.vendingmachine.dao.OrderMapper;
import com.rem.vendingmachine.dao.UserMapper;
import com.rem.vendingmachine.dao.VendingMachineProductMapper;
import com.rem.vendingmachine.model.Order;
import com.rem.vendingmachine.model.OrderItem;
import com.rem.vendingmachine.model.VendingMachine;
import com.rem.vendingmachine.model.VendingMachineProduct;
import com.rem.vendingmachine.service.OrderService;
import com.rem.vendingmachine.service.VendingMachineProductService;
import com.rem.vendingmachine.service.VendingMachineService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MqttSubscriberService {

    @Autowired
    private MqttClient mqttClient;  // 服务端 MQTT 客户端

    @Autowired
    private VendingMachineService vendingMachineService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VendingMachineProductService vendingMachineProductService;

    @Autowired
    private  MqttPublisherService mqttPublisherService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VendingMachineProductMapper vendingMachineProductMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, DeviceSnapshot> deviceSnapshots = new ConcurrentHashMap<>();

    private final Map<String, Integer> mqttToDbOrderMapping = new ConcurrentHashMap<>();

    // 内部类，用于存储设备快照数据
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceSnapshot {
        private String machineId;
        private double temperature = 0.0;
        private int status = 0; // 默认离线
        private long lastHeartbeat = 0;
        private long lastStateUpdate = 0;
        private String alerts = "无";

        @Override
        public String toString() {
            return "DeviceSnapshot{" +
                    "machineId='" + machineId + '\'' +
                    ", temperature=" + temperature +
                    ", status=" + status +
                    ", lastHeartbeat=" + lastHeartbeat +
                    ", lastStateUpdate=" + lastStateUpdate +
                    ", alerts='" + alerts + '\'' +
                    '}';
        }
    }

    // 内存中的 MQTT 消息存储
    private final Map<String, List<MqttLog>> messageLogs = new ConcurrentHashMap<>();

    // 构造函数初始化存储结构
    public MqttSubscriberService() {
        messageLogs.put("heartbeat", new ArrayList<>());
        messageLogs.put("state", new ArrayList<>());
        messageLogs.put("order", new ArrayList<>());
        messageLogs.put("processed_order", new ArrayList<>()); // 添加处理后的订单主题
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
            } else if (topic.startsWith("vendingmachine/order/processed/")) {
                // 处理包含真实ID的订单消息
                handleProcessedOrder(topic, payload, log);
            } else if (topic.startsWith("vendingmachine/order/")) {
                // 原始订单消息
                handleOrder(topic, payload, log);
            } else if (topic.startsWith("vendingmachine/inventory/request/")) {
                handleInventoryRequest(topic, payload, log);
            } else {
                System.err.println("未知主题消息：" + topic);
            }
        } catch (Exception e) {
            System.err.println("处理消息时发生错误：" + e.getMessage());
        }
    }

    /**
     * 处理包含真实ID的订单消息
     */
    private void handleProcessedOrder(String topic, String payload, MqttLog log) {
        try {
            // 将处理后的订单消息存储到专门的日志中
            synchronized (messageLogs) {
                messageLogs.get("processed_order").add(log);
            }
            System.out.println("处理后的订单消息已存储 - 主题: " + topic);
        } catch (Exception e) {
            System.err.println("处理处理后的订单消息失败：" + e.getMessage());
        }
    }

    // --- 处理库存请求 ---
    private void handleInventoryRequest(String topic, String payload, MqttLog log) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String machineId = rootNode.path("machineId").asText();
            System.out.println("收到设备 " + machineId + " 的库存请求。");

            // 查询数据库获取该设备的商品和库存
            List<VendingMachineProduct> products = vendingMachineProductService.getProductsByMachineId(Integer.parseInt(machineId));

            // 构造响应载荷
            Map<String, Object> response = new HashMap<>();
            response.put("machineId", machineId);

            List<Map<String, Object>> productList = products.stream().map(p -> {
                Map<String, Object> item = new HashMap<>();
                item.put("productId", p.getProductId());
                item.put("productName", p.getProductName());
                item.put("stock", p.getStock());
                return item;
            }).collect(Collectors.toList());

            response.put("products", productList);

            String responsePayload = objectMapper.writeValueAsString(response);
            String responseTopic = "vendingmachine/inventory/response/" + machineId;

            // --- 新增/修改：强制打印设备1的响应，方便快速检查 ---
            if ("1".equals(machineId)) {
                System.out.println(">>>>>>>>>>>>>>> [快速检查 - 设备1库存响应] <<<<<<<<<<<<<<<");
                System.out.println(">>> 发往主题: " + responseTopic);
                System.out.println(">>> 响应内容: " + responsePayload);
                System.out.println(">>>>>>>>>>>>>>> [快速检查结束] <<<<<<<<<<<<<<<");
            }
            // --- 新增/修改结束 ---

            // 使用 MqttPublisherService 发送响应
            mqttPublisherService.publish(responseTopic, responsePayload);
            System.out.println("已向设备 " + machineId + " 发送库存响应。");

        } catch (Exception e) {
            System.err.println("处理库存请求失败: " + e.getMessage() + ", Payload: " + payload);
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

            // --- 新增/修改：更新设备快照 ---
            deviceSnapshots.computeIfAbsent(String.valueOf(machineId), k -> {
                DeviceSnapshot snapshot = new DeviceSnapshot();
                snapshot.setMachineId(k);
                return snapshot;
            }).setLastHeartbeat(log.getTimestamp());

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
            JsonNode root = objectMapper.readTree(payload);
            String machineId = root.get("machineId").asText(); // 从 payload 获取 ID 更安全
            double temperature = root.get("temperature").asDouble();
            int status = root.get("status").asInt();
            String alerts = root.has("alerts") ? root.get("alerts").asText() : "无"; // 获取告警

            if (!"无".equals(alerts)) {
                System.out.println("[后端收到状态] 设备 " + machineId + " 上报告警: " + alerts);
            }

            // --- 更新设备快照 ---
            deviceSnapshots.computeIfAbsent(machineId, k -> {
                DeviceSnapshot snapshot = new DeviceSnapshot();
                snapshot.setMachineId(k);
                return snapshot;
            });
            DeviceSnapshot snapshot = deviceSnapshots.get(machineId);
            snapshot.setTemperature(temperature);
            snapshot.setStatus(status);
            snapshot.setAlerts(alerts);


            vendingMachineService.updateVendingMachineStatus(Integer.parseInt(machineId), temperature, status);
            System.out.println("状态已更新 - 设备ID: " + machineId + ", 温度: " + temperature + ", 状态: " + status + ", 告警: " + alerts);

            synchronized (messageLogs) {
                messageLogs.get("state").add(log);
            }
        } catch (Exception e) {
            System.err.println("处理状态消息失败：" + e.getMessage() + ", Payload: " + payload);
        }
    }

    /**
     * 处理订单消息
     */
    private void handleOrder(String topic, String payload, MqttLog log) {
        try {
            System.out.println("=== 处理MQTT订单 ===");
            System.out.println("收到订单消息 - 主题: " + topic);
            System.out.println("收到订单消息 - payload: " + payload);

            // 解析 JSON 消息
            JsonNode root = objectMapper.readTree(payload);

            String originalOrderId = root.get("orderId").asText();
            int userId = root.get("userId").asInt();
            String vendingMachineIdStr = root.get("machineId").asText();
            double totalPrice = root.get("totalPrice").asDouble();

            // 解析商品项目
            List<Map<String, Object>> items = new ArrayList<>();
            if (root.has("items") && root.get("items").isArray()) {
                for (JsonNode itemNode : root.get("items")) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", itemNode.get("productId").asInt());
                    item.put("quantity", itemNode.get("quantity").asInt());
                    items.add(item);
                }
            }

            int vendingMachineId = Integer.parseInt(vendingMachineIdStr);

            System.out.println("解析订单信息 - 原始订单ID: " + originalOrderId + ", 用户ID: " + userId +
                    ", 售货机ID: " + vendingMachineId + ", 总价: " + totalPrice);

            // 处理订单并获取真实ID
            int realOrderId = processOrderAndGetRealId(userId, vendingMachineId, totalPrice, items);

            if (realOrderId > 0) {
                // 构造包含真实ID的新消息并重新发布
                Map<String, Object> newPayload = new HashMap<>();
                newPayload.put("orderId", String.valueOf(realOrderId)); // 使用真实ID
                newPayload.put("userId", userId);
                newPayload.put("machineId", vendingMachineIdStr);
                newPayload.put("totalPrice", totalPrice);
                newPayload.put("items", items);
                newPayload.put("realOrderId", realOrderId);
                newPayload.put("originalOrderId", originalOrderId);
                newPayload.put("timestamp", System.currentTimeMillis());

                String finalPayload = objectMapper.writeValueAsString(newPayload);
                String finalTopic = "vendingmachine/order/processed/" + realOrderId;

                // 重新发布包含真实ID的消息
                mqttPublisherService.publish(finalTopic, finalPayload);
                System.out.println("重新发布包含真实ID的订单消息 - 主题: " + finalTopic);
            }

            System.out.println("订单已完成处理");

            // 存储原始日志
            synchronized (messageLogs) {
                messageLogs.get("order").add(log);
            }
            System.out.println("订单数据已写入日志");
        } catch (Exception e) {
            System.err.println("处理MQTT订单时出错: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("========================");
    }

    /**
     * 处理订单并返回真实数据库ID
     */
    private int processOrderAndGetRealId(int userId, int vendingMachineId, double totalPrice, List<Map<String, Object>> items) {
        try {
            // 1. 检查用户余额
            BigDecimal userBalance = userMapper.getBalanceByUserId(userId);
            if (userBalance == null) {
                System.err.println("【错误】用户余额查询失败");
                return -1;
            }

            BigDecimal totalAmount = BigDecimal.valueOf(totalPrice);
            if (userBalance.compareTo(totalAmount) < 0) {
                System.err.println("【错误】用户余额不足");
                return -1;
            }

            // 2. 验证所有商品库存
            for (Map<String, Object> item : items) {
                int productId = (Integer) item.get("productId");
                int quantity = (Integer) item.get("quantity");

                VendingMachineProduct vmProduct = vendingMachineProductMapper.selectVendingMachineProduct(vendingMachineId, productId);
                if (vmProduct == null) {
                    System.err.println("【错误】售货机 " + vendingMachineId + " 中不存在商品 " + productId);
                    return -1;
                }

                if (vmProduct.getStock() < quantity) {
                    System.err.println("【错误】商品 " + productId + " 库存不足，需要 " + quantity + "，现有 " + vmProduct.getStock());
                    return -1;
                }
            }

            // 3. 创建主订单
            Order order = new Order();
            order.setUserId(userId);
            order.setTotalPrice(totalAmount);
            order.setCreateTime(LocalDateTime.now());

            orderMapper.insertOrder(order);
            int realOrderId = order.getId();
            System.out.println("【调试】订单创建成功，真实数据库ID: " + realOrderId);

            // 4. 处理订单项和库存扣减
            for (Map<String, Object> item : items) {
                int productId = (Integer) item.get("productId");
                int quantity = (Integer) item.get("quantity");

                VendingMachineProduct vmProduct = vendingMachineProductMapper.selectVendingMachineProduct(vendingMachineId, productId);

                // 扣减库存
                vendingMachineProductMapper.updateVendingMachineProductStock(vendingMachineId, productId, -quantity, quantity);

                // 插入订单项
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(realOrderId);
                orderItem.setProductId(productId);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(vmProduct.getPrice());
                orderItem.setSubtotal(vmProduct.getPrice().multiply(BigDecimal.valueOf(quantity)));
                orderItemMapper.insertOrderItem(orderItem);
            }

            // 5. 更新用户余额
            BigDecimal newBalance = userBalance.subtract(totalAmount);
            userMapper.updateBalanceByUserId(userId, newBalance);

            System.out.println("【成功】MQTT订单处理完成，真实订单ID: " + realOrderId);

            return realOrderId;

        } catch (Exception e) {
            System.err.println("【错误】处理MQTT订单时发生异常: " + e.getMessage());
            e.printStackTrace();
            return -1;
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

    public Collection<DeviceSnapshot> getAllDeviceSnapshots() {
        return this.deviceSnapshots.values();
    }

    /**
     * 获取所有设备的快照 (合并数据库中的所有设备和有活动记录的设备快照)
     * 改进：对于数据库中有但快照中没有的设备，使用数据库的实时数据进行初始化
     * @return 包含所有设备信息的集合
     */
    public Collection<DeviceSnapshot> getAllDeviceSnapshotsComprehensive() {
        // 1. 获取数据库中所有的售货机 (这应该总是最新的数据库状态)
        List<VendingMachine> allMachines = vendingMachineService.getAllVendingMachines();

        // 2. 获取当前有活动记录的快照 (来自 MQTT 消息)
        Map<String, DeviceSnapshot> activeSnapshots = this.deviceSnapshots; // ConcurrentHashMap

        // 3. 合并信息
        Map<String, DeviceSnapshot> comprehensiveMap = new HashMap<>();

        // a. 先处理所有有活动记录的快照，放入 Map
        for (Map.Entry<String, DeviceSnapshot> entry : activeSnapshots.entrySet()) {
            comprehensiveMap.put(entry.getKey(), entry.getValue());
        }

        // b. 再遍历数据库中的所有机器，如果 Map 中没有这个 ID，则用数据库实时数据创建一个快照
        for (VendingMachine machine : allMachines) {
            String machineIdStr = String.valueOf(machine.getId());
            if (!comprehensiveMap.containsKey(machineIdStr)) {
                // 如果快照中没有，则用数据库的实时数据初始化一个快照
                DeviceSnapshot snapshot = new DeviceSnapshot();
                snapshot.setMachineId(machineIdStr);
                // 使用数据库中的当前温度和状态
                snapshot.setTemperature(machine.getTemperature() != null ? machine.getTemperature() : 0.0);
                Integer status = machine.getStatus();   // 先拿到包装类型
                snapshot.setStatus(status != null ? status : 0);
                // 由于没有活动记录，设置为非常久远的过去或特殊标记
                snapshot.setLastHeartbeat(0); // 或者可以设置一个非常老的时间戳
                snapshot.setLastStateUpdate(0);
                // 由于没有告警信息，可以设置为 "无实时告警信息" 或其他提示
                snapshot.setAlerts("暂无实时告警信息"); // <--- 修改提示信息
                comprehensiveMap.put(machineIdStr, snapshot);
            }
            // 如果 Map 中已经有 (来自 activeSnapshots)，则跳过，保留 MQTT 的动态数据
        }

        return comprehensiveMap.values();
    }

    public List<MqttLog> getProcessedOrders() {
        synchronized (messageLogs) {
            return new ArrayList<>(messageLogs.getOrDefault("processed_order", new ArrayList<>()));
        }
    }

    /**
     * 获取所有订单消息（包括原始和处理后的）
     */
    public List<MqttLog> getAllOrderLogs() {
        List<MqttLog> allOrders = new ArrayList<>();
        synchronized (messageLogs) {
            allOrders.addAll(messageLogs.getOrDefault("order", new ArrayList<>()));
            allOrders.addAll(messageLogs.getOrDefault("processed_order", new ArrayList<>()));
        }
        // 按时间排序
        allOrders.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return allOrders;
    }
}