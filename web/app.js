/* ==========================================================================
   ATHENA LIBRARY MANAGEMENT SYSTEM - FRONTEND APP JAVASCRIPT
   ========================================================================== */

const API_BASE = '/api';

// --- APPLICATION STATE ---
let state = {
    books: [],
    members: [],
    borrows: [],
    stats: {},
    currentView: 'dashboard',
    theme: localStorage.getItem('athena_theme') || 'dark'
};

// --- DOM ELEMENTS ---
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initNavigation();
    initForms();
    initSearchAndFilters();
    fetchData();
});

// --- THEME MANAGEMENT ---
function initTheme() {
    document.documentElement.setAttribute('data-theme', state.theme);
    const themeBtn = document.getElementById('theme-toggle-btn');
    updateThemeIcon();

    themeBtn.addEventListener('click', () => {
        state.theme = state.theme === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', state.theme);
        localStorage.setItem('athena_theme', state.theme);
        updateThemeIcon();
        showToast(`Switched to ${state.theme} mode`, 'info');
    });
}

function updateThemeIcon() {
    const btn = document.getElementById('theme-toggle-btn');
    btn.innerHTML = state.theme === 'dark' 
        ? '<i class="fa-solid fa-sun"></i>' 
        : '<i class="fa-solid fa-moon"></i>';
}

// --- NAVIGATION & VIEW SWITCHING ---
function initNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetView = link.getAttribute('data-view');
            switchView(targetView);
        });
    });
}

function switchView(viewName) {
    state.currentView = viewName;

    // Update Nav
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.toggle('active', link.getAttribute('data-view') === viewName);
    });

    // Update View Sections
    document.querySelectorAll('.view-section').forEach(sec => {
        sec.classList.remove('active');
    });

    const activeSec = document.getElementById(`view-${viewName}`);
    if (activeSec) activeSec.classList.add('active');

    // Update Page Header Title
    const titles = {
        dashboard: 'Dashboard Overview',
        catalog: 'Book Repository Catalog',
        borrows: 'Active Borrowing & Returns Desk',
        members: 'Library Cardholders Directory',
        analytics: 'Analytics & Reporting'
    };
    document.getElementById('page-title').innerText = titles[viewName] || 'Dashboard';

    if (viewName === 'analytics') {
        renderCharts();
    }
}

// --- DATA FETCHING ---
async function fetchData() {
    try {
        const [statsRes, booksRes, membersRes, borrowsRes] = await Promise.all([
            fetch(`${API_BASE}/stats`).then(r => r.json()),
            fetch(`${API_BASE}/books`).then(r => r.json()),
            fetch(`${API_BASE}/members`).then(r => r.json()),
            fetch(`${API_BASE}/borrows`).then(r => r.json())
        ]);

        state.stats = statsRes;
        state.books = booksRes;
        state.members = membersRes;
        state.borrows = borrowsRes;

        renderAll();
    } catch (err) {
        console.warn('Backend API unavailable or error fetching. Using client fallback store.', err);
        showToast('Running in standalone preview mode', 'warning');
    }
}

// --- RENDER ALL VIEWS ---
function renderAll() {
    renderStats();
    renderRecentBorrows();
    renderCatalog();
    renderBorrowsTable();
    renderMembers();
}

function renderStats() {
    document.getElementById('stat-total-books').innerText = state.stats.totalBooks || state.books.reduce((acc, b) => acc + b.totalCopies, 0);
    document.getElementById('stat-available-copies').innerText = state.stats.availableCopies || state.books.reduce((acc, b) => acc + b.availableCopies, 0);
    document.getElementById('stat-active-borrowers').innerText = state.stats.activeBorrowers || state.borrows.filter(b => b.status === 'Borrowed' || b.status === 'Overdue').length;
    document.getElementById('stat-overdue-count').innerText = state.stats.overdueCount || state.borrows.filter(b => b.status === 'Overdue').length;
}

