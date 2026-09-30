// ==========================================
// ATHENA Premium Library Management System
// ==========================================

const API_BASE = window.location.origin;
let currentUser = null;

// ==========================================
// UI Helpers
// ==========================================
function showToast(message, type = 'success') {
    Toastify({
        text: message,
        duration: 3000,
        gravity: "top",
        position: "right",
        style: {
            background: type === 'success' ? "#10b981" : "#ef4444",
            borderRadius: "8px",
            fontFamily: "Inter, sans-serif",
            fontSize: "14px",
            boxShadow: "0 4px 12px rgba(0,0,0,0.1)"
        }
    }).showToast();
}

function switchAuthTab(tab) {
    document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.auth-form').forEach(f => f.style.display = 'none');
    
    if (tab === 'login') {
        document.querySelector('.auth-tab:nth-child(1)').classList.add('active');
        document.getElementById('login-form').style.display = 'block';
    } else {
        document.querySelector('.auth-tab:nth-child(2)').classList.add('active');
        document.getElementById('signup-form').style.display = 'block';
    }
}

function switchNav(target) {
    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    document.querySelector(`.nav-item[data-target="${target}"]`)?.classList.add('active');
    
    document.querySelectorAll('.view').forEach(view => view.classList.remove('active-view'));
    document.getElementById(`view-${target}`).classList.add('active-view');

    // Trigger data loads
    if (target === 'dashboard') loadStats();
    if (target === 'catalog') loadCatalog();
    if (target === 'members') loadMembers();
}

// ==========================================
// Authentication
// ==========================================
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = e.target.querySelector('button');
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Authenticating...';
    
    try {
        const res = await fetch(`${API_BASE}/api/login`, {
            method: 'POST',
            body: JSON.stringify({
                email: document.getElementById('login-email').value,
                password: document.getElementById('login-password').value
            })
        });
        const data = await res.json();
        if (data.success) {
            currentUser = data;
            document.getElementById('auth-page').style.display = 'none';
            document.getElementById('dashboard-page').style.display = 'flex';
            document.getElementById('current-user-name').innerText = data.name;
            document.getElementById('current-user-role').innerText = data.roleId === 1 ? 'Admin' : 'Patron';
            showToast("Welcome back, " + data.name);
            switchNav('dashboard');
        } else {
            showToast(data.error || "Invalid credentials", "error");
        }
    } catch (err) {
        showToast("Connection failed", "error");
    } finally {
        btn.innerHTML = 'Sign In <i class="fa-solid fa-arrow-right"></i>';
    }
});

document.getElementById('signup-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = e.target.querySelector('button');
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Creating...';
    
    try {
        const res = await fetch(`${API_BASE}/api/signup`, {
            method: 'POST',
            body: JSON.stringify({
                name: document.getElementById('signup-name').value,
                email: document.getElementById('signup-email').value,
                password: document.getElementById('signup-password').value
            })
        });
        const data = await res.json();
        if (data.success) {
            showToast("Account created! Please sign in.");
            switchAuthTab('login');
        } else {
            showToast(data.error || "Signup failed", "error");
        }
    } catch (err) {
        showToast("Connection failed", "error");
    } finally {
        btn.innerHTML = 'Create Account <i class="fa-solid fa-user-plus"></i>';
    }
});

function logout() {
    currentUser = null;
    document.getElementById('dashboard-page').style.display = 'none';
    document.getElementById('auth-page').style.display = 'flex';
    document.getElementById('login-form').reset();
    showToast("Signed out successfully");
}

// ==========================================
// Dashboard Logic
// ==========================================
document.querySelectorAll('.sidebar-nav .nav-item').forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        const target = e.currentTarget.getAttribute('data-target');
        if (target) switchNav(target);
    });
});

async function loadStats() {
    try {
        const res = await fetch(`${API_BASE}/api/stats`);
        const data = await res.json();
        document.getElementById('stat-books').innerText = data.totalBooks || '--';
        document.getElementById('stat-members').innerText = data.activeMembers || '--';
        document.getElementById('stat-loans').innerText = data.activeLoans || '--';
        document.getElementById('stat-overdue').innerText = data.overdueBooks || '--';
    } catch (e) {
        console.error("Failed to load stats");
    }
}

