// 页面加载时初始化逻辑
window.onload = function () {
    loadVendingMachines(); // 加载售货机列表
    loadAvailableProducts(); // 加载厂商商品列表
    setupAddMachineForm(); // 设置添加售货机表单事件处理
};

// 加载厂商提供的商品列表
function loadAvailableProducts() {
    fetch('/api/product/all') // API: 获取厂商商品列表
        .then(response => response.json())
        .then(products => {
            const availableProducts = document.getElementById('availableProducts');
            availableProducts.innerHTML = ''; // 清空已有数据

            // 动态添加项到商品列表
            products.forEach(product => {
                const option = document.createElement('option');
                option.value = product.id;
                option.textContent = `${product.name} - ${product.price.toFixed(2)}元`; // 商品名称和价格展示
                availableProducts.appendChild(option);
            });
        })
        .catch(error => console.error('Error loading available products:', error));
}

// 添加商品到售货机
document.getElementById('addProductToMachineForm').onsubmit = function (event) {
    event.preventDefault();

    const machineId = document.getElementById('selectMachine').value; // 当前选中的售货机 ID
    const productId = document.getElementById('availableProducts').value; // 选中的商品 ID
    const stock = document.getElementById('newProductStock').value.trim(); // 输入的库存数量

    if (!machineId) {
        alert('请先选择售货机！');
        return;
    }

    if (!productId || stock <= 0) {
        alert('请选择商品并设定有效库存！');
        return;
    }

    // 调用后端 API，执行添加操作
    fetch(`/api/vending-machine-product/${machineId}/add-product?productId=${productId}&stock=${stock}`, {
        method: 'POST'
    })
        .then(response => response.text())
        .then(message => {
            alert(message); // 提示结果
            loadMachineProducts(machineId); // 重新加载售货机商品列表
        })
        .catch(error => {
            console.error('Error adding product to machine:', error);
            alert('添加商品失败，请重试！');
        });
};

// 加载售货机列表
function loadVendingMachines() {
    fetch('/api/vending-machine/all') // API: 获取所有售货机
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
            select.innerHTML = '<option value="">请选择售货机</option>';
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

// 加载指定售货机内的商品列表
document.getElementById('selectMachine').onchange = function () {
    const machineId = this.value; // 获取选中的售货机 ID
    if (!machineId) {
        document.getElementById('vendingMachineProductList').innerHTML = ''; // 清空商品列表
        return;
    }

    // 加载对应售货机的商品
    loadMachineProducts(machineId);
};

function loadMachineProducts(machineId) {
    fetch(`/api/vending-machine-product/${machineId}/products`) // API: 获取售货机内商品
        .then(response => response.json())
        .then(products => {
            const productList = document.getElementById('vendingMachineProductList');
            productList.innerHTML = ''; // 清空上次加载

            products.forEach(product => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${product.productId}</td>
                    <td>${product.productName}</td>
                    <td>${product.price.toFixed(2)}</td>
                    <td>
                        <input type="number" value="${product.stock}" 
                               onchange="updateStock(${machineId}, ${product.productId}, this.value)">
                    </td>
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

// 更新售货机内商品库存
function updateStock(machineId, productId, newStock) {
    if (newStock <= 0) {
        alert("库存数量必须大于 0！");
        return;
    }

    fetch(`/api/vending-machine-product/${machineId}/set-stock?productId=${productId}&newStock=${newStock}`, {
        method: 'PUT' // 保持 PUT 方法
    })
        .then(response => response.text())
        .then(message => {
            alert(message); // 提示更新结果
            loadMachineProducts(machineId); // 重新加载售货机商品
        })
        .catch(error => {
            console.error('Error updating stock:', error);
            alert('库存更新失败，请重试！');
        });
}

// 从售货机删除商品
function deleteProductFromMachine(machineId, productId) {
    if (confirm("确定要从售货机中删除该商品吗？")) {
        fetch(`/api/vending-machine-product/${machineId}/delete-product/${productId}`, {
            method: 'DELETE'
        })
            .then(response => response.text())
            .then(message => {
                alert(message); // 提示删除结果
                loadMachineProducts(machineId); // 重新加载售货机商品
            })
            .catch(error => console.error('Error deleting product from machine:', error));
    }
}

function setupAddMachineForm() {
    const form = document.getElementById('vendingMachineCreateForm');
    form.onsubmit = function (event) {
        event.preventDefault(); // 阻止表单提交的默认刷新行为

        // 收集表单数据
        const machineCode = document.getElementById('machineCode').value.trim();
        const status = parseInt(document.getElementById('machineStatus').value);
        const temperature = parseFloat(document.getElementById('temperature').value);
        const locationDesc = document.getElementById('locationDesc').value.trim();

        // 表单校验
        if (!machineCode) {
            alert("请输入售货机编号！");
            return;
        }
        if (isNaN(status) || (status !== 1 && status !== 0 && status !== 2)) {
            alert("状态必须是 1（在线），0（离线）或 2（需要维护）！");
            return;
        }
        if (isNaN(temperature) || temperature < -50 || temperature > 50) {
            alert("请填写正确的温度值（-50 到 50℃）！");
            return;
        }
        if (!locationDesc) {
            alert("请输入售货机位置描述！");
            return;
        }

        // 发起 POST 请求，添加售货机
        fetch('/api/vending-machine/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ machineCode, status, temperature, locationDesc }) // JSON 格式的请求体
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => Promise.reject(err));
                }
                return response.text();
            })
            .then(message => {
                alert(message); // 提示操作结果
                loadVendingMachines(); // 重新加载售货机列表
                form.reset(); // 清空表单
            })
            .catch(error => {
                console.error("Error adding vending machine:", error);
                alert("添加售货机失败，请检查输入数据或联系系统管理员！");
            });
    };
}