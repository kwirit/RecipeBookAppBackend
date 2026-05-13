/**
 * Nutrition Tracker SPA
 * Vanilla JS Implementation
 */
const API_BASE = 'http://localhost:8080/api';
// ==========================================
// CONFIG & STATE
// ==========================================
const STATE = {
    products: [],
    dishes: [],
    productCache: [], // For ingredient selects
    categories: {
        product: ['Замороженный', 'Мясной', 'Овощи', 'Зелень', 'Специи', 'Крупы', 'Консервы', 'Жидкость', 'Сладости'],
        dish: ['Десерт', 'Первое', 'Второе', 'Напиток', 'Салат', 'Суп', 'Перекус']
    },
    productFilter: {name: '', category: '', cookingRequirement: '', vegan: false, glutenFree: false, sugarFree: false},
    productSort: {field: 'name', direction: 'ASC'},
    productPage: {current: 1, totalPages: 1},
    dishFilter: {name: '', category: '', vegan: false, glutenFree: false, sugarFree: false},
    isDirty: {} // Track manual KBZU edits in dish form
};

const CATEGORY_MAP = {
    'Замороженный': 'FROZEN',
    'Мясной': 'MEAT',
    'Овощи': 'VEGETABLES',
    'Зелень': 'GREENS',
    'Специи': 'SPICES',
    'Крупы': 'GRAINS',        // ✅ Было 'Бакалея' → исправлено на 'Крупы'
    'Консервы': 'CANNED',
    'Жидкость': 'LIQUID',
    'Сладости': 'SWEETS'
};

const EN_TO_RU_CATEGORY = {
    'FROZEN': 'Замороженный',
    'MEAT': 'Мясной',
    'VEGETABLES': 'Овощи',
    'GREENS': 'Зелень',
    'SPICES': 'Специи',
    'GRAINS': 'Крупы',        // ✅ Было 'Бакалея' → исправлено на 'Крупы'
    'CANNED': 'Консервы',
    'LIQUID': 'Жидкость',
    'SWEETS': 'Сладости'
};

const RU_TO_EN_COOKING = {
    'Готовый': 'READY_TO_EAT',
    'Требует готовки': 'REQUIRES_COOKING',
    'Полуфабрикат': 'SEMI_FINISHED'
};

const COOKING_REQ_MAP = {
    'READY_TO_EAT': 'Готовый',
    'REQUIRES_COOKING': 'Требует готовки',
    'SEMI_FINISHED': 'Полуфабрикат'
};

const DISH_CATEGORY_MAP = {
    'Десерт': 'DESSERT',
    'Первое': 'FIRST_COURSE',
    'Второе': 'SECOND_COURSE',
    'Напиток': 'DRINK',
    'Салат': 'SALAD',
    'Суп': 'SOUP',
    'Перекус': 'SNACK'
};

// Маппинг категорий блюд: enum → русский (для отображения)
const EN_TO_RU_DISH_CATEGORY = {
    'DESSERT': 'Десерт',
    'FIRST_COURSE': 'Первое',
    'SECOND_COURSE': 'Второе',
    'DRINK': 'Напиток',
    'SALAD': 'Салат',
    'SOUP': 'Суп',
    'SNACK': 'Перекус'
};


// 🔹 НОВЫЙ: Красивый алерт только с кнопкой "Понятно"
// 🔹 НОВЫЙ: Красивый алерт только с кнопкой "Понятно"
// 🔹 НОВЫЙ: Красивый алерт только с кнопкой "Понятно" (без кликабельных блюд)
const showAlert = (title, message, extraHtml = null) => new Promise(resolve => {
    const modal = document.getElementById('confirm-modal');
    const titleEl = document.getElementById('confirm-title');
    const messageEl = document.getElementById('confirm-message');
    const extraEl = document.getElementById('confirm-extra');
    const okBtn = document.getElementById('confirm-ok');
    const cancelBtn = document.getElementById('confirm-cancel');

    // 🔹 Настройка контента
    titleEl.textContent = title;
    messageEl.innerHTML = message;
    extraEl.innerHTML = extraHtml || '';

    // 🔹 Скрываем "Подтвердить", меняем текст кнопки отмены
    okBtn.classList.add('hidden');
    cancelBtn.textContent = 'Понятно';
    cancelBtn.className = 'btn btn-primary';

    // 🔹 Показываем модальное окно
    modal.classList.remove('hidden');

    // 🔹 Обработчик закрытия
    const onClose = () => {
        modal.classList.add('hidden');
        // 🔹 Восстанавливаем кнопки
        okBtn.classList.remove('hidden');
        cancelBtn.textContent = 'Отмена';
        cancelBtn.className = 'btn btn-secondary';
        resolve();
    };

    // 🔹 УБРАНО: делегирование кликов на блюда (теперь они не кликабельны)

    cancelBtn.onclick = onClose;

    // 🔹 Закрытие по клику вне модалки
    modal.onclick = (e) => {
        if (e.target === modal) onClose();
    };
});

const hasFlag = (flags, flagName) => {
    if (!flags) return false;
    // flags может быть: Set<string>, string[], или объектом из JSON
    if (flags instanceof Set) return flags.has(flagName);
    if (Array.isArray(flags)) return flags.includes(flagName);
    if (typeof flags === 'object') return Object.values(flags).includes(flagName);
    return false;
};

// ==========================================
// UTILS
// ==========================================
const debounce = (fn, delay = 400) => {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => fn(...args), delay);
    };
};

const formatNum = (n, decimals = 2) => {
    // 🔹 Явная проверка на все возможные "пустые" значения
    if (n === null || n === undefined || n === '' || isNaN(Number(n))) {
        return '0.00';
    }
    const num = Number(n);
    // 🔹 Дополнительная проверка на бесконечность
    if (!isFinite(num)) return '0.00';
    return num.toFixed(decimals);
};

const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
};

// ==========================================
// TOAST SYSTEM
// ==========================================
const showToast = (message, type = 'info') => {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
};

// ==========================================
// LOADING
// ==========================================
const showLoading = () => document.getElementById('loading-overlay').classList.remove('hidden');
const hideLoading = () => document.getElementById('loading-overlay').classList.add('hidden');

// ==========================================
// MODAL
// ==========================================
const showModal = (title, message, extra = null) => new Promise(resolve => {
    const modal = document.getElementById('confirm-modal');
    document.getElementById('confirm-title').textContent = title;
    document.getElementById('confirm-message').textContent = message;
    const extraContainer = document.getElementById('confirm-extra');
    extraContainer.innerHTML = extra || '';

    const hide = () => modal.classList.add('hidden');
    hide();
    modal.classList.remove('hidden');

    document.getElementById('confirm-ok').onclick = () => {
        hide();
        resolve(true);
    };
    document.getElementById('confirm-cancel').onclick = () => {
        hide();
        resolve(false);
    };
});

// ==========================================
// API CLIENT
// ==========================================
const API = {
    async request(method, url, data = null) {
        showLoading();
        try {
            const opts = {method, headers: {'Content-Type': 'application/json'}};
            if (data) opts.body = JSON.stringify(data);
            const res = await fetch(API_BASE + url, opts);
            console.log('API:', method, url, res.status);
            if (res.status === 400) {
                const err = await res.json();
                hideLoading();
                throw err; // Returns validation errors object
            }
            if (res.status === 404) {
                if (method === 'GET') return {content: [], totalPages: 1};

                throw {_global: 'Ресурс не найден (проверь API)'};
            }
            if (res.status === 409) return await res.json(); // Conflict returns body with dishes
            if (res.status >= 500) throw {_global: 'Ошибка сервера. Попробуйте позже.'};
            if (res.ok && method !== 'DELETE') return await res.json();
            return null;
        } catch (e) {
            throw e;
        } finally {
            hideLoading();
        }
    },

    getProducts: (params = {}) => {
        // 🔹 Конвертируем 1-базовую нумерацию фронтенда в 0-базовую для Spring
        const springPage = Math.max(0, (params.page || 1) - 1);

        // 🔹 Собираем только непустые параметры + маппинг имён
        const cleanParams = {};
        const paramMapping = {
            'name': 'nameSearch',
            'sortDirection': 'sortDir',
            'sortField': 'sortBy'  // ← 🔹 ДОБАВИТЬ ЭТУ СТРОКУ
        };

        for (const [key, value] of Object.entries(params)) {
            if (value === null || value === undefined || value === '') continue;
            if (key === 'page') continue;

            const backendKey = paramMapping[key] || key;

            if (key === 'flags' && Array.isArray(value) && value.length > 0) {
                value.forEach(flag => {
                    if (!cleanParams[backendKey]) cleanParams[backendKey] = [];
                    cleanParams[backendKey].push(flag);
                });
            } else {
                cleanParams[backendKey] = value;
            }
        }
        cleanParams.page = springPage;

        const qs = new URLSearchParams(cleanParams).toString();
        return API.request('GET', `/products?${qs}`).then(res => {
            const content = res?.content || res;
            const totalPages = res?.totalPages ?? res?.page?.totalPages ?? 1;
            return {content: content || [], totalPages};
        });
    },
    getProduct: id => API.request('GET', `/products/${id}`),
    createProduct: data => API.request('POST', '/products', data),
    updateProduct: (id, data) => API.request('PUT', `/products/${id}`, data),
    deleteProduct: async id => {
        showLoading();
        try {
            const res = await fetch(`${API_BASE}/products/${id}`, { method: 'DELETE' });

            // 🔹 Обработка конфликта (409) — продукт используется в блюдах
            if (res.status === 409) {
                hideLoading();
                return await res.json(); // { message, conflictingDishes: [...] }
            }

            if (res.status === 404) {
                hideLoading();
                throw { _global: 'Продукт не найден' };
            }

            if (!res.ok) {
                hideLoading();
                throw { _global: 'Ошибка удаления' };
            }

            hideLoading();
            return { success: true };
        } catch (e) {
            hideLoading();
            throw e;
        }
    },

    getDishes: (params = {}) => {
        const springPage = Math.max(0, (params.page || 1) - 1);
        const cleanParams = {};
        const paramMapping = {
            'sortField': 'sortBy',  // ← 🔹 Добавить, если DishController тоже использует sortBy
            'sortDirection': 'sortDir'
        };

        for (const [key, value] of Object.entries(params)) {
            if (value === null || value === undefined || value === '') continue;
            if (key === 'page') continue;

            const backendKey = paramMapping[key] || key;

            if (key === 'flags' && Array.isArray(value) && value.length > 0) {
                value.forEach(flag => {
                    if (!cleanParams[backendKey]) cleanParams[backendKey] = [];
                    cleanParams[backendKey].push(flag);
                });
            } else {
                cleanParams[backendKey] = value;
            }
        }
        cleanParams.page = springPage;

        const qs = new URLSearchParams(cleanParams).toString();
        return API.request('GET', `/dishes?${qs}`).then(res => {
            const content = res?.content || res;
            const totalPages = res?.totalPages ?? res?.page?.totalPages ?? 1;
            return {content: content || [], totalPages};
        });
    },
    getDish: id => API.request('GET', `/dishes/${id}`),
    createDish: data => API.request('POST', '/dishes', data),
    updateDish: (id, data) => API.request('PUT', `/dishes/${id}`, data),
    deleteDish: id => API.request('DELETE', `/dishes/${id}`),
    uploadFile: async (file) => {
        const formData = new FormData();
        formData.append('file', file);
        showLoading();
        try {
            const res = await fetch(`${API_BASE}/files/upload`, {
                method: 'POST',
                body: formData
            });
            if (!res.ok) throw new Error('Ошибка загрузки файла');
            return await res.json(); // { url: "/api/files/uuid.jpg" }
        } finally {
            hideLoading();
        }
    }
};

