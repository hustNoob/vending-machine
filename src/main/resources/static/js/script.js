// 初始化
window.onload = function () {
    loadUsers();
    loadVendingMachines();
};

// 加载用户列表
function loadUsers() {
    fetch('/api/user/all')
        .then(response => response.json())
        .then(users => {
            const userSelect = document.getElementById('userSelect');
            users.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.text = user.username;
                userSelect.appendChild(option);
            });
            // 加载默认用户信息
            userSelect.onchange = loadUserInfo;
            userSelect.onchange();
        })
        .catch(error => console.error('Error loading users:', error));
}

// 加载售货机列表
function loadVendingMachines() {
    fetch('/api/vending-machine/all')
        .then(response => response.json())
        .then(machines => {
            const machineSelect = document.getElementById('machineSelect');
            machines.forEach(machine => {
                const option = document.createElement('option');
                option.value = machine.id;
                option.text = `售货机 #${machine.id}`;
                machineSelect.appendChild(option);
            });
            // 加载默认售货机的商品
            machineSelect.onchange = loadProducts;
            machineSelect.onchange();
        })
        .catch(error => console.error('Error loading vending machines:', error));
}

// 加载用户信息
function loadUserInfo() {
    const userId = document.getElementById('userSelect').value;
    fetch(`/api/user/id/${userId}`)
        .then(response => response.json())
        .then(user => {
            document.getElementById('userBalance').innerText = user.balance;
            loadUserOrders(userId);
        })
        .catch(error => console.error('Error loading user info:', error));
}

// 加载用户订单
function loadUserOrders(userId) {
    fetch(`/api/order/user/${userId}`)
        .then(response => response.json())
        .then(orders => {
            const orderList = document.getElementById('orderList');
            orderList.innerHTML = ''; // 清空
            orders.forEach(order => {
                const div = document.createElement('div');
                div.innerText = `订单 #${order.id} - 总价: ${order.totalPrice}`;
                orderList.appendChild(div);
            });
        })
        .catch(error => console.error('Error loading orders:', error));
}

// 加载商品列表
function loadProducts() {
    const machineId = document.getElementById('machineSelect').value;
    fetch(`/api/vending-machine-product/${machineId}/products`)
        .then(response => response.json())
        .then(products => {
            const productsDiv = document.getElementById('products');
            productsDiv.innerHTML = ''; // 清空
            products.forEach(product => {
                const div = document.createElement('div');
                div.className = 'product';
                div.innerHTML = `
                  <p>${product.productName}</p>
                  <p>价格: ${product.price}</p>
                  <p>库存: ${product.stock}</p>
                  <button onclick="buyProduct(${product.productId})">购买</button>
                `;
                productsDiv.appendChild(div);
            });
        })
        .catch(error => console.error('Error loading products:', error));
}

// 购买商品
function buyProduct(productId) {
    const userId = document.getElementById('userSelect').value;
    const machineId = document.getElementById('machineSelect').value;
    console.log(`用户 ${userId} 尝试从售货机 ${machineId} 购买商品 ${productId}`);
    // 后续补充创建订单逻辑
}
