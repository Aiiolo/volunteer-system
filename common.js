const API_BASE = "http://localhost:8080";

function ensureThemeStylesheet() {
    if (document.querySelector('link[href="css/theme.css"]')) return;
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = "css/theme.css";
    document.head.appendChild(link);
}

ensureThemeStylesheet();

function bindActiveNavFlip() {
    document.querySelectorAll(".nav-item.active").forEach(item => {
        if (item.dataset.flipBound) return;
        item.dataset.flipBound = "1";
        item.addEventListener("mouseleave", () => {
            item.classList.remove("nav-flip-once");
            void item.offsetWidth;
            item.classList.add("nav-flip-once");
        });
        item.addEventListener("animationend", () => item.classList.remove("nav-flip-once"));
    });
}

function ensureSiteFooter() {
    if (!document.body || document.querySelector(".site-footer")) return;
    if (/login\.html|role-select\.html|index\.html$/i.test(location.pathname)) return;
    const footer = document.createElement("footer");
    footer.className = "site-footer";
    footer.innerHTML = `
        <div class="site-footer-inner">
            <div class="site-footer-title">联系我们</div>
            <div class="site-footer-info">
                <span>电话：025-52897064</span>
                <span>邮箱：zxp@sju.edu.cn</span>
                <span>地址：江苏省南京市雨花台区龙西路 310 号（三江学院铁心桥校区）</span>
            </div>
            <div class="site-footer-copy">© 2026 三江学院志愿者招募与活动管理系统 版权所有</div>
        </div>
    `;
    document.body.appendChild(footer);
}

function token() {
    return localStorage.getItem("token") || "";
}

function currentUserId() {
    return Number(localStorage.getItem("userId") || 0);
}

function currentUsername() {
    return localStorage.getItem("username") || "用户";
}

function currentRealName() {
    return localStorage.getItem("realName") || currentUsername();
}

function currentRole() {
    return localStorage.getItem("backendRole") || "";
}

function getTimeGreeting() {
    const hour = new Date().getHours();
    if (hour >= 5 && hour <= 11) return "早上好";
    if (hour >= 12 && hour <= 17) return "下午好";
    if (hour >= 18 && hour <= 21) return "晚上好";
    return "夜深了，注意休息";
}

function buildUserBadge(roleKey) {
    const config = ROLE_NAV_CONFIG[roleKey];
    const roleLabel = config ? config.roleText : statusText(currentRole()) || "用户";
    return `${roleLabel}：${currentRealName()}`;
}

function isLoginPage() {
    return /login\.html|role-select\.html|index\.html$/i.test(location.pathname);
}

function requireLogin(requiredRole) {
    if (!token()) {
        if (!isLoginPage()) {
            alert("请先登录");
            location.href = navHref("role-select.html");
        }
        return false;
    }
    if (requiredRole && currentRole() !== requiredRole) {
        alert("当前账号无权访问该页面");
        const map = { VOLUNTEER: "volunteer.html", ADMIN: "admin.html", ORGANIZER: "active.html" };
        location.href = navHref(map[currentRole()] || "role-select.html");
        return false;
    }
    return true;
}

function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("username");
    localStorage.removeItem("realName");
    localStorage.removeItem("backendRole");
    localStorage.removeItem("userRole");
    sessionStorage.clear();
    location.href = navHref("role-select.html");
}

async function api(path, options = {}) {
    const headers = Object.assign({ "Content-Type": "application/json" }, options.headers || {});
    if (token()) headers.Authorization = `Bearer ${token()}`;
    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, Object.assign({}, options, { headers }));
    } catch (error) {
        throw new Error("无法连接后端服务，请确认 Spring Boot 已启动");
    }
    const text = await response.text();
    let result = null;
    try {
        result = text ? JSON.parse(text) : null;
    } catch (error) {
        throw new Error("接口返回格式异常");
    }
    if (response.status === 401 || response.status === 403) {
        throw new Error("登录已过期或权限不足，请重新登录");
    }
    if (!response.ok || (result && result.success === false)) {
        throw new Error((result && result.message) || "请求失败");
    }
    return result ? result.data : null;
}