// ==========================================
// ROUTER
// ==========================================
const routes = {
    '/products': renderProductList,
    '/products/new': () => renderProductForm(),
    '/products/edit/:id': (params) => renderProductForm(params.id),
    '/products/:id': renderProductDetail,
    '/dishes': renderDishList,
    '/dishes/new': () => renderDishForm(),
    '/dishes/edit/:id': (params) => renderDishForm(params.id),
    '/dishes/:id': renderDishDetail,
};

function navigate() {
    const hash = location.hash.slice(1) || '/products';

    // 🔹 Обновляем активные ссылки
    document.querySelectorAll('.nav-link').forEach(a => {
        a.classList.remove('active');
        if (a.getAttribute('href').includes(hash.split('/')[1])) {
            a.classList.add('active');
        }
    });

    // 🔹 Маршрутизация
    for (const pattern in routes) {
        const regex = pattern.replace(/:id/g, '(\\d+)');
        const match = hash.match(new RegExp(`^${regex}$`));
        if (match) {
            const params = match[1] ? {id: match[1]} : {};
            console.log('🔍 Route matched:', pattern, params);

            // 🔹 Сначала рендерим контент
            routes[pattern](params);
            window.scrollTo(0, 0);

            // 🔹 Потом закрываем меню (с небольшой задержкой для анимаций)
            setTimeout(closeMobileMenu, 50);

            return;
        }
    }

    // ❌ Страница не найдена
    document.getElementById('app').innerHTML = `<div class="card"><h2>Страница не найдена</h2></div>`;
    closeMobileMenu();
}

window.addEventListener('hashchange', navigate);
window.addEventListener('load', navigate);

// ==========================================
// PRODUCT LIST
// ==========================================
async function renderProductList() {
    const app = document.getElementById('app');
    app.innerHTML = `
        <div class="card">
            <div class="card-header">
                <h2>Продукты</h2>
                <a href="#/products/new" class="btn btn-primary">+ Добавить продукт</a>
            </div>
            <div class="filters-bar">
                <div class="filter-item"><label class="form-label">Поиск</label><input id="filter-name" class="form-control" placeholder="Название..."></div>
                <div class="filter-item"><label class="form-label">Категория</label><select id="filter-category" class="form-control"><option value="">Все</option>${STATE.categories.product.map(c => `<option>${c}</option>`).join('')}</select></div>
                <div class="filter-item"><label class="form-label">Готовка</label>
                    <select id="filter-cooking" class="form-control">
                        <option value="">Любая</option>
                        <option value="READY_TO_EAT">Готовый</option>
                        <option value="REQUIRES_COOKING">Требует готовки</option>
                        <option value="SEMI_FINISHED">Полуфабрикат</option>
                    </select>
                </div>
                <div class="filter-item" style="flex:0 1 auto; padding-top:1.5rem">
                    <div class="checkbox-group">
                        <label class="checkbox-item"><input type="checkbox" id="filter-vegan"> Веган</label>
                        <label class="checkbox-item"><input type="checkbox" id="filter-gluten"> Без глютена</label>
                        <label class="checkbox-item"><input type="checkbox" id="filter-sugar"> Без сахара</label>
                    </div>
                </div>
            </div>
            <div class="filter-row">
                <div class="filter-item"><label class="form-label">Сортировка</label><select id="sort-field" class="form-control"><option value="name">Название</option><option value="calories">Калории</option><option value="proteins">Белки</option><option value="fats">Жиры</option><option value="carbs">Углеводы</option></select></div>
                <div class="filter-item" style="max-width:100px"><label class="form-label">Порядок</label><select id="sort-dir" class="form-control"><option value="ASC">↑</option><option value="DESC">↓</option></select></div>
            </div>
        </div>
        <div id="product-grid" class="product-grid"></div>
        <div id="pagination" class="pagination"></div>
    `;

    const load = debounce(async () => {
        // 🔹 Собираем флаги как массив enum-строк
        const flags = [];
        if (document.getElementById('filter-vegan').checked) flags.push('VEGAN');
        if (document.getElementById('filter-gluten').checked) flags.push('GLUTEN_FREE');
        if (document.getElementById('filter-sugar').checked) flags.push('SUGAR_FREE');

        // 🔹 Преобразуем категорию из русской в enum
        const ruCategory = document.getElementById('filter-category').value;
        const enCategory = CATEGORY_MAP[ruCategory] || null;

        // 🔹 Получаем cookingRequirement (уже в enum-формате)
        const cookingReq = document.getElementById('filter-cooking').value || null;
        const nameSearch = document.getElementById('filter-name').value || null;

        const params = {
            name: nameSearch,  // будет переименован в nameSearch внутри getProducts
            category: enCategory,
            cookingRequirement: cookingReq,
            flags: flags.length > 0 ? flags : null,  // null будет отфильтрован
            sortField: document.getElementById('sort-field').value,
            sortDirection: document.getElementById('sort-dir').value,
            page: STATE.productPage.current,
            size: 10
        };

        try {
            const res = await API.getProducts(params);
            STATE.products = res.content || res;
            STATE.productPage.totalPages = res.totalPages || 1;
            STATE.productCache = [...STATE.products];
            renderProductCards();
            renderPagination();
        } catch (e) {
            showToast(e._global || 'Ошибка загрузки', 'error');
        }
    }, 350);

    ['filter-name', 'filter-category', 'filter-cooking', 'filter-vegan', 'filter-gluten', 'filter-sugar', 'sort-field', 'sort-dir'].forEach(id => {
        document.getElementById(id).addEventListener('input', () => {
            STATE.productPage.current = 1;
            load();
        });
        document.getElementById(id).addEventListener('change', () => {
            STATE.productPage.current = 1;
            load();
        });
    });

    load();
}

function renderProductCards() {
    const grid = document.getElementById('product-grid');
    grid.innerHTML = STATE.products.map(p => {
        const hasPhotos = p.photos && p.photos.length > 0;

        return `
        <div class="product-card" onclick="location.href='#/products/${p.id}'" style="cursor:pointer">
            <div style="position:relative;height:160px;background:#eee;">
                ${hasPhotos ? `
                    <img src="${p.photos[0]}" alt="${p.name}" style="width:100%;height:100%;object-fit:cover;">
                    ${p.photos.length > 1 ?
            `<div style="position:absolute;bottom:5px;right:5px;background:rgba(0,0,0,0.7);color:white;padding:2px 6px;border-radius:10px;font-size:11px;">+${p.photos.length - 1}</div>`
            : ''}
                ` : `<div style="height:100%;display:flex;align-items:center;justify-content:center;color:#aaa">Нет фото</div>`}
            </div>
            <div class="product-card-body">
                <div class="product-card-title">${p.name}</div>
                <div class="product-meta">${EN_TO_RU_CATEGORY[p.category] || p.category} | Кал: ${formatNum(p.calories)} ккал</div>                
                <div class="flags">
                    ${p.flags?.includes('VEGAN') ? '<span class="flag-badge vegan">Веган</span>' : ''}
                    ${p.flags?.includes('GLUTEN_FREE') ? '<span class="flag-badge gluten-free">Без глютена</span>' : ''}
                    ${p.flags?.includes('SUGAR_FREE') ? '<span class="flag-badge sugar-free">Без сахара</span>' : ''}
                </div>
            </div>
        </div>`;
    }).join('');
}

