// 获取商品列表
async function fetchProducts() {
    const response = await fetch('/api/product/all');
    const products = await response.json();
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
                    <button onclick="deleteProduct(${product.id})">删除</button>
                </td>
            </tr>
        `;
        tableBody.innerHTML += row;
    });
}

// 添加商品
async function addProduct() {
    const product = {
        name: document.getElementById('productName').value,
        brand: document.getElementById('productBrand').value,
        price: parseFloat(document.getElementById('productPrice').value),
        stock: parseInt(document.getElementById('productStock').value)
    };

    const response = await fetch('/api/product/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(product)
    });

    if (response.ok) {
        alert('商品添加成功！');
        fetchProducts(); // 重新加载列表
    } else {
        alert('商品添加失败！');
    }
}

// 删除商品
async function deleteProduct(id) {
    const response = await fetch(`/api/product/delete/${id}`, { method: 'DELETE' });

    if (response.ok) {
        alert('商品删除成功！');
        fetchProducts();
    } else {
        alert('商品删除失败！');
    }
}

// 加载页面时自动获取商品列表
window.onload = fetchProducts;
