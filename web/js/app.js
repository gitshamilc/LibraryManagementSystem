// Base API URL
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
    dots[slideIndex].classList.add('active');
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
        slideInterval = setInterval(() => { showSlides(slideIndex + 1); }, 5000);
    }
}
startSlider();
// --- END SLIDER LOGIC ---

// DOM Elements
const loginScreen = document.getElementById('login-screen');
const appScreen = document.getElementById('app-screen');
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');
const navItems = document.querySelectorAll('.nav-item');
const views = document.querySelectorAll('.view');
const pageTitle = document.getElementById('page-title');

// Auth Tabs (Mocks for UX)
const tabSignin = document.getElementById('tab-signin');
const tabSignup = document.getElementById('tab-signup');
const formSignin = document.getElementById('signin-form-wrapper');
const formSignup = document.getElementById('signup-form-wrapper');
const signupForm = document.getElementById('signup-form');
const signupError = document.getElementById('signup-error');

tabSignin.addEventListener('click', () => {
    tabSignin.classList.add('active');
    tabSignup.classList.remove('active');
    formSignin.style.display = 'block';
    formSignup.style.display = 'none';
});

tabSignup.addEventListener('click', () => {
    tabSignup.classList.add('active');
    tabSignin.classList.remove('active');
    formSignup.style.display = 'block';
    formSignin.style.display = 'none';
});

// Auth State
let currentUser = null;

// --- LOGIN LOGIC ---
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    
    loginError.style.display = 'none';
    
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
            document.getElementById('user-role').textContent = data.roleId === 1 ? 'Administrator' : 'User';
            document.getElementById('nav-avatar').textContent = data.name.charAt(0).toUpperCase();
            
            // Switch screen
            loginScreen.classList.remove('active');
            appScreen.classList.add('active');
            
            // Load initial data
            loadDashboardStats();
            initChart();
        } else {
            loginError.textContent = data.error || 'Login failed';
            loginError.style.display = 'block';
        }
    } catch (err) {
        loginError.textContent = 'Server connection error. Is the backend running?';
        loginError.style.display = 'block';
        console.error(err);
    }
});

signupForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;
    
    signupError.style.display = 'none';
    
    try {
        const response = await fetch(`${API_BASE}/signup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password })
        });
        
        const data = await response.json();
        
        if (data.success) {
            alert('Account created successfully! Please sign in.');
            tabSignin.click();
            document.getElementById('email').value = email;
            document.getElementById('password').value = password;
        } else {
            signupError.textContent = data.error || 'Signup failed';
            signupError.style.display = 'block';
        }
    } catch (err) {
        signupError.textContent = 'Server connection error.';
        signupError.style.display = 'block';
        console.error(err);
    }
});

// --- NAVIGATION LOGIC ---
navItems.forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        
        // Update active nav
        navItems.forEach(n => n.classList.remove('active'));
        item.classList.add('active');
        
        // Update title
        pageTitle.textContent = item.textContent.trim().substring(2).trim(); // Remove emoji
        
        // Switch view
        const targetView = item.getAttribute('data-target');
        views.forEach(v => v.classList.remove('active'));
        document.getElementById(`view-${targetView}`).classList.add('active');
        
        // Load data based on view
        if (targetView === 'dashboard') { loadDashboardStats(); initChart(); }
        if (targetView === 'catalog') loadCatalog();
    });
});

// Logout
document.getElementById('logout-btn').addEventListener('click', () => {
    currentUser = null;
    appScreen.classList.remove('active');
    loginScreen.classList.add('active');
    document.getElementById('password').value = '';
});

// Modal Toggles
function toggleModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal.classList.contains('active')) {
        modal.classList.remove('active');
    } else {
        modal.classList.add('active');
    }
}

// --- DATA FETCHING ---
async function loadDashboardStats() {
    try {
        const res = await fetch(`${API_BASE}/stats`);
        const data = await res.json();
        document.getElementById('stat-books').textContent = data.totalBooks || '--';
        document.getElementById('stat-members').textContent = data.totalBooks ? Math.floor(data.totalBooks * 0.85) : '--';
        document.getElementById('stat-loans').textContent = data.activeLoans || '--';
        document.getElementById('stat-fines').textContent = data.overdueBooks || '--';
    } catch (err) {
        console.error('Error loading stats:', err);
    }
}

let allBooksCache = [];

async function loadCatalog() {
    const grid = document.getElementById('book-grid');
    grid.innerHTML = '<div class="loader">Loading books from database...</div>';
    
    try {
        const res = await fetch(`${API_BASE}/books`);
        allBooksCache = await res.json();
        renderBooks(allBooksCache);
    } catch (err) {
        grid.innerHTML = '<div class="loader">Failed to load catalog. Please check backend connection.</div>';
        console.error('Error loading catalog:', err);
    }
}

function renderBooks(books) {
    const grid = document.getElementById('book-grid');
    grid.innerHTML = '';
    
    if (books.length === 0) {
        grid.innerHTML = '<div class="loader">No books found matching search.</div>';
        return;
    }
    
    books.forEach(book => {
        const card = document.createElement('div');
        card.className = 'book-card';
        
        const coverStyle = book.coverUrl ? `background: ${book.coverUrl}` : 'background: linear-gradient(135deg, #0f172a, #334155)';
        
        card.innerHTML = `
            <div class="book-cover" style="${coverStyle}">
                <div class="book-badge">${book.id}</div>
                <div class="book-title">${book.title}</div>
            </div>
            <div class="book-info">
                <p class="book-desc">${book.description}</p>
                <div class="book-actions">
                    <button class="btn-text" onclick="alert('Viewing deep stats for ${book.title}')">Details</button>
                    <button class="btn-primary" style="padding: 8px 12px; width: auto;" onclick="document.getElementById('issue-book-id').value = '${book.id}'; document.querySelector('[data-target=issue]').click();">Issue</button>
                </div>
            </div>
        `;
        grid.appendChild(card);
    });
}

// Global Search Logic
document.getElementById('global-search').addEventListener('input', (e) => {
    const term = e.target.value.toLowerCase();
    
    // Switch to catalog view automatically if not already there
    if (!document.getElementById('view-catalog').classList.contains('active')) {
        document.querySelector('[data-target=catalog]').click();
    }
    
    if (!allBooksCache.length) {
        loadCatalog().then(() => {
            const filtered = allBooksCache.filter(b => b.title.toLowerCase().includes(term) || b.id.toLowerCase().includes(term));
            renderBooks(filtered);
        });
    } else {
        const filtered = allBooksCache.filter(b => b.title.toLowerCase().includes(term) || b.id.toLowerCase().includes(term));
        renderBooks(filtered);
    }
});

// --- CIRCULATION LOGIC ---
async function issueBook(bookId, title, userId) {
    if (!currentUser || !userId || !bookId) {
        alert("Please provide both Book ID and Member ID.");
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/issue`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: userId, bookId: bookId })
        });
        const result = await response.json();
        
        if (result.success) {
            alert(result.message);
            loadDashboardStats(); 
            document.getElementById('issue-book-id').value = '';
        } else {
            alert("Failed: " + result.error);
        }
    } catch (err) {
        alert('Server error while issuing book.');
    }
}

async function returnBook(copyId) {
    if (!copyId) return;
    try {
        const response = await fetch(`${API_BASE}/return`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ copyId: copyId })
        });
        const result = await response.json();
        
        if (result.success) {
            // Trigger Confetti!
            confetti({
                particleCount: 100,
                spread: 70,
                origin: { y: 0.6 }
            });
            
            alert(result.message);
            loadDashboardStats();
            document.getElementById('return-copy-id').value = '';
        } else {
            alert("Failed: " + result.error);
        }
    } catch (err) {
        alert('Server error while returning book.');
    }
}

// Chart init
let myChart;
function initChart() {
    const ctx = document.getElementById('trendChart');
    if (!ctx) return;
    
    if (myChart) myChart.destroy();
    
    myChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
            datasets: [{
                label: 'Books Borrowed',
                data: [12, 19, 15, 25, 22, 30],
                borderColor: '#d97706',
                tension: 0.4,
                fill: true,
                backgroundColor: 'rgba(217, 119, 6, 0.1)'
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } }
        }
    });
}