function fmt(value) {
    if (!value) return "-";
    return String(value).replace("T", " ").substring(0, 16);
}

function resolveActivityDisplayStatus(activityOrStatus, startTime, endTime) {
    let status = activityOrStatus;
    let start = startTime;
    let end = endTime;
    if (activityOrStatus && typeof activityOrStatus === "object") {
        status = activityOrStatus.status;
        start = activityOrStatus.startTime;
        end = activityOrStatus.endTime;
    }
    if (status === "DRAFT" || status === "PENDING") return status;
    if (!start || !end) return status;
    const now = Date.now();
    const startMs = new Date(start).getTime();
    const endMs = new Date(end).getTime();
    if (now < startMs) return "PUBLISHED";
    if (now >= endMs) return "COMPLETED";
    return "ONGOING";
}

function activityStatusText(status, activity) {
    const effective = activity
        ? resolveActivityDisplayStatus(activity)
        : resolveActivityDisplayStatus(status, activity && activity.startTime, activity && activity.endTime);
    return {
        DRAFT: "草稿",
        PENDING: "待审核",
        PUBLISHED: "报名中",
        ONGOING: "进行中",
        COMPLETED: "已结束"
    }[effective] || status || "-";
}

function participantStudentNo(item) {
    return (item && (item.studentNo || item.volunteerStudentNo)) || "";
}

function volunteerRegisterHint(activity, application) {
    if (application) {
        if (application.status === "PENDING") return "已报名，等待审核";
        if (application.status === "REJECTED") return "报名未通过";
        if (application.status !== "REJECTED") return "已报名";
    }
    const raw = activity && activity.status;
    const effective = activity ? resolveActivityDisplayStatus(activity) : "";
    if (raw === "DRAFT" || raw === "PENDING") return "活动尚未发布，暂不可报名";
    if (effective === "COMPLETED") return "活动已结束，不可报名";
    if (effective === "ONGOING") return "活动进行中，报名已关闭";
    return "暂不可报名";
}

function buildApplicationByActivityId(applications) {
    const map = {};
    (applications || []).forEach(a => {
        if (a && a.activityId != null) map[a.activityId] = a;
    });
    return map;
}

function volunteerHasApplied(application) {
    return !!(application && application.status !== "REJECTED");
}

function forumActivityOptionId(a) {
    return a && (a.activityId != null ? a.activityId : a.id);
}

function forumActivityOptionTitle(a) {
    return (a && (a.activityTitle || a.title)) || "活动";
}

function normalizeApplicationStatus(status) {
    return status === "APPROVED" ? "WAIT_CHECK_IN" : status;
}

function applicationStatusText(status) {
    const s = normalizeApplicationStatus(status);
    return {
        PENDING: "待审核",
        WAIT_CHECK_IN: "待签到",
        WAIT_CHECK_IN_CONFIRM: "待签到确认",
        WAIT_CHECK_OUT: "待签退",
        WAIT_CHECK_OUT_CONFIRM: "待签退确认",
        COMPLETED: "已完成",
        REJECTED: "已拒绝",
        ABSENT: "已缺勤"
    }[s] || s || "-";
}

function organizerAppStatusText(item) {
    if (!item) return "-";
    return applicationStatusText(item.status);
}

function isActivityRegisterable(activity) {
    return resolveActivityDisplayStatus(activity) === "PUBLISHED";
}

function isActivityInProgress(activity) {
    return resolveActivityDisplayStatus(activity) === "ONGOING";
}

function isActivityTimeCompleted(activity) {
    return resolveActivityDisplayStatus(activity) === "COMPLETED";
}

function mediaUrl(path) {
    if (!path) return "";
    if (/^https?:\/\//i.test(path)) return path;
    return `${API_BASE}${path.startsWith("/") ? path : "/" + path}`;
}

async function apiUpload(path, file) {
    const formData = new FormData();
    formData.append("file", file);
    const headers = {};
    if (token()) headers.Authorization = `Bearer ${token()}`;
    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, { method: "POST", headers, body: formData });
    } catch (error) {
        throw new Error("无法连接后端服务，请确认 Spring Boot 已启动");
    }
    const text = await response.text();
    let result = null;
    try {
        result = text ? JSON.parse(text) : null;
    } catch (error) {
        throw new Error("接口返回格式异常");
    }
    if (response.status === 401 || response.status === 403) {
        throw new Error("登录已过期或权限不足，请重新登录");
    }
    if (!response.ok || (result && result.success === false)) {
        throw new Error((result && result.message) || "上传失败");
    }
    return result ? result.data : null;
}

