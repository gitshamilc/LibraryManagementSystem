// ==========================================
// ATHENA Library Management System - Master JS
// ==========================================

const API_BASE = window.location.origin + '/api';

// --- SLIDER LOGIC ---
let slideIndex = 0;
const slides = document.querySelectorAll('.slide');
const dots = document.querySelectorAll('.dot');
let slideInterval;

function showSlides(n) {
    if (!slides.length) return;
    slides.forEach(s => s.classList.remove('active'));
    dots.forEach(d => d.classList.remove('active'));
    
    slideIndex = n;
    if (slideIndex >= slides.length) slideIndex = 0;
    if (slideIndex < 0) slideIndex = slides.length - 1;
    
    slides[slideIndex].classList.add('active');
    if (dots[slideIndex]) dots[slideIndex].classList.add('active');
}

function changeSlide(n) {
    clearInterval(slideInterval);
    showSlides(slideIndex + n);
    startSlider();
}

function currentSlide(n) {
    clearInterval(slideInterval);
    showSlides(n);
    startSlider();
}

function startSlider() {
    if (slides.length) {
        clearInterval(slideInterval);
        slideInterval = setInterval(() => { showSlides(slideIndex + 1); }, 5000);
    }
}
startSlider();

function goToLogin() {
    document.getElementById('landing-screen').style.display = 'none';
    document.getElementById('landing-screen').classList.remove('active');
    document.getElementById('login-screen').classList.add('active');
}

