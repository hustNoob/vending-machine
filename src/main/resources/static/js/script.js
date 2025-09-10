// 定义购物车 - 全局变量
let cart = [];

// 初始化
window.onload = function () {
    loadUsers();
    loadVendingMachines();
    updateCart(); // 初始加载购物车为空状态
    bindCartButtonEvents(); // 绑定购物车按钮事件
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
            machineSelect.onchange = function() {
                const userId = document.getElementById('userSelect').value;
                if (userId) {
                    loadUserInfo(); // 重新加载用户信息和推荐商品
                }
            };
            machineSelect.onchange();
        })
        .catch(error => console.error('Error loading vending machines:', error));
}

// 修改 loadUserInfo 函数
function loadUserInfo() {
    const userId = document.getElementById('userSelect').value;
    const machineId = document.getElementById('machineSelect').value;

    if (!userId || !machineId) return;

    fetch(`/api/user/id/${userId}`)
        .then(response => response.json())
        .then(user => {
            document.getElementById('userBalance').innerText = user.balance;
            loadUserPurchaseStats(userId); // 加载用户购买统计
            loadProductSalesRanking(); // 加载商品销量排行
            loadRecommendedProducts(userId); // 加载推荐商品
        })
        .catch(error => console.error('Error loading user info:', error));
}