function volunteerAttendanceLabel(item) {
    return applicationStatusText(item && item.status);
}

function renderVolunteerAttendanceActions(item) {
    const s = normalizeApplicationStatus(item && item.status);
    const id = item.activityId;
    if (s === "PENDING" || s === "COMPLETED" || s === "REJECTED" || s === "ABSENT") {
        return s === "COMPLETED" ? '<span class="pill status-done">活动已完结</span>' : "";
    }
    if (s === "WAIT_CHECK_IN_CONFIRM") {
        return '<button class="btn-pass btn-volunteer-pending" type="button" disabled>已提交签到</button>';
    }
    if (s === "WAIT_CHECK_OUT_CONFIRM") {
        return '<button class="btn-pass btn-volunteer-pending" type="button" disabled>已提交签退</button>';
    }
    if (s === "WAIT_CHECK_IN") {
        return `<button class="btn-pass" type="button" onclick="volunteerCheckIn(${id})">签到</button>`;
    }
    if (s === "WAIT_CHECK_OUT") {
        return `<button class="btn-pass" type="button" onclick="volunteerCheckOut(${id})">签退</button>`;
    }
    return "";
}

function attendanceManageApiBase() {
    return currentRole() === "ADMIN" ? "/api/v1/admin" : "/api/v1/organizer";
}

async function organizerConfirmCheckIn(activityId, studentNo) {
    if (!confirm("确定同意该志愿者的签到申请？")) return;
    try {
        await api(`${attendanceManageApiBase()}/attendance/confirm-check-in`, {
            method: "POST",
            body: JSON.stringify({ activityId, studentNo })
        });
        await refreshParticipantListAfterAction();
        alert("已同意签到");
    } catch (e) { alert(e.message); }
}

async function organizerConfirmCheckOut(activityId, studentNo) {
    if (!confirm("确定同意该志愿者的签退申请？")) return;
    try {
        await api(`${attendanceManageApiBase()}/attendance/confirm-check-out`, {
            method: "POST",
            body: JSON.stringify({ activityId, studentNo })
        });
        await refreshParticipantListAfterAction();
        alert("已同意签退，活动已完结");
    } catch (e) { alert(e.message); }
}

async function organizerMarkAbsent(activityId, studentNo) {
    if (!confirm("确定将该志愿者标记为旷活动？")) return;
    try {
        await api(`${attendanceManageApiBase()}/attendance/mark-absent`, {
            method: "POST",
            body: JSON.stringify({ activityId, studentNo })
        });
        await refreshParticipantListAfterAction();
        alert("已标记为已缺勤");
    } catch (e) { alert(e.message); }
}

async function organizerProxyCheckIn(activityId, studentNo) {
    if (!confirm("确定为该志愿者代为签到？")) return;
    try {
        await api(`${attendanceManageApiBase()}/attendance/proxy-check-in`, {
            method: "POST",
            body: JSON.stringify({ activityId, studentNo })
        });
        await refreshParticipantListAfterAction();
        alert("代为签到成功");
    } catch (e) { alert(e.message); }
}

async function organizerProxyCheckOut(activityId, studentNo) {
    if (!confirm("确定为该志愿者代为签退？")) return;
    try {
        await api(`${attendanceManageApiBase()}/attendance/proxy-check-out`, {
            method: "POST",
            body: JSON.stringify({ activityId, studentNo })
        });
        await refreshParticipantListAfterAction();
        alert("代为签退成功，活动已完结");
    } catch (e) { alert(e.message); }
}

async function refreshParticipantListAfterAction() {
    if (typeof refreshAfterAppAction === "function") await refreshAfterAppAction();
    else if (typeof loadApps === "function" && typeof selectedActivityId !== "undefined" && selectedActivityId) {
        await loadApps(selectedActivityId, false);
    }
}

