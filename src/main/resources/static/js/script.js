// 初始化
window.onload = function () {
    loadUsers();
    loadVendingMachines();
};

// 定义购物车
let cart = []; // 用于存储购物车中的商品信息

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

// 修改 loadUserInfo 函数，添加推荐商品加载
function loadUserInfo() {
    const userId = document.getElementById('userSelect').value;
    fetch(`/api/user/id/${userId}`)
        .then(response => response.json())
        .then(user => {
            document.getElementById('userBalance').innerText = user.balance;
            loadUserOrders(userId);
            loadRecommendedProducts(userId); // 加载推荐商品
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

// 加载商品列表（绑定购买按钮）
function loadProducts() {
    const machineId = document.getElementById('machineSelect').value; // 获取当前选择的售货机ID
    fetch(`/api/vending-machine-product/${machineId}/products`) // 修正了API地址
        .then(response => response.json())
        .then(products => {
            const productsDiv = document.getElementById('products');
            productsDiv.innerHTML = ''; // 清空

            products.forEach(product => {
                const div = document.createElement('div');
                div.className = 'product';
                // Pass machineId along with product info
                div.innerHTML = `
                  <p>${product.productName}</p>
                  <p>价格: ${product.price}</p>
                  <p>库存: ${product.stock}</p>
                  <button onclick='addToCart({
                      productId: ${product.productId}, 
                      productName: "${product.productName}", 
                      price: ${product.price}, 
                      stock: ${product.stock}, 
                      vendingMachineId: ${machineId} // 传递售货机 ID
                  })'>购买</button>
                `;
                productsDiv.appendChild(div);
            });
        })
        .catch(error => console.error('Error loading products:', error));
}

// 添加商品到购物车
function addToCart(product) {
    console.log("购买商品：", product); // 调试日志

    // 检查是否包含 vendingMachineId
    if (product.vendingMachineId === undefined) {
        alert("无法获取售货机信息，请刷新页面重试。");
        return;
    }

    // 如果购物车中存在该商品（在同一台售货机上），更新其数量
    const existingItem = cart.find(item =>
        item.productId === product.productId &&
        item.vendingMachineId === product.vendingMachineId // Ensure same machine
    );
    if (existingItem) {
        if (existingItem.quantity + 1 > product.stock) {
            alert("库存不足，无法添加更多商品！");
            return;
        }
        existingItem.quantity += 1;
    } else {
        // 如果购物车中不存在该商品（或在不同售货机上），直接加入
        if (product.stock < 1) {
            alert("库存不足，无法添加商品！");
            return;
        }
        cart.push({
            productId: product.productId,    // 商品ID
            vendingMachineId: product.vendingMachineId, // 售货机ID
            name: product.productName,       // 商品名称
            price: product.price,            // 商品单价
            quantity: 1,                     // 初始数量为 1
            stock: product.stock             // 最大库存
        });
    }
    updateCart(); // 更新购物车界面
}

// 更新购物车显示
function updateCart() {
    const cartItems = document.getElementById("cartItems");
    cartItems.innerHTML = ""; // 清空购物车内容

    let total = 0; // 总金额
    cart.forEach(item => {
        total += item.quantity * item.price;
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.name}</td>
            <td>${item.price.toFixed(2)}</td>
            <td>
                <button onclick="changeQuantity(${item.productId}, -1)">-</button>
                ${item.quantity}
                <button onclick="changeQuantity(${item.productId}, 1)">+</button>
            </td>
            <td>${(item.quantity * item.price).toFixed(2)}</td>
            <td>
                <button onclick="removeFromCart(${item.productId})">移除</button>
            </td>
        `;
        cartItems.appendChild(row);
    });

    // 显示购物车总金额
    const totalRow = document.createElement('tr');
    totalRow.innerHTML = `
        <td colspan="3"><b>总金额</b></td>
        <td colspan="2">${total.toFixed(2)}</td>
    `;
    cartItems.appendChild(totalRow);
}

// 更新购物车数量
function changeQuantity(productId, change) {
    // 获取当前售货机 ID
    const vendingMachineId = parseInt(document.getElementById('machineSelect').value, 10);

    const item = cart.find(item =>
        item.productId === productId &&
        item.vendingMachineId === vendingMachineId
    );

    if (!item) {
        console.warn("未在购物车中找到指定商品（售货机不匹配？）");
        return;
    }
    const newQuantity = item.quantity + change;

    if (newQuantity <= 0) {
        removeFromCart(productId); // 这里调用的是修改后的 removeFromCart
    } else if (newQuantity > item.stock) {
        alert("库存不足，无法增加更多商品！");
    } else {
        item.quantity = newQuantity;
    }
    updateCart();
}

// 从购物车移除商品
function removeFromCart(productId) {
    // 获取当前售货机 ID
    const vendingMachineId = parseInt(document.getElementById('machineSelect').value, 10);

    // 只移除当前售货机中的该商品
    cart = cart.filter(item =>
        !(item.productId === productId && item.vendingMachineId === vendingMachineId)
    );
    updateCart();
}

function checkout() {
    const userId = parseInt(document.getElementById('userSelect').value, 10);
    const machineId = document.getElementById('machineSelect').value;

    if (!userId || userId <= 0) {
        alert("请先选择用户！");
        return;
    }

    if (!machineId) {
        alert("请先选择售货机！");
        return;
    }

    if (cart.length === 0) {
        alert("购物车为空，请先添加商品！");
        return;
    }

    // 直接通过MQTT发送订单信息（不通过API创建订单）
    // 使用时间戳作为临时订单ID，MQTT接收端会处理并生成真实的数据库订单
    const tempOrderId = "TEMP_" + Date.now() + "_" + Math.floor(Math.random() * 1000);
    const totalPrice = cart.reduce((sum, item) => sum + (item.quantity * item.price), 0);

    // 构造MQTT消息
    const orderPayload = {
        orderId: tempOrderId, // 临时ID，MQTT端会替换为真实ID
        userId: userId,
        machineId: machineId,
        totalPrice: totalPrice,
        items: cart.map(item => ({
            productId: item.productId,
            vendingMachineId: item.vendingMachineId,
            quantity: item.quantity
        })),
        timestamp: new Date().toISOString(),
        source: "web" // 标记来源
    };

    console.log("即将通过MQTT发送订单:", orderPayload);

    // 通过后端API发送MQTT消息到指定主题
    const publishData = {
        topic: "vendingmachine/order/" + tempOrderId,
        payload: JSON.stringify(orderPayload)
    };

    fetch('/api/test-publish', {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(publishData)
    })
        .then(response => response.text())
        .then(message => {
            console.log("MQTT订单发送成功:", message);
            alert("订单已提交，正在处理中...");

            // 清空购物车
            cart = [];
            updateCart();

            // 重新加载商品和用户信息
            loadProducts();
            loadUserInfo();

            console.log("订单已通过MQTT上报");
        })
        .catch(error => {
            console.error("MQTT订单发送失败：", error);
            alert("订单提交失败，请重试！");
        });
}

// 添加推荐商品加载函数
function loadRecommendedProducts(userId) {
    fetch(`/api/user/recommend/${userId}`)
        .then(response => response.json())
        .then(products => {
            const recommendedDiv = document.getElementById('recommendedProducts');
            if (recommendedDiv) {
                recommendedDiv.innerHTML = ''; // 清空

                if (products && products.length > 0) {
                    products.forEach(product => {
                        const div = document.createElement('div');
                        div.className = 'product';
                        div.innerHTML = `
                            <p>${product.name}</p>
                            <p>价格: ¥${product.price.toFixed(2)}</p>
                            <button onclick='addToRecommendedCart(${product.id}, "${product.name}", ${product.price})'>推荐购买</button>
                        `;
                        recommendedDiv.appendChild(div);
                    });
                } else {
                    recommendedDiv.innerHTML = '<p>暂无推荐商品</p>';
                }
            }
        })
        .catch(error => console.error('Error loading recommended products:', error));
}

// 添加推荐商品购物车函数
function addToRecommendedCart(productId, productName, price) {
    const machineId = document.getElementById('machineSelect').value;

    // 先获取商品库存信息
    fetch(`/api/vending-machine-product/${machineId}/products`)
        .then(response => response.json())
        .then(products => {
            const product = products.find(p => p.productId === productId);
            if (product) {
                addToCart({
                    productId: productId,
                    productName: productName,
                    price: price,
                    stock: product.stock,
                    vendingMachineId: machineId
                });
            } else {
                alert("该商品在当前售货机中无库存！");
            }
        })
        .catch(error => {
            console.error('Error checking product stock:', error);
            alert("无法获取商品库存信息！");
        });
}
