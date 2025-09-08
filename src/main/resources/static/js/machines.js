// 获取售货机列表
async function fetchVendingMachines() {
    const response = await fetch('/api/vending-machine/all');
    const machines = await response.json();
    renderMachineTable(machines);
}

// 渲染售货机表格
function renderMachineTable(machines) {
    const tableBody = document.querySelector('#machineTable tbody');
    tableBody.innerHTML = ''; // 清空表格内容

    machines.forEach(machine => {
        const row = `
            <tr>
                <td>${machine.id}</td>
                <td>${machine.machineCode}</td>
                <td>${getStatusText(machine.status)}</td>
                <td>${machine.temperature || '未知'}</td>
                <td>${machine.locationDesc || '未设置'}</td>
                <td>${machine.activationTime || '未知'}</td>
                <td>
                    <button onclick="deleteVendingMachine(${machine.id})">删除</button>
                    <button onclick="viewMachineDetails(${machine.id})">查看详情</button>
                </td>
            </tr>
        `;
        tableBody.innerHTML += row;
    });
}

// 状态编码解释
function getStatusText(status) {
    switch (status) {
        case 1: return '在线';
        case 0: return '离线';
        case 2: return '需要维护';
        default: return '未知状态';
    }
}

// 新增售货机
async function addVendingMachine() {
    const machine = {
        machineCode: document.getElementById('machineCode').value,
        status: parseInt(document.getElementById('machineStatus').value),
        temperature: parseFloat(document.getElementById('machineTemperature').value),
        locationDesc: document.getElementById('machineLocation').value
    };

    const response = await fetch('/api/vending-machine/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(machine)
    });

    if (response.ok) {
        alert('售货机添加成功！');
        fetchVendingMachines(); // 刷新列表
    } else {
        alert('售货机添加失败！');
    }
}

// 删除售货机
async function deleteVendingMachine(id) {
    const response = await fetch(`/api/vending-machine/delete/${id}`, { method: 'DELETE' });

    if (response.ok) {
        alert('售货机删除成功！');
        fetchVendingMachines(); // 刷新列表
    } else {
        alert('售货机删除失败！');
    }
}

// 查看售货机详情
async function viewMachineDetails(machineId) {
    const response = await fetch(`/api/vending-machine/${machineId}`);
    const machine = await response.json();

    alert(`
        设备编号: ${machine.machineCode}\n
        状态: ${getStatusText(machine.status)}\n
        温度: ${machine.temperature || '未知'}\n
        位置信息: ${machine.locationDesc || '未设置'}\n
        激活时间: ${machine.activationTime || '未知'}
    `);
}

// 发布MQTT命令
async function sendCommandToMachine() {
    const machineCode = document.getElementById('commandMachineCode').value;
    const commandContent = document.getElementById('commandContent').value;

    const response = await fetch(`/api/vending-machine/mqtt/command/${machineCode}?command=${commandContent}`, {
        method: 'POST'
    });

    if (response.ok) {
        alert('命令发送成功！');
    } else {
        alert('命令发送失败！');
    }
}

// 页面加载时自动获取售货机列表
window.onload = fetchVendingMachines;
