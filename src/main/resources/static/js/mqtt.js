// --- 简化版 mqtt.js ---

// --- 初始化 ---
window.onload = function () {
    setupCommandForms();
    startRealtimeUpdates();
};

// --- 工具函数 ---
function formatTime(timestamp) {
    return new Date(timestamp).toLocaleTimeString();
}

// --- 命令处理 ---
function setupCommandForms() {
    // 通用命令表单
    const commonForm = document.getElementById("mqttCommonCommandForm");
    commonForm.onsubmit = function (event) {
        event.preventDefault();
        const topic = document.getElementById("commonTopic").value.trim();
        const payload = document.getElementById("commonPayload").value.trim();
        if (!topic || !payload) {
            alert("主题和消息内容不能为空！");
            return;
        }
        sendMqttCommand(topic, payload);
    };
}

function sendSetTemperatureCommand() {
    const machineId = document.getElementById("setTempMachineId").value.trim();
    const temp = document.getElementById("setTargetTemperature").value.trim();
    if (!machineId || !temp) {
        alert("售货机 ID 和温度不能为空！");
        return;
    }
    const topic = `vendingmachine/command/${machineId}`;
    const payload = JSON.stringify({ command: "CHANGE_TEMPERATURE", value: parseFloat(temp) });
    sendMqttCommand(topic, payload);
}

function sendSetStatusCommand() {
    const machineId = document.getElementById("setStatusMachineId").value.trim();
    const status = document.getElementById("setStatusValue").value.trim();
    if (!machineId || !status) {
        alert("售货机 ID 和状态不能为空！");
        return;
    }
    const topic = `vendingmachine/command/${machineId}`;
    const payload = JSON.stringify({ command: "SET_STATUS", value: parseInt(status) });
    sendMqttCommand(topic, payload);
}

function sendMqttCommand(topic, payload) {
    fetch('/api/test-publish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ topic, payload }),
    })
        .then(response => {
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.text();
        })
        .then(message => {
            console.log("命令发送成功:", message);
            alert(`命令已发送！\n主题: ${topic}\n内容: ${payload}`);
        })
        .catch(error => {
            console.error("发送命令失败:", error);
            alert("发送命令失败: " + error.message);
        });
}
// --- 命令处理结束 ---


// 修改 startRealtimeUpdates 函数
function startRealtimeUpdates() {
    const interval = 2000; // 每2秒轮询一次
    setInterval(() => {
        // 并行获取设备快照和订单
        Promise.all([
            fetch('/api/mqtt/devices').then(res => res.json()),
            fetch('/api/mqtt/data?type=order').then(res => res.json()) // 这里会获取所有订单消息
        ])
            .then(([devices, orders]) => {
                updateDeviceList(devices);
                updateOrderLogs(orders);
            })
            .catch(error => {
                console.error("轮询获取 MQTT 数据失败:", error);
            });
    }, interval);
}

// 简单的状态码转文本
function getStatusText(code) {
    switch(parseInt(code)) {
        case 0: return '<span class="status-offline">离线</span>';
        case 1: return '<span class="status-online">在线</span>';
        case 2: return '<span class="status-maintenance">维护中</span>';
        default: return '未知';
    }
}

// 简化版 updateOrderLogs 函数，只显示处理完成的订单
let lastProcessedOrderTimestamp = 0;

function updateOrderLogs(logs) {
    // 过滤出新的订单
    const newLogs = logs.filter(log => log.timestamp > lastProcessedOrderTimestamp);
    if (newLogs.length === 0) return;

    // 更新时间戳
    lastProcessedOrderTimestamp = Math.max(...newLogs.map(log => log.timestamp), lastProcessedOrderTimestamp);

    const listElement = document.getElementById("orderList");

    // 如果是初始状态，则清空
    if (listElement.innerHTML.trim() === '<tr><td colspan="4">等待订单...</td></tr>') {
        listElement.innerHTML = '';
    }

    // 只处理包含真实ID的订单消息
    const processedLogs = newLogs.filter(log => {
        try {
            const payload = JSON.parse(log.payload);
            // 只处理包含realOrderId的订单（即处理完成的订单）
            return payload.realOrderId !== undefined;
        } catch (e) {
            return false;
        }
    });

    // 生成新行并追加
    const newRowsHtml = processedLogs.map(log => {
        let payload = {};
        try {
            payload = JSON.parse(log.payload);
        } catch (e) {
            console.error("解析订单日志失败:", e);
            payload = { orderId: '解析失败', userId: 'N/A', totalPrice: 'N/A' };
        }

        // 显示真实订单ID
        let displayOrderId = payload.realOrderId || 'N/A';

        return `
            <tr>
                <td>${displayOrderId}</td>
                <td>${payload.userId || 'N/A'}</td>
                <td>¥${(payload.totalPrice !== undefined ? payload.totalPrice.toFixed(2) : '0.00')}</td>
                <td>${new Date(log.timestamp).toLocaleString()}</td>
            </tr>
        `;
    }).join('');

    if (newRowsHtml) {
        listElement.innerHTML += newRowsHtml;

        // 自动滚动到底部
        const container = listElement.parentElement;
        if (container) {
            container.scrollTop = container.scrollHeight;
        }
    }
}


// --- 更新设备列表 ---
function updateDeviceList(devices) {
    const listElement = document.getElementById("deviceList");

    if (!devices || devices.length === 0) {
        listElement.innerHTML = '<tr><td colspan="5">暂无设备数据</td></tr>';
        return;
    }

    // 按设备ID排序，保证顺序一致
    devices.sort((a, b) => a.machineId.localeCompare(b.machineId));

    listElement.innerHTML = devices.map(device => {
        // 计算心跳时间差，判断是否在线
        const now = Date.now();
        const isOnline = (now - device.lastHeartbeat) < (2 * 60 * 1000); // 2分钟内有心跳算在线
        const statusText = isOnline ? getStatusText(device.status) : '<span class="status-offline">离线</span>';
        const tempDisplay = device.temperature !== undefined ? device.temperature.toFixed(2) + '℃' : 'N/A';
        const heartbeatTime = device.lastHeartbeat ? formatTime(device.lastHeartbeat) : '从未';

        // --- 修改：截断过长的告警信息 ---
        let alertsDisplay = device.alerts || '无';
        const maxAlertLength = 50; // 最大显示字符数
        if (alertsDisplay.length > maxAlertLength) {
            alertsDisplay = alertsDisplay.substring(0, maxAlertLength) + '...';
        }
        // --- 修改结束 ---

        return `
            <tr>
                <td>${device.machineId}</td>
                <td>${statusText}</td>
                <td>${tempDisplay}</td>
                <td>${heartbeatTime}</td>
                <td style="color: #d9534f; word-break: break-all;">${alertsDisplay}</td> <!-- 应用截断后的信息并允许换行 -->
            </tr>
        `;
    }).join('');
}