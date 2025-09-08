window.onload = function () {
    const form = document.getElementById("orderFilterForm");
    form.onsubmit = function (e) {
        e.preventDefault();
        filterOrders(); // 筛选订单逻辑
    };

    loadOrders(); // 加载所有订单
};

// 加载所有订单
function loadOrders() {
    fetch('/api/order/all') // 调用后端接口加载所有订单
        .then(response => response.json()) // 解析 JSON 数据
        .then(orders => {
            displayOrders(orders);
        })
        .catch(error => console.error('加载订单失败:', error));
}

// 显示订单列表
function displayOrders(orders) {
    const orderList = document.getElementById("orderList");
    orderList.innerHTML = ''; // 清空列表

    if (orders.length === 0) {
        const row = document.createElement("tr");
        row.innerHTML = `<td colspan="5">暂无订单数据</td>`; // 没有数据提示
        orderList.appendChild(row);
        return;
    }

    orders.forEach(order => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${order.id}</td>
            <td>${order.userId}</td>
            <td>${formatPrice(order.totalPrice)}</td>
            <td>${formatDateTime(order.createTime)}</td>
            <td>${order.isPaid ? '已支付' : '未支付'}</td> <!-- 支付状态 -->
            <td>
                <button onclick="viewOrderDetails(${order.id})">详情</button>
            </td>
        `;
        orderList.appendChild(row);
    });
}

// 格式化价格
function formatPrice(price) {
    return price ? `${price.toFixed(2)} 元` : 'N/A';
}

// 格式化日期时间
function formatDateTime(dateTime) {
    return dateTime ? new Date(dateTime).toLocaleString() : 'N/A';
}

// 获取订单状态文本（基于后端返回的数据）
function getOrderStatus(order) {
    if (order.isCompleted) return '已完成';
    if (order.isPaid) return '已支付';
    return '未支付';
}


// 筛选订单
function filterOrders() {
    const orderId = document.getElementById("orderId").value; // 订单 ID
    const userId = document.getElementById("userId").value; // 用户 ID

    if (orderId) {
        // 按订单 ID 筛选
        fetch(`/api/order/${orderId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('订单未找到');
                }
                return response.json();
            })
            .then(order => displayOrders([order])) // 展示找到的订单
            .catch(error => {
                console.error(error);
                alert("订单不存在！");
                displayOrders([]); // 清空列表
            });
    } else if (userId) {
        // 按用户 ID 筛选
        fetch(`/api/order/user/${userId}`)
            .then(response => response.json())
            .then(orders => displayOrders(orders))
            .catch(error => {
                console.error('按用户筛选订单失败:', error);
                alert('无此用户订单！');
                displayOrders([]); // 清空列表
            });
    } else {
        // 无筛选条件时恢复所有数据
        loadOrders();
    }
}

// 将状态值转换为文本表示
function getOrderStatusText(status) {
    switch (status) {
        case 0:
            return '进行中';
        case 1:
            return '已完成';
        case 2:
            return '已取消';
        default:
            return '未知状态';
    }
}

//订单详情
function viewOrderDetails(orderId) {
    fetch(`/api/order/details/${orderId}`)
        .then(response => response.json())
        .then(order => {
            const details = document.getElementById("orderDetails");
            details.innerHTML = `
                <p>订单 ID: ${order.id}</p>
                <p>用户 ID: ${order.userId}</p>
                <p>总金额: ${formatPrice(order.totalPrice)}</p>
                <p>下单时间: ${formatDateTime(order.createTime)}</p>
                <p>支付时间: ${formatDateTime(order.paymentTime)}</p>
                <p>完成时间: ${formatDateTime(order.completionTime)}</p>
                <h4>商品清单:</h4>
                <ul>
                    ${
                order.orderItems && order.orderItems.length > 0
                    ? order.orderItems.map(item => `
                            <li>商品名称: ${item.productName} | 商品 ID: ${item.productId} | 数量: ${item.quantity} | 单价: ${formatPrice(item.price)} | 小计: ${formatPrice(item.subtotal)}</li>
                        `).join("")
                    : "<li>暂无商品清单</li>"
            }
                </ul>
            `;
            openModal();
        })
        .catch(error => {
            console.error("加载订单详情失败:", error);
            alert("无法加载订单详情");
        });
}

function openModal() {
    document.getElementById("orderDetailsModal").style.display = "block";
}

function closeModal() {
    document.getElementById("orderDetailsModal").style.display = "none";
}