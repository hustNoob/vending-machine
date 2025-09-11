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
import java.sql.Connection;
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
     * 处理来自设备的完整订单上报消息
     * 接收由设备模拟出货后上报的包含完整商品明细的订单，并进行处理。
     * 这是新的核心处理流程。
     * @param topic     MQTT 主题
     * @param payload   MQTT 消息内容 (JSON)
     * @param log       原始日志对象
     */
    private void handleOrder(String topic, String payload, MqttLog log) {
        System.out.println("=== 开始处理设备上报的【完整】订单 ===");
        System.out.println("收到订单消息 - 主题: " + topic);
        System.out.println("收到订单消息 - payload: " + payload);

        try {
            // 1. 解析 JSON 消息
            JsonNode root = objectMapper.readTree(payload);

            String originalOrderId = root.path("orderId").asText();
            int userId = root.path("userId").asInt();
            String vendingMachineIdStr = root.path("machineId").asText();

            // 2. --- 核心修改：解析并验证商品列表 ---
            List<Map<String, Object>> items = new ArrayList<>();
            JsonNode itemsNode = root.path("items");
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", itemNode.path("productId").asInt());
                    item.put("quantity", itemNode.path("quantity").asInt());
                    // 可根据需要添加商品名称、单价等，如果设备上报的话
                    items.add(item);
                }
            } else {
                System.err.println("[服务端] 订单消息缺少有效的 'items' 列表, Payload: " + payload);
                // 可以记录错误日志或发送MQTT Ack错误消息
                return;
            }

            if (items.isEmpty()) {
                System.err.println("[服务端] 订单消息中的 'items' 列表为空, Payload: " + payload);
                return;
            }
            // --- 核心修改结束 ---

            int vendingMachineId = Integer.parseInt(vendingMachineIdStr);

            System.out.println("解析订单信息 - 原始订单ID: " + originalOrderId + ", 用户ID: " + userId +
                    ", 售货机ID: " + vendingMachineId + ", 商品项数: " + items.size());

            // 3. --- 调用服务层的处理核心逻辑 ---
            //    注意：我们不再需要原来的 processOrderAndGetRealId，
            //    因为其功能已被整合进 processOrderCore (将在下一步实现)
            boolean success = orderService.processOrderFromMQTT(
                    originalOrderId, // tempOrderId
                    userId,
                    vendingMachineId,
                    0.0, // totalPrice, 由 processOrderCore 内部计算，此处传0或忽略
                    items // 包含完整商品明细的列表
            );

            if (success) {
                System.out.println("[服务端] 订单 " + originalOrderId + " 处理成功。");
                // 可选：发布一个确认消息给设备
                // String ackTopic = "vendingmachine/order/ack/" + originalOrderId;
                // mqttPublisherService.publish(ackTopic, "{\"status\":\"success\", \"orderId\":\"" + originalOrderId + "\"}");
            } else {
                System.err.println("[服务端] 订单 " + originalOrderId + " 处理失败。");
                // 可选：发布一个失败消息给设备
                // String ackTopic = "vendingmachine/order/ack/" + originalOrderId;
                // mqttPublisherService.publish(ackTopic, "{\"status\":\"failed\", \"orderId\":\"" + originalOrderId + "\", \"reason\":\"Processing failed on server.\"}");
            }

            System.out.println("[服务端] 订单 " + originalOrderId + " 已完成处理流程。");

            // 4. 存储原始日志 (保持不变)
            synchronized (messageLogs) {
                messageLogs.get("order").add(log);
            }
            System.out.println("[服务端] 订单数据已写入日志");

        } catch (Exception e) {
            System.err.println("[服务端] 处理MQTT订单时出错: " + e.getMessage());
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

    /**
     * 处理订单的核心逻辑 (重构后)
     * 根据来自设备或API的用户、机器、商品明细信息，完成订单创建、库存扣减、余额扣除。
     * @param userId 用户ID
     * @param vendingMachineId 售货机ID
     * @param items 商品明细列表 (包含productId, quantity)
     * @return 是否处理成功
     */
    public boolean processOrderCore(int userId, int vendingMachineId, List<Map<String, Object>> items) {
        System.out.println("=== processOrderCore 开始执行 ===");
        Connection conn = null;
        boolean success = false;
        try {
            // 1. 开启事务 (如果需要，虽然Spring Service层应该已经管理了)
            // conn = dataSource.getConnection(); // 示例，实际由Spring管理
            // conn.setAutoCommit(false);

            // 2. 检查用户余额
            BigDecimal userBalance = userMapper.getBalanceByUserId(userId);
            if (userBalance == null) {
                System.err.println("【错误】用户(ID: " + userId + ") 余额查询失败或用户不存在");
                return false;
            }

            BigDecimal calculatedTotal = BigDecimal.ZERO;

            // 3. 预先验证所有商品库存和计算总价
            List<VendingMachineProduct> vmProducts = new ArrayList<>(); // 缓存查询结果
            List<BigDecimal> itemSubtotals = new ArrayList<>();
            for (Map<String, Object> item : items) {
                int productId = (Integer) item.get("productId");
                int quantity = (Integer) item.get("quantity");

                // 查询售货机商品信息
                VendingMachineProduct vmProduct = vendingMachineProductMapper.selectVendingMachineProduct(vendingMachineId, productId);
                if (vmProduct == null) {
                    System.err.println("【错误】售货机 " + vendingMachineId + " 中不存在商品 " + productId);
                    return false; // 或抛异常回滚
                }
                vmProducts.add(vmProduct); // 保存查询结果

                if (vmProduct.getStock() < quantity) {
                    System.err.println("【错误】商品 " + productId + " 库存不足，需要 " + quantity + "，现有 " + vmProduct.getStock());
                    return false; // 或抛异常回滚
                }

                // 计算小计 (必须用数据库里的价格，不能信设备)
                BigDecimal itemSubtotal = vmProduct.getPrice().multiply(BigDecimal.valueOf(quantity));
                calculatedTotal = calculatedTotal.add(itemSubtotal);
                itemSubtotals.add(itemSubtotal); // 保存小计
            }


            // 4. 检查用户余额是否足够支付计算出的总价
            if (userBalance.compareTo(calculatedTotal) < 0) {
                System.err.println("【错误】用户(ID: " + userId + ") 余额不足。余额: " + userBalance + ", 订单总价: " + calculatedTotal);
                return false; // 或抛异常回滚
            }

            // 5. 所有检查通过，开始处理

            // 5a. 创建主订单
            Order order = new Order();
            order.setUserId(userId);
            order.setTotalPrice(calculatedTotal); // 使用服务端计算的总价
            order.setCreateTime(LocalDateTime.now());

            orderMapper.insertOrder(order); // 插入订单，获取数据库真实ID
            int realOrderId = order.getId(); // 获取真实ID
            System.out.println("【调试】订单创建成功，真实数据库ID: " + realOrderId);


            // 5b. 处理订单项和库存扣减
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                VendingMachineProduct vmProduct = vmProducts.get(i); // 使用预查询结果
                BigDecimal itemSubtotal = itemSubtotals.get(i);      // 使用预计算小计

                int productId = (Integer) item.get("productId");
                int quantity = (Integer) item.get("quantity");

                // 5b1. 扣减库存 (注意：传入负数表示减少)
                vendingMachineProductMapper.updateVendingMachineProductStock(vendingMachineId, productId, -quantity, quantity);
                System.out.println("【调试】商品 " + productId + " 库存已扣减 " + quantity);

                // 5b2. 插入订单项
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(realOrderId); // 使用真实的数据库订单ID
                orderItem.setProductId(productId);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(vmProduct.getPrice()); // 使用数据库价格
                orderItem.setSubtotal(itemSubtotal);      // 使用预计算小计
                orderItemMapper.insertOrderItem(orderItem);
                System.out.println("【调试】订单项已插入，商品ID: " + productId + ", 数量: " + quantity);
            }

            // 5c. 更新用户余额
            BigDecimal newBalance = userBalance.subtract(calculatedTotal);
            userMapper.updateBalanceByUserId(userId, newBalance);
            System.out.println("【调试】用户(ID: " + userId + ") 余额已更新为: " + newBalance);

            // 6. 提交事务 (如果手动管理)
            // conn.commit();
            success = true;
            System.out.println("【成功】核心订单处理流程完成。");
            return true;

        } catch (Exception e) {
            System.err.println("【严重错误】处理核心订单流程时发生异常: " + e.getMessage());
            e.printStackTrace();
            // 7. 回滚事务 (如果手动管理)
            // if (conn != null) try { conn.rollback(); } catch (SQLException se) { se.printStackTrace(); }
            return false;
        } finally {
            // 8. 关闭连接 (如果手动管理)
            // if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            System.out.println("=== processOrderCore 执行结束 (成功: " + success + ") ===");
        }
        // Spring 的 @Transactional 注解应该会处理好数据库事务
    }

}