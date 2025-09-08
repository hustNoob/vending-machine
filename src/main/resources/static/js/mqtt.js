// 初始化事件处理逻辑
window.onload = function () {
    setupCommandForm(); // 初始化命令下发逻辑
    loadMqttData(); // 初始化实时数据加载逻辑
};

// 1. 设置命令下发逻辑
function setupCommandForm() {
    const form = document.getElementById("mqttCommandForm");
    form.onsubmit = function (event) {
        event.preventDefault(); // 阻止页面刷新

        const topic = document.getElementById("topic").value.trim();
        const payload = document.getElementById("payload").value.trim();

        if (!topic || !payload) {
            alert("主题和消息内容不能为空！");
            return;
        }

        // 调用后端 API 执行发布命令的操作
        fetch('/api/test-publish', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ topic, payload }),
        })
            .then(response => {
                if (!response.ok) throw new Error("MQTT命令发送失败！");
                return response.text();
            })
            .then(message => {
                alert(message); // 提示成功
            })
            .catch(error => {
                console.error("Error sending MQTT command:", error);
                alert("命令发送失败，请检查输入或联系管理员！");
            });
    };
}

// 2. 加载实时上报数据（轮询方式）
function loadMqttData() {
    const dataContainer = document.getElementById("mqttDataList");

    // 模拟轮询数据，更新上报的设备数据
    setInterval(() => {
        // 调用后端 API 获取最新的设备上报数据（假设后端提供 `/api/mqtt/data`）
        fetch('/api/mqtt/data')
            .then(response => response.json())
            .then(data => {
                // 清空旧数据
                dataContainer.innerHTML = '';

                if (data.length === 0) {
                    dataContainer.innerHTML = '<p>暂无数据上报</p>';
                    return;
                }

                // 动态生成上报数据的展示
                data.forEach(item => {
                    const entry = document.createElement('div');
                    entry.className = 'mqtt-data-entry';
                    entry.innerHTML = `
                        <p><b>主题：</b> ${item.topic}</p>
                        <p><b>内容：</b> ${item.payload}</p>
                        <p><b>时间：</b> ${new Date(item.timestamp).toLocaleString()}</p>
                        <hr>
                    `;
                    dataContainer.appendChild(entry);
                });
            })
            .catch(error => {
                console.error("Error loading MQTT data:", error);
            });
    }, 5000); // 每 5 秒轮询一次
}