function renderParticipantActions(item) {
    if (!item) return "-";
    const s = normalizeApplicationStatus(item.status);
    const aid = item.activityId;
    const studentNo = participantStudentNo(item);
    if (!studentNo) return "-";
    const sn = JSON.stringify(studentNo);
    if (s === "PENDING") {
        return `<button class="btn-pass" type="button" onclick="auditApp(${item.id}, 'WAIT_CHECK_IN')">同意</button>
                <button class="btn-reject" type="button" onclick="auditApp(${item.id}, 'REJECTED')">拒绝</button>`;
    }
    if (s === "WAIT_CHECK_IN") {
        return `<button class="btn-pass" type="button" onclick="organizerProxyCheckIn(${aid}, ${sn})">代为签到</button>
                <button class="btn-reject" type="button" onclick="organizerMarkAbsent(${aid}, ${sn})">旷活动</button>`;
    }
    if (s === "WAIT_CHECK_IN_CONFIRM") {
        return `<button class="btn-pass" type="button" onclick="organizerConfirmCheckIn(${aid}, ${sn})">同意签到</button>
                <button class="btn-reject" type="button" onclick="organizerMarkAbsent(${aid}, ${sn})">旷活动</button>`;
    }
    if (s === "WAIT_CHECK_OUT") {
        return `<button class="btn-pass" type="button" onclick="organizerProxyCheckOut(${aid}, ${sn})">代为签退</button>`;
    }
    if (s === "WAIT_CHECK_OUT_CONFIRM") {
        return `<button class="btn-pass" type="button" onclick="organizerConfirmCheckOut(${aid}, ${sn})">同意签退</button>`;
    }
    return "-";
}

function renderParticipantTableHtml(list) {
    if (!list || !list.length) return "";
    return `<table>
        <thead><tr>
            <th>报名ID</th><th>学号</th><th>姓名</th><th>报名时间</th><th>状态</th><th>负责人操作</th>
        </tr></thead>
        <tbody>${renderParticipantTableRows(list)}</tbody>
    </table>`;
}

function renderParticipantTableRows(list) {
    return list.map(x => `<tr>
        <td>${x.id}</td>
        <td>${participantStudentNo(x) || "-"}</td>
        <td>${x.volunteerName || "-"}</td>
        <td>${fmt(x.applyTime)}</td>
        <td>${organizerAppStatusText(x)}</td>
        <td class="ops-cell">${renderParticipantActions(x)}</td>
    </tr>`).join("");
}

async function auditApp(applicationId, status) {
    const label = status === "REJECTED" ? "拒绝" : "同意";
    if (!confirm(`确定${label}该报名？`)) return;
    const path = currentRole() === "ADMIN"
        ? "/api/v1/admin/activities/applications/audit"
        : "/api/v1/organizer/applications/audit";
    try {
        await api(path, {
            method: "POST",
            body: JSON.stringify({ applicationId, status })
        });
        await refreshParticipantListAfterAction();
        alert(status === "REJECTED" ? "已拒绝" : "已同意");
    } catch (e) { alert(e.message); }
}

const ACTIVITY_IMAGE_ACCEPT = ".jpg,.jpeg,.png,.webp";
const ACTIVITY_IMAGE_MAX_BYTES = 5 * 1024 * 1024;
let activityImageUrlDraft = "";

function resetActivityImageDraft(url) {
    activityImageUrlDraft = url || "";
    const preview = document.getElementById("activityImagePreview");
    const input = document.getElementById("activityImageInput");
    if (input) input.value = "";
    if (!preview) return;
    if (activityImageUrlDraft) {
        preview.innerHTML = `<img src="${mediaUrl(activityImageUrlDraft)}" alt="活动图片"><button type="button" class="btn-reject" style="margin-top:8px;" onclick="clearActivityImage()">删除图片</button>`;
    } else {
        preview.innerHTML = '<span class="field-hint" style="color:#999;">请上传 jpg/png/webp，不超过 5MB</span>';
    }
}

