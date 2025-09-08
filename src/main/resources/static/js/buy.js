let selectedProducts = []; // 用户选择的商品列表

// 加载商品数据并渲染
async function fetchProducts() {
    const userId = localStorage.getItem("loggedInUserId");

    // 先获取推荐优先商品 ID
    let productIds = await fetch(`/api/product/recommend/${userId}`).then(res => res.json());

    // 再获取商品列表
    let products = await fetch('/api/product/all').then(res => res.json());

    // 重新排序商品列表：推荐商品靠前
    products.sort((a, b) => {
        const indexA = productIds.indexOf(a.id);
        const indexB = productIds.indexOf(b.id);
        return (indexA === -1 ? 999 : indexA) - (indexB === -1 ? 999 : indexB);
    });

    renderProductTable(products);
}

// 渲染商品表格
function renderProductTable(products) {
    const tableBody = document.querySelector('#productTable tbody');
    tableBody.innerHTML = ''; // 清空表格内容

    products.forEach(product => {
        const row = `
            <tr>
                <td>${product.id}</td>
                <td>${product.name}</td>
                <td>${product.brand}</td>
                <td>${product.price}</td>
                <td>${product.stock}</td>
                <td>
                    <input type="number" min="0" max="${product.stock}" 
                           id="qty_${product.id}" placeholder="购买数量" />
                </td>
            </tr>
        `;
        tableBody.innerHTML += row;
    });
}

// 提交订单
async function submitOrder() {
    const userId = localStorage.getItem("loggedInUserId");
    if (!userId) {
        alert("请先登录！");
        return;
    }

    const productRows = document.querySelectorAll('#productTable tbody tr');
    let productIds = [];
    let quantities = [];

    productRows.forEach(row => {
        const productId = row.children[0].innerText;
        const quantity = Number(document.querySelector(`#qty_${productId}`).value);
        if (quantity > 0) {
            productIds.push(Number(productId));
            quantities.push(quantity);
        }
    });

    if (productIds.length === 0) {
        alert('请选择至少一种商品！');
        return;
    }

    const orderRequest = {
        userId: Number(userId),
        productIds: productIds,
        quantities: quantities
    };

    const response = await fetch('/api/order/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(orderRequest)
    });

    if (response.ok) {
        alert('下单成功，请前往[订单页面]进行支付！');
        window.location.href = "/html/orders.html";
    } else {
        alert('订单提交失败');
    }
}


// 页面加载时获取商品列表
window.onload = function () {
    const userId = localStorage.getItem("loggedInUserId");
    if (!userId) {
        alert("请先登录！");
        // 如果未登录，跳转到登录页面
        window.location.href = "/html/users.html";
        return;
    }
    fetchProducts();
    loadBalance();
    displayUserDetails();
};


async function loadBalance() {
    const userId = localStorage.getItem("loggedInUserId");
    const res = await fetch(`/api/user/id/${userId}`);
    const user = await res.json();
    const balance = user.balance?.toFixed(2) || "0.00";
    document.getElementById("balanceDisplay").innerText = balance;
    document.getElementById('currentUserBalance').innerText = balance;
}

async function recharge() {
    const userId = localStorage.getItem("loggedInUserId");
    const add = parseFloat(document.getElementById("rechargeAmount").value);
    if (isNaN(add) || add <= 0) return alert("请输入有效金额");

    const res = await fetch(`/api/user/id/${userId}`);
    const user = await res.json();
    user.balance = (parseFloat(user.balance) || 0) + add;

    await fetch("/api/user/update", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(user)
    });

    alert("充值成功！");
    loadBalance();
}

async function displayUserDetails() {
    const userId = localStorage.getItem("loggedInUserId");

    if (userId) {
        // 获取用户数据
        const response = await fetch(`/api/user/id/${userId}`);
        const user = await response.json();

        // 显示用户信息
        document.getElementById('currentUserId').innerText = user.id;
        const balance = user.balance?.toFixed(2) || "0.00";
        document.getElementById('currentUserBalance').innerText = balance;
        document.getElementById('balanceDisplay').innerText = balance;
    }
}