function renderPagination() {
    const el = document.getElementById('pagination');
    if (STATE.productPage.totalPages <= 1) {
        el.innerHTML = '';
        return;
    }
    let html = `<button ${STATE.productPage.current <= 1 ? 'disabled' : ''} data-page="${STATE.productPage.current - 1}">«</button>`;
    for (let i = 1; i <= STATE.productPage.totalPages; i++) {
        html += `<button class="${i === STATE.productPage.current ? 'active' : ''}" data-page="${i}">${i}</button>`;
    }
    html += `<button ${STATE.productPage.current >= STATE.productPage.totalPages ? 'disabled' : ''} data-page="${STATE.productPage.current + 1}">»</button>`;
    el.innerHTML = html;
    el.querySelectorAll('button:not(:disabled)').forEach(btn => {
        btn.addEventListener('click', () => {
            STATE.productPage.current = Number(btn.dataset.page);
            renderProductList();
        });
    });
}

// ==========================================
// PRODUCT FORM
// ==========================================
async function renderProductForm(id = null) {
    const app = document.getElementById('app');
    const isEdit = !!id;
    let product;

    try {
        product = isEdit
            ? await API.getProduct(id)
            : {
                name: '', category: 'Другое', cookingRequirement: 'READY_TO_EAT',
                calories: 0, proteins: 0, fats: 0, carbs: 0, composition: '',
                vegan: false, glutenFree: false, sugarFree: false, photos: []
            };
    } catch (e) {
        showToast(e._global || 'Ошибка загрузки продукта', 'error');
        location.hash = '#/products';
        return;
    }

    // Хранилище URL загруженных фото
    let currentPhotos = [...(product.photos || [])];

    app.innerHTML = `
        <div class="card">
            <h2>${isEdit ? 'Редактирование продукта' : 'Новый продукт'}</h2>
            <form id="product-form" class="mt-4" novalidate>
                <!-- ... остальные поля без изменений ... -->
                <div class="form-group"><label class="form-label">Название *</label><input id="p-name" class="form-control" value="${product.name}" required minlength="2"></div>
                <div class="form-group"><label class="form-label">Категория</label><select id="p-category" class="form-control">${STATE.categories.product.map(c => `<option ${c === product.category ? 'selected' : ''}>${c}</option>`).join('')}</select></div>
                <div class="form-group"><label class="form-label">Требует готовки</label>
                    <select id="p-cooking" class="form-control">
                        <option value="READY_TO_EAT" ${product.cookingRequirement === 'READY_TO_EAT' ? 'selected' : ''}>Готов к употреблению</option>
                        <option value="REQUIRES_COOKING" ${product.cookingRequirement === 'REQUIRES_COOKING' ? 'selected' : ''}>Требует готовки</option>
                        <option value="SEMI_FINISHED" ${product.cookingRequirement === 'SEMI_FINISHED' ? 'selected' : ''}>Полуфабрикат</option>
                    </select>
                </div>
                <div class="kbju-grid">
                    <div class="form-group kbju-field"><label>Калории</label><input id="p-cal" type="number" step="0.01" min="0" class="form-control" value="${product.calories}"></div>
                    <div class="form-group kbju-field"><label>Белки</label><input id="p-pro" type="number" step="0.01" min="0" max="100" class="form-control" value="${product.proteins}"></div>
                    <div class="form-group kbju-field"><label>Жиры</label><input id="p-fat" type="number" step="0.01" min="0" max="100" class="form-control" value="${product.fats}"></div>
                    <div class="form-group kbju-field"><label>Углеводы</label><input id="p-carb" type="number" step="0.01" min="0" max="100" class="form-control" value="${product.carbs}"></div>
                </div>
                <div class="form-group"><label class="form-label">Состав</label><textarea id="p-comp" class="form-control">${product.composition || ''}</textarea></div>
                <div class="form-group"><label class="form-label">Флаги</label>
                    <div class="checkbox-group">
                        <label class="checkbox-item"><input type="checkbox" id="p-vegan" ${product.vegan ? 'checked' : ''}> Веган</label>
                        <label class="checkbox-item"><input type="checkbox" id="p-gluten" ${product.glutenFree ? 'checked' : ''}> Без глютена</label>
                        <label class="checkbox-item"><input type="checkbox" id="p-sugar" ${product.sugarFree ? 'checked' : ''}> Без сахара</label>
                    </div>
                </div>
                
                <!-- БЛОК ФОТОГРАФИЙ -->
                <div class="form-group">
                    <label class="form-label">Фотографии (макс. 5)</label>
                    <input type="file" id="p-photo-file" accept="image/*" multiple class="form-control">
                    <div id="photo-previews" style="display:flex;gap:10px;flex-wrap:wrap;margin-top:10px;">
                        ${currentPhotos.map(url => `
                            <div style="position:relative;">
                                <img src="${url}" style="width:80px;height:80px;object-fit:cover;border-radius:4px;">
                                <button type="button" class="btn-remove-photo" data-url="${url}" style="position:absolute;top:-5px;right:-5px;background:red;color:white;border:none;border-radius:50%;width:20px;height:20px;cursor:pointer;">×</button>
                            </div>
                        `).join('')}
                    </div>
                </div>

                <div style="margin-top:1rem;display:flex;gap:0.5rem">
                    <button type="submit" class="btn btn-primary">Сохранить</button>
                    <a href="#/products" class="btn btn-secondary">Отмена</a>
                </div>
            </form>
        </div>
    `;
    if (isEdit && product.category) {
        const catSelect = document.getElementById('p-category');
        const ruCategory = EN_TO_RU_CATEGORY[product.category] || product.category;
        if (catSelect) {
            // Находим опцию с нужным русским названием и выбираем её
            for (let option of catSelect.options) {
                if (option.textContent === ruCategory) {
                    option.selected = true;
                    break;
                }
            }
        }
    }

    if (isEdit && product.cookingRequirement) {
        const cookingSelect = document.getElementById('p-cooking');
        if (cookingSelect) {
            cookingSelect.value = product.cookingRequirement; // уже enum, всё ок
        }
    }

    if (isEdit && product.flags) {
        const veganCb = document.getElementById('p-vegan');
        const glutenCb = document.getElementById('p-gluten');
        const sugarCb = document.getElementById('p-sugar');

        // Используем hasFlag для безопасной проверки (работает с Array/Set)
        if (veganCb) veganCb.checked = hasFlag(product.flags, 'VEGAN');
        if (glutenCb) glutenCb.checked = hasFlag(product.flags, 'GLUTEN_FREE');
        if (sugarCb) sugarCb.checked = hasFlag(product.flags, 'SUGAR_FREE');
    }


    // Обработка загрузки фото
    const fileInput = document.getElementById('p-photo-file');
    const previewsContainer = document.getElementById('photo-previews');

    fileInput.addEventListener('change', async (e) => {
        const files = Array.from(e.target.files);
        if (currentPhotos.length + files.length > 5) {
            showToast('Максимум 5 фотографий', 'error');
            fileInput.value = '';
            return;
        }

        for (const file of files) {
            try {
                const res = await API.uploadFile(file);
                currentPhotos.push(res.url);
                renderPreviews();
            } catch (err) {
                showToast(`Ошибка загрузки ${file.name}`, 'error');
            }
        }
        fileInput.value = ''; // Сброс input для повторной загрузки тех же файлов
    });

    // Удаление фото из превью
    previewsContainer.addEventListener('click', (e) => {
        if (e.target.classList.contains('btn-remove-photo')) {
            const urlToRemove = e.target.dataset.url;
            currentPhotos = currentPhotos.filter(u => u !== urlToRemove);
            renderPreviews();
        }
    });

    function renderPreviews() {
        previewsContainer.innerHTML = currentPhotos.map(url => `
            <div style="position:relative;">
                <img src="${url}" style="width:80px;height:80px;object-fit:cover;border-radius:4px;">
                <button type="button" class="btn-remove-photo" data-url="${url}" style="position:absolute;top:-5px;right:-5px;background:red;color:white;border:none;border-radius:50%;width:20px;height:20px;cursor:pointer;">×</button>
            </div>
        `).join('');
    }

    // Отправка формы
    // Отправка формы продукта
    document.getElementById('product-form').addEventListener('submit', async (e) => {
        e.preventDefault();

        const errors = [];
        const name = document.getElementById('p-name');
        const calories = document.getElementById('p-cal');
        const proteins = document.getElementById('p-pro');
        const fats = document.getElementById('p-fat');
        const carbs = document.getElementById('p-carb');

        // 🔹 Валидация имени
        if (name.value.trim().length < 2) {
            errors.push('Название должно содержать минимум 2 символа');
            name.classList.add('is-invalid');
        } else {
            name.classList.remove('is-invalid');
        }

        // 🔹 Валидация КБЖУ
        const cal = Number(calories.value);
        const pro = Number(proteins.value);
        const fat = Number(fats.value);
        const carb = Number(carbs.value);

        if (cal < 0) errors.push('Калорийность не может быть отрицательной');
        if (pro < 0 || pro > 100) errors.push('Белки должны быть от 0 до 100 г');
        if (fat < 0 || fat > 100) errors.push('Жиры должны быть от 0 до 100 г');
        if (carb < 0 || carb > 100) errors.push('Углеводы должны быть от 0 до 100 г');
        if (pro + fat + carb > 100) errors.push('Сумма БЖУ не может превышать 100 г на 100 г продукта');

        // 🔹 Если есть ошибки — показываем алёрт
        if (errors.length > 0) {
            await showValidationError('Продукт', errors);
            return;
        }

        // 🔹 Сбор данных и отправка
        const flags = [];
        if (document.getElementById('p-vegan').checked) flags.push('VEGAN');
        if (document.getElementById('p-gluten').checked) flags.push('GLUTEN_FREE');
        if (document.getElementById('p-sugar').checked) flags.push('SUGAR_FREE');

        const data = {
            name: name.value,
            category: CATEGORY_MAP[document.getElementById('p-category').value] || null,
            cookingRequirement: document.getElementById('p-cooking').value,
            calories: cal,
            proteins: pro,
            fats: fat,
            carbs: carb,
            composition: document.getElementById('p-comp').value,
            flags: flags,
            photos: currentPhotos
        };

        try {
            if (isEdit) await API.updateProduct(id, data);
            else await API.createProduct(data);
            showToast(isEdit ? 'Продукт обновлен' : 'Продукт создан', 'success');
            location.hash = '#/products';
        } catch (err) {
            // 🔹 Обработка ошибок от сервера (400)
            if (err && typeof err === 'object' && !err._global) {
                const serverErrors = Object.entries(err)
                    .map(([field, msgs]) => `${field}: ${Array.isArray(msgs) ? msgs.join(', ') : msgs}`);
                await showValidationError('Сервер', serverErrors);
            } else {
                showToast(err._global || 'Ошибка сохранения', 'error');
            }
        }
    });
}