// --- TOAST NOTIFICATIONS ---
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast-notification ${type}`;
    const icon = type === 'success' ? '✅' : (type === 'error' ? '❌' : 'ℹ️');
    toast.innerHTML = `<span style="font-size:1.1rem;">${icon}</span><span>${message}</span>`;
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(15px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// --- DOM ELEMENTS & AUTH STATE ---
let currentUser = null;
let allBooksCache = [];
let allMembersCache = [];
let allLoansCache = [];

const loginScreen = document.getElementById('login-screen');
const appScreen = document.getElementById('app-screen');
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');
const tabSignin = document.getElementById('tab-signin');
const tabSignup = document.getElementById('tab-signup');
const formSignin = document.getElementById('signin-form-wrapper');
const formSignup = document.getElementById('signup-form-wrapper');
const signupForm = document.getElementById('signup-form');
const signupError = document.getElementById('signup-error');
const navItems = document.querySelectorAll('.nav-item');
const views = document.querySelectorAll('.view');
const pageTitle = document.getElementById('page-title');

// Auth Tabs
tabSignin.addEventListener('click', () => {
    tabSignin.classList.add('active');
    tabSignup.classList.remove('active');
    formSignin.style.display = 'block';
    formSignup.style.display = 'none';
    loginError.style.display = 'none';
    signupError.style.display = 'none';
});

tabSignup.addEventListener('click', () => {
    tabSignup.classList.add('active');
    tabSignin.classList.remove('active');
    formSignup.style.display = 'block';
    formSignin.style.display = 'none';
    loginError.style.display = 'none';
    signupError.style.display = 'none';
});

// --- PASSWORD VISIBILITY TOGGLES ---
const toggleSigninPw = document.getElementById('toggle-signin-password');
if (toggleSigninPw) {
    toggleSigninPw.addEventListener('click', () => {
        const input = document.getElementById('password');
        const isPw = input.type === 'password';
        input.type = isPw ? 'text' : 'password';
        toggleSigninPw.textContent = isPw ? '🙈' : '👁️';
    });
}

const toggleSignupPw = document.getElementById('toggle-signup-password');
if (toggleSignupPw) {
    toggleSignupPw.addEventListener('click', () => {
        const input = document.getElementById('reg-password');
        const isPw = input.type === 'password';
        input.type = isPw ? 'text' : 'password';
        toggleSignupPw.textContent = isPw ? '🙈' : '👁️';
    });
}

// --- REAL-TIME EMAIL & PASSWORD STRENGTH VALIDATION ---
const regEmailInput = document.getElementById('reg-email');
const regEmailHint = document.getElementById('reg-email-hint');
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

if (regEmailInput) {
    regEmailInput.addEventListener('input', () => {
        const val = regEmailInput.value.trim();
        if (val.length > 0 && !emailRegex.test(val)) {
            regEmailHint.style.display = 'block';
            regEmailInput.style.borderColor = '#dc2626';
        } else {
            regEmailHint.style.display = 'none';
            regEmailInput.style.borderColor = val.length > 0 ? '#16a34a' : 'var(--border)';
        }
    });
}

const regPasswordInput = document.getElementById('reg-password');
const strengthFill = document.getElementById('strength-fill');
const strengthLabel = document.getElementById('strength-label');
const strengthHint = document.getElementById('strength-hint');
const ruleLength = document.getElementById('rule-length');
const ruleChars = document.getElementById('rule-chars');

if (regPasswordInput) {
    regPasswordInput.addEventListener('input', () => {
        const val = regPasswordInput.value;
        const hasLen = val.length >= 6;
        const hasLetter = /[a-zA-Z]/.test(val);
        const hasNumber = /[0-9]/.test(val);
        const hasSpecial = /[^a-zA-Z0-9]/.test(val) || val.length >= 9;

        // Update checklist rules
        if (hasLen) {
            ruleLength.classList.add('valid');
            ruleLength.innerHTML = '<span>✓</span> At least 6 characters';
        } else {
            ruleLength.classList.remove('valid');
            ruleLength.innerHTML = '<span>○</span> At least 6 characters';
        }

        if (hasLetter && hasNumber) {
            ruleChars.classList.add('valid');
            ruleChars.innerHTML = '<span>✓</span> Contains both letters & numbers';
        } else {
            ruleChars.classList.remove('valid');
            ruleChars.innerHTML = '<span>○</span> Contains both letters & numbers';
        }

        // Calculate score (0 to 3)
        let score = 0;
        if (val.length > 0) score = 1;
        if (hasLen && (hasLetter || hasNumber)) score = 2;
        if (hasLen && hasLetter && hasNumber) score = hasSpecial ? 3 : 2;

        if (val.length === 0) {
            strengthFill.style.width = '0%';
            strengthFill.style.backgroundColor = '#e2e8f0';
            strengthLabel.textContent = 'Enter password';
            strengthLabel.style.color = 'var(--text-muted)';
            strengthHint.textContent = 'Min 6 characters';
        } else if (score === 1) {
            strengthFill.style.width = '33%';
            strengthFill.style.backgroundColor = '#dc2626';
            strengthLabel.textContent = 'Weak';
            strengthLabel.style.color = '#dc2626';
            strengthHint.textContent = 'Needs letters & numbers';
        } else if (score === 2) {
            strengthFill.style.width = '66%';
            strengthFill.style.backgroundColor = '#d97706';
            strengthLabel.textContent = 'Good';
            strengthLabel.style.color = '#d97706';
            strengthHint.textContent = 'Meets requirements';
        } else if (score === 3) {
            strengthFill.style.width = '100%';
            strengthFill.style.backgroundColor = '#16a34a';
            strengthLabel.textContent = 'Strong';
            strengthLabel.style.color = '#16a34a';
            strengthHint.textContent = 'Excellent password';
        }
    });
}

// Modal Toggles
function toggleModal(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;
    if (modal.classList.contains('active')) {
        modal.classList.remove('active');
    } else {
        modal.classList.add('active');
    }
}

// Close modals when clicking overlay background
document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
            overlay.classList.remove('active');
        }
    });
});

// --- AUTHENTICATION ---
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const btn = document.getElementById('login-submit-btn');
    
    loginError.style.display = 'none';

    // Client-side quick checks
    if (!email) {
        loginError.textContent = 'Please enter your email address or User ID.';
        loginError.style.display = 'block';
        return;
    }
    if (!password) {
        loginError.textContent = 'Please enter your password.';
        loginError.style.display = 'block';
        return;
    }

    btn.disabled = true;
    btn.textContent = 'Verifying credentials...';
    
    try {
        const response = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        
        const data = await response.json();
        
        if (data.success) {
            currentUser = data;
            document.getElementById('user-name').textContent = data.name;
            document.getElementById('user-role').textContent = data.role || (data.roleId === 1 ? 'Administrator' : 'Student Patron');
            document.getElementById('nav-avatar').textContent = data.name.charAt(0).toUpperCase();
            
            // Switch screen
            loginScreen.classList.remove('active');
            appScreen.classList.add('active');
            
            showToast(`Welcome back, ${data.name}!`, 'success');
            
            // Load all dashboard components
            loadDashboardStats();
            loadLoansAndActivity();
            initChart();
        } else {
            loginError.textContent = data.error || 'Invalid user credentials.';
            loginError.style.display = 'block';
        }
    } catch (err) {
        loginError.textContent = 'Server connection error. Please verify backend service.';
        loginError.style.display = 'block';
        console.error(err);
    } finally {
        btn.disabled = false;
        btn.textContent = 'Sign In to Dashboard';
    }
});

signupForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('reg-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value;
    const btn = document.getElementById('signup-submit-btn');
    
    signupError.style.display = 'none';

    // 1. Full name validation
    if (name.length < 2) {
        signupError.textContent = 'Please enter your full name (at least 2 characters).';
        signupError.style.display = 'block';
        return;
    }

    // 2. Email format validation
    if (!emailRegex.test(email)) {
        signupError.textContent = 'Please enter a valid email address (e.g. name@example.com).';
        signupError.style.display = 'block';
        regEmailInput.focus();
        return;
    }

    // 3. Password strength validation
    if (password.length < 6) {
        signupError.textContent = 'Password is not strong enough. Minimum 6 characters required.';
        signupError.style.display = 'block';
        regPasswordInput.focus();
        return;
    }
    const hasLetter = /[a-zA-Z]/.test(password);
    const hasNumber = /[0-9]/.test(password);
    if (!hasLetter || !hasNumber) {
        signupError.textContent = 'Password is not strong enough. It must contain both letters and numbers.';
        signupError.style.display = 'block';
        regPasswordInput.focus();
        return;
    }

    btn.disabled = true;
    btn.textContent = 'Creating account...';
    
    try {
        const response = await fetch(`${API_BASE}/signup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Account created successfully! You can now sign in.', 'success');
            tabSignin.click();
            document.getElementById('email').value = email;
            document.getElementById('password').value = password;
        } else {
            signupError.textContent = data.error || 'Registration failed.';
            signupError.style.display = 'block';
        }
    } catch (err) {
        signupError.textContent = 'Server connection error.';
        signupError.style.display = 'block';
        console.error(err);
    } finally {
        btn.disabled = false;
        btn.textContent = 'Register Account';
    }
});

