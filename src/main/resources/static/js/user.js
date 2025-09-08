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
                row.innerHTML = `
                    <td>${user.id}</td>
                    <td>${user.username}</td>
                    <td>${user.balance}</td>
                    <td>
                        <button onclick="deleteUser(${user.id})">删除</button>
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
        })
        .catch(error => console.error('Error creating user:', error));
}

// 删除用户
function deleteUser(userId) {
    fetch(`/api/user/delete/${userId}`, {
        method: 'DELETE'
    })
        .then(response => response.text())
        .then(message => {
            alert(message);
            loadUsers(); // 重新加载用户列表
        })
        .catch(error => console.error('Error deleting user:', error));
}