// ==========================================
// PRODUCT DETAIL
// ==========================================
async function renderProductDetail({id}) {
    const app = document.getElementById('app');
    try {
        const p = await API.getProduct(id);
        if (!p) throw new Error();

        // Маппинг значений для отображения
        const ruCategory = EN_TO_RU_CATEGORY[p.category] || p.category;
        const ruCooking = COOKING_REQ_MAP[p.cookingRequirement] || p.cookingRequirement;

        app.innerHTML = `
            <div class="card">
                <div class="detail-header">
                    <div>
                        <h2>${p.name}</h2>
                        <div class="detail-meta">
                            Категория: ${ruCategory} |
                            Готовка: ${ruCooking} |
                            Создано: ${formatDate(p.createdAt)}
                            ${(p.updatedAt && formatDate(p.updatedAt) !== formatDate(p.createdAt)) ? ` | Обновлено: ${formatDate(p.updatedAt)}` : ''}                        </div>
                        <div class="flags">
                            ${hasFlag(p.flags, 'VEGAN') ? '<span class="flag-badge vegan">Веган</span>' : ''}
                            ${hasFlag(p.flags, 'GLUTEN_FREE') ? '<span class="flag-badge gluten-free">Без глютена</span>' : ''}
                            ${hasFlag(p.flags, 'SUGAR_FREE') ? '<span class="flag-badge sugar-free">Без сахара</span>' : ''}
                        </div>
                    </div>
                    <div style="display:flex;gap:0.5rem;flex-wrap:wrap">
                        <a href="#/products/edit/${p.id}" class="btn btn-primary btn-sm">Редактировать</a>
                        <button id="delete-product" class="btn btn-danger btn-sm">Удалить</button>
                    </div>
                </div>

                <!-- Галерея фото -->
                ${p.photos && p.photos.length > 0 ? `
                    <div style="margin:1.5rem 0;">
                        <img id="main-photo" class="detail-image" src="${p.photos[0]}" alt="${p.name}" style="width:100%;max-height:400px;object-fit:contain;border-radius:var(--radius);cursor:pointer;">
                        ${p.photos.length > 1 ? `
                            <div style="display:flex;gap:8px;overflow-x:auto;padding:8px 0;margin-top:8px;scrollbar-width:thin;">
                                ${p.photos.map((url, idx) => `
                                    <img src="${url}" style="width:70px;height:70px;object-fit:cover;border-radius:4px;cursor:pointer;border:2px solid ${idx === 0 ? 'var(--primary)' : 'transparent'};flex-shrink:0;"
                                         onclick="document.getElementById('main-photo').src='${url}'; this.parentElement.querySelectorAll('img').forEach(i=>i.style.borderColor='transparent'); this.style.borderColor='var(--primary)';">
                                `).join('')}
                            </div>
                        ` : ''}
                    </div>
                ` : ''}

                <!-- КБЖУ -->
                <div class="kbju-grid">
                    <div class="card"><strong>Калории</strong><br>${formatNum(p.calories)} ккал</div>
                    <div class="card"><strong>Белки</strong><br>${formatNum(p.proteins)} г</div>
                    <div class="card"><strong>Жиры</strong><br>${formatNum(p.fats)} г</div>
                    <div class="card"><strong>Углеводы</strong><br>${formatNum(p.carbs)} г</div>
                </div>

                ${p.composition ? `<div style="margin-top:1rem"><strong>Состав:</strong> ${p.composition}</div>` : ''}
            </div>
        `;

        document.getElementById('delete-product').addEventListener('click', async () => {
            const confirmed = await showModal('Удалить продукт?', `Вы уверены, что хотите удалить "${p.name}"?`);
            if (!confirmed) return;

            try {
                const result = await API.deleteProduct(p.id);

                if (result?.conflictingDishes?.length > 0) {
                    // 🔹 Красивый НЕкликабельный список блюд
                    const dishList = result.conflictingDishes.map(d => {
                        const category = EN_TO_RU_DISH_CATEGORY[d.category] || d.category || 'Без категории';
                        return `<li class="conflict-dish-item">
                            <span class="dish-name">${d.name}</span>
                            <span class="dish-category">${category}</span>
                        </li>`;
                    }).join('');

                    await showAlert(
                        '⚠️ Невозможно удалить',
                        `Продукт <strong>"${p.name}"</strong> используется в следующих блюдах:<br><br>Удалите его из этих блюд или измените состав.`,
                        `<ul class="conflict-dishes-list">${dishList}</ul>`
                    );
                    return;
                }

                showToast('Продукт удален', 'success');
                location.hash = '#/products';
            } catch (err) {
                showToast(err._global || 'Ошибка удаления', 'error');
            }
        });
    } catch (e) {
        console.error('renderProductDetail error:', e);
        app.innerHTML = `<div class="card"><h2>Продукт не найден</h2><p><a href="#/products">← Назад к списку</a></p></div>`;
    }
}

// ==========================================
// DISH LIST
// ==========================================
async function renderDishList() {
    const app = document.getElementById('app');
    app.innerHTML = `
        <div class="card">
            <div class="card-header">
                <h2>Блюда</h2>
                <a href="#/dishes/new" class="btn btn-primary">+ Добавить блюдо</a>
            </div>
            <div class="filters-bar">
                <div class="filter-item"><label class="form-label">Поиск</label><input id="d-filter-name" class="form-control" placeholder="Название..."></div>
                <div class="filter-item"><label class="form-label">Категория</label><select id="d-filter-category" class="form-control"><option value="">Все</option>${STATE.categories.dish.map(c => `<option>${c}</option>`).join('')}</select></div>
                <div class="filter-item" style="flex:0 1 auto; padding-top:1.5rem">
                    <div class="checkbox-group">
                        <label class="checkbox-item"><input type="checkbox" id="d-filter-vegan"> Веган</label>
                        <label class="checkbox-item"><input type="checkbox" id="d-filter-gluten"> Без глютена</label>
                        <label class="checkbox-item"><input type="checkbox" id="d-filter-sugar"> Без сахара</label>
                    </div>
                </div>
            </div>
        </div>
        <div class="table-wrapper">
            <table id="dish-table">
                <thead>
                    <tr>
                        <th style="width:60px">Фото</th>  <!-- 🔹 Новая колонка -->
                        <th>Название</th>
                        <th>Категория</th>
                        <th>Порция</th>
                        <th>Ккал</th>
                        <th>Б</th>
                        <th>Ж</th>
                        <th>У</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody id="dish-tbody"></tbody>
            </table>        
        </div>
        <div id="dish-pagination" class="pagination"></div>
    `;

    const load = debounce(async () => {
        // 🔹 Собираем флаги как массив enum-строк
        const flags = [];
        if (document.getElementById('d-filter-vegan').checked) flags.push('VEGAN');
        if (document.getElementById('d-filter-gluten').checked) flags.push('GLUTEN_FREE');
        if (document.getElementById('d-filter-sugar').checked) flags.push('SUGAR_FREE');

        // 🔹 Преобразуем категорию из русской в enum
        const ruCategory = document.getElementById('d-filter-category').value;
        // Используем DISH_CATEGORY_MAP для конвертации "Суп" -> "SOUP"
        const enCategory = DISH_CATEGORY_MAP[ruCategory] || null;

        const nameSearch = document.getElementById('d-filter-name').value || null;

        const params = {
            nameSearch: nameSearch,
            category: enCategory,  // 🔹 Отправляем enum, а не русское название
            flags: flags.length > 0 ? flags : null,
            page: STATE.dishPage?.current || 1,
            size: 10
        };

        try {
            const res = await API.getDishes(params);
            STATE.dishes = res.content || res;
            STATE.dishPage = {current: params.page, totalPages: res.totalPages || 1};
            renderDishRows();
            renderDishPagination();
        } catch (e) {
            showToast(e._global || 'Ошибка загрузки блюд', 'error');
        }
    }, 350);

    ['d-filter-name', 'd-filter-category', 'd-filter-vegan', 'd-filter-gluten', 'd-filter-sugar'].forEach(id => {
        document.getElementById(id).addEventListener('input', () => {
            if (STATE.dishPage) STATE.dishPage.current = 1;
            load();
        });
        document.getElementById(id).addEventListener('change', () => {
            if (STATE.dishPage) STATE.dishPage.current = 1;
            load();
        });
    });

    STATE.dishPage = {current: 1};
    load();
}