function clearActivityImage() {
    resetActivityImageDraft("");
}

async function onActivityImageSelected(input) {
    const file = input && input.files && input.files[0];
    if (!file) return;
    const okType = ["image/jpeg", "image/png", "image/webp"].includes(file.type)
        || /\.(jpe?g|png|webp)$/i.test(file.name);
    if (!okType) {
        alert("仅支持 jpg、png、webp 格式");
        input.value = "";
        return;
    }
    if (file.size > ACTIVITY_IMAGE_MAX_BYTES) {
        alert("图片不能超过 5MB");
        input.value = "";
        return;
    }
    try {
        const data = await apiUpload("/api/v1/forum/upload", file);
        resetActivityImageDraft(data && data.url ? data.url : "");
    } catch (e) {
        alert(e.message);
        input.value = "";
    }
}

function validateActivityImageRequired() {
    if (!activityImageUrlDraft) return "请上传活动图片";
    return "";
}

function statusText(status) {
    const activity = activityStatusText(status);
    if (activity !== status && activity !== "-") return activity;
    return {
        PENDING: "待审核",
        APPROVED: "已通过",
        REJECTED: "已拒绝",
        ACTIVE: "正常",
        DISABLED: "禁用",
        VOLUNTEER: "志愿者",
        ADMIN: "管理员",
        ORGANIZER: "活动负责人"
    }[status] || status || "-";
}

function goActivityDetail(id) {
    if (!id) return;
    const base = location.pathname.includes("/activity/") ? "detail.html" : "activity/detail.html";
    location.href = `${base}?id=${encodeURIComponent(id)}`;
}

function updateHeaderUserInfo() {
    const greetingEl = document.querySelector(".user-info .greeting");
    const badgeEl = document.querySelector(".user-info .user-badge");
    const roleKey = inferPageRole();
    if (greetingEl) greetingEl.textContent = `${getTimeGreeting()}，`;
    if (badgeEl && roleKey) badgeEl.textContent = buildUserBadge(roleKey);
}

async function refreshUserRealName() {
    if (!token()) return;
    const role = currentRole();
    if (role === "VOLUNTEER") {
        try {
            const profile = await api("/api/v1/volunteer/profile");
            if (profile && profile.realName) {
                localStorage.setItem("realName", profile.realName);
            }
        } catch (error) {
            /* 保留已有缓存或回退到用户名 */
        }
    } else if (!localStorage.getItem("realName")) {
        localStorage.setItem("realName", currentUsername());
    }
}

function lightweightImage(seed, width = 600, height = 260) {
    return `https://picsum.photos/${width}/${height}?random=${seed}`;
}

function frontendBasePath() {
    const path = location.pathname.replace(/\\/g, "/");
    if (path.includes("/activity/")) return "../";
    return "";
}

