// 初始化事件处理逻辑
window.onload = function () {
    setupCommandForm(); // 初始化命令下发逻辑
    startRealtimeUpdates(); // 启动实时数据更新逻辑
};

// 命令下发的表单逻辑（无改动）
function setupCommandForm() {
    const form = document.getElementById("mqttCommandForm");
    form.onsubmit = function (event) {
        event.preventDefault();

        const topic = document.getElementById("topic").value.trim();
        const payload = document.getElementById("payload").value.trim();

        if (!topic || !payload) {
            alert("主题和消息内容不能为空！");
            return;
        }

        fetch('/api/test-publish', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ topic, payload }),
        })
            .then(response => response.text())
            .then(message => alert("消息已发送！" + message))
            .catch(error => alert("发送命令失败：" + error));
    };
}

// 实时数据轮询
function startRealtimeUpdates() {
    // 轮询间隔时间（单位：毫秒）
    const interval = 1000; // 每 1 秒轮询一次，可根据需要改成 5000（5 秒）或其他时间

    // 定时器逻辑
    setInterval(() => {
        fetch('/api/mqtt/data?type=heartbeat')
            .then(response => response.json())
            .then(data => updateHeartbeatLogs(data))
            .catch(error => console.error("Failed to load heartbeat logs:", error));

        fetch('/api/mqtt/data?type=state')
            .then(response => response.json())
            .then(data => updateStateLogs(data))
            .catch(error => console.error("Failed to load state logs:", error));

        fetch('/api/mqtt/data?type=order')
            .then(response => response.json())
            .then(data => updateOrderLogs(data))
            .catch(error => console.error("Failed to load order logs:", error));
    }, interval); // 替换定时器间隔时间
}

// 更新心跳数据
function updateHeartbeatLogs(logs) {
    const heartbeatList = document.getElementById("heartbeatList");
    heartbeatList.innerHTML = logs.map(log => `
        <tr>
            <td>${log.topic.split('/')[2]}</td>
            <td>${new Date(log.timestamp).toLocaleString()}</td>
        </tr>`).join('') || '<tr><td colspan="2">暂无数据</td></tr>';
}

// 更新状态数据
function updateStateLogs(logs) {
    const stateList = document.getElementById("stateList");
    stateList.innerHTML = logs.map(log => {
        const payload = JSON.parse(log.payload); // 解析 JSON 内容
        return `
            <tr>
                <td>${log.topic.split('/')[2]}</td>
                <td>${payload.temperature}</td>
                <td>${payload.status}</td>
                <td>${new Date(log.timestamp).toLocaleString()}</td>
            </tr>`;
    }).join('') || '<tr><td colspan="4">暂无数据</td></tr>';
}

// 更新订单数据
function updateOrderLogs(logs) {
    const orderList = document.getElementById("orderList");
    orderList.innerHTML = logs.map(log => {
        const payload = JSON.parse(log.payload); // 解析 JSON 内容
        return `
            <tr>
                <td>${payload.orderId}</td>
                <td>${payload.userId}</td>
                <td>${payload.totalPrice.toFixed(2)} 元</td>
                <td>${new Date(log.timestamp).toLocaleString()}</td>
            </tr>`;
    }).join('') || '<tr><td colspan="4">暂无数据</td></tr>';
}