document.getElementById('logout-btn').addEventListener('click', () => {
    currentUser = null;
    appScreen.classList.remove('active');
    loginScreen.classList.add('active');
    document.getElementById('password').value = '';
    showToast('You have been logged out safely.', 'info');
});

// --- NAVIGATION LOGIC ---
navItems.forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        
        navItems.forEach(n => n.classList.remove('active'));
        item.classList.add('active');
        
        const rawTitle = item.textContent.trim();
        pageTitle.textContent = rawTitle.replace(/[^\w\s&]/gi, '').trim();
        
        const targetView = item.getAttribute('data-target');
        views.forEach(v => v.classList.remove('active'));
        const targetElem = document.getElementById(`view-${targetView}`);
        if (targetElem) targetElem.classList.add('active');
        
        if (targetView === 'dashboard') {
            loadDashboardStats();
            loadLoansAndActivity();
            initChart();
        }
        if (targetView === 'catalog') loadCatalog();
        if (targetView === 'issue') loadLoansAndActivity();
        if (targetView === 'members') loadMembers();
    });
});

function switchView(target) {
    const navBtn = document.querySelector(`.nav-item[data-target="${target}"]`);
    if (navBtn) navBtn.click();
}

// --- 1. DASHBOARD DATA & REAL STATS ---
async function loadDashboardStats() {
    try {
        const res = await fetch(`${API_BASE}/stats`);
        const data = await res.json();
        
        document.getElementById('stat-books').textContent = data.totalBooks ?? '--';
        document.getElementById('stat-available').textContent = data.availableBooks ?? '--';
        document.getElementById('stat-copies-count').textContent = `${data.totalCopies || 0} total physical copies`;
        
        const availPct = data.totalCopies ? Math.round((data.availableBooks / data.totalCopies) * 100) : 0;
        document.getElementById('stat-available-rate').textContent = `${availPct}% shelf availability`;
        
        document.getElementById('stat-loans').textContent = data.activeLoans ?? '--';
        document.getElementById('stat-overdue').textContent = data.overdueBooks ?? '0';
        
        const fineText = data.finesCollected > 0 ? `₹${data.finesCollected.toFixed(2)} in late penalties` : 'All loans in good standing';
        document.getElementById('stat-fines-summary').textContent = fineText;
        
    } catch (err) {
        console.error('Error loading stats:', err);
    }
}