function navHref(href) {
    if (!href || /^https?:\/\//i.test(href) || href.startsWith("/")) return href;
    const base = frontendBasePath();
    return href.startsWith(base) ? href : base + href;
}

function assetHref(path) {
    if (!path) return path;
    if (/^https?:\/\//i.test(path) || path.startsWith("/")) return path;
    return frontendBasePath() + String(path).replace(/^\.\//, "");
}

function activityImageUrl(title, index) {
    const base = frontendBasePath();
    if ((title || "").includes("敬老院")) return base + "images/index4.jpg";
    if ((title || "").includes("文明劝导")) return base + "images/index5.jpg";
    if ((title || "").includes("环保")) return base + "images/index6.png";
    return lightweightImage(Number(index) + 20, 400, 200);
}

function isNavHrefActive(href, page) {
    if (href === page) return true;
    if (page === "detail.html") {
        const role = currentRole();
        if (role === "VOLUNTEER" && href === "volunteer-activity.html") return true;
        if (role === "ORGANIZER" && href === "active-activity.html") return true;
        if (role === "ADMIN" && href === "admin-activity.html") return true;
    }
    return false;
}

const ROLE_NAV_CONFIG = {
    admin: {
        roleText: "管理员",
        subText: "后台管理运营平台",
        slogan: "规范管理 服务志愿",
        searchText: "全部数据",
        searchPlaceholder: "输入关键词查询",
        home: "admin.html",
        items: [
            { text: "首页", href: "admin.html" },
            { text: "平台介绍", href: "admin-about.html" },
            { text: "人员管理", href: "admin-volunteer.html" },
            { text: "活动管理", href: "admin-activity.html" },
            { text: "消息通知", href: "admin-message.html" },
            { text: "数据统计", href: "admin-statistics.html" },
            { text: "社区论坛", href: "admin-forum.html" },
            { text: "志愿之星", href: "admin-stars.html" },
            { text: "个人中心", href: "admin-profile.html" }
        ]
    },
    volunteer: {
        roleText: "志愿者",
        subText: "江苏高校志愿服务平台",
        slogan: "奉献 友爱 互助 进步",
        searchText: "志愿活动",
        searchPlaceholder: "请输入关键词...",
        home: "volunteer.html",
        items: [
            { text: "首页", href: "volunteer.html" },
            { text: "平台介绍", href: "volunteer-about.html" },
            { text: "志愿服务", children: [
                { text: "活动报名", href: "volunteer-activity.html" },
                { text: "我的活动", href: "my-activity.html" },
                { text: "志愿之星", href: "volunteer-stars.html" }
            ] },
            { text: "消息通知", href: "volunteer-message.html" },
            { text: "社区论坛", href: "volunteer-forum.html" },
            { text: "个人中心", href: "volunteer-profile.html" }
        ]
    },
    active: {
        roleText: "活动负责人",
        subText: "活动管理运营平台",
        slogan: "用心办好每一场志愿活动",
        searchText: "全部活动",
        searchPlaceholder: "输入关键词查询",
        home: "active.html",
        items: [
            { text: "首页", href: "active.html" },
            { text: "平台介绍", href: "active-about.html" },
            { text: "活动管理", children: [
                { text: "我负责的活动", href: "active-activity.html" },
                { text: "活动总结", href: "active-summary.html" }
            ] },
            { text: "社区论坛", href: "active-forum.html" },
            { text: "个人中心", href: "active-profile.html" }
        ]
    }
};

function currentPageName() {
    return location.pathname.split(/[\\/]/).pop() || "index.html";
}

function inferPageRole() {
    const page = currentPageName();
    const path = location.pathname.toLowerCase();
    if (page.startsWith("admin-") || page === "admin.html") return "admin";
    if (page.startsWith("volunteer-") || page === "volunteer.html" || page === "my-activity.html") return "volunteer";
    if (page.startsWith("active-") || page === "active.html") return "active";
    if (page === "admin-forum-detail.html") return "admin";
    if (page === "active-forum-detail.html") return "active";
    if (path.includes("/activity/detail")) {
        const role = currentRole();
        if (role === "ADMIN") return "admin";
        if (role === "ORGANIZER") return "active";
        if (role === "VOLUNTEER") return "volunteer";
    }
    return "";
}

function navItemHtml(item, page) {
    if (item.children) {
        const active = item.children.some(child => isNavHrefActive(child.href, page));
        const children = item.children.map(child => {
            const childActive = isNavHrefActive(child.href, page);
            return `<a class="${childActive ? "active" : ""}" href="${navHref(child.href)}">${child.text}</a>`;
        }).join("");
        return `<div class="nav-item has-dropdown ${active ? "active" : ""}"><span class="nav-title">${item.text}</span><div class="dropdown-menu">${children}</div></div>`;
    }
    const itemActive = isNavHrefActive(item.href, page);
    return `<div class="nav-item ${itemActive ? "active" : ""}"><a href="${navHref(item.href)}">${item.text}</a></div>`;
}

function ensureUnifiedNavStyles() {
    if (document.getElementById("unified-nav-style")) return;
    const style = document.createElement("style");
    style.id = "unified-nav-style";
    style.textContent = `
        body { padding-top: 160px !important; }
        a { text-decoration: none; color: inherit; }
        .top-header { position: fixed; top: 0; left: 0; width: 100%; background: #fff; padding: 15px 30px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; z-index: 999; }
        .header-left { display: flex; align-items: center; gap: 15px; }
        .header-left .avatar, .avatar { width: 50px; height: 50px; background: #e0e0e0; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #666; font-size: 20px; }
        .header-left h1 { font-size: 32px; color: #222; margin: 0; }
        .header-left .tag, .tag { background: #d92121; color: white; padding: 5px 10px; border-radius: 20px; font-size: 14px; }
        .header-right { display: flex; align-items: center; gap: 20px; }
        .user-info { font-size: 16px; }
        .role { color: #d92121; font-weight: bold; }
        .logout-btn { background: #d92121; color: white; border: none; padding: 8px 15px; border-radius: 20px; cursor: pointer; }
        .slogan { text-align: right; color: #d92121; font-size: 18px; margin-top: 5px; }
        .search-bar { display: flex; align-items: center; gap: 10px; }
        .search-select { border: 1px solid #d92121; border-radius: 20px 0 0 20px; padding: 8px 15px; color: #d92121; }
        .search-input { border: 1px solid #d92121; border-left: none; padding: 8px 15px; width: 250px; }
        .search-btn { background: #d92121; color: white; border: none; padding: 9px 15px; border-radius: 0 20px 20px 0; cursor: pointer; }
        .nav-bar { position: fixed; top: 90px; left: 0; width: 100%; background: #d92121; display: flex; justify-content: center; z-index: 999; }
        .nav-item { position: relative; color: white; cursor: pointer; font-size: 16px; padding: 0 !important; }
        .nav-item > a, .nav-title { display: block; padding: 15px 20px; color: inherit; text-decoration: none; }
        .nav-item.active, .nav-item:hover { background: white; color: #d92121; }
        .dropdown-menu { display: none; position: absolute; top: 100%; left: 0; min-width: 130px; background: #b91616; box-shadow: 0 6px 16px rgba(0,0,0,0.18); text-align: left; }
        .dropdown-menu a { display: block; padding: 12px 16px; color: white; white-space: nowrap; text-decoration: none; }
        .dropdown-menu a:hover, .dropdown-menu a.active { background: white; color: #d92121; }
        .nav-item:hover .dropdown-menu { display: block; }
    `;
    document.head.appendChild(style);
}

function buildUnifiedHeader(roleKey) {
    const config = ROLE_NAV_CONFIG[roleKey];
    const page = currentPageName();
    const navHtml = config.items.map(item => navItemHtml(item, page)).join("");
    return `
        <div class="top-header">
            <div class="header-left">
                <div class="avatar" style="background: url('${assetHref("images/sju-logo.jpeg")}') center/cover no-repeat; border-radius: 50%;"></div>
                <div>
                    <h1>三江学院志愿者服务系统</h1>
                    <p style="color:#666; margin: 0;">${config.subText}</p>
                </div>
            </div>
            <div class="header-right">
                <div>
                    <div class="user-info"><span class="greeting">${getTimeGreeting()}，</span><span class="tag user-badge">${buildUserBadge(roleKey)}</span> <button class="logout-btn" onclick="logout()">退出登录</button></div>
                    <div class="slogan">${config.slogan}</div>
                </div>
                <div class="search-bar">
                    <div class="search-select">${config.searchText}</div>
                    <input id="keyword" type="text" class="search-input" placeholder="${config.searchPlaceholder}">
                    <button class="search-btn">🔍</button>
                </div>
            </div>
        </div>
        <div class="nav-bar">${navHtml}</div>
    `;
}

function removeOldHeaders() {
    document.querySelectorAll(".top-header,.nav-bar,.top,.nav,.top-bar").forEach(el => el.remove());
}

function initUnifiedNavigation() {
    const roleKey = inferPageRole();
    if (!roleKey || !document.body) return;
    ensureUnifiedNavStyles();
    removeOldHeaders();
    document.body.insertAdjacentHTML("afterbegin", buildUnifiedHeader(roleKey));
}
document.addEventListener("DOMContentLoaded", async () => {
    initUnifiedNavigation();
   bindActiveNavFlip();
    ensureSiteFooter();
    await refreshUserRealName();
   updateHeaderUserInfo();
});