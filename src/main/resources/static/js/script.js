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

// 加载商品列表（并挂载购买按钮）
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
                  <button onclick="addToCart(${JSON.stringify(product)})">购买</button>
                `;
                productsDiv.appendChild(div);
            });
        })
        .catch(error => console.error('Error loading products:', error));
}

// 添加商品到购物车
function addToCart(product) {
    console.log(product);
    // 判断购物车中是否已有该商品
    const existingItem = cart.find(item => item.productId === product.productId);
    if (existingItem) {
        if (existingItem.quantity + 1 > product.stock) {
            alert("库存不足，无法添加更多商品！");
            return;
        }
        existingItem.quantity += 1; // 增加商品数量
    } else {
        if (product.stock < 1) {
            alert("库存不足，无法添加商品！");
            return;
        }
        cart.push({
            productId: product.productId, // 使用 product 的结构化属性
            name: product.productName,
            price: product.price,
            quantity: 1, // 初始数量为 1
            stock: product.stock,
        });
    }
    updateCart(); // 更新购物车显示
}

// 更新购物车展示
function updateCart() {
    const cartItems = document.getElementById("cartItems");
    cartItems.innerHTML = ""; // 清空购物车内容

    let total = 0;
    cart.forEach(item => {
        total += item.quantity * item.price;
        const row = document.createElement("tr");
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

    // 购物车总金额
    const row = document.createElement("tr");
    row.innerHTML = `
        <td colspan="3"><b>总金额</b></td>
        <td colspan="2">${total.toFixed(2)}</td>
    `;
    cartItems.appendChild(row);
}

// 更新购物车数量
function changeQuantity(productId, change) {
    const item = cart.find(item => item.productId === productId);
    if (!item) return;
    const newQuantity = item.quantity + change;

    if (newQuantity <= 0) {
        removeFromCart(productId);
    } else if (newQuantity > item.stock) {
        alert("库存不足，无法增加更多商品！");
    } else {
        item.quantity = newQuantity;
    }
    updateCart();
}

// 从购物车移除商品
function removeFromCart(productId) {
    cart = cart.filter(item => item.productId !== productId);
    updateCart();
}

// 执行结账流程
function checkout() {
    const userId = document.getElementById('userSelect').value;

    if (cart.length === 0) {
        alert("购物车为空，无法结账！");
        return;
    }

    const order = cart.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
    }));

    fetch(`/api/order/checkout`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId, order })
    })
        .then(response => response.json())
        .then(result => {
            if (result.success) {
                alert("支付成功！");
                cart = []; // 清空购物车
                updateCart();
                loadProducts(); // 更新产品库存
            } else {
                alert(result.message);
            }
        })
        .catch(error => console.error("Error during checkout:", error));
}
