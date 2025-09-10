window.onload = function () {
    loadUsers();
    loadUserStatsSelect();

    // 提交新增用户表单
    const userCreateForm = document.getElementById("userCreateForm");
    userCreateForm.onsubmit = function (event) {
        event.preventDefault();
        createUser();
    };
};

// 用户列表加载
function loadUsers() {
    fetch('/api/user/all')
        .then(response => response.json())
        .then(users => {
            const userList = document.getElementById("userList");
            userList.innerHTML = ''; // 清空列表

            users.forEach(user => {
                const row = document.createElement("tr");
                row.setAttribute('data-user-id', user.id);
                row.innerHTML = `
                    <td>${user.id}</td>
                    <td>
                        <span class="user-display">${user.username}</span>
                        <input type="text" class="edit-username-input" value="${user.username}" style="display: none;">
                    </td>
                    <td>
                        <span class="balance-display">${user.balance}</span>
                        <input type="number" class="edit-input" value="${user.balance}" step="0.01" style="display: none;">
                    </td>
                    <td>
                        <div class="normal-buttons">
                            <button class="action-button balance-button" onclick="updateUserBalance(${user.id})">修改余额</button>
                            <button class="action-button delete-button" onclick="deleteUser(${user.id})">删除</button>
                        </div>
                        <div class="edit-buttons" style="display: none;">
                            <button class="action-button save-button" onclick="saveUser(${user.id})">保存</button>
                            <button class="action-button cancel-button" onclick="cancelEdit(${user.id})">取消</button>
                        </div>
                    </td>
                `;
                userList.appendChild(row);
            });
        })
        .catch(error => console.error('Error loading users:', error));
}

// 加载用户选择器
function loadUserStatsSelect() {
    fetch('/api/user/all')
        .then(response => response.json())
        .then(users => {
            const select = document.getElementById('userStatsSelect');
            select.innerHTML = '<option value="">请选择用户查看统计</option>';

            users.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.text = user.username;
                select.appendChild(option);
            });

            // 绑定选择事件
            select.onchange = function() {
                const userId = this.value;
                if (userId) {
                    loadUserPurchaseStats(userId);
                } else {
                    document.getElementById('userPurchaseStats').innerHTML = '';
                }
            };
        })
        .catch(error => console.error('Error loading users for select:', error));
}

// 创建用户
function createUser() {
    const username = document.getElementById("newUsername").value;
    const password = document.getElementById("newPassword").value;

    fetch('/api/user/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
    })
        .then(response => response.text())
        .then(message => {
            alert(message);
            loadUsers(); // 重新加载用户列表
            loadUserStatsSelect(); // 重新加载用户选择器
            // 清空表单
            document.getElementById("newUsername").value = '';
            document.getElementById("newPassword").value = '';
        })
        .catch(error => console.error('Error creating user:', error));
}

// 编辑用户
function editUser(userId) {
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);
    if (!row) return;

    // 显示编辑输入框
    row.querySelector('.user-display').style.display = 'none';
    row.querySelector('.edit-username-input').style.display = 'inline-block';

    row.querySelector('.balance-display').style.display = 'none';
    row.querySelector('.edit-input').style.display = 'inline-block';

    // 显示编辑按钮
    row.querySelector('.normal-buttons').style.display = 'none';
    row.querySelector('.edit-buttons').style.display = 'flex';
}

// 保存用户修改
function saveUser(userId) {
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);
    if (!row) return;

    const newUsername = row.querySelector('.edit-username-input').value;
    const newBalance = row.querySelector('.edit-input').value;

    // 构造更新数据
    const userData = {
        id: userId,
        username: newUsername,
        balance: parseFloat(newBalance)
    };

    // 发送更新请求
    fetch('/api/user/update', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userData),
    })
        .then(response => response.text())
        .then(message => {
            alert(message);
            loadUsers(); // 重新加载用户列表
            loadUserStatsSelect(); // 重新加载用户选择器
        })
        .catch(error => {
            console.error('Error updating user:', error);
            alert('更新用户失败: ' + error.message);
        });
}

// 取消编辑
function cancelEdit(userId) {
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);
    if (!row) return;

    // 恢复显示
    row.querySelector('.user-display').style.display = 'inline';
    row.querySelector('.edit-username-input').style.display = 'none';

    row.querySelector('.balance-display').style.display = 'inline';
    row.querySelector('.edit-input').style.display = 'none';

    // 恢复按钮
    row.querySelector('.normal-buttons').style.display = 'block';
    row.querySelector('.edit-buttons').style.display = 'none';
}

// 删除用户
function deleteUser(userId) {
    if (confirm("确定要删除这个用户吗？")) {
        fetch(`/api/user/delete/${userId}`, {
            method: 'DELETE'
        })
            .then(response => response.text())
            .then(message => {
                alert(message);
                loadUsers(); // 重新加载用户列表
                loadUserStatsSelect(); // 重新加载用户选择器
            })
            .catch(error => console.error('Error deleting user:', error));
    }
}

// 修改用户余额的函数
function updateUserBalance(userId) {
    const newBalance = prompt("请输入新的余额:");
    if (newBalance === null) return; // 用户取消了

    const balance = parseFloat(newBalance);
    if (isNaN(balance) || balance < 0) {
        alert("请输入有效的非负数余额");
        return;
    }

    fetch(`/api/user/update-balance/${userId}?balance=${balance}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
    })
        .then(response => response.text())
        .then(message => {
            alert(message);
            loadUsers(); // 重新加载用户列表
            loadUserStatsSelect(); // 重新加载用户选择器
        })
        .catch(error => {
            console.error('Error updating user balance:', error);
            alert('更新用户余额失败');
        });
}

// 加载用户购买统计
function loadUserPurchaseStats(userId) {
    fetch(`/api/order/user/${userId}`)
        .then(response => response.json())
        .then(orders => {
            const statsDiv = document.getElementById('userPurchaseStats');
            statsDiv.innerHTML = ''; // 清空内容

            // 调试信息
            console.log("用户订单数据:", orders);

            if (orders && orders.length > 0) {
                // 收集所有订单项
                const allOrderItems = [];
                orders.forEach(order => {
                    if (order.orderItems && Array.isArray(order.orderItems)) {
                        allOrderItems.push(...order.orderItems);
                    }
                });

                console.log("所有订单项:", allOrderItems);

                if (allOrderItems.length > 0) {
                    // 统计商品购买数量
                    const productStats = {};
                    allOrderItems.forEach(item => {
                        const productId = item.productId;
                        const productName = item.productName || `商品${productId}`;
                        const quantity = item.quantity || 0;

                        if (productStats[productId]) {
                            productStats[productId].quantity += quantity;
                        } else {
                            productStats[productId] = {
                                productId: productId,
                                productName: productName,
                                quantity: quantity
                            };
                        }
                    });

                    console.log("商品统计:", productStats);

                    // 转换为数组并排序
                    const sortedStats = Object.values(productStats)
                        .filter(stat => stat.quantity > 0)
                        .sort((a, b) => b.quantity - a.quantity);

                    if (sortedStats.length > 0) {
                        // 创建表格
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
            } else {
                statsDiv.innerHTML = '<p>暂无购买记录</p>';
            }
        })
        .catch(error => {
            console.error('加载用户购买统计失败:', error);
            document.getElementById('userPurchaseStats').innerHTML = '<p>加载失败</p>';
        });
}
