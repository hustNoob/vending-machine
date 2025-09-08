// 页面加载时初始化逻辑
window.onload = function () {
    loadVendingMachines(); // 加载售货机列表
    setupAddMachineForm(); // 设置添加售货机表单
};

// 添加售货机
function setupAddMachineForm() {
    const form = document.getElementById('vendingMachineCreateForm');
    form.onsubmit = function (e) {
        e.preventDefault();
        const machineCode = document.getElementById('machineCode').value.trim(); // 售货机编号
        const status = parseInt(document.getElementById('machineStatus').value); // 状态
        const temperature = parseFloat(document.getElementById('temperature').value); // 当前温度
        const locationDesc = document.getElementById('locationDesc').value.trim(); // 位置信息

        if (!machineCode) {
            alert("请填写售货机编号！");
            return;
        }
        if (isNaN(status) || (status !== 1 && status !== 0 && status !== 2)) {
            alert("状态必须是 1（在线），0（离线）或 2（需要维护）！");
            return;
        }
        if (isNaN(temperature)) {
            alert("请填写正确的温度！");
            return;
        }
        if (!locationDesc) {
            alert("请填写位置信息！");
            return;
        }

        fetch('/api/vending-machine/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ machineCode, status, temperature, locationDesc }),
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => Promise.reject(err));
                }
                return response.text();
            })
            .then(message => {
                alert(message);
                loadVendingMachines();
            })
            .catch(error => {
                console.error("Error adding vending machine:", error);
                alert("添加售货机失败，请检查输入或联系系统管理员！");
            });
    };
}

// 加载售货机列表
function loadVendingMachines() {
    fetch('/api/vending-machine/all')
        .then(response => response.json())
        .then(machines => {
            const list = document.getElementById('vendingMachineList');
            list.innerHTML = ''; // 清空上次加载的内容

            machines.forEach(machine => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${machine.id}</td>
                    <td>${machine.machineCode}</td>
                    <td>${machine.status === 1 ? '在线' : machine.status === 0 ? '离线' : '需要维护'}</td>
                    <td>${machine.temperature ?? 'N/A'}℃</td>
                    <td>${machine.locationDesc || 'N/A'}</td>
                    <td>
                        <button onclick="deleteMachine(${machine.id})">删除</button>
                    </td>
                `;
                list.appendChild(row);
            });

            // 更新商品管理模块的选择框
            const select = document.getElementById('selectMachine');
            select.innerHTML = '<option value="">选择售货机</option>';
            machines.forEach(machine => {
                const option = document.createElement('option');
                option.value = machine.id;
                option.text = machine.machineCode;
                select.appendChild(option);
            });
        })
        .catch(error => console.error('Error loading vending machines:', error));
}

// 删除售货机
function deleteMachine(id) {
    if (confirm("确定要删除这台售货机吗？")) {
        fetch(`/api/vending-machine/delete/${id}`, {
            method: 'DELETE'
        })
            .then(response => response.text())
            .then(message => {
                alert(message); // 提示删除结果
                loadVendingMachines(); // 重新加载售货机列表
            })
            .catch(error => console.error('Error deleting vending machine:', error));
    }
}

// 售货机商品加载逻辑
document.getElementById('selectMachine').onchange = function () {
    const machineId = this.value; // 获取当前选中的售货机 ID
    if (!machineId) {
        document.getElementById('vendingMachineProductList').innerHTML = ''; // 清空商品列表
        return;
    }

    // 加载售货机的商品
    loadMachineProducts(machineId);
};

function loadMachineProducts(machineId) {
    fetch(`/api/vending-machine-product/${machineId}/products`)
        .then(response => response.json())
        .then(products => {
            const productList = document.getElementById('vendingMachineProductList');
            productList.innerHTML = ''; // 清空上次加载的内容

            // 动态生成售货机商品数据
            products.forEach(product => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${product.productId}</td>
                    <td>${product.productName}</td>
                    <td>${product.price.toFixed(2)}</td>
                    <td>${product.stock}</td>
                    <td>
                        <button onclick="deleteProductFromMachine(${machineId}, ${product.productId})">删除</button>
                    </td>
                `;
                productList.appendChild(row);
            });

            console.log(`售货机 ${machineId} 的商品列表加载完成`);
        })
        .catch(error => console.error('Error loading products for vending machine:', error));
}