function renderDishRows() {
    const tbody = document.getElementById('dish-tbody');

    if (!STATE.dishes || STATE.dishes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:2rem;color:var(--text-muted)">Нет блюд</td></tr>';
        return;
    }

    tbody.innerHTML = STATE.dishes.map(d => {
        const hasPhotos = d.photos && d.photos.length > 0;

        return `
            <tr style="cursor:pointer" onclick="location.href='#/dishes/${d.id}'" class="dish-row">
                <!-- 🔹 Колонка с фото -->
                <td style="vertical-align:middle;">
                    ${hasPhotos ? `
                        <div style="position:relative;display:inline-block;">
                            <img src="${d.photos[0]}" alt="${d.name}" 
                                 style="width:50px;height:50px;object-fit:cover;border-radius:4px;background:#eee;">
                            ${d.photos.length > 1 ?
            `<span style="position:absolute;bottom:-2px;right:-2px;background:rgba(0,0,0,0.7);color:white;padding:1px 4px;border-radius:8px;font-size:9px;line-height:1;">+${d.photos.length-1}</span>`
            : ''}
                        </div>
                    ` : `<span style="color:var(--text-muted);font-size:0.85rem">—</span>`}
                </td>
                
                <td style="vertical-align:middle;font-weight:500">${d.name}</td>
                <td style="vertical-align:middle">${EN_TO_RU_DISH_CATEGORY[d.category] || d.category}</td>
                <td style="vertical-align:middle;text-align:center">${d.portionSize} г</td>
                <td style="vertical-align:middle;text-align:center;font-weight:600">${formatNum(d.calories)}</td>
                <td style="vertical-align:middle;text-align:center">${formatNum(d.proteins)}</td>
                <td style="vertical-align:middle;text-align:center">${formatNum(d.fats)}</td>
                <td style="vertical-align:middle;text-align:center">${formatNum(d.carbs)}</td>
                <td style="vertical-align:middle;">
                    <a href="#/dishes/edit/${d.id}" class="btn-icon" onclick="event.stopPropagation()" title="Редактировать">✏️</a>
                </td>
            </tr>
        `;
    }).join('');
}

function renderDishPagination() {
    const el = document.getElementById('dish-pagination');
    if (STATE.dishPage.totalPages <= 1) {
        el.innerHTML = '';
        return;
    }
    let html = `<button ${STATE.dishPage.current <= 1 ? 'disabled' : ''} data-p="${STATE.dishPage.current - 1}">«</button>`;
    for (let i = 1; i <= STATE.dishPage.totalPages; i++) html += `<button class="${i === STATE.dishPage.current ? 'active' : ''}" data-p="${i}">${i}</button>`;
    html += `<button ${STATE.dishPage.current >= STATE.dishPage.totalPages ? 'disabled' : ''} data-p="${STATE.dishPage.current + 1}">»</button>`;
    el.innerHTML = html;
    el.querySelectorAll('button:not(:disabled)').forEach(btn => btn.addEventListener('click', () => {
        STATE.dishPage.current = Number(btn.dataset.p);
        renderDishList();
    }));
}

