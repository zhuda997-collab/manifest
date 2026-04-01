/**
 * 货单管理系统 — 前端 JS
 * 核心逻辑：货单卡片列表 + 预览面板 + 新增/编辑表单
 */

const MANIFEST_API = '/api/manifest';
const CUSTOMER_API = '/api/customer/all';
const PRODUCT_API = '/api/product/all';

// ── 全局数据 ─────────────────────────────────────────────────
let allManifests = [];
let filteredManifests = [];
let customers = [];
let products = [];
let deleteTargetId = null;
let editMode = false;

// ── 初始化 ─────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    loadData();
});

// ── 加载数据 ────────────────────────────────────────────────────
async function loadData() {
    try {
        const [mRes, cRes, pRes] = await Promise.all([
            fetch(MANIFEST_API),
            fetch(CUSTOMER_API),
            fetch(PRODUCT_API)
        ]);
        allManifests = await mRes.json();
        customers = await cRes.json();
        products = await pRes.json();
        updateStats();
        renderGrid();
    } catch (err) {
        console.error('加载数据失败', err);
        alert('加载数据失败：' + err.message);
    }
}

// ── 统计面板 ───────────────────────────────────────────────────
function updateStats() {
    document.getElementById('totalCount').textContent = allManifests.length;

    const today = new Date().toISOString().slice(0, 10);
    const todayCount = allManifests.filter(m => m.orderDate === today || m.createdAt?.slice(0,10) === today).length;
    document.getElementById('todayCount').textContent = todayCount;

    const now = new Date();
    const monthStart = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-01`;
    const monthTotal = allManifests
        .filter(m => {
            const d = m.orderDate || m.createdAt?.slice(0,10);
            return d >= monthStart;
        })
        .reduce((sum, m) => sum + (m.totalPrice || 0), 0);
    document.getElementById('monthTotal').textContent = '¥' + (monthTotal / 100).toFixed(2);
}

// ── 搜索 ───────────────────────────────────────────────────────
function handleSearch() {
    const kw = document.getElementById('searchInput').value.trim().toLowerCase();
    filteredManifests = kw
        ? allManifests.filter(m =>
            (m.customerName && m.customerName.toLowerCase().includes(kw)) ||
            (m.customerPhone && m.customerPhone.includes(kw))
        )
        : [...allManifests];
    renderGrid();
}

// ── 渲染货单卡片网格 ─────────────────────────────────────────────
function renderGrid() {
    const grid = document.getElementById('manifestGrid');
    const data = filteredManifests.length ? filteredManifests : (filteredManifests = [...allManifests], allManifests);

    if (!data.length) {
        grid.innerHTML = `<div style="grid-column:1/-1;text-align:center;color:var(--gray-400);padding:40px 0);">暂无货单，点击「新增货单」创建</div>`;
        return;
    }

    // 按时间倒序
    const sorted = [...data].sort((a, b) => {
        const ta = a.orderDate || a.createdAt || '';
        const tb = b.orderDate || b.createdAt || '';
        return tb.localeCompare(ta);
    });

    grid.innerHTML = sorted.map(m => {
        const items = m.items || [];
        const itemCount = items.reduce((s, i) => s + (i.quantity || 0), 0);
        const totalYuan = ((m.totalPrice || 0) / 100).toFixed(2);
        const date = formatDate(m.orderDate || m.createdAt);
        const orderDate = m.orderDate ? formatDate(m.orderDate) : '';
        return `<div class="manifest-card" onclick="showPreview(${m.id})">
            <div class="card-header">
                <div>
                    <div class="card-customer">${esc(m.customerName || '未知客户')}</div>
                    <div class="card-phone">📞 ${esc(m.customerPhone || '—')}</div>
                </div>
                <div style="text-align:right;">
                    <div class="card-total">¥${totalYuan}</div>
                    ${orderDate ? `<div class="card-date">${orderDate}</div>` : ''}
                </div>
            </div>
            <div class="card-items-count">共 ${items.length} 种产品 · ${itemCount} 件</div>
            <div class="card-actions" onclick="event.stopPropagation()">
                <button class="btn btn-secondary btn-sm" onclick="openEditModal(${m.id})">编辑</button>
                <button class="btn btn-danger btn-sm" onclick="openConfirm(${m.id})">删除</button>
            </div>
        </div>`;
    }).join('');
}

// ── 预览面板 ────────────────────────────────────────────────────
async function showPreview(id) {
    const m = allManifests.find(x => x.id === id);
    if (!m) return;

    const items = m.items || [];
    const grandTotal = (m.totalPrice || 0) / 100;

    const itemsHtml = items.map(item => {
        const submodelDisplay = item.submodelName ? ` / ${esc(item.submodelName)}` : '';
        const unitYuan = (item.unitPrice / 100).toFixed(2);
        const subYuan = (item.subtotal / 100).toFixed(2);
        return `<tr>
            <td>${esc(item.productName || '')}${submodelDisplay}</td>
            <td>${item.productNo || ''}</td>
            <td>${item.quantity || 0}</td>
            <td>¥${unitYuan}</td>
            <td style="text-align:right;font-weight:600;color:var(--primary);">¥${subYuan}</td>
        </tr>`;
    }).join('');

    const panel = document.getElementById('previewPanel');
    panel.innerHTML = `
        <div class="preview-header">
            <div>
                <h2>📋 货单详情</h2>
                <div style="font-size:13px;color:var(--gray-500);margin-top:4px;">
                    单号：${esc(m.guid || '')} &nbsp;|&nbsp; 日期：${formatDate(m.orderDate || m.createdAt)}
                </div>
            </div>
            <button class="close-btn" onclick="closePreview()">×</button>
        </div>
        <div class="preview-body">
            <div class="preview-section">
                <div class="preview-section-title">👤 客户信息</div>
                <div class="preview-customer-info">
                    <strong>${esc(m.customerName || '—')}</strong><br>
                    📞 ${esc(m.customerPhone || '—')}<br>
                    📍 ${esc(m.customerAddress || '—')}
                </div>
            </div>
            <div class="preview-section">
                <div class="preview-section-title">📦 产品明细（${items.length} 项）</div>
                <div class="item-table-wrapper" style="border-radius:8px;">
                    <table class="preview-table">
                        <thead>
                            <tr>
                                <th>产品 / 子型号</th>
                                <th>产品号</th>
                                <th>件数</th>
                                <th>单价</th>
                                <th style="text-align:right;">小计</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${itemsHtml}
                            <tr class="preview-total-row">
                                <td colspan="4" style="text-align:right;">💰 货单总价</td>
                                <td style="text-align:right;">¥${grandTotal.toFixed(2)}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            ${m.notes ? `<div class="preview-section">
                <div class="preview-section-title">📝 备注</div>
                <div class="preview-notes">${esc(m.notes)}</div>
            </div>` : ''}
            <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:16px;">
                <button class="btn btn-secondary" onclick="closePreview();openEditModal(${m.id})">✏️ 编辑</button>
                <button class="btn btn-cancel" onclick="closePreview()">关闭</button>
            </div>
        </div>`;

    document.getElementById('previewOverlay').classList.add('show');
}

function closePreview() {
    document.getElementById('previewOverlay').classList.remove('show');
}

// ── 新增货单 ────────────────────────────────────────────────────
async function openFormModal() {
    editMode = false;
    document.getElementById('formModalTitle').textContent = '新增货单';
    document.getElementById('manifestForm').reset();
    document.getElementById('editId').value = '';
    document.getElementById('customerSnapshot').style.display = 'none';
    document.getElementById('itemsBody').innerHTML = '';

    // 填充客户下拉框
    const sel = document.getElementById('customerSelect');
    sel.innerHTML = '<option value="">— 请选择客户 —</option>' +
        customers.map(c => `<option value="${c.id}">${esc(c.customerName)} (${esc(c.phone || '无手机')})</option>`).join('');

    // 设置默认日期为今天
    document.getElementById('orderDate').value = new Date().toISOString().slice(0, 10);

    // 添加第一行空明细
    addItemRow();

    document.getElementById('formModal').style.display = 'flex';
    updateGrandTotal();
}

// ── 编辑货单 ────────────────────────────────────────────────────
async function openEditModal(id) {
    const m = allManifests.find(x => x.id === id);
    if (!m) return;

    editMode = true;
    document.getElementById('formModalTitle').textContent = '编辑货单';
    document.getElementById('manifestForm').reset();
    document.getElementById('editId').value = id;
    document.getElementById('notes').value = m.notes || '';
    document.getElementById('orderDate').value = m.orderDate || '';

    // 填充客户下拉框
    const sel = document.getElementById('customerSelect');
    sel.innerHTML = '<option value="">— 请选择客户 —</option>' +
        customers.map(c => `<option value="${c.id}" ${c.id == m.customerId ? 'selected' : ''}>${esc(c.customerName)} (${esc(c.phone || '无手机')})</option>`).join('');

    // 显示客户快照
    showCustomerSnapshot(m.customerId, m.customerName, m.customerPhone, m.customerAddress);

    // 填充明细行
    const tbody = document.getElementById('itemsBody');
    tbody.innerHTML = '';
    const items = m.items || [];
    if (items.length === 0) {
        addItemRow();
    } else {
        items.forEach(item => {
            addItemRow(item.productId, item.quantity, item.unitPrice);
        });
    }

    document.getElementById('formModal').style.display = 'flex';
    updateGrandTotal();
}

function closeFormModal() {
    document.getElementById('formModal').style.display = 'none';
}

// ── 客户快照 ────────────────────────────────────────────────────
function onCustomerChange() {
    const id = parseInt(document.getElementById('customerSelect').value);
    if (!id) {
        document.getElementById('customerSnapshot').style.display = 'none';
        return;
    }
    const c = customers.find(x => x.id === id);
    if (c) {
        showCustomerSnapshot(c.id, c.customerName, c.phone, c.address);
    }
}

function showCustomerSnapshot(id, name, phone, address) {
    const div = document.getElementById('customerSnapshot');
    document.getElementById('snapshotContent').innerHTML =
        `<strong>${esc(name)}</strong> &nbsp;📞 ${esc(phone || '—')} &nbsp;&nbsp;📍 ${esc(address || '—')}`;
    div.style.display = 'block';
}

// ── 添加 / 删除 明细行 ──────────────────────────────────────────
function addItemRow(productId, quantity, unitPrice) {
    const tbody = document.getElementById('itemsBody');
    const row = document.createElement('tr');

    // 产品下拉框
    const productOptions = products.map(p => {
        const label = p.submodelName
            ? `${esc(p.productName)} / ${esc(p.submodelName)} (¥${(p.unitPrice/100).toFixed(2)})`
            : `${esc(p.productName)} (¥${(p.unitPrice/100).toFixed(2)})`;
        const selected = productId == p.id ? 'selected' : '';
        return `<option value="${p.id}" data-price="${p.unitPrice}" data-name="${esc(p.productName)}" data-submodel="${esc(p.submodelName||'')}" data-submodelNo="${p.submodelNo||''}" ${selected}>${label}</option>`;
    }).join('');

    row.innerHTML = `
        <td>
            <select class="product-sel" onchange="onProductChange(this)">${productOptions ? '<option value="">— 选择产品 —</option>' + productOptions : '<option value="">无可用产品</option>'}</select>
        </td>
        <td><input type="number" class="qty-input" value="${quantity || 1}" min="1" oninput="recalcRow(this);updateGrandTotal()"></td>
        <td><input type="number" class="price-input" value="${unitPrice ? (unitPrice/100).toFixed(2) : ''}" step="0.01" min="0" placeholder="0.00" oninput="updateGrandTotal()"></td>
        <td class="subtotal-cell">¥0.00</td>
        <td><button type="button" class="delete-btn" onclick="removeItemRow(this)">删除</button></td>
        <td class="product-no-cell" style="font-size:12px;color:var(--gray-500);"></td>
    `;

    tbody.appendChild(row);

    // 如果传入了 productId，需要触发 product change
    if (productId) {
        const sel = row.querySelector('.product-sel');
        onProductChange(sel);
    }
}

function removeItemRow(btn) {
    const row = btn.closest('tr');
    const tbody = document.getElementById('itemsBody');
    if (tbody.children.length <= 1) {
        alert('至少要保留一行产品明细');
        return;
    }
    row.remove();
    updateGrandTotal();
}

function onProductChange(sel) {
    const row = sel.closest('tr');
    const selected = sel.options[sel.selectedIndex];
    const price = parseInt(selected.dataset.price) || 0;
    const productNo = selected.value ? products.find(p => p.id == parseInt(selected.value))?.productNo || '' : '';

    row.querySelector('.price-input').value = (price / 100).toFixed(2);
    row.querySelector('.product-no-cell').textContent = productNo;
    recalcRow(sel);
    updateGrandTotal();
}

function recalcRow(el) {
    const row = el.closest('tr');
    const qty = parseInt(row.querySelector('.qty-input').value) || 0;
    const priceYuan = parseFloat(row.querySelector('.price-input').value) || 0;
    const priceFen = Math.round(priceYuan * 100);
    const subtotal = qty * priceFen;
    row.querySelector('.subtotal-cell').textContent = '¥' + (subtotal / 100).toFixed(2);
}

function updateGrandTotal() {
    const rows = document.querySelectorAll('#itemsBody tr');
    let total = 0;
    rows.forEach(row => {
        const qty = parseInt(row.querySelector('.qty-input')?.value) || 0;
        const priceYuan = parseFloat(row.querySelector('.price-input')?.value) || 0;
        const priceFen = Math.round(priceYuan * 100);
        total += qty * priceFen;
    });
    document.getElementById('grandTotalDisplay').textContent = (total / 100).toFixed(2);
}

// ── 提交表单 ────────────────────────────────────────────────────
async function submitForm(e) {
    e.preventDefault();

    const customerId = parseInt(document.getElementById('customerSelect').value);
    if (!customerId) { alert('请选择客户'); return; }

    const rows = document.querySelectorAll('#itemsBody tr');
    const items = [];
    let hasValidItem = false;

    rows.forEach(row => {
        const productSel = row.querySelector('.product-sel');
        const productId = parseInt(productSel.value);
        const qty = parseInt(row.querySelector('.qty-input').value) || 0;
        const priceYuan = parseFloat(row.querySelector('.price-input').value) || 0;
        const priceFen = Math.round(priceYuan * 100);

        if (productId && qty > 0) {
            hasValidItem = true;
            const selected = productSel.options[productSel.selectedIndex];
            items.push({
                productId,
                quantity: qty,
                unitPrice: priceFen,
                productName: selected.dataset.name,
                submodelName: selected.dataset.submodel || null,
                submodelNo: parseInt(selected.dataset.submodelNo) || null
            });
        }
    });

    if (!hasValidItem) { alert('请至少添加一个有效产品明细'); return; }

    const payload = {
        customerId,
        notes: document.getElementById('notes').value.trim() || null,
        orderDate: document.getElementById('orderDate').value || null,
        items
    };

    const editId = document.getElementById('editId').value;
    const url = editId ? `${MANIFEST_API}/${editId}` : MANIFEST_API;
    const method = editId ? 'PUT' : 'POST';

    document.getElementById('submitBtn').disabled = true;
    document.getElementById('submitBtn').textContent = '保存中...';

    try {
        const resp = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const result = await resp.json();
        if (result.success !== false) {
            closeFormModal();
            loadData();
        } else {
            alert('保存失败：' + (result.message || '未知错误'));
        }
    } catch (err) {
        alert('请求异常：' + err.message);
    } finally {
        document.getElementById('submitBtn').disabled = false;
        document.getElementById('submitBtn').textContent = '保存货单';
    }
}

// ── 删除 ────────────────────────────────────────────────────────
function openConfirm(id) {
    deleteTargetId = id;
    document.getElementById('confirmModal').style.display = 'flex';
}

function closeConfirm() {
    document.getElementById('confirmModal').style.display = 'none';
    deleteTargetId = null;
}

async function confirmDelete() {
    if (!deleteTargetId) return;
    try {
        const resp = await fetch(`${MANIFEST_API}/${deleteTargetId}`, { method: 'DELETE' });
        const result = await resp.json();
        if (result.success !== false) {
            closeConfirm();
            loadData();
        } else {
            alert('删除失败：' + (result.message || '未知错误'));
        }
    } catch (err) {
        alert('请求异常：' + err.message);
    }
}

// ── 工具函数 ────────────────────────────────────────────────────
function esc(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    if (isNaN(d)) return dateStr;
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`;
}