// --- 2. LOANS & OVERDUE MONITORING ---
async function loadLoansAndActivity() {
    try {
        const res = await fetch(`${API_BASE}/loans`);
        allLoansCache = await res.json();
        
        renderActivityList(allLoansCache);
        renderOverdueTable(allLoansCache);
        renderActiveLoansTable(allLoansCache);
        
    } catch (err) {
        console.error('Error loading loans:', err);
    }
}

function renderActivityList(loans) {
    const list = document.getElementById('activity-list');
    list.innerHTML = '';
    
    if (!loans || loans.length === 0) {
        list.innerHTML = '<li style="color: var(--text-muted); padding: 15px 0;">No circulation transactions recorded yet.</li>';
        return;
    }
    
    const recent = loans.slice(0, 5);
    recent.forEach(loan => {
        const isReturn = loan.status === 'RETURNED';
        const isOverdue = loan.isOverdue;
        
        let badgeClass = 'blue';
        let badgeLabel = 'Issued';
        if (isReturn) {
            badgeClass = 'green';
            badgeLabel = 'Returned';
        } else if (isOverdue) {
            badgeClass = 'red';
            badgeLabel = 'Overdue';
        }
        
        list.innerHTML += `
            <li>
                <span class="badge ${badgeClass}">${badgeLabel}</span>
                <div class="activity-text">
                    <strong>${loan.userName}</strong> ${isReturn ? 'returned' : 'borrowed'} <em>${loan.bookTitle}</em>
                    <br><small>Issue: ${loan.issueDate} | Due: ${loan.dueDate} ${isOverdue ? '(₹' + loan.fine + ' fine)' : ''}</small>
                </div>
            </li>
        `;
    });
}

function renderOverdueTable(loans) {
    const tbody = document.getElementById('overdue-table-body');
    tbody.innerHTML = '';
    
    const overdueList = loans.filter(l => l.status === 'ACTIVE' && l.isOverdue);
    
    if (overdueList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: #16a34a; font-weight: 600; padding: 25px;">🎉 No books are currently overdue. Excellent!</td></tr>';
        return;
    }
    
    overdueList.forEach(loan => {
        tbody.innerHTML += `
            <tr>
                <td><strong>${loan.userName}</strong><br><small style="color:var(--text-muted);">${loan.userEmail}</small></td>
                <td><strong>${loan.bookTitle}</strong><br><small style="color:var(--text-muted);">Copy: ${loan.copyId}</small></td>
                <td>${loan.dueDate}</td>
                <td><span style="color:#dc2626; font-weight:bold;">${loan.daysOverdue} days late</span></td>
                <td><strong style="color:#b91c1c;">₹${loan.fine.toFixed(2)}</strong></td>
                <td>
                    <button class="btn-primary" style="padding: 6px 12px; width: auto; font-size: 0.85rem;" onclick="processReturn('${loan.copyId}')">Process Return</button>
                </td>
            </tr>
        `;
    });
}