// --- DASHBOARD RECENT BORROWS ---
function renderRecentBorrows() {
    const tbody = document.getElementById('recent-borrows-tbody');
    tbody.innerHTML = '';

    const recent = [...state.borrows].reverse().slice(0, 5);
    recent.forEach(b => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${escapeHtml(b.bookTitle)}</strong></td>
            <td>${escapeHtml(b.memberName)}</td>
            <td>${b.issueDate}</td>
            <td>${b.dueDate}</td>
            <td><span class="badge ${getStatusBadgeClass(b.status)}">${b.status}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// --- BOOK CATALOG RENDER ---
function renderCatalog() {
    const container = document.getElementById('books-grid-container');
    const search = document.getElementById('catalog-search').value.toLowerCase();
    const cat = document.getElementById('category-filter').value;

    const filtered = state.books.filter(b => {
        const matchesSearch = b.title.toLowerCase().includes(search) || 
                              b.author.toLowerCase().includes(search) || 
                              b.isbn.includes(search);
        const matchesCat = cat === 'all' || b.category === cat;
        return matchesSearch && matchesCat;
    });

    container.innerHTML = '';

    if (filtered.length === 0) {
        container.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-muted);">No books found matching criteria.</div>`;
        return;
    }

    filtered.forEach(book => {
        const card = document.createElement('div');
        card.className = 'glass-card book-card';
        card.innerHTML = `
            <div class="book-cover-container">
                <img src="${book.coverUrl}" alt="${escapeHtml(book.title)}" class="book-cover" onerror="this.src='https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400&q=80'">
                <span class="book-category-tag">${book.category}</span>
            </div>
            <div class="book-info">
                <h4>${escapeHtml(book.title)}</h4>
                <span class="author">by ${escapeHtml(book.author)}</span>
                <div style="font-size: 0.75rem; color: var(--text-muted); margin-bottom: 8px;">
                    ISBN: ${book.isbn} | Year: ${book.publishedYear || 'N/A'}
                </div>
                <div class="book-meta">
                    <span class="rating"><i class="fa-solid fa-star"></i> ${book.rating}</span>
                    <span class="badge ${book.availableCopies > 0 ? 'badge-success' : 'badge-danger'}">
                        ${book.availableCopies}/${book.totalCopies} Available
                    </span>
                </div>
                <div style="margin-top: 14px; display: flex; gap: 8px;">
                    <button class="btn btn-primary btn-sm" style="flex:1;" ${book.availableCopies === 0 ? 'disabled' : ''} onclick="openIssueModalForBook('${book.id}')">
                        <i class="fa-solid fa-hand-holding"></i> Issue
                    </button>
                    <button class="icon-btn" title="Delete Book" onclick="deleteBook('${book.id}')">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
        container.appendChild(card);
    });
}

// --- BORROWS DESK TABLE ---
function renderBorrowsTable() {
    const tbody = document.getElementById('borrows-table-body');
    tbody.innerHTML = '';

    state.borrows.forEach(b => {
        const tr = document.createElement('tr');
        const fineText = b.fineAmount > 0 ? `$${b.fineAmount.toFixed(2)}` : '$0.00';
        tr.innerHTML = `
            <td><code>${b.id}</code></td>
            <td><strong>${escapeHtml(b.bookTitle)}</strong></td>
            <td>${escapeHtml(b.memberName)}</td>
            <td>${b.issueDate}</td>
            <td>${b.dueDate}</td>
            <td><span class="badge ${getStatusBadgeClass(b.status)}">${b.status}</span></td>
            <td><span class="${b.fineAmount > 0 ? 'text-warning font-bold' : ''}">${fineText}</span></td>
            <td>
                ${b.status !== 'Returned' ? `<button class="btn btn-secondary btn-sm" onclick="returnBook('${b.id}')"><i class="fa-solid fa-rotate-left"></i> Return</button>` : '<span style="color: var(--text-muted); font-size: 0.8rem;"><i class="fa-solid fa-check"></i> Completed</span>'}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// --- MEMBERS DIRECTORY RENDER ---
function renderMembers() {
    const container = document.getElementById('members-grid-container');
    const search = document.getElementById('member-search').value.toLowerCase();

    const filtered = state.members.filter(m => 
        m.name.toLowerCase().includes(search) || 
        m.email.toLowerCase().includes(search) || 
        m.id.toLowerCase().includes(search)
    );

    container.innerHTML = '';
    filtered.forEach(m => {
        const activeLoans = state.borrows.filter(b => b.memberId === m.id && b.status !== 'Returned').length;
        const initials = m.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();

        const card = document.createElement('div');
        card.className = 'glass-card member-card';
        card.innerHTML = `
            <div class="member-avatar">${initials}</div>
            <div class="member-details">
                <h4>${escapeHtml(m.name)} <span class="badge ${m.status === 'Active' ? 'badge-success' : 'badge-danger'}" style="font-size: 0.65rem; margin-left: 6px;">${m.status}</span></h4>
                <p><i class="fa-solid fa-envelope"></i> ${escapeHtml(m.email)}</p>
                <p><i class="fa-solid fa-phone"></i> ${escapeHtml(m.phone)}</p>
                <div style="margin-top: 10px; font-size: 0.8rem; display: flex; gap: 12px; color: var(--text-muted);">
                    <span>Tier: <strong>${m.membershipType}</strong></span>
                    <span>Loans: <strong>${activeLoans} Active</strong></span>
                </div>
            </div>
        `;
        container.appendChild(card);
    });
}

// --- SEARCH & FILTER LISTENERS ---
function initSearchAndFilters() {
    document.getElementById('catalog-search').addEventListener('input', renderCatalog);
    document.getElementById('category-filter').addEventListener('change', renderCatalog);
    document.getElementById('member-search').addEventListener('input', renderMembers);
}

// --- MODAL & FORM HANDLERS ---
function initForms() {
    // Book Form
    document.getElementById('book-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const newBook = {
            title: document.getElementById('book-title').value,
            author: document.getElementById('book-author').value,
            category: document.getElementById('book-category').value,
            isbn: document.getElementById('book-isbn').value,
            totalCopies: parseInt(document.getElementById('book-copies').value),
            coverUrl: document.getElementById('book-cover').value || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400&q=80',
            description: document.getElementById('book-description').value
        };

        try {
            const res = await fetch(`${API_BASE}/books`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(newBook)
            }).then(r => r.json());

            state.books.push(res);
            closeModal('book-modal');
            renderAll();
            showToast('Book added successfully!', 'success');
        } catch (err) {
            showToast('Failed to add book', 'danger');
        }
    });

    // Member Form
    document.getElementById('member-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const newMember = {
            name: document.getElementById('member-name').value,
            email: document.getElementById('member-email').value,
            phone: document.getElementById('member-phone').value,
            membershipType: document.getElementById('member-tier').value
        };

        try {
            const res = await fetch(`${API_BASE}/members`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(newMember)
            }).then(r => r.json());

            state.members.push(res);
            closeModal('member-modal');
            renderAll();
            showToast('Member registered successfully!', 'success');
        } catch (err) {
            showToast('Failed to register member', 'danger');
        }
    });

    // Issue Form
    document.getElementById('issue-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            bookId: document.getElementById('issue-book-select').value,
            memberId: document.getElementById('issue-member-select').value,
            days: document.getElementById('issue-days').value
        };

        try {
            const res = await fetch(`${API_BASE}/borrows`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                const errData = await res.json();
                showToast(errData.error || 'Failed to issue book', 'danger');
                return;
            }

            const record = await res.json();
            state.borrows.push(record);
            
            // update local available copies
            const b = state.books.find(x => x.id === record.bookId);
            if (b && b.availableCopies > 0) b.availableCopies--;

            closeModal('issue-modal');
            renderAll();
            showToast('Book issued successfully!', 'success');
        } catch (err) {
            showToast('Error processing borrow request', 'danger');
        }
    });
}