async function renderDishForm(id = null) {
    const app = document.getElementById('app');
    const isEdit = !!id;

    async function ensureProductCache() {
        if (STATE.productCache.length === 0) {
            try {
                const res = await API.getProducts({ size: 1000 });
                STATE.productCache = res.content || res;
                return true;
            } catch (e) {
                console.error('Failed to load product cache:', e);
                showToast('Не удалось загрузить список продуктов', 'error');
                return false;
            }
        }
        return true;
    }

    const cacheLoaded = await ensureProductCache();
    if (!cacheLoaded) {
        location.hash = '#/dishes';
        return;
    }

    let dish = isEdit ? await API.getDish(id) : {
        name: '', category: '', portionSize: 100, ingredients: [],
        calories: 0, proteins: 0, fats: 0, carbs: 0,
        vegan: false, glutenFree: false, sugarFree: false, photos: []
    };
    if (!dish) return showToast('Блюдо не найдено', 'error');

    let ingredients = (dish.ingredients || []).map(ing => ({
        productId: Number(ing.productId),
        weight: Number(ing.quantityInGrams)
    }));

    let currentPhotos = [...(dish.photos || [])];

    app.innerHTML = `
        <div class="card">
            <h2>${isEdit ? 'Редактирование блюда' : 'Новое блюдо'}</h2>
            <form id="dish-form" novalidate>
                <div class="form-group">
                    <label class="form-label">Название *</label>
                    <input id="d-name" class="form-control" value="${dish.name}" placeholder="!суп Том Ям">
                    <div class="invalid-feedback">Введите название</div>
                </div>

                <div class="form-group">
                    <label class="form-label">Категория</label>
                    <select id="d-category" class="form-control">
                        <option value="">Авто</option>
                        ${STATE.categories.dish.map(c =>
                            `<option ${c === (isEdit ? EN_TO_RU_DISH_CATEGORY[dish.category] : '') ? 'selected' : ''}>${c}</option>`
                        ).join('')}
                    </select>
                </div>

                <div class="form-group">
                    <label class="form-label">Размер порции (г) *</label>
                    <input id="d-portion" type="number" step="1" min="1" class="form-control" value="${dish.portionSize}">
                </div>

                <div class="form-group">
                    <label class="form-label">Фотографии (макс. 5)</label>
                    <input type="file" id="d-photo-file" accept="image/*" multiple class="form-control">
                    <div id="d-photo-previews" style="display:flex;gap:10px;flex-wrap:wrap;margin-top:10px;">
                        ${currentPhotos.map(url => `
                            <div style="position:relative;">
                                <img src="${url}" style="width:80px;height:80px;object-fit:cover;border-radius:4px;">
                                <button type="button" class="btn-remove-dish-photo" data-url="${url}"
                                        style="position:absolute;top:-5px;right:-5px;background:red;color:white;border:none;border-radius:50%;width:20px;height:20px;cursor:pointer;">×</button>
                            </div>
                        `).join('')}
                    </div>
                </div>

                <h3 style="margin:1.5rem 0 0.5rem">Ингредиенты</h3>
                <div id="ingredients-list"></div>
                <button type="button" id="add-ingredient" class="btn btn-secondary btn-sm" style="margin-top:0.5rem">+ Добавить ингредиент</button>

                <div class="ingredient-summary">
                    <h4>Расчет КБЖУ</h4>

                    <!-- Основные поля для редактирования (На порцию) -->
                    <div class="kbju-grid" style="margin-bottom: 1rem; border-bottom: 1px solid #eee; padding-bottom: 1rem;">
                        <div class="kbju-field"><label>На порцию</label><input id="d-cal" type="number" step="0.01" class="form-control" value="${formatNum(dish.calories)}"></div>
                        <div class="kbju-field"><label>Белки</label><input id="d-pro" type="number" step="0.01" class="form-control" value="${formatNum(dish.proteins)}"></div>
                        <div class="kbju-field"><label>Жиры</label><input id="d-fat" type="number" step="0.01" class="form-control" value="${formatNum(dish.fats)}"></div>
                        <div class="kbju-field"><label>Углеводы</label><input id="d-carb" type="number" step="0.01" class="form-control" value="${formatNum(dish.carbs)}"></div>
                    </div>

                    <!-- Информационные поля (только чтение) -->
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; font-size: 0.9rem; color: var(--text-muted);">
                        <div>
                            <strong>На 100г:</strong><br>
                            <span id="info-cal-100">—</span> ккал |
                            <span id="info-pro-100">—</span> Б |
                            <span id="info-fat-100">—</span> Ж |
                            <span id="info-carb-100">—</span> У
                        </div>
                    </div>
                </div>

                <div class="form-group" style="margin-top:1rem">
                    <label class="form-label">Флаги</label>
                    <div class="checkbox-group">
                        <label class="checkbox-item"><input type="checkbox" id="d-vegan"> Веган</label>
                        <label class="checkbox-item"><input type="checkbox" id="d-gluten"> Без глютена</label>
                        <label class="checkbox-item"><input type="checkbox" id="d-sugar"> Без сахара</label>
                    </div>
                </div>

                <div style="margin-top:1rem;display:flex;gap:0.5rem">
                    <button type="submit" class="btn btn-primary">Сохранить</button>
                    <a href="#/dishes" class="btn btn-secondary">Отмена</a>
                </div>
            </form>
        </div>
    `;

    // 🔹 5. Инициализация isDirty для КБЖУ
    STATE.isDirty = { calories: false, proteins: false, fats: false, carbs: false };
    if (isEdit && dish.calories != null) {
        Object.keys(STATE.isDirty).forEach(k => STATE.isDirty[k] = true);
    }

    // ==========================================
    // ФУНКЦИИ ПЕРЕСЧЁТА (объявляем ДО обработчиков)
    // ==========================================

    // 🔹 Расчёт КБЖУ
// 🔹 Расчёт КБЖУ (абсолютные значения, без учёта порции)
// 🔹 Расширенный расчёт КБЖУ: На 100г, На порцию, Абсолютное
        // 🔹 Расчёт КБЖУ: Абсолютные значения (сумма всех ингредиентов)
    function recalculate() {
        let totalCal = 0, totalPro = 0, totalFat = 0, totalCarb = 0;
        let totalWeight = 0;

        ingredients.forEach(ing => {
            const prod = STATE.productCache.find(p => String(p.id) === String(ing.productId));
            if (!prod || !ing.weight || ing.weight <= 0) return;

            const ratio = ing.weight / 100;
            totalCal += (prod.calories || 0) * ratio;
            totalPro += (prod.proteins || 0) * ratio;
            totalFat += (prod.fats || 0) * ratio;
            totalCarb += (prod.carbs || 0) * ratio;
            totalWeight += ing.weight;
        });

        const portionSize = Number(document.getElementById('d-portion')?.value) || 100;
        const hasIngredients = ingredients.some(i => i.productId && i.weight > 0);

        const fmt = (val) => hasIngredients && val > 0.01 ? formatNum(val) : '—';

        // 1. Расчет на 100г блюда (для инфо-блока)
        const calPer100 = totalWeight > 0 ? (totalCal / totalWeight) * 100 : 0;
        const proPer100 = totalWeight > 0 ? (totalPro / totalWeight) * 100 : 0;
        const fatPer100 = totalWeight > 0 ? (totalFat / totalWeight) * 100 : 0;
        const carbPer100 = totalWeight > 0 ? (totalCarb / totalWeight) * 100 : 0;

        // 🔹 Обновляем ОСНОВНЫЕ поля формы АБСОЛЮТНЫМИ значениями
        const setIfNotDirty = (id, val) => {
            const el = document.getElementById(id);
            const key = id.replace('d-', '');
            if (el && !STATE.isDirty[key]) el.value = fmt(val);
        };

        // Сохраняем абсолютные значения в основные поля
        setIfNotDirty('d-cal', totalCal);
        setIfNotDirty('d-pro', totalPro);
        setIfNotDirty('d-fat', totalFat);
        setIfNotDirty('d-carb', totalCarb);

        // Заполняем информационные поля
        const setText = (id, val) => {
            const el = document.getElementById(id);
            if (el) el.textContent = fmt(val);
        };

        setText('info-cal-100', calPer100);
        setText('info-pro-100', proPer100);
        setText('info-fat-100', fatPer100);
        setText('info-carb-100', carbPer100);

        // В блоке "Всего" теперь дублируем абсолютные значения (так как они основные)
        setText('info-cal-abs', totalCal);
        setText('info-pro-abs', totalPro);
        setText('info-fat-abs', totalFat);
        setText('info-carb-abs', totalCarb);
    }

    // 🔹 Обновление доступности флагов
    function updateFlagsAvailability() {
        const veganCb = document.getElementById('d-vegan');
        const glutenCb = document.getElementById('d-gluten');
        const sugarCb = document.getElementById('d-sugar');
        if (!veganCb || !glutenCb || !sugarCb) return;

        const selectedProducts = ingredients
            .map(ing => {
                const prod = STATE.productCache.find(p => String(p.id) === String(ing.productId));
                return prod; // может быть undefined
            })
            .filter(Boolean); // убираем undefined

        console.log('🔍 Flags check:', {
            ingredientsCount: ingredients.length,
            validProducts: selectedProducts.map(p => ({
                name: p.name,
                flags: Array.from(p.flags || [])
            })),
            allVegan: selectedProducts.every(p => hasFlag(p.flags, 'VEGAN')),
            allGlutenFree: selectedProducts.every(p => hasFlag(p.flags, 'GLUTEN_FREE')),
            allSugarFree: selectedProducts.every(p => hasFlag(p.flags, 'SUGAR_FREE'))
        });

        if (selectedProducts.length === 0) {
            [veganCb, glutenCb, sugarCb].forEach(cb => {
                cb.checked = false;
                cb.disabled = true; // 🔹 Блокируем, т.к. нет данных
                cb.parentElement?.classList.add('disabled');
                cb.title = 'Добавьте ингредиенты для установки флагов';
            });
            return;
        }

        const allVegan = selectedProducts.every(p => hasFlag(p.flags, 'VEGAN'));
        const allGlutenFree = selectedProducts.every(p => hasFlag(p.flags, 'GLUTEN_FREE'));
        const allSugarFree = selectedProducts.every(p => hasFlag(p.flags, 'SUGAR_FREE'));

        const updateCheckbox = (cb, allowed, flagName) => {
            if (!allowed) {
                cb.checked = false;
                cb.disabled = true;
                cb.parentElement?.classList.add('disabled');
                cb.title = `Недоступно: не все продукты имеют флаг "${flagName}"`;
            } else {
                cb.disabled = false;
                cb.parentElement?.classList.remove('disabled');
                cb.title = '';
            }
        };

        updateCheckbox(veganCb, allVegan, 'Веган');
        updateCheckbox(glutenCb, allGlutenFree, 'Без глютена');
        updateCheckbox(sugarCb, allSugarFree, 'Без сахара');
    }

    // 🔹 Единая функция обновления (КБЖУ + флаги)
    function updateAll() {
        recalculate();
        updateFlagsAvailability();
    }

    // 🔹 Debounced-версия для плавного UX
    const debouncedUpdate = debounce(updateAll, 150);

    // 🔹 Рендер строк ингредиентов
    function renderIngredients() {
        const list = document.getElementById('ingredients-list');
        if (!list) return;

        if (STATE.productCache.length === 0) {
            list.innerHTML = '<div style="color:var(--text-muted);padding:1rem">Загрузка списка продуктов...</div>';
            return;
        }

        list.innerHTML = '';
        ingredients.forEach((ing, idx) => {
            const row = document.createElement('div');
            row.className = 'ingredient-row';
            row.innerHTML = `
                <select class="ing-select form-control" data-idx="${idx}">
                    <option value="">Выберите продукт</option>
                    ${STATE.productCache.map(p =>
                        `<option value="${p.id}" ${String(p.id) === String(ing.productId) ? 'selected' : ''}>${p.name}</option>`
                    ).join('')}
                </select>
                <input type="number" step="1" min="0" class="ing-weight form-control"
                       value="${ing.weight || ''}" placeholder="г" data-idx="${idx}">
                <button type="button" class="btn-icon delete-ing" data-idx="${idx}">❌</button>
            `;
            list.appendChild(row);
        });

        updateAll(); // 🔹 Пересчёт после рендера
    }

    // 🔹 Рендер превью фото
    function renderDishPhotoPreviews() {
        const container = document.getElementById('d-photo-previews');
        if (!container) return;
        container.innerHTML = currentPhotos.map(url => `
            <div style="position:relative;">
                <img src="${url}" style="width:80px;height:80px;object-fit:cover;border-radius:4px;">
                <button type="button" class="btn-remove-dish-photo" data-url="${url}"
                        style="position:absolute;top:-5px;right:-5px;background:red;color:white;border:none;border-radius:50%;width:20px;height:20px;cursor:pointer;">×</button>
            </div>
        `).join('');
    }

    // ==========================================
    // ПРИВЯЗКА ОБРАБОТЧИКОВ
    // ==========================================

    // 🔹 Загрузка фото
    document.getElementById('d-photo-file')?.addEventListener('change', async (e) => {
        const files = Array.from(e.target.files);
        if (currentPhotos.length + files.length > 5) {
            showToast('Максимум 5 фотографий', 'error');
            e.target.value = '';
            return;
        }
        for (const file of files) {
            try {
                const res = await API.uploadFile(file);
                currentPhotos.push(res.url);
                renderDishPhotoPreviews();
            } catch (err) {
                showToast(`Ошибка загрузки ${file.name}`, 'error');
            }
        }
        e.target.value = '';
    });

    document.getElementById('d-photo-previews')?.addEventListener('click', (e) => {
        if (e.target.classList.contains('btn-remove-dish-photo')) {
            currentPhotos = currentPhotos.filter(u => u !== e.target.dataset.url);
            renderDishPhotoPreviews();
        }
    });

    // 🔹 Авто-определение категории по названию
    const nameInput = document.getElementById('d-name');
    const catSelect = document.getElementById('d-category');
    const macros = {
        '!десерт': 'Десерт', '!суп': 'Суп', '!второе': 'Второе',
        '!салат': 'Салат', '!перекус': 'Перекус', '!напиток': 'Напиток', '!первое': 'Первое'
    };

    catSelect?.addEventListener('change', () => {
        if (catSelect.value && catSelect.value !== 'Авто') {
            catSelect.dataset.manual = 'true';
        } else {
            delete catSelect.dataset.manual;
        }
    });

    // 🔹 Улучшенная обработка макросов: удаляет ВСЕ, берёт первый для категории
    // 🔹 Улучшенная обработка макросов: удаляет ВСЕ, берёт первый для категории
// 🔹 Улучшенная обработка макросов: удаляет ВСЕ, берёт первый для категории
    // 🔹 Улучшенная обработка макросов: удаляет ВСЕ, берёт первый для категории
    const updateCategoryFromName = () => {
        if (catSelect?.dataset?.manual === 'true') return;
        if (catSelect.value && catSelect.value !== '' && catSelect.value !== 'Авто') return;

        let val = nameInput.value;
        const originalVal = val;

        // 🔹 Паттерны: сортировка по длине (убывание) для корректного матча
        const macroPatterns = Object.keys(macros)
            .sort((a, b) => b.length - a.length)
            .map(m => m.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));

        // 🔹 RegExp для ПОИСКА (чтобы найти первый по позиции)
        const searchRegex = new RegExp(`(${macroPatterns.join('|')})`, 'gi');
        const matches = [...val.matchAll(searchRegex)];

        if (matches.length > 0) {
            // 🔹 Первый по ПОЗИЦИИ в строке
            const firstMatch = matches.reduce((prev, curr) =>
                prev.index < curr.index ? prev : curr
            );

            // 🔹 Находим точный ключ в macros (учитываем регистр)
            const macroKey = Object.keys(macros).find(
                key => key.toLowerCase() === firstMatch[0].toLowerCase()
            );
            if (macroKey && macros[macroKey]) {
                catSelect.value = macros[macroKey];
            }

            // 🔹 ОТДЕЛЬНЫЙ RegExp для ЗАМЕНЫ (свежий, без состояния)
            const replaceRegex = new RegExp(`(${macroPatterns.join('|')})`, 'gi');
            let cleanedName = val.replace(replaceRegex, '');

            // 🔹 Аккуратная очистка
            cleanedName = cleanedName
                .replace(/\s{2,}/g, ' ')
                .replace(/^\s+|\s+$/g, '')
                .replace(/\s*!\s*/g, ' ')
                .replace(/!/g, '')
                .trim()
                .replace(/\s+/g, ' ');

            if (cleanedName && cleanedName !== originalVal.trim()) {
                nameInput.value = cleanedName;
                nameInput.dispatchEvent(new Event('input', { bubbles: true }));
            }
        }
    };
    nameInput?.addEventListener('input', updateCategoryFromName);
    if (dish.name) updateCategoryFromName();

    // 🔹 Предустановка категории при редактировании
    if (isEdit && dish.category) {
        const ruCategory = EN_TO_RU_DISH_CATEGORY[dish.category] || dish.category;
        if (catSelect) {
            for (let option of catSelect.options) {
                if (option.textContent === ruCategory) {
                    option.selected = true;
                    catSelect.dataset.manual = 'true';
                    break;
                }
            }
        }
    }

    // 🔹 Предустановка флагов при редактировании
    if (isEdit && dish.flags) {
        document.getElementById('d-vegan').checked = hasFlag(dish.flags, 'VEGAN');
        document.getElementById('d-gluten').checked = hasFlag(dish.flags, 'GLUTEN_FREE');
        document.getElementById('d-sugar').checked = hasFlag(dish.flags, 'SUGAR_FREE');
    }
    updateAll(); // 🔹 Пересчёт после предустановки

    // 🔹 Управление ингредиентами
    const list = document.getElementById('ingredients-list');

    document.getElementById('add-ingredient')?.addEventListener('click', () => {
        ingredients.push({ productId: '', weight: 0 });
        renderIngredients();
    });

    // 🔹 Изменение веса → пересчёт
    list?.addEventListener('input', (e) => {
        if (e.target.classList.contains('ing-weight')) {
            const idx = +e.target.dataset.idx;
            const val = Number(e.target.value);
            ingredients[idx].weight = isNaN(val) ? 0 : val;
            debouncedUpdate();
        }
    });

    // 🔹 Изменение продукта → пересчёт
    list?.addEventListener('change', (e) => {
        if (e.target.classList.contains('ing-select')) {
            ingredients[+e.target.dataset.idx].productId = e.target.value ? Number(e.target.value) : '';
            debouncedUpdate();
        }
    });

    // 🔹 Удаление ингредиента → пересчёт
    list?.addEventListener('click', (e) => {
        if (e.target.classList.contains('delete-ing')) {
            ingredients.splice(+e.target.dataset.idx, 1);
            renderIngredients(); // внутри уже есть updateAll()
        }
    });

    // 🔹 Изменение порции → пересчёт КБЖУ на 100г
    document.getElementById('d-portion')?.addEventListener('input', () => {
        recalculate(); // флаги не зависят от порции
    });

    // 🔹 Ручное редактирование КБЖУ
    ['d-cal', 'd-pro', 'd-fat', 'd-carb'].forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('input', () => {
                STATE.isDirty[id.replace('d-', '')] = true;
                el.style.borderColor = 'var(--warning)';
                el.title = 'Значение изменено вручную';
            });
            el.addEventListener('focus', () => {
                if (el.value === '—') el.value = '';
            });
        }
    });

    // ==========================================
    // ОТПРАВКА ФОРМЫ
    // ==========================================
    // Отправка формы блюда
    document.getElementById('dish-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();

        const errors = [];
        const nameVal = document.getElementById('d-name');
        const portionVal = document.getElementById('d-portion');

        // 🔹 Валидация имени
        if (!nameVal.value.trim()) {
            errors.push('Название блюда обязательно');
            nameVal.classList.add('is-invalid');
        } else {
            nameVal.classList.remove('is-invalid');
        }

        // 🔹 Валидация порции
        const portion = Number(portionVal.value);
        if (portion <= 0) {
            errors.push('Размер порции должен быть больше 0');
            portionVal.classList.add('is-invalid');
        } else {
            portionVal.classList.remove('is-invalid');
        }

        // 🔹 Валидация ингредиентов
        const validIngredients = ingredients.filter(i => i.productId && i.weight > 0);
        if (validIngredients.length === 0) {
            errors.push('Добавьте хотя бы один ингредиент');
        }

        // 🔹 Валидация КБЖУ (опционально, если пользователь редактировал вручную)
        if (STATE.isDirty.calories || STATE.isDirty.proteins || STATE.isDirty.fats || STATE.isDirty.carbs) {
            const cal = Number(document.getElementById('d-cal').value) || 0;
            const pro = Number(document.getElementById('d-pro').value) || 0;
            const fat = Number(document.getElementById('d-fat').value) || 0;
            const carb = Number(document.getElementById('d-carb').value) || 0;

            if (cal < 0) errors.push('Калорийность не может быть отрицательной');
            if (pro < 0) errors.push('Белки не могут быть отрицательными');
            if (fat < 0) errors.push('Жиры не могут быть отрицательными');
            if (carb < 0) errors.push('Углеводы не могут быть отрицательными');
        }

        // 🔹 Если есть ошибки — показываем алёрт
        if (errors.length > 0) {
            await showValidationError('Блюдо', errors);
            return;
        }

        // 🔹 Сбор данных и отправка
        const flags = [];
        if (document.getElementById('d-vegan').checked) flags.push('VEGAN');
        if (document.getElementById('d-gluten').checked) flags.push('GLUTEN_FREE');
        if (document.getElementById('d-sugar').checked) flags.push('SUGAR_FREE');

        const isManualEdit = STATE.isDirty.calories || STATE.isDirty.proteins ||
                             STATE.isDirty.fats || STATE.isDirty.carbs;
        const parseKbzhu = (id) => {
            const val = document.getElementById(id)?.value;
            if (!val || val === '—') return null;
            const num = Number(val);
            return isNaN(num) ? null : num;
        };
        const selectedCategory = catSelect.value;

        const data = {
            name: nameVal.value,
            category: (selectedCategory && selectedCategory !== 'Авто')
                ? DISH_CATEGORY_MAP[selectedCategory]
                : undefined,
            portionSize: portion,
            photos: currentPhotos,
            ingredients: validIngredients.map(i => ({
                productId: i.productId,
                quantityInGrams: i.weight
            })),
            calories: isManualEdit ? parseKbzhu('d-cal') : null,
            proteins: isManualEdit ? parseKbzhu('d-pro') : null,
            fats: isManualEdit ? parseKbzhu('d-fat') : null,
            carbs: isManualEdit ? parseKbzhu('d-carb') : null,
            flags: flags.length > 0 ? flags : undefined
        };

        try {
            if (isEdit) await API.updateDish(id, data);
            else await API.createDish(data);
            showToast(isEdit ? 'Блюдо обновлено' : 'Блюдо создано', 'success');
            location.hash = '#/dishes';
        } catch (err) {
            if (err && typeof err === 'object' && !err._global) {
                const serverErrors = Object.entries(err)
                    .map(([field, msgs]) => `${field}: ${Array.isArray(msgs) ? msgs.join(', ') : msgs}`);
                await showValidationError('Сервер', serverErrors);
            } else {
                showToast(err._global || 'Ошибка сохранения', 'error');
            }
        }
    });

    // 🔹 Финальный рендер
    renderIngredients();
}