function renderActiveLoansTable(loans) {
    const tbody = document.getElementById('active-loans-table-body');
    if (!tbody) return;
    tbody.innerHTML = '';
    
    const activeList = loans.filter(l => l.status === 'ACTIVE');
    
    if (activeList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 25px;">No active loans found. All copies are currently in the library!</td></tr>';
        return;
    }
    
    activeList.forEach(loan => {
        const statusBadge = loan.isOverdue 
            ? `<span class="badge red">Overdue (${loan.daysOverdue}d)</span>` 
            : `<span class="badge green">Active (On Time)</span>`;
            
        tbody.innerHTML += `
            <tr>
                <td><code>${loan.id}</code></td>
                <td><strong>${loan.bookTitle}</strong></td>
                <td><code>${loan.copyId}</code></td>
                <td><strong>${loan.userName}</strong></td>
                <td>${loan.issueDate}</td>
                <td>${loan.dueDate}</td>
                <td>${statusBadge}</td>
                <td>
                    <button class="btn-primary" style="padding: 6px 12px; width: auto; font-size: 0.85rem;" onclick="processReturn('${loan.copyId}')">Return</button>
                </td>
            </tr>
        `;
    });
}

// --- 3. BOOK CATALOG LOGIC & FILTERS ---
async function loadCatalog() {
    const grid = document.getElementById('book-grid');
    grid.innerHTML = '<div class="loader">Loading catalog from database...</div>';
    
    try {
        const res = await fetch(`${API_BASE}/books`);
        allBooksCache = await res.json();
        applyCatalogFilters();
    } catch (err) {
        grid.innerHTML = '<div class="loader" style="color:#dc2626;">Failed to load catalog from server.</div>';
        console.error('Error loading catalog:', err);
    }
}

function applyCatalogFilters() {
    let filtered = [...allBooksCache];
    
    // Category filter
    const cat = document.getElementById('catalog-category-filter').value;
    if (cat !== 'ALL') {
        filtered = filtered.filter(b => b.category && b.category.toLowerCase() === cat.toLowerCase());
    }
    
    // Status filter
    const status = document.getElementById('catalog-status-filter').value;
    if (status === 'AVAILABLE') {
        filtered = filtered.filter(b => b.availableCopies > 0);
    } else if (status === 'CHECKED_OUT') {
        filtered = filtered.filter(b => b.availableCopies === 0);
    }
    
    // Global search term
    const term = document.getElementById('global-search').value.toLowerCase().trim();
    if (term) {
        filtered = filtered.filter(b => 
            (b.title && b.title.toLowerCase().includes(term)) ||
            (b.author && b.author.toLowerCase().includes(term)) ||
            (b.id && b.id.toLowerCase().includes(term)) ||
            (b.isbn && b.isbn.toLowerCase().includes(term)) ||
            (b.category && b.category.toLowerCase().includes(term))
        );
    }
    
    // Sorting
    const sort = document.getElementById('catalog-sort').value;
    if (sort === 'AZ') {
        filtered.sort((a, b) => a.title.localeCompare(b.title));
    } else if (sort === 'ZA') {
        filtered.sort((a, b) => b.title.localeCompare(a.title));
    } else if (sort === 'ID') {
        filtered.sort((a, b) => a.id.localeCompare(b.id));
    } else if (sort === 'COPIES') {
        filtered.sort((a, b) => (b.totalCopies || 0) - (a.totalCopies || 0));
    }
    
    renderBooks(filtered);
}

document.getElementById('catalog-category-filter').addEventListener('change', applyCatalogFilters);
document.getElementById('catalog-status-filter').addEventListener('change', applyCatalogFilters);
document.getElementById('catalog-sort').addEventListener('change', applyCatalogFilters);

