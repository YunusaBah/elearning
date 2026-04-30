const state = {
    username: localStorage.getItem("elearning.username") || "",
    password: localStorage.getItem("elearning.password") || "",
    role: localStorage.getItem("elearning.role") || "",
    department: localStorage.getItem("elearning.department") || ""
};

const API_BASE = (() => {
    if (window.location.protocol === "file:") {
        return "http://localhost:8080";
    }
    const localHosts = new Set(["localhost", "127.0.0.1", "[::1]", "::1"]);
    if (localHosts.has(window.location.hostname) && window.location.port && window.location.port !== "8080") {
        return `http://${window.location.hostname}:8080`;
    }
    return "";
})();

const els = {
    navItems: document.querySelectorAll(".nav-item"),
    sections: {
        library: document.querySelector("#librarySection"),
        upload: document.querySelector("#uploadSection"),
        account: document.querySelector("#accountSection")
    },
    statusPill: document.querySelector("#statusPill"),
    sessionName: document.querySelector("#sessionName"),
    sessionMeta: document.querySelector("#sessionMeta"),
    availableCount: document.querySelector("#availableCount"),
    accessMode: document.querySelector("#accessMode"),
    departmentName: document.querySelector("#departmentName"),
    filesBody: document.querySelector("#filesBody"),
    fileCount: document.querySelector("#fileCount"),
    departmentFilter: document.querySelector("#departmentFilter"),
    refreshBtn: document.querySelector("#refreshBtn"),
    filterBtn: document.querySelector("#filterBtn"),
    loginForm: document.querySelector("#loginForm"),
    registerForm: document.querySelector("#registerForm"),
    uploadForm: document.querySelector("#uploadForm"),
    toast: document.querySelector("#toast")
};

function authHeader() {
    return "Basic " + btoa(`${state.username}:${state.password}`);
}

function setSession(username, password, role, department) {
    state.username = username;
    state.password = password;
    state.role = role || state.role;
    state.department = department || state.department;
    localStorage.setItem("elearning.username", state.username);
    localStorage.setItem("elearning.password", state.password);
    localStorage.setItem("elearning.role", state.role);
    localStorage.setItem("elearning.department", state.department);
    renderSession();
}

function renderSession() {
    const signedIn = Boolean(state.username && state.password);
    els.statusPill.textContent = signedIn ? "Online" : "Offline";
    els.statusPill.classList.toggle("online", signedIn);
    els.sessionName.textContent = signedIn ? state.username : "Guest";
    els.sessionMeta.textContent = signedIn
        ? [state.role || "User", state.department].filter(Boolean).join(" / ")
        : "Register or sign in";
    els.accessMode.textContent = signedIn ? (state.role || "User") : "Guest";
    els.departmentName.textContent = signedIn ? (state.department || "All") : "None";
}

function showToast(message) {
    els.toast.textContent = message;
    els.toast.classList.add("show");
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => els.toast.classList.remove("show"), 3200);
}

function showSection(name) {
    Object.entries(els.sections).forEach(([sectionName, section]) => {
        section.classList.toggle("hidden", sectionName !== name);
    });
    els.navItems.forEach(item => item.classList.toggle("active", item.dataset.section === name));
}

function formatDate(value) {
    if (!value) {
        return "";
    }
    return new Intl.DateTimeFormat(undefined, {
        year: "numeric",
        month: "short",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.username && state.password) {
        headers.set("Authorization", authHeader());
    }
    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, { ...options, headers });
    } catch (error) {
        throw new Error(`Cannot reach the backend at ${API_BASE || window.location.origin}. Make sure Spring Boot is running on port 8080.`);
    }
    if (!response.ok) {
        const contentType = response.headers.get("Content-Type") || "";
        const text = await response.text();
        const message = contentType.includes("text/html")
            ? `${response.status} ${response.statusText || "Request failed"}`
            : text;
        throw new Error(message || "Request failed");
    }
    return response;
}

async function loadFiles() {
    if (!state.username || !state.password) {
        els.filesBody.innerHTML = `<tr><td colspan="5" class="empty">Sign in to load files.</td></tr>`;
        els.fileCount.textContent = "No files loaded";
        return;
    }

    const department = els.departmentFilter.value.trim();
    const path = department ? `/api/files/department/${encodeURIComponent(department)}` : "/api/files";
    try {
        const response = await api(path);
        const files = await response.json();
        renderFiles(files);
    } catch (error) {
        els.filesBody.innerHTML = `<tr><td colspan="5" class="empty">Unable to load files.</td></tr>`;
        els.fileCount.textContent = "Load failed";
        showToast(error.message);
    }
}