// 🔹 Алёрт для ошибок валидации формы
const showValidationError = (fieldLabel, errors) => new Promise(resolve => {
    const modal = document.getElementById('confirm-modal');
    const titleEl = document.getElementById('confirm-title');
    const messageEl = document.getElementById('confirm-message');
    const extraEl = document.getElementById('confirm-extra');
    const okBtn = document.getElementById('confirm-ok');
    const cancelBtn = document.getElementById('confirm-cancel');

    // 🔹 Формируем сообщение об ошибках
    const errorList = Array.isArray(errors)
        ? errors.map(e => `<li>${e}</li>`).join('')
        : `<li>${errors}</li>`;

    // 🔹 Настройка контента
    titleEl.textContent = '⚠️ Ошибка валидации';
    messageEl.innerHTML = `Поле <strong>"${fieldLabel}"</strong> содержит ошибки:<br><ul style="margin:0.5rem 0 0 1.5rem;padding:0;text-align:left">${errorList}</ul>`;
    extraEl.innerHTML = '';

    // 🔹 Показываем только кнопку "Понятно"
    okBtn.classList.add('hidden');
    cancelBtn.textContent = 'Понятно';
    cancelBtn.className = 'btn btn-primary';

    // 🔹 Показываем модальное окно
    modal.classList.remove('hidden');

    // 🔹 Обработчик закрытия
    const onClose = () => {
        modal.classList.add('hidden');
        okBtn.classList.remove('hidden');
        cancelBtn.textContent = 'Отмена';
        cancelBtn.className = 'btn btn-secondary';
        resolve();
    };

    cancelBtn.onclick = onClose;
    modal.onclick = (e) => { if (e.target === modal) onClose(); };
});