document.getElementById('global-search').addEventListener('input', () => {
    if (!document.getElementById('view-catalog').classList.contains('active')) {
        switchView('catalog');
    }
    applyCatalogFilters();
});

function filterAvailableOnly() {
    document.getElementById('catalog-status-filter').value = 'AVAILABLE';
    applyCatalogFilters();
}

function renderBooks(books) {
    const grid = document.getElementById('book-grid');
    grid.innerHTML = '';
    window.booksById = window.booksById || {};
    
    if (books.length === 0) {
        grid.innerHTML = '<div class="loader">No books found matching the selected filters.</div>';
        return;
    }
    
    books.forEach(book => {
        window.booksById[book.id] = book;
        const card = document.createElement('div');
        card.className = 'book-card';
        
        const coverStyle = book.coverUrl ? `background: ${book.coverUrl}` : 'background: linear-gradient(135deg, #1e3a8a, #0f172a)';
        const isAvailable = book.availableCopies > 0;
        const availClass = isAvailable ? 'in-stock' : 'out-of-stock';
        const availText = isAvailable ? `🟢 ${book.availableCopies}/${book.totalCopies} Available` : `🔴 All Loaned Out`;
        
        card.innerHTML = `
            <div class="book-cover" style="${coverStyle}">
                <div class="book-badge">${book.id}</div>
                <div class="book-title">${book.title}</div>
            </div>
            <div class="book-info">
                <div>
                    <div class="book-author">by ${book.author || 'Author'}</div>
                    <div class="book-card-meta">
                        <span class="badge gray">${book.category || 'General'}</span>
                        <span class="avail-pill ${availClass}">${availText}</span>
                    </div>
                    <p class="book-desc">${book.description || 'No description available.'}</p>
                </div>
                <div class="book-actions">
                    <button class="btn-text" onclick="showBookDetailsModal('${book.id}')">Details</button>
                    <button class="btn-primary" style="padding: 8px 16px; width: auto; font-size: 0.9rem;" 
                        onclick="prepareIssue('${book.id}')">Issue Book</button>
                </div>
            </div>
        `;
        grid.appendChild(card);
    });
}

function prepareIssue(bookId) {
    const book = (window.booksById && window.booksById[bookId]) || allBooksCache.find(b => b.id === bookId);
    const bookTitle = book ? book.title : bookId;
    switchView('issue');
    const input = document.getElementById('issue-book-id');
    input.value = bookId;
    document.getElementById('issue-member-id').focus();
    showToast(`Selected "${bookTitle}" for checkout. Enter Member ID to complete.`, 'info');
}

function showBookDetailsModal(bookId) {
    const book = (window.booksById && window.booksById[bookId]) || allBooksCache.find(b => b.id === bookId);
    if (!book) return;
    
    document.getElementById('modal-book-title').textContent = book.title;
    document.getElementById('modal-book-author').textContent = `by ${book.author || 'Unknown'}`;
    document.getElementById('modal-book-category').textContent = book.category || 'General';
    document.getElementById('modal-book-id').textContent = book.id;
    document.getElementById('modal-book-isbn').textContent = book.isbn || 'N/A';
    document.getElementById('modal-book-desc').textContent = book.description || 'No detailed description available.';
    
    const cover = document.getElementById('modal-book-cover');
    cover.style.background = book.coverUrl || 'linear-gradient(135deg, #1e3a8a, #0f172a)';
    cover.textContent = book.title;
    
    const isAvail = book.availableCopies > 0;
    const availElem = document.getElementById('modal-book-availability');
    availElem.className = isAvail ? 'badge green' : 'badge red';
    availElem.textContent = isAvail ? `${book.availableCopies} of ${book.totalCopies} Copies Available` : 'All Copies Currently Checked Out';
    
    const issueBtn = document.getElementById('modal-issue-book-btn');
    issueBtn.onclick = () => {
        toggleModal('book-details-modal');
        prepareIssue(book.id);
    };
    
    toggleModal('book-details-modal');
}

