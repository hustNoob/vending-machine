// 获取全量用户
async function fetchUsers() {
    const response = await fetch('/api/user/all');
    const users = await response.json();
    renderUserTable(users);
}

// 渲染用户表格
function renderUserTable(users) {
    const tableBody = document.querySelector('#userTable tbody');
    tableBody.innerHTML = ''; // 清空表格内容

    // 遍历用户数据
    users.forEach(user => {
        const row = `
            <tr>
                <td>${user.id}</td>
                <td><input value="${user.username}" onchange="editUser(${user.id}, 'username', this.value)" /></td>
                <td><input type="text" value="${user.password}" onchange="editUser(${user.id}, 'password', this.value)" /></td>
                <td><input value="${user.email}" onchange="editUser(${user.id}, 'email', this.value)" /></td>
                <td><input value="${user.phone}" onchange="editUser(${user.id}, 'phone', this.value)" /></td>
                <td>
                    <input type="number" value="${user.balance || 0}" step="0.01" onchange="editUser(${user.id}, 'balance', this.value)" />
                </td>
                <td>
                    <button onclick="saveUser(${user.id})">保存</button>
                    <button onclick="deleteUser(${user.id})">删除</button>
                </td>
            </tr>
        `;
        tableBody.innerHTML += row;

        // 初始化用户编辑缓存
        userEditMap[user.id] = {
            id: user.id,
            username: user.username,
            password: user.password,
            email: user.email,
            phone: user.phone,
            balance: user.balance || 0
        };
    });
}

let userEditMap = {}; // 缓存每个用户修改的数据

function editUser(userId, field, value) {
    if (!userEditMap[userId]) {
        console.warn(`用户 ${userId} 不存在`);
        return;
    }
    userEditMap[userId][field] = value;
}

async function saveUser(userId) {
    const userData = userEditMap[userId];
    if (!userData) {
        alert("无效用户");
        return;
    }
    const response = await fetch('/api/user/update', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userData)
    });

    const text = await response.text();
    alert(text);
    fetchUsers();
}

//删除用户
async function deleteUser(userId) {
    if (!confirm("确认要删除该用户吗？")) return;

    const response = await fetch(`/api/user/delete/${userId}`, {
        method: 'DELETE'
    });

    const text = await response.text();
    alert(text);
    fetchUsers();
}


// 获取单个用户详细信息
async function fetchUserDetails(userId) {
    const response = await fetch(`/api/user/id/${userId}`);
    const user = await response.json();

    alert(`
        用户ID: ${user.id}\n
        用户名: ${user.username}\n
        密码： ${user.password}\n
        邮箱: ${user.email}\n
        电话: ${user.phone}\n
        余额: ${user.balance || 0}
    `);
}

// 注册用户
async function registerUser() {
    const user = {
        username: document.getElementById('username').value,
        password: document.getElementById('password').value,
        email: document.getElementById('email').value,
        phone: document.getElementById('phone').value
    };

    const response = await fetch('/api/user/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(user)
    });

    if (response.ok) {
        alert('用户注册成功！');
        fetchUsers(); // 刷新列表
    } else {
        alert('用户注册失败，可能用户名已存在！');
    }
}

// 用户登录并保存 ID
async function loginUser() {
    const user = {
        username: document.getElementById('loginUsername').value,
        password: document.getElementById('loginPassword').value
    };

    const response = await fetch('/api/user/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(user)
    });

    if (response.ok) {
        const text = await response.text();
        alert(text);
        // 再获取完整用户信息
        const userInfoResp = await fetch(`/api/user/username/${user.username}`);
        const userInfo = await userInfoResp.json();
        localStorage.setItem("loggedInUserId", userInfo.id); // 保存用户ID
        location.href = "/html/buy.html"; // 跳转到购买页
    } else {
        alert("登录失败，请重试！");
    }
}


// 页面加载时自动获取用户列表
window.onload = fetchUsers;
