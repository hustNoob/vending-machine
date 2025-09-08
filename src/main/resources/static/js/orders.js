// 获取订单列表
async function fetchOrders() {
    const response = await fetch('/api/order/all'); // 假设用户ID为1
    const orders = await response.json();
    renderOrderTable(orders);
}

// 渲染订单表格
function renderOrderTable(orders) {
    const tableBody = document.querySelector('#orderTable tbody');
    tableBody.innerHTML = ''; // 清空表格内容

    orders.forEach(order => {
        const row = `
            <tr>
                <td>${order.id}</td>
                <td>${order.userId}</td>
                <td>${order.totalPrice}</td>
                <td>${order.createTime}</td>
                <td>${order.paymentTime || '未支付'}</td>
                <td>${order.completionTime || '未完成'}</td>
                <td>
                    <button onclick="viewOrderDetails(${order.id})">查看详情</button>
                    <button onclick="markOrderAsPaid(${order.id})">支付</button>
                    <button onclick="markOrderAsCompleted(${order.id})">完成</button>
                </td>
            </tr>
        `;
        tableBody.innerHTML += row;
    });
}

// 订单详情展示
async function viewOrderDetails(orderId) {
    const response = await fetch(`/api/order/details/${orderId}`);
    const order = await response.json();

    const detailsElement = document.getElementById('orderDetailsContent');
    detailsElement.innerHTML = `
        <p><strong>订单ID:</strong> ${order.id}</p>
        <p><strong>用户ID:</strong> ${order.userId}</p>
        <p><strong>金额:</strong> ${order.totalPrice}</p>
        <p><strong>下单时间:</strong> ${order.createTime}</p>
        <p><strong>支付时间:</strong> ${order.paymentTime || '未支付'}</p>
        <p><strong>完成时间:</strong> ${order.completionTime || '未完成'}</p>
        <h4>商品清单</h4>
        <ul>
            ${order.orderItems?.map(item => `
                <li>商品ID: ${item.productId}，数量: ${item.quantity}，单价: ${item.price}，小计: ${item.subtotal}</li>
            `).join('') || '<li>无订单详情</li>'}
        </ul>
    `;
}

// 支付订单
async function markOrderAsPaid(orderId) {
    const response = await fetch(`/api/order/pay/${orderId}`, {
        method: 'PUT'
    });

    if (response.ok) {
        alert(`订单 ${orderId} 已支付！`);
        fetchOrders();
        viewOrderDetails(orderId); // 更新详情显示
    } else {
        alert("支付失败");
    }
}

// 完成操作
async function markOrderAsCompleted(orderId) {
    const response = await fetch(`/api/order/complete/${orderId}`, { method: 'PUT' });

    if (response.ok) {
        alert(`订单 ${orderId} 已完成！`);
        fetchOrders(); // 刷新订单列表
    } else {
        alert(`操作失败，请重试！`);
    }
}

// 页面加载时获取订单列表
window.onload = fetchOrders;