async function loadCurrentUser() {
    const response = await api("/api/auth/me");
    return response.json();
}

function renderFiles(files) {
    els.fileCount.textContent = files.length === 1 ? "1 file" : `${files.length} files`;
    els.availableCount.textContent = String(files.length);
    if (!files.length) {
        els.filesBody.innerHTML = `<tr><td colspan="5" class="empty">No files found.</td></tr>`;
        return;
    }

    els.filesBody.innerHTML = files.map(file => `
        <tr>
            <td>${escapeHtml(file.fileName || "Untitled")}</td>
            <td>${escapeHtml(file.department || "")}</td>
            <td>${escapeHtml(file.uploadedBy || "")}</td>
            <td>${escapeHtml(formatDate(file.uploadedAt))}</td>
            <td>
                <div class="file-actions">
                    <button class="download-btn" type="button" data-download-id="${file.id}">Download</button>
                    ${state.role === "LECTURER" ? `<button class="delete-btn" type="button" data-delete-id="${file.id}" data-file-name="${escapeHtml(file.fileName || "this file")}">Delete</button>` : ""}
                </div>
            </td>
        </tr>
    `).join("");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function downloadFile(id) {
    try {
        const response = await api(`/api/files/download/${id}`);
        const blob = await response.blob();
        const disposition = response.headers.get("Content-Disposition") || "";
        const match = disposition.match(/filename="(.+)"/);
        const fileName = match ? match[1] : `course-file-${id}`;
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    } catch (error) {
        showToast(error.message);
    }
}

async function deleteFile(id, fileName) {
    if (!window.confirm(`Delete ${fileName}?`)) {
        return;
    }

    try {
        await api(`/api/files/${id}`, { method: "DELETE" });
        showToast("File deleted");
        loadFiles();
    } catch (error) {
        showToast(error.message || "Delete failed");
    }
}

els.navItems.forEach(item => {
    item.addEventListener("click", () => showSection(item.dataset.section));
});

els.filesBody.addEventListener("click", event => {
    const downloadButton = event.target.closest("[data-download-id]");
    if (downloadButton) {
        downloadFile(downloadButton.dataset.downloadId);
        return;
    }

    const deleteButton = event.target.closest("[data-delete-id]");
    if (deleteButton) {
        deleteFile(deleteButton.dataset.deleteId, deleteButton.dataset.fileName);
    }
});

els.refreshBtn.addEventListener("click", loadFiles);
els.filterBtn.addEventListener("click", loadFiles);

els.loginForm.addEventListener("submit", async event => {
    event.preventDefault();
    const username = document.querySelector("#loginUsername").value.trim();
    const password = document.querySelector("#loginPassword").value;
    state.username = username;
    state.password = password;
    try {
        const currentUser = await loadCurrentUser();
        setSession(username, password, currentUser.role, currentUser.department);
        showToast("Signed in");
        showSection("library");
        loadFiles();
    } catch (error) {
        state.username = "";
        state.password = "";
        renderSession();
        showToast("Sign in failed");
    }
});

els.registerForm.addEventListener("submit", async event => {
    event.preventDefault();
    const username = document.querySelector("#registerUsername").value.trim();
    const password = document.querySelector("#registerPassword").value;
    const role = document.querySelector("#registerRole").value;
    const department = document.querySelector("#registerDepartment").value.trim();

    try {
        await api("/api/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, email: username, password, role, department })
        }).then(async response => {
            if (!response.ok) {
                throw new Error(await response.text());
            }
        });
        setSession(username, password, role, department);
        showToast("Account created");
        showSection("library");
        loadFiles();
    } catch (error) {
        showToast(error.message || "Registration failed");
    }
});

els.uploadForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (!state.username || !state.password) {
        showToast("Sign in with a lecturer account before uploading.");
        showSection("account");
        return;
    }
    if (state.role !== "LECTURER") {
        showToast("Only lecturer accounts can upload files.");
        return;
    }

    const formData = new FormData();
    formData.append("department", document.querySelector("#uploadDepartment").value.trim());
    formData.append("file", document.querySelector("#uploadFile").files[0]);

    try {
        await api("/api/files/upload", {
            method: "POST",
            body: formData
        });
        els.uploadForm.reset();
        showToast("File uploaded");
        showSection("library");
        loadFiles();
    } catch (error) {
        showToast(error.message || "Upload failed");
    }
});

renderSession();
loadFiles();