// --- ACTIONS ---
async function returnBook(borrowId) {
    try {
        const res = await fetch(`${API_BASE}/borrows`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: borrowId })
        }).then(r => r.json());

        const idx = state.borrows.findIndex(x => x.id === borrowId);
        if (idx !== -1) state.borrows[idx] = res;

        const book = state.books.find(x => x.id === res.bookId);
        if (book) book.availableCopies = Math.min(book.totalCopies, book.availableCopies + 1);

        renderAll();
        showToast('Book returned to inventory', 'success');
    } catch (err) {
        showToast('Failed to return book', 'danger');
    }
}

async function deleteBook(bookId) {
    if (!confirm('Are you sure you want to delete this book?')) return;

    try {
        await fetch(`${API_BASE}/books?id=${bookId}`, { method: 'DELETE' });
        state.books = state.books.filter(b => b.id !== bookId);
        renderAll();
        showToast('Book deleted', 'info');
    } catch (err) {
        showToast('Failed to delete book', 'danger');
    }
}

// --- MODALS OPEN/CLOSE ---
function openAddBookModal() {
    document.getElementById('book-form').reset();
    document.getElementById('book-modal').classList.add('active');
}

function openAddMemberModal() {
    document.getElementById('member-form').reset();
    document.getElementById('member-modal').classList.add('active');
}

