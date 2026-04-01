/**
 * 客户管理系统 — 前端 JS
 */

const API_BASE = '/api/customer';
let allData = [];
let currentData = [];
let currentPage = 1;
const PAGE_SIZE = 10;
let deleteTargetId = null;

async function loadData() {
    try {
        const resp = await fetch(API_BASE);
        if (!resp.ok) throw new Error('加载数据失败');
        allData = await resp.json();
        currentData = [...allData];
        updateStats();
        goToPage(1);
    } catch (err) {
        alert(err.message);
    }
}

function handleSearch() {
    const kw = document.getElementById('searchInput').value.trim().toLowerCase();
    if (!kw) {
        currentData = [...allData];
    } else {
        currentData = allData.filter(item =>
            (item.customerName && item.customerName.toLowerCase().includes(kw)) ||
            (item.phone && item.phone.includes(kw))
        );
    }
    currentPage = 1;
    renderTable();
    renderPagination();
}

function renderTable() {
    const start = (currentPage - 1) * PAGE_SIZE;
    const pageItems = currentData.slice(start, start + PAGE_SIZE);
    const tbody = document.getElementById('tableBody');

    if (pageItems.length === 0) {
        tbody.innerHTML = `<tr class="empty-row"><td colspan="8">暂无数据</td></tr>`;
        return;
    }

    tbody.innerHTML = pageItems.map(item => {
        const createdAt = item.createdAt ? formatDate(item.createdAt) : '—';
        const updatedAt = item.updatedAt ? formatDate(item.updatedAt) : '—';
        return `<tr>
            <td>${item.id}</td>
            <td><span style="font-family:monospace;font-size:11px;color:var(--gray-500)">${esc(item.guid || '')}</span></td>
            <td><strong>${esc(item.customerName || '')}</strong></td>
            <td>${esc(item.phone || '—')}</td>
            <td title="${esc(item.address || '')}">${esc(truncate(item.address, 30) || '—')}</td>
            <td style="font-size:12px;color:var(--gray-500)">${createdAt}</td>
            <td style="font-size:12px;color:var(--gray-500)">${updatedAt}</td>
            <td>
                <div class="actions">
                    <button class="btn btn-secondary btn-sm" onclick='openModal("edit", ${item.id})'>编辑</button>
                    <button class="btn btn-danger btn-sm" onclick='openConfirm(${item.id})'>删除</button>
                </div>
            </td>
        </tr>`;
    }).join('');
}

function updateStats() {
    document.getElementById('totalCount').textContent = allData.length;
}

function goToPage(page) {
    const totalPages = Math.max(1, Math.ceil(currentData.length / PAGE_SIZE));
    if (page < 1) page = 1;
    if (page > totalPages) page = totalPages;
    currentPage = page;
    renderTable();
    renderPagination();
}

function renderPagination() {
    const totalPages = Math.max(1, Math.ceil(currentData.length / PAGE_SIZE));
    const container = document.getElementById('pagination');
    if (totalPages <= 1) { container.innerHTML = ''; return; }

    let html = '';
    html += `<button class="page-btn" onclick="goToPage(${currentPage - 1})" ${currentPage === 1 ? 'disabled' : ''}>‹</button>`;
    for (let i = 1; i <= totalPages; i++) {
        if (i === 1 || i === totalPages || (i >= currentPage - 2 && i <= currentPage + 2)) {
            html += `<button class="page-btn ${i === currentPage ? 'active' : ''}" onclick="goToPage(${i})">${i}</button>`;
        } else if (i === currentPage - 3 || i === currentPage + 3) {
            html += `<span style="padding:0 4px;color:var(--gray-500)">…</span>`;
        }
    }
    html += `<button class="page-btn" onclick="goToPage(${currentPage + 1})" ${currentPage === totalPages ? 'disabled' : ''}>›</button>`;
    container.innerHTML = html;
}

function openModal(mode, id) {
    const form = document.getElementById('customerForm');
    form.reset();
    document.getElementById('editId').value = '';
    document.getElementById('modalTitle').textContent = '新增客户';

    if (mode === 'edit' && id) {
        const item = allData.find(i => i.id === id);
        if (!item) return;
        document.getElementById('modalTitle').textContent = '编辑客户';
        document.getElementById('editId').value = id;
        document.getElementById('customerName').value = item.customerName;
        document.getElementById('phone').value = item.phone || '';
        document.getElementById('address').value = item.address || '';
    }

    document.getElementById('formModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('formModal').style.display = 'none';
}

async function submitForm(e) {
    e.preventDefault();
    const editId = document.getElementById('editId').value;

    const payload = {
        customerName: document.getElementById('customerName').value.trim(),
        phone: document.getElementById('phone').value.trim() || null,
        address: document.getElementById('address').value.trim() || null,
    };
    if (!editId) {
        payload.guid = generateUUID();
    }

    const url = editId ? `${API_BASE}/${editId}` : API_BASE;
    const method = editId ? 'PUT' : 'POST';

    try {
        const resp = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        const result = await resp.json();
        if (result.success !== false) {
            closeModal();
            loadData();
        } else {
            alert('操作失败：' + result.message);
        }
    } catch (err) {
        alert('请求异常：' + err.message);
    }
}

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
        const resp = await fetch(`${API_BASE}/${deleteTargetId}`, { method: 'DELETE' });
        const result = await resp.json();
        if (result.success !== false) {
            closeConfirm();
            loadData();
        } else {
            alert('删除失败：' + result.message);
        }
    } catch (err) {
        alert('请求异常：' + err.message);
    }
}

function esc(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function truncate(str, len) {
    if (!str) return '';
    return str.length > len ? str.slice(0, len) + '…' : str;
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

document.addEventListener('DOMContentLoaded', loadData);