window.showBookDetailsModal = showBookDetailsModal;
window.prepareIssue = prepareIssue;
window.filterAvailableOnly = filterAvailableOnly;
window.switchView = switchView;

// --- 4. ISSUE & RETURN WORKFLOW ---
async function processIssue() {
    const userId = document.getElementById('issue-member-id').value.trim();
    const bookId = document.getElementById('issue-book-id').value.trim();
    const btn = document.getElementById('issue-btn');
    
    if (!userId || !bookId) {
        showToast('Please provide both Member ID and Book ID / Barcode.', 'error');
        return;
    }
    
    btn.disabled = true;
    btn.textContent = 'Processing loan...';
    
    try {
        const response = await fetch(`${API_BASE}/issue`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, bookId })
        });
        const result = await response.json();
        
        if (result.success) {
            showToast(result.message, 'success');
            document.getElementById('issue-book-id').value = '';
            loadDashboardStats();
            loadLoansAndActivity();
            if (allBooksCache.length) loadCatalog();
        } else {
            showToast(result.error || 'Failed to issue book.', 'error');
        }
    } catch (err) {
        showToast('Server error during book issue.', 'error');
        console.error(err);
    } finally {
        btn.disabled = false;
        btn.textContent = 'Issue Book (14-Day Loan)';
    }
}

async function processReturn(specificCopyId) {
    const copyId = (specificCopyId || document.getElementById('return-copy-id').value).trim();
    const btn = document.getElementById('return-btn');
    
    if (!copyId) {
        showToast('Please enter a Copy ID, Barcode, or Book ID to return.', 'error');
        return;
    }
    
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Processing return...';
    }
    
    try {
        const response = await fetch(`${API_BASE}/return`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ copyId })
        });
        const result = await response.json();
        
        if (result.success) {
            // Confetti celebration
            try {
                confetti({
                    particleCount: 80,
                    spread: 60,
                    origin: { y: 0.6 }
                });
            } catch (ignored) {}
            
            showToast(result.message, 'success');
            document.getElementById('return-copy-id').value = '';
            
            loadDashboardStats();
            loadLoansAndActivity();
            if (allBooksCache.length) loadCatalog();
            if (allMembersCache.length) loadMembers();
        } else {
            showToast(result.error || 'Failed to return book.', 'error');
        }
    } catch (err) {
        showToast('Server error during return.', 'error');
        console.error(err);
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Complete Return & Calculate Fines';
        }
    }
}

// Global hook for buttons
window.issueBook = function(bookId, title, userId) {
    document.getElementById('issue-book-id').value = bookId;
    document.getElementById('issue-member-id').value = userId;
    processIssue();
};
window.returnBook = function(copyId) {
    processReturn(copyId);
};

// --- 5. ADD NEW BOOK (FORM SUBMISSION) ---
document.getElementById('add-book-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const title = document.getElementById('new-book-title').value.trim();
    const author = document.getElementById('new-book-author').value.trim();
    const category = document.getElementById('new-book-category').value.trim();
    const isbn = document.getElementById('new-book-isbn').value.trim();
    const copies = document.getElementById('new-book-copies').value.trim();
    const btn = document.getElementById('save-book-btn');
    
    btn.disabled = true;
    btn.textContent = 'Saving book to database...';
    
    try {
        const res = await fetch(`${API_BASE}/books`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, author, category, isbn, copies })
        });
        const data = await res.json();
        
        if (data.success) {
            showToast(data.message, 'success');
            toggleModal('add-book-modal');
            document.getElementById('add-book-form').reset();
            loadDashboardStats();
            loadCatalog();
        } else {
            showToast(data.error || 'Failed to add book.', 'error');
        }
    } catch (err) {
        showToast('Network error while saving book.', 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Save Book & Create Copies';
    }
});