function openIssueModal() {
    populateIssueDropdowns();
    document.getElementById('issue-modal').classList.add('active');
}

function openIssueModalForBook(bookId) {
    populateIssueDropdowns();
    document.getElementById('issue-book-select').value = bookId;
    document.getElementById('issue-modal').classList.add('active');
}

function populateIssueDropdowns() {
    const bookSelect = document.getElementById('issue-book-select');
    bookSelect.innerHTML = '';
    state.books.filter(b => b.availableCopies > 0).forEach(b => {
        bookSelect.innerHTML += `<option value="${b.id}">${escapeHtml(b.title)} (${b.availableCopies} available)</option>`;
    });

    const memberSelect = document.getElementById('issue-member-select');
    memberSelect.innerHTML = '';
    state.members.filter(m => m.status === 'Active').forEach(m => {
        memberSelect.innerHTML += `<option value="${m.id}">${escapeHtml(m.name)} (${m.membershipType})</option>`;
    });
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

// --- CHARTS (CHART.JS) ---
let monthlyChart = null;
let categoryChart = null;
let miniGenreChart = null;

function renderCharts() {
    // Monthly Trend Chart
    const ctx1 = document.getElementById('monthlyTrendChart').getContext('2d');
    if (monthlyChart) monthlyChart.destroy();

    monthlyChart = new Chart(ctx1, {
        type: 'line',
        data: {
            labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug'],
            datasets: [{
                label: 'Books Borrowed',
                data: [42, 58, 65, 78, 90, 84, 95, 112],
                borderColor: '#8b5cf6',
                backgroundColor: 'rgba(139, 92, 246, 0.15)',
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: { grid: { color: 'rgba(255,255,255,0.05)' } },
                x: { grid: { display: false } }
            }
        }
    });

    // Category Pie Chart
    const ctx2 = document.getElementById('categoryPieChart').getContext('2d');
    if (categoryChart) categoryChart.destroy();

    const counts = {};
    state.books.forEach(b => { counts[b.category] = (counts[b.category] || 0) + 1; });

    categoryChart = new Chart(ctx2, {
        type: 'doughnut',
        data: {
            labels: Object.keys(counts),
            datasets: [{
                data: Object.values(counts),
                backgroundColor: ['#8b5cf6', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899']
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { position: 'bottom' } }
        }
    });
}

// --- UTILITY HELPERS ---
function getStatusBadgeClass(status) {
    switch(status) {
        case 'Borrowed': return 'badge-info';
        case 'Overdue': return 'badge-danger';
        case 'Returned': return 'badge-success';
        default: return 'badge-secondary';
    }
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <i class="fa-solid ${type === 'success' ? 'fa-check-circle' : type === 'danger' ? 'fa-circle-exclamation' : 'fa-info-circle'}"></i>
        <span>${escapeHtml(message)}</span>
    `;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}
