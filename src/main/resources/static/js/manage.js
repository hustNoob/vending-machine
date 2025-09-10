// manage.js - 管理员界面专用脚本

window.onload = function () {
    // 加载系统统计信息
    loadSystemStats();
};

// 加载系统统计数据
function loadSystemStats() {
    // 获取用户总数
    fetch('/api/user/all')
        .then(response => response.json())
        .then(users => {
            document.getElementById('userCount').textContent = users.length;
        })
        .catch(error => {
            console.error('获取用户统计失败:', error);
            document.getElementById('userCount').textContent = '获取失败';
        });

    // 获取商品总数
    fetch('/api/product/all')
        .then(response => response.json())
        .then(products => {
            document.getElementById('productCount').textContent = products.length;
        })
        .catch(error => {
            console.error('获取商品统计失败:', error);
            document.getElementById('productCount').textContent = '获取失败';
        });

    // 获取订单总数
    fetch('/api/order/all')
        .then(response => response.json())
        .then(orders => {
            document.getElementById('orderCount').textContent = orders.length;
        })
        .catch(error => {
            console.error('获取订单统计失败:', error);
            document.getElementById('orderCount').textContent = '获取失败';
        });

    // 获取售货机总数
    fetch('/api/vending-machine/all')
        .then(response => response.json())
        .then(machines => {
            document.getElementById('machineCount').textContent = machines.length;
        })
        .catch(error => {
            console.error('获取售货机统计失败:', error);
            document.getElementById('machineCount').textContent = '获取失败';
        });
}