// --- 6. REGISTER MEMBER (FORM SUBMISSION) ---
document.getElementById('add-member-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('new-member-name').value.trim();
    const email = document.getElementById('new-member-email').value.trim();
    const password = document.getElementById('new-member-password').value;
    const role = document.getElementById('new-member-role').value;
    const btn = document.getElementById('save-member-btn');
    
    btn.disabled = true;
    btn.textContent = 'Registering member...';
    
    try {
        const res = await fetch(`${API_BASE}/members`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password, role })
        });
        const data = await res.json();
        
        if (data.success) {
            showToast(data.message || `Member created with ID: ${data.userId}`, 'success');
            toggleModal('add-member-modal');
            document.getElementById('add-member-form').reset();
            loadDashboardStats();
            loadMembers();
        } else {
            showToast(data.error || 'Failed to register member.', 'error');
        }
    } catch (err) {
        showToast('Network error while registering member.', 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Register Member';
    }
});

// --- 7. MEMBER DIRECTORY ---
async function loadMembers() {
    const tbody = document.getElementById('member-table-body');
    tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 30px;"><div class="loader">Loading member directory...</div></td></tr>';
    
    try {
        const res = await fetch(`${API_BASE}/members`);
        allMembersCache = await res.json();
        renderMembers(allMembersCache);
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: #dc2626; padding: 25px;">Failed to load member directory.</td></tr>';
        console.error('Error loading members:', err);
    }
}

function renderMembers(members) {
    const tbody = document.getElementById('member-table-body');
    tbody.innerHTML = '';
    
    if (!members || members.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 25px;">No members found.</td></tr>';
        return;
    }
    
    members.forEach(m => {
        let roleBadge = 'gray';
        if (m.role === 'ADMIN' || m.role === 'Administrator') roleBadge = 'purple';
        else if (m.role === 'LIBRARIAN' || m.role === 'Librarian') roleBadge = 'blue';
        
        const activeLoansBadge = m.activeLoans > 0 
            ? `<span class="badge blue">${m.activeLoans} Active Loan(s)</span>` 
            : `<span class="badge gray">0 Loans</span>`;
            
        tbody.innerHTML += `
            <tr>
                <td><code>${m.id}</code></td>
                <td><strong>${m.name}</strong></td>
                <td>${m.email}</td>
                <td><span class="badge ${roleBadge}">${m.role}</span></td>
                <td>${activeLoansBadge}</td>
                <td><span class="badge green">Active</span></td>
                <td>
                    <button class="btn-text" onclick="prepareMemberIssue('${m.id}', '${m.name.replace(/'/g, "\\'")}')">Issue Book</button>
                </td>
            </tr>
        `;
    });
}

function prepareMemberIssue(memberId, memberName) {
    switchView('issue');
    document.getElementById('issue-member-id').value = memberId;
    document.getElementById('issue-book-id').focus();
    showToast(`Member "${memberName}" selected. Enter Book ID to complete loan.`, 'info');
}

document.getElementById('member-search-input').addEventListener('input', (e) => {
    const term = e.target.value.toLowerCase().trim();
    const filtered = allMembersCache.filter(m => 
        m.name.toLowerCase().includes(term) || 
        m.email.toLowerCase().includes(term) || 
        m.id.toLowerCase().includes(term) ||
        m.role.toLowerCase().includes(term)
    );
    renderMembers(filtered);
});

// --- 8. CHART INITIALIZATION ---
let myChart = null;
function initChart() {
    try {
        const ctx = document.getElementById('trendChart');
        if (!ctx || typeof Chart === 'undefined') return;
        
        if (myChart) myChart.destroy();
        
        myChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: ['May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct'],
                datasets: [{
                    label: 'Books Circulated',
                    data: [18, 27, 34, 45, 52, 68],
                    borderColor: '#c29352',
                    backgroundColor: 'rgba(194, 147, 82, 0.12)',
                    borderWidth: 3,
                    tension: 0.35,
                    fill: true,
                    pointBackgroundColor: '#c29352',
                    pointRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: '#0f172a',
                        padding: 10,
                        cornerRadius: 8
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { color: 'rgba(0,0,0,0.05)' }
                    },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        });
    } catch (e) {
        console.warn('Chart initialization deferred:', e);
    }
}
