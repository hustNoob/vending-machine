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

// 执行结账流程
function checkout() {
    const userId = parseInt(document.getElementById('userSelect').value, 10); // 获取并转换为数字

    if (!userId || userId <= 0) {
        alert("请先选择用户！");
        return;
    }

    if (cart.length === 0) {
        alert("购物车为空，请先添加商品！");
        return;
    }

    // 组织购物车结算数据，确保包含 vendingMachineId
    const order = cart.map(item => ({
        productId: item.productId,
        vendingMachineId: item.vendingMachineId, // 添加售货机ID
        quantity: item.quantity
    }));

    // 调试日志
    console.log("准备付款的购物车订单：", { userId, order });

    // 发送请求到后端
    fetch(`/api/order/checkout`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId, order }) // 正确发送包含 vendingMachineId 的请求体
    })
        .then(response => {
            // 更加健壮的响应处理
            if (!response.ok) {
                return response.text().then(text => { throw new Error(text || "支付请求失败"); });
            }
            return response.text(); // 如果是成功的文本响应
        })
        .then(message => {
            alert(message); // 提示用户支付结果
            cart = []; // 清空购物车
            updateCart(); // 更新购物车UI
            loadProducts(); // 更新商品库存
            loadUserInfo(); // 更新用户余额等信息
        })
        .catch(error => {
            console.error("支付错误：", error);
            alert("支付失败，请稍后重试！\n错误详情: " + error.message); // 显示更详细的错误
        });
}