async function loadCatalog() {
    const grid = document.getElementById('catalog-grid');
    grid.innerHTML = '<div class="loader-container"><div class="spinner"></div><p>Loading catalog from Supabase...</p></div>';
    
    try {
        const res = await fetch(`${API_BASE}/api/books`);
        const books = await res.json();
        
        if (!books || books.length === 0) {
            grid.innerHTML = '<div class="loader-container"><p>No books found. The database seeder might still be running in the background!</p></div>';
            return;
        }

        grid.innerHTML = books.map(book => `
            <div class="book-card">
                <div class="book-cover-wrapper">
                    <img src="${book.coverUrl || 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=400&auto=format&fit=crop'}" class="book-cover" alt="Cover" onerror="this.src='https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=400&auto=format&fit=crop'">
                </div>
                <div class="book-info">
                    <span class="book-id-badge">${book.id}</span>
                    <h3 class="book-title">${book.title}</h3>
                    <div class="book-actions">
                        <button class="btn-primary full-width" onclick="switchNav('issue-return'); document.getElementById('issue-book-id').value='${book.id}';"><i class="fa-solid fa-book-bookmark"></i> Issue Book</button>
                    </div>
                </div>
            </div>
        `).join('');

        // Populate trending
        document.getElementById('trending-books-list').innerHTML = books.slice(0, 3).map(book => `
            <div style="display: flex; gap: 15px; margin-bottom: 15px; align-items: center; border-bottom: 1px solid #e2e8f0; padding-bottom: 15px;">
                <img src="${book.coverUrl}" style="width: 40px; height: 60px; object-fit: cover; border-radius: 4px;" onerror="this.style.display='none'">
                <div>
                    <strong style="display: block;">${book.title}</strong>
                    <span style="font-size: 0.8rem; color: #64748b;">ID: ${book.id}</span>
                </div>
            </div>
        `).join('');
    } catch (e) {
        grid.innerHTML = '<div class="loader-container" style="color: #ef4444;"><p>Failed to load catalog.</p></div>';
    }
}

async function loadMembers() {
    const tbody = document.getElementById('member-table-body');
    tbody.innerHTML = '<tr><td colspan="5" class="text-center" style="padding: 40px;"><div class="spinner"></div>Loading...</td></tr>';
    
    try {
        const res = await fetch(`${API_BASE}/api/members`);
        const members = await res.json();
        
        tbody.innerHTML = members.map(m => `
            <tr>
                <td><strong>${m.id}</strong></td>
                <td>${m.name}</td>
                <td>${m.email}</td>
                <td><span class="badge ${m.roleId === 1 ? 'purple' : 'gray'}">${m.roleId === 1 ? 'Admin' : 'Patron'}</span></td>
                <td><span class="badge green">Active</span></td>
            </tr>
        `).join('');
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center" style="color: #ef4444;">Failed to load members</td></tr>';
    }
}

// ==========================================
// Transactions
// ==========================================
document.getElementById('issue-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = e.target.querySelector('button');
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Processing...';
    
    try {
        const res = await fetch(`${API_BASE}/api/issue`, {
            method: 'POST',
            body: JSON.stringify({
                userId: document.getElementById('issue-member-id').value,
                bookId: document.getElementById('issue-book-id').value
            })
        });
        const data = await res.json();
        if (data.success) {
            showToast("Book issued successfully!");
            e.target.reset();
            loadStats();
        } else {
            showToast(data.error || "Failed to issue book", "error");
        }
    } catch (err) {
        showToast("Connection failed", "error");
    } finally {
        btn.innerHTML = 'Process Issue (14 Days) <i class="fa-solid fa-check"></i>';
    }
});

document.getElementById('return-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = e.target.querySelector('button');
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Processing...';
    
    try {
        const res = await fetch(`${API_BASE}/api/return`, {
            method: 'POST',
            body: JSON.stringify({
                copyId: document.getElementById('return-copy-id').value
            })
        });
        const data = await res.json();
        if (data.success) {
            showToast("Book returned successfully!");
            e.target.reset();
            loadStats();
        } else {
            showToast(data.error || "Failed to return book", "error");
        }
    } catch (err) {
        showToast("Connection failed", "error");
    } finally {
        btn.innerHTML = 'Process Return & Check Fines <i class="fa-solid fa-check"></i>';
    }
});