// ==========================================
// DISH DETAIL
// ==========================================
// ==========================================
// DISH DETAIL
// ==========================================
async function renderDishDetail({id}) {
    const app = document.getElementById('app');
    try {
        const d = await API.getDish(id);
        if (!d) throw new Error();

        // 🔹 Исправлено: используем quantityInGrams вместо weight
        const totalWeight = d.ingredients.reduce((s, i) => s + (i.quantityInGrams || 0), 0);

        // 🔹 Упрощённая таблица без колонки "Вклад"
        const tableRows = (d.ingredients || []).map(ing => {
            let p = STATE.productCache.find(x => String(x?.id) === String(ing?.productId));

            // 🔹 Если не нашли в кэше — пробуем дозагрузить
            if (!p && ing?.productId) {
                API.getProduct(ing.productId).then(freshProduct => {
                    STATE.productCache.push(freshProduct);
                    renderDishDetail({id: d.id}); // Рекурсивный ререндер
                }).catch(() => {
                    // Если не удалось — показываем заглушку
                });
                p = { name: 'Загрузка...', calories: 0, proteins: 0, fats: 0, carbs: 0, flags: [] };
            }

            p = p || { name: 'Продукт не найден', calories: 0, proteins: 0, fats: 0, carbs: 0, flags: [] };

            const weight = ing?.quantityInGrams || 0;

            // 🔹 Возвращаем только 3 колонки
            return `<tr>
                <td>${p.name}</td>
                <td>${formatNum(weight)} г</td>
                <td>${formatNum(p.calories)} | ${formatNum(p.proteins)} | ${formatNum(p.fats)} | ${formatNum(p.carbs)}</td>
            </tr>`;
        }).join('');

        app.innerHTML = `
            <div class="card">
                <div class="detail-header">
                    <div>
                        <h2>${d.name}</h2>
                        <div class="detail-meta">Категория: ${EN_TO_RU_DISH_CATEGORY[d.category] || d.category || '—'} | Порция: ${d.portionSize} г | Создано: ${formatDate(d.createdAt)}
                            ${(d.updatedAt && formatDate(d.updatedAt) !== formatDate(d.createdAt)) ? ` | Обновлено: ${formatDate(d.updatedAt)}` : ''}                        </div>
                        </div>
                        <div class="flags">
                            ${hasFlag(d.flags, 'VEGAN') ? '<span class="flag-badge vegan">Веган</span>' : ''}
                            ${hasFlag(d.flags, 'GLUTEN_FREE') ? '<span class="flag-badge gluten-free">Без глютена</span>' : ''}
                            ${hasFlag(d.flags, 'SUGAR_FREE') ? '<span class="flag-badge sugar-free">Без сахара</span>' : ''}
                        </div>
                    </div>
                    <div style="display:flex;gap:0.5rem;flex-wrap:wrap">
                        <a href="#/dishes/edit/${d.id}" class="btn btn-primary btn-sm">Редактировать</a>
                        <button id="delete-dish" class="btn btn-danger btn-sm">Удалить</button>
                    </div>
                </div>
                ${d.photos && d.photos.length > 0 ? `
                    <div style="margin:1.5rem 0;">
                        <img id="dish-main-photo" class="detail-image" src="${d.photos[0]}" alt="${d.name}" style="width:100%;max-height:400px;object-fit:contain;border-radius:var(--radius);">
                        ${d.photos.length > 1 ? `
                            <div style="display:flex;gap:8px;overflow-x:auto;padding:8px 0;margin-top:8px;">
                                ${d.photos.map((url, idx) => `
                                    <img src="${url}" style="width:70px;height:70px;object-fit:cover;border-radius:4px;cursor:pointer;border:2px solid ${idx===0?'var(--primary)':'transparent'};"
                                         onclick="document.getElementById('dish-main-photo').src='${url}'; this.parentElement.querySelectorAll('img').forEach(i=>i.style.borderColor='transparent'); this.style.borderColor='var(--primary)';">
                                `).join('')}
                            </div>
                        ` : ''}
                    </div>
                ` : ''}
                <div class="kbju-grid">
                    <div class="card"><strong>Калории</strong><br>${formatNum(d.calories)} ккал</div>
                    <div class="card"><strong>Белки</strong><br>${formatNum(d.proteins)} г</div>
                    <div class="card"><strong>Жиры</strong><br>${formatNum(d.fats)} г</div>
                    <div class="card"><strong>Углеводы</strong><br>${formatNum(d.carbs)} г</div>
                </div>
                <h3 style="margin:1.5rem 0 0.5rem">Состав (${totalWeight} г)</h3>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr>
                                <th>Продукт</th>
                                <th>Вес</th>
                                <th>На 100г</th>
                            </tr>
                        </thead>
                        <tbody>${tableRows || '<tr><td colspan="4">Нет ингредиентов</td></tr>'}</tbody>
                    </table>
                </div>
            </div>
        `;

        document.getElementById('delete-dish').addEventListener('click', async () => {
            if (!await showModal('Удалить блюдо?', `Удалить "${d.name}"?`)) return;
            try {
                await API.deleteDish(d.id);
                showToast('Блюдо удалено', 'success');
                location.hash = '#/dishes';
            } catch (err) {
                showToast(err._global || 'Ошибка удаления', 'error');
            }
        });
    } catch {
        app.innerHTML = `<div class="card"><h2>Блюдо не найдено</h2><p><a href="#/dishes">← Назад</a></p></div>`;
    }
}

// ==========================================
// INIT: Load cache for selects
// ==========================================
(async () => {
    try {
        const res = await API.getProducts({ size: 1000 });
        STATE.productCache = res.content || res;
        console.log('✅ Product cache loaded:', STATE.productCache.length, 'items');
    } catch (e) {
        console.error('❌ Failed to load product cache:', e);
        showToast('Не удалось загрузить список продуктов', 'error');
    }
})();

// 🔹 УНИВЕРСАЛЬНЫЙ ОБРАБОТЧИК: закрывает меню при любом переходе по ссылке
document.addEventListener('click', (e) => {
    // 🔹 Проверяем, был ли клик по ссылке с hash-навигацией
    const link = e.target.closest('a[href^="#"]');
    if (link && link.getAttribute('href').startsWith('#')) {
        // 🔹 Небольшая задержка, чтобы переход успел начаться
        setTimeout(() => {
            closeMobileMenuUniversal();
        }, 50);
    }
});

// 🔹 Максимально универсальная функция закрытия меню
const closeMobileMenu = () => {
    console.log('🔍 closeMobileMenu() вызвана');

    // 🔹 1. Закрываем ВСЕ возможные меню (десктоп + мобильные)
    const menuSelectors = [
        '.sidebar', '.nav-sidebar', 'aside', '[role="navigation"]',
        '#mobile-menu', '.mobile-menu', '.offcanvas', '.drawer',
        '[data-toggle="menu"]', '[data-bs-toggle="offcanvas"]'
    ];

    menuSelectors.forEach(sel => {
        const el = document.querySelector(sel);
        if (el) {
            console.log('🔍 Found menu:', sel, el);
            el.classList.remove('active', 'open', 'show', 'visible', 'expanded');
            el.setAttribute('aria-hidden', 'true');
            el.style.display = el.dataset.originalDisplay || '';

            // Bootstrap Offcanvas
            if (window.bootstrap?.Offcanvas && el.classList.contains('offcanvas')) {
                const instance = bootstrap.Offcanvas.getInstance(el);
                instance?.hide();
                console.log('🔍 Bootstrap offcanvas hidden');
            }
        }
    });

    // 🔹 2. Убираем оверлеи
    ['.overlay', '.backdrop', '.modal-backdrop', '#nav-overlay'].forEach(sel => {
        const el = document.querySelector(sel);
        if (el) {
            el.classList.remove('active', 'show', 'visible');
            el.style.display = 'none';
            setTimeout(() => el.remove?.(), 300); // Удаляем после анимации
        }
    });

    // 🔹 3. Сбрасываем гамбургер-кнопки
    ['.nav-toggle', '.hamburger', '.menu-toggle', '.navbar-toggler'].forEach(sel => {
        const btn = document.querySelector(sel);
        if (btn) {
            btn.classList.remove('active', 'is-active', 'toggled');
            btn.setAttribute('aria-expanded', 'false');
        }
    });

    // 🔹 4. Разблокируем скролл
    ['html', 'body'].forEach(tag => {
        const el = document.querySelector(tag);
        if (el) {
            el.style.overflow = '';
            el.style.position = '';
            el.classList.remove('menu-open', 'no-scroll', 'overflow-hidden');
        }
    });

    // 🔹 5. Сбрасываем активные ссылки навигации (если нужно)
    // ❗️ Раскомментируйте, если активные классы мешают
    // document.querySelectorAll('.nav-link.active').forEach(a => {
    //     if (!location.href.includes(a.href)) {
    //         a.classList.remove('active');
    //     }
    // });

    console.log('✅ closeMobileMenu() завершена');
};