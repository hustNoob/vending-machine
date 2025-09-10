window.onload = function () {
    loadUsers();

    // 提交新增用户表单
    const userCreateForm = document.getElementById("userCreateForm");
    userCreateForm.onsubmit = function (event) {
        event.preventDefault();
        createUser();
    };
};

// 加载用户列表
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
                            <button class="action-button edit-button" onclick="editUser(${user.id})">编辑</button>
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
            .then(response => {
                // 检查响应是否成功
                if (!response.ok) {
                    // 直接读取响应文本，而不是尝试解析 JSON（因为可能不是 JSON）
                    return response.text().then(text => {
                        // 如果返回的是文本错误信息（我们的业务异常）
                        if (text.includes("无法删除用户")) {
                            throw new Error(text); // 抛出一个包含业务错误的消息的 Error 对象
                        } else {
                            // 其他非业务异常的错误（可能是 500 错误等）
                            throw new Error(`删除失败: ${response.status} ${response.statusText}`);
                        }
                    });
                }
                // 成功的话，返回默认消息
                return response.text();
            })
            .then(message => {
                alert(message); // 显示从后端返回的文本消息
                loadUsers(); // 重新加载用户列表
            })
            .catch(error => {
                console.error('Error deleting user:', error);
                if (error.message) {
                    alert(error.message); // 显示业务错误信息，或通用错误
                } else {
                    alert('删除用户失败');
                }
            });
    }
}
