window.onload = function () {
    loadProducts(); // 页面加载时加载商品列表

    // 新增商品提交表单
    const productCreateForm = document.getElementById("productCreateForm");
    productCreateForm.onsubmit = function (event) {
        event.preventDefault();
        createProduct();
    };
};

// 加载商品列表
function loadProducts() {
    fetch('/api/product/all') // 调用后端接口，获取商品列表
        .then(response => response.json())
        .then(products => {
            const productList = document.getElementById("productList");
            productList.innerHTML = ''; // 清除上一次的列表内容
            products.forEach(product => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${product.id}</td>
                    <td>${product.name}</td>
                    <td>${product.price.toFixed(2)}</td>
                    <td>
                        <button onclick="deleteProduct(${product.id})">删除</button>
                    </td>
                `;
                productList.appendChild(row);
            });
        })
        .catch(error => console.error('Error loading products:', error));
}

// 新增商品
function createProduct() {
    const name = document.getElementById("productName").value; // 获取商品名称
    const price = document.getElementById("productPrice").value; // 获取商品价格

    fetch('/api/product/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, price }),
    })
        .then(response => response.text())
        .then(message => {
            alert(message); // 提示成功或失败消息
            loadProducts(); // 重新加载商品列表
        })
        .catch(error => console.error('Error creating product:', error));
}

// 删除商品
function deleteProduct(productId) {
    if (confirm("确定要删除这个商品吗？")) {
        fetch(`/api/product/delete/${productId}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (!response.ok) {
                    // 同样处理自定义错误信息
                    return response.text().then(text => {
                        if (text.includes("无法删除商品")) {
                            throw new Error(text);
                        } else {
                            throw new Error(`删除失败: ${response.status} ${response.statusText}`);
                        }
                    });
                }
                return response.text();
            })
            .then(message => {
                alert(message);
                loadProducts(); // 重新加载商品列表
            })
            .catch(error => {
                console.error('Error deleting product:', error);
                if (error.message) {
                    alert(error.message);
                } else {
                    alert('删除商品失败');
                }
            });
    }
}