// 加载用户购买统计
function loadUserPurchaseStats(userId) {
    fetch(`/api/order/user/${userId}`)
        .then(response => response.json())
        .then(orders => {
            const statsDiv = document.getElementById('userPurchaseStats');
            statsDiv.innerHTML = ''; // 清空

            if (orders && orders.length > 0) {
                // 统计用户购买的商品数量
                const productStats = {};
                orders.forEach(order => {
                    if (order.orderItems) {
                        order.orderItems.forEach(item => {
                            if (productStats[item.productId]) {
                                productStats[item.productId].quantity += item.quantity;
                            } else {
                                productStats[item.productId] = {
                                    productId: item.productId,
                                    productName: item.productName,
                                    quantity: item.quantity
                                };
                            }
                        });
                    }
                });

                // 按数量排序
                const sortedStats = Object.values(productStats).sort((a, b) => b.quantity - a.quantity);

                if (sortedStats.length > 0) {
                    const table = document.createElement('table');
                    table.innerHTML = `
                        <thead>
                            <tr>
                                <th>商品名称</th>
                                <th>购买数量</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${sortedStats.map(stat => `
                                <tr>
                                    <td>${stat.productName}</td>
                                    <td>${stat.quantity}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    `;
                    statsDiv.appendChild(table);
                } else {
                    statsDiv.innerHTML = '<p>暂无购买记录</p>';
                }
            } else {
                statsDiv.innerHTML = '<p>暂无购买记录</p>';
            }
        })
        .catch(error => {
            console.error('Error loading user purchase stats:', error);
            document.getElementById('userPurchaseStats').innerHTML = '<p>加载失败</p>';
        });
}

// 修改商品销量排行函数，显示具体销量
function loadProductSalesRanking() {
    // 获取全局热销商品及其销量
    fetch('/api/order/top-selling-with-quantities')
        .then(response => response.json())
        .then(salesData => {
            const rankingDiv = document.getElementById('productSalesRanking');
            rankingDiv.innerHTML = ''; // 清空

            if (salesData && salesData.length > 0) {
                // 获取所有商品信息
                fetch('/api/product/all')
                    .then(response => response.json())
                    .then(allProducts => {
                        // 创建商品ID到商品对象的映射
                        const productMap = {};
                        allProducts.forEach(product => {
                            productMap[product.id] = product;
                        });

                        // 构建排行榜显示
                        const rankingHtml = `
                            <table>
                                <thead>
                                    <tr>
                                        <th>排名</th>
                                        <th>商品名称</th>
                                        <th>价格</th>
                                        <th>销量</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${salesData.map((item, index) => {
                            const product = productMap[item.productId];
                            return product ? `
                                            <tr>
                                                <td>${index + 1}</td>
                                                <td>${product.name}</td>
                                                <td>¥${product.price.toFixed(2)}</td>
                                                <td>${item.quantity}</td>
                                            </tr>
                                        ` : '';
                        }).join('')}
                                </tbody>
                            </table>
                        `;

                        rankingDiv.innerHTML = rankingHtml;
                    })
                    .catch(error => {
                        console.error('Error loading products:', error);
                        rankingDiv.innerHTML = '<p>加载失败</p>';
                    });
            } else {
                rankingDiv.innerHTML = '<p>暂无销量数据</p>';
            }
        })
        .catch(error => {
            console.error('Error loading product sales ranking:', error);
            document.getElementById('productSalesRanking').innerHTML = '<p>加载失败</p>';
        });
}

// 修复后的添加商品到购物车函数
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
        item.vendingMachineId === product.vendingMachineId
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

// 修复后的更新购物车显示函数
function updateCart() {
    const cartItems = document.getElementById("cartItems");
    cartItems.innerHTML = ""; // 清空购物车内容

    if (cart.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="5">购物车为空</td>';
        cartItems.appendChild(row);
        return;
    }

    let total = 0; // 总金额
    cart.forEach((item, index) => {
        total += item.quantity * item.price;
        const row = document.createElement('tr');
        // 给每个按钮添加唯一的 data 属性
        row.innerHTML = `
            <td>${item.name}</td>
            <td>${item.price.toFixed(2)}</td>
            <td>
                <button class="cart-btn" data-action="decrease" data-product-id="${item.productId}">-</button>
                ${item.quantity}
                <button class="cart-btn" data-action="increase" data-product-id="${item.productId}">+</button>
            </td>
            <td>${(item.quantity * item.price).toFixed(2)}</td>
            <td>
                <button class="cart-btn" data-action="remove" data-product-id="${item.productId}">移除</button>
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

    // 重新绑定事件监听器
    bindCartButtonEvents();
}

// 绑定购物车按钮事件
function bindCartButtonEvents() {
    // 移除之前可能存在的事件监听器，防止重复绑定
    document.removeEventListener('click', handleCartButtonClick);

    // 添加新的事件监听器
    document.addEventListener('click', handleCartButtonClick);
}

// 处理购物车按钮点击事件
function handleCartButtonClick(e) {
    if (e.target.classList.contains('cart-btn')) {
        const action = e.target.getAttribute('data-action');
        const productId = parseInt(e.target.getAttribute('data-product-id'));

        if (productId) {
            switch(action) {
                case 'decrease':
                    changeQuantity(productId, -1);
                    break;
                case 'increase':
                    changeQuantity(productId, 1);
                    break;
                case 'remove':
                    removeFromCart(productId);
                    break;
            }
        }
    }
}

// 修复后的更新购物车数量函数
function changeQuantity(productId, change) {
    // 获取当前售货机 ID
    const vendingMachineId = parseInt(document.getElementById('machineSelect').value, 10);

    // 查找购物车中的商品
    const itemIndex = cart.findIndex(item =>
        item.productId === productId &&
        item.vendingMachineId === vendingMachineId
    );

    if (itemIndex === -1) {
        console.warn("未在购物车中找到指定商品");
        return;
    }

    const item = cart[itemIndex];
    const newQuantity = item.quantity + change;

    if (newQuantity <= 0) {
        // 如果数量为0或负数，移除商品
        removeFromCart(productId);
    } else if (newQuantity > item.stock) {
        alert("库存不足，无法增加更多商品！");
    } else {
        item.quantity = newQuantity;
        updateCart();
    }
}

// 修复后的从购物车移除商品函数
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
            loadUserInfo();

            console.log("订单已通过MQTT上报");
        })
        .catch(error => {
            console.error("MQTT订单发送失败：", error);
            alert("订单提交失败，请重试！");
        });
}

function loadRecommendedProducts(userId) {
    const machineId = document.getElementById('machineSelect').value;

    // 先获取当前售货机的商品库存信息
    fetch(`/api/vending-machine-product/${machineId}/products`)
        .then(response => response.json())
        .then(machineProducts => {
            // 创建商品ID到库存的映射
            const stockMap = {};
            machineProducts.forEach(p => {
                stockMap[p.productId] = p.stock;
            });

            // 获取推荐商品
            return fetch(`/api/user/recommend/${userId}`)
                .then(response => response.json())
                .then(products => {
                    return { products, stockMap };
                });
        })
        .then(({ products, stockMap }) => {
            const recommendedDiv = document.getElementById('recommendedProducts');

            if (products && products.length > 0) {
                products.forEach(product => {
                    // 获取该商品在当前售货机中的库存
                    const stock = stockMap[product.id] || 0;

                    const div = document.createElement('div');
                    div.className = 'product';
                    div.innerHTML = `
                        <p>${product.name}</p>
                        <p>价格: ¥${product.price.toFixed(2)}</p>
                        <p>库存: ${stock}</p>
                        <button onclick='addToCart({
                            productId: ${product.id},
                            productName: "${product.name}",
                            price: ${product.price},
                            stock: ${stock},
                            vendingMachineId: ${machineId}
                        })'>购买</button>
                    `;
                    recommendedDiv.appendChild(div);
                });
            } else {
                recommendedDiv.innerHTML += '<p>暂无推荐商品</p>';
            }
        })
        .catch(error => {
            console.error('Error loading recommended products:', error);
            document.getElementById('recommendedProducts').innerHTML = '<p>加载失败</p>';
        });
}
