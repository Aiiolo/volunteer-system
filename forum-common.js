/**
 * 社区论坛统一逻辑（志愿者 / 活动负责人 / 管理员）
 */
const FORUM_MAX_IMAGES = 8;
const FORUM_MAX_FILE_BYTES = 5 * 1024 * 1024;

const FORUM_MANAGE_FILTERS = {
    organizer: [
        { key: "my-published", label: "我发布的帖子" },
        { key: "my-deleted", label: "我删除的帖子" },
        { key: "my-hidden", label: "我隐藏的帖子" }
    ],
    admin: [
        { key: "all-posts", label: "所有帖子" },
        { key: "all-notices", label: "所有通知" },
        { key: "all-hidden", label: "已隐藏帖子" },
        { key: "all-deleted", label: "已删除帖子" }
    ]
};

const FORUM_EMPTY_MESSAGES = {
    feed: "暂无帖子，快来发布第一条吧",
    "my-published": "您还没有发布过帖子或通知",
    "my-deleted": "回收站为空",
    "my-hidden": "暂无已隐藏的帖子",
    "all-posts": "暂无普通帖子",
    "all-notices": "暂无活动通知",
    "all-hidden": "暂无已隐藏帖子",
    "all-deleted": "暂无已删除帖子"
};

let forumListFilter = "feed";
let forumModalImages = [];
let forumEditingId = null;
let forumModalMode = "post";

function forumRoleKey() {
    const map = { VOLUNTEER: "volunteer", ADMIN: "admin", ORGANIZER: "active" };
    return map[currentRole()] || inferPageRole() || "volunteer";
}

function forumListPage() {
    const pages = { volunteer: "volunteer-forum.html", admin: "admin-forum.html", active: "active-forum.html" };
    return pages[forumRoleKey()] || "volunteer-forum.html";
}

function forumDetailPage() {
    return forumRoleKey() === "volunteer" ? "volunteer-forum-detail.html" : forumListPage().replace(".html", "-detail.html");
}

function forumCanNotify() {
    const role = currentRole();
    return role === "ADMIN" || role === "ORGANIZER";
}

function forumIsOrganizer() {
    return currentRole() === "ORGANIZER";
}

function forumCanManageDropdown() {
    const role = currentRole();
    return role === "ADMIN" || role === "ORGANIZER";
}

function forumIsAdmin() {
    return currentRole() === "ADMIN";
}

function forumManageFilterOptions() {
    return forumIsAdmin() ? FORUM_MANAGE_FILTERS.admin : FORUM_MANAGE_FILTERS.organizer;
}

function forumFormatDate(t) {
    if (!t) return "-";
    return String(t).replace("T", " ").substring(0, 10);
}

function forumEscapeHtml(s) {
    const d = document.createElement("div");
    d.textContent = s || "";
    return d.innerHTML;
}

function forumActivityLink(activityId, title, evt) {
    if (evt) evt.stopPropagation();
    if (!activityId) return;
    goActivityDetail(activityId);
}

function forumIsManageMode() {
    return forumListFilter !== "feed";
}

function forumInitManageDropdown() {
    const wrap = document.getElementById("forumManageDrop");
    if (!wrap || !forumCanManageDropdown()) return;

    const menu = document.getElementById("forumManageMenu");
    const btn = document.getElementById("forumManageBtn");
    menu.innerHTML = forumManageFilterOptions().map(opt =>
        `<button type="button" data-filter="${opt.key}">${opt.label}</button>`
    ).join("");

    btn.addEventListener("click", (e) => {
        e.stopPropagation();
        wrap.classList.toggle("open");
    });

    menu.querySelectorAll("button").forEach(item => {
        item.addEventListener("click", () => {
            forumSetFilter(item.dataset.filter, item.textContent.trim());
            wrap.classList.remove("open");
        });
    });

    document.addEventListener("click", (e) => {
        if (!wrap.contains(e.target)) wrap.classList.remove("open");
    });
}

function forumSetFilter(filter, label) {
    forumListFilter = filter;
    const hint = document.getElementById("forumFilterHint");
    const bar = document.getElementById("forumFilterBar");
    if (hint && bar) {
        hint.textContent = label ? `当前筛选：${label}` : "";
        bar.classList.toggle("show", !!label);
    }
    menuHighlight(filter);
    forumRenderPosts();
}

function menuHighlight(filter) {
    const menu = document.getElementById("forumManageMenu");
    if (!menu) return;
    menu.querySelectorAll("button").forEach(btn => {
        btn.classList.toggle("active", btn.dataset.filter === filter);
    });
}

function forumResetToFeed() {
    forumListFilter = "feed";
    const hint = document.getElementById("forumFilterHint");
    const bar = document.getElementById("forumFilterBar");
    if (hint && bar) {
        hint.textContent = "";
        bar.classList.remove("show");
    }
    menuHighlight("");
}

function forumOpenPostModal(editPost) {
    forumModalMode = "post";
    forumEditingId = editPost ? editPost.id : null;
    forumModalImages = editPost && editPost.imageUrls ? [...editPost.imageUrls] : [];
    const modal = document.getElementById("forumComposeModal");
    const titleEl = document.getElementById("forumModalTitle");
    const activityRow = document.getElementById("forumActivityRow");
    const activitySelect = document.getElementById("forumActivityId");
    const hint = document.getElementById("forumActivityHint");
    document.getElementById("forumPostTitle").value = editPost ? editPost.title : "";
    document.getElementById("forumPostContent").value = editPost ? editPost.content : "";
    titleEl.textContent = editPost ? "编辑帖子" : "发布帖子";
    document.getElementById("forumSubmitBtn").textContent = editPost ? "保存" : "发布";
    activityRow.style.display = "";
    activitySelect.innerHTML = '<option value="">不关联活动</option>';
    activitySelect.disabled = false;
    activitySelect.required = false;
    const role = currentRole();
    if (role === "VOLUNTEER") hint.textContent = "仅可选择您本人已参加（已签到/已签退）的活动，也可选择「不关联活动」";
    else if (role === "ORGANIZER") hint.textContent = "可选择您负责的所有已发布/已完结活动";
    else hint.textContent = "可选择系统内所有已发布/已完结活动";
    forumLoadActivities("post", editPost ? editPost.activityId : null);
    forumRenderModalImages();
    modal.classList.add("show");
}

function forumOpenNoticeModal(editPost) {
    if (!forumCanNotify()) return;
    forumModalMode = "notice";
    forumEditingId = editPost ? editPost.id : null;
    forumModalImages = editPost && editPost.imageUrls ? [...editPost.imageUrls] : [];
    const modal = document.getElementById("forumComposeModal");
    document.getElementById("forumModalTitle").textContent = editPost ? "编辑活动通知" : "发布活动通知";
    document.getElementById("forumSubmitBtn").textContent = editPost ? "保存" : "发布";
    document.getElementById("forumPostTitle").value = editPost ? editPost.title : "";
    document.getElementById("forumPostContent").value = editPost ? editPost.content : "";
    const activitySelect = document.getElementById("forumActivityId");
    const hint = document.getElementById("forumActivityHint");
    activitySelect.innerHTML = '<option value="">请选择活动</option>';
    activitySelect.required = true;
    activitySelect.disabled = false;
    hint.textContent = forumIsAdmin() ? "请选择要通知的活动" : "必须关联您负责的活动";
    forumLoadActivities("notice", editPost ? editPost.activityId : null);
    forumRenderModalImages();
    modal.classList.add("show");
}

function forumCloseModal() {
    document.getElementById("forumComposeModal").classList.remove("show");
    forumEditingId = null;
    forumModalImages = [];
}

async function forumLoadActivities(purpose, selectedId) {
    const select = document.getElementById("forumActivityId");
    const hint = document.getElementById("forumActivityHint");
    const noneOption = purpose === "notice"
        ? '<option value="">请选择关联活动</option>'
        : '<option value="">不关联活动</option>';
    select.disabled = false;
    try {
        const list = await api(`/api/v1/forum/linkable-activities?purpose=${purpose}`) || [];
        if (!list.length && purpose === "notice") {
            select.innerHTML = '<option value="">暂无可选活动</option>';
            select.disabled = true;
            hint.textContent = "您暂无负责的活动，无法发布通知";
            return;
        }
        if (!list.length && purpose === "post") {
            select.innerHTML = noneOption;
            select.disabled = false;
            hint.textContent = currentRole() === "VOLUNTEER"
                ? "暂无可关联的活动，您仍可发布不关联活动的帖子"
                : "暂无可关联的活动";
            return;
        }
        select.innerHTML = noneOption + list.map(a => {
            const id = forumActivityOptionId(a);
            const title = forumActivityOptionTitle(a);
            return `<option value="${id}"${selectedId && Number(selectedId) === Number(id) ? " selected" : ""}>${forumEscapeHtml(title)}</option>`;
        }).join("");
        hint.textContent = purpose === "notice"
            ? (forumIsAdmin() ? "请选择要通知的活动" : "必须关联您负责的活动")
            : (currentRole() === "VOLUNTEER"
                ? "可选择已参加的活动，也可不关联"
                : "可选择关联活动，也可不关联");
    } catch (e) {
        select.innerHTML = noneOption;
        select.disabled = false;
        hint.textContent = "暂无可关联的活动";
    }
}

function forumRenderModalImages() {
    const grid = document.getElementById("forumImgGrid");
    const uploadLabel = document.getElementById("forumUploadLabel");
    grid.querySelectorAll(".img-item").forEach(el => el.remove());
    forumModalImages.forEach((url, idx) => {
        const div = document.createElement("div");
        div.className = "img-item";
        div.innerHTML = `<img src="${mediaUrl(url)}" alt=""><button type="button" onclick="forumRemoveImage(${idx})">×</button>`;
        grid.insertBefore(div, uploadLabel);
    });
    uploadLabel.style.display = forumModalImages.length >= FORUM_MAX_IMAGES ? "none" : "inline-flex";
}

function forumRemoveImage(idx) {
    forumModalImages.splice(idx, 1);
    forumRenderModalImages();
}

async function forumHandleFileSelect(e) {
    const files = Array.from(e.target.files || []);
    e.target.value = "";
    for (const file of files) {
        if (forumModalImages.length >= FORUM_MAX_IMAGES) {
            alert("最多上传8张图片");
            break;
        }
        if (file.size > FORUM_MAX_FILE_BYTES) {
            alert(`「${file.name}」超过 5MB，请压缩后重试`);
            continue;
        }
        try {
            const data = await apiUpload("/api/v1/forum/upload", file);
            if (data && data.url) forumModalImages.push(data.url);
        } catch (err) {
            alert(err.message || "上传失败");
            break;
        }
    }
    forumRenderModalImages();
}

async function forumSubmitModal(e) {
    e.preventDefault();
    const title = document.getElementById("forumPostTitle").value.trim();
    const content = document.getElementById("forumPostContent").value.trim();
    const activityVal = document.getElementById("forumActivityId").value;
    if (!title || !content) {
        alert("请填写标题和内容");
        return;
    }
    const body = { title, content, imageUrls: forumModalImages };
    if (activityVal) body.activityId = Number(activityVal);
    if (forumModalMode === "notice" && !activityVal) {
        alert("活动通知必须关联活动");
        return;
    }
    const btn = document.getElementById("forumSubmitBtn");
    btn.disabled = true;
    try {
        if (forumEditingId) {
            await api(`/api/v1/forum/posts/${forumEditingId}`, { method: "PUT", body: JSON.stringify(body) });
        } else if (forumModalMode === "notice") {
            await api("/api/v1/forum/notices", { method: "POST", body: JSON.stringify(body) });
        } else {
            await api("/api/v1/forum/posts", { method: "POST", body: JSON.stringify(body) });
        }
        forumCloseModal();
        if (document.getElementById("postList")) {
            await forumRenderPosts();
        }
        if (document.getElementById("forumDetail") && forumDetailId()) {
            await forumLoadDetail();
        }
        alert(forumEditingId ? "保存成功" : "发布成功");
    } catch (err) {
        alert(err.message || "操作失败");
    } finally {
        btn.disabled = false;
    }
}

function forumPostBadges(p) {
    let html = "";
    if (p.postType === "NOTICE") html += '<span class="forum-badge notice">通知</span>';
    if (p.deleted) html += '<span class="forum-badge deleted">已删除</span>';
    if (p.hidden && !p.deleted) html += '<span class="forum-badge hidden">已隐藏</span>';
    if (p.topped && !p.deleted) html += '<span class="forum-badge top">置顶</span>';
    return html;
}

function forumManageBar(p) {
    if (!forumIsManageMode()) return "";
    const isDeletedView = forumListFilter === "my-deleted" || forumListFilter === "all-deleted" || p.deleted;
    const hasActions = isDeletedView || p.deleted
        ? (p.canDelete || forumIsAdmin())
        : (p.canEdit || p.canHide || p.canDelete || forumIsAdmin());
    if (!hasActions) return "";

    let html = `<div class="post-manage" onclick="event.stopPropagation()">`;
    if (isDeletedView || p.deleted) {
        if (p.canDelete || forumIsAdmin()) {
            html += `<button type="button" class="pill-btn" onclick="forumRestorePost(${p.id})">恢复</button>`;
        }
        if (forumIsAdmin()) {
            html += `<button type="button" class="pill-btn danger" onclick="forumPurgePost(${p.id})">永久删除</button>`;
        }
    } else {
        if (forumIsAdmin()) {
            const topLabel = p.topped ? "取消置顶" : "置顶";
            html += `<button type="button" class="pill-btn" onclick="forumToggleTop(${p.id}, ${!p.topped})">${topLabel}</button>`;
        }
        if (p.canEdit) {
            html += `<button type="button" class="pill-btn" onclick="forumEditPost(${p.id})">编辑</button>`;
        }
        if (p.canHide) {
            const hideLabel = p.hidden ? "恢复显示" : "隐藏";
            html += `<button type="button" class="pill-btn" onclick="forumToggleHidden(${p.id}, ${!p.hidden})">${hideLabel}</button>`;
        }
        if (p.canDelete) {
            html += `<button type="button" class="pill-btn danger" onclick="forumDeletePost(${p.id})">删除</button>`;
        }
    }
    html += `</div>`;
    return html;
}

async function forumRenderPosts() {
    const el = document.getElementById("postList");
    if (!el) return;
    try {
        const url = forumIsManageMode()
            ? `/api/v1/forum/posts/manage?filter=${encodeURIComponent(forumListFilter)}`
            : "/api/v1/forum/posts";
        const list = await api(url) || [];
        if (!list.length) {
            el.innerHTML = `<div class="empty">${FORUM_EMPTY_MESSAGES[forumListFilter] || "暂无数据"}</div>`;
            return;
        }
        const detailPage = forumDetailPage();
        const manageMode = forumIsManageMode();
        el.innerHTML = list.map(p => `
            <article class="post-item ${p.deleted ? "is-deleted" : ""} ${p.hidden && !p.deleted ? "is-hidden" : ""} ${p.topped && !p.deleted ? "is-top" : ""}" data-id="${p.id}">
                <div class="post-item-main" ${manageMode && p.deleted ? "" : `onclick="location.href='${detailPage}?id=${p.id}'"`}>
                    <div class="post-title-row">
                        <div class="post-title">${forumEscapeHtml(p.title)}</div>
                        <div class="post-badges">${forumPostBadges(p)}</div>
                    </div>
                    <div class="post-meta">
                        <span>发布者：${forumEscapeHtml(p.authorName)}</span>
                        <span>发布时间：${forumFormatDate(p.createTime)}</span>
                        <span>浏览：${p.viewCount || 0}</span>
                        ${p.activityTitle ? `<span class="activity-link" onclick="forumActivityLink(${p.activityId}, '', event)">关联活动：${forumEscapeHtml(p.activityTitle)}</span>` : ""}
                    </div>
                    <div class="post-preview">${forumEscapeHtml(p.preview || "")}</div>
                    <div class="post-actions">
                        <span class="pill">点赞 ${p.likeCount || 0}</span>
                        <span class="pill">评论 ${p.commentCount || 0}</span>
                        ${!manageMode || p.deleted ? "" : `<span class="pill link" onclick="event.stopPropagation();location.href='${detailPage}?id=${p.id}'">查看详情</span>`}
                    </div>
                </div>
                ${forumManageBar(p)}
            </article>
        `).join("");
    } catch (e) {
        el.innerHTML = `<div class="empty">${forumEscapeHtml(e.message)}</div>`;
    }
}

async function forumToggleTop(id, topped) {
    const msg = topped ? "确认置顶该帖子？置顶后将显示在列表最前。" : "确认取消置顶？";
    if (!confirm(msg)) return;
    try {
        await api(`/api/v1/forum/posts/${id}/manage`, { method: "PUT", body: JSON.stringify({ topped }) });
        await forumRenderPosts();
    } catch (e) {
        alert(e.message);
    }
}

async function forumToggleHidden(id, hidden) {
    const msg = hidden
        ? "确认隐藏该帖子？隐藏后仅管理员与发布者可见。"
        : "确认恢复显示该帖子？恢复后将重新对所有人可见。";
    if (!confirm(msg)) return;
    try {
        await api(`/api/v1/forum/posts/${id}/manage`, { method: "PUT", body: JSON.stringify({ hidden }) });
        await forumRenderPosts();
    } catch (e) {
        alert(e.message);
    }
}

async function forumDeletePost(id) {
    if (!confirm("确认删除该帖子？删除后将移入回收站，可在「已删除」中恢复。")) return;
    try {
        await api(`/api/v1/forum/posts/${id}`, { method: "DELETE" });
        await forumRenderPosts();
    } catch (e) {
        alert(e.message);
    }
}

async function forumRestorePost(id) {
    if (!confirm("确认恢复该帖子？恢复后将重新出现在论坛列表中。")) return;
    try {
        await api(`/api/v1/forum/posts/${id}/restore`, { method: "POST", body: "{}" });
        await forumRenderPosts();
    } catch (e) {
        alert(e.message);
    }
}

async function forumPurgePost(id) {
    if (!confirm("确认永久删除？此操作不可恢复，帖子及相关评论将彻底删除。")) return;
    try {
        await api(`/api/v1/forum/posts/${id}/permanent`, { method: "DELETE" });
        await forumRenderPosts();
    } catch (e) {
        alert(e.message);
    }
}

async function forumEditPost(id) {
    try {
        const p = await api(`/api/v1/forum/posts/${id}`);
        if (p.deleted) {
            alert("已删除的帖子请先恢复后再编辑");
            return;
        }
        if (p.postType === "NOTICE") forumOpenNoticeModal(p);
        else forumOpenPostModal(p);
    } catch (e) {
        alert(e.message);
    }
}

function forumInitComposeModal() {
    const form = document.getElementById("forumComposeForm");
    if (!form || form.dataset.bound === "1") return;
    form.dataset.bound = "1";
    form.addEventListener("submit", forumSubmitModal);
    document.getElementById("forumFileInput").addEventListener("change", forumHandleFileSelect);
    document.getElementById("forumUploadLabel").addEventListener("click", () => {
        if (forumModalImages.length >= FORUM_MAX_IMAGES) {
            alert("最多上传8张图片");
            return;
        }
        document.getElementById("forumFileInput").click();
    });
    document.getElementById("forumComposeModal").addEventListener("click", (e) => {
        if (e.target.id === "forumComposeModal") forumCloseModal();
    });
}

function forumInitPage() {
    const role = currentRole();
    if (!role || !token()) {
        requireLogin();
        return;
    }
    const notifyBtn = document.getElementById("forumNoticeBtn");
    if (notifyBtn) notifyBtn.style.display = forumCanNotify() ? "" : "none";

    const manageDrop = document.getElementById("forumManageDrop");
    if (manageDrop) manageDrop.style.display = forumCanManageDropdown() ? "" : "none";

    forumInitManageDropdown();
    forumInitComposeModal();

    const backFeed = document.getElementById("forumBackFeed");
    if (backFeed) {
        backFeed.addEventListener("click", () => {
            forumResetToFeed();
            forumRenderPosts();
        });
    }

    const params = new URLSearchParams(location.search);
    if (params.get("action") === "post") forumOpenPostModal();
    if (params.get("action") === "notice") forumOpenNoticeModal();

    forumRenderPosts();
}

/* ---------- 详情页 ---------- */
function forumDetailId() {
    return Number(new URLSearchParams(location.search).get("id") || 0);
}

function forumFormatDateTime(t) {
    if (!t) return "-";
    return String(t).replace("T", " ").substring(0, 16);
}

function forumDetailBadges(p) {
    let html = "";
    if (p.postType === "NOTICE") html += '<span class="forum-badge notice">通知</span>';
    if (p.deleted) html += '<span class="forum-badge deleted">已删除</span>';
    if (p.hidden && !p.deleted) html += '<span class="forum-badge hidden">已隐藏</span>';
    if (p.topped && !p.deleted) html += '<span class="forum-badge top">置顶</span>';
    return html;
}

function forumDetailManageHtml(p) {
    const buttons = [];
    const isVolunteer = currentRole() === "VOLUNTEER";
    if (p.canEdit) {
        buttons.push(`<button type="button" class="pill-btn" onclick="forumDetailEdit()">编辑</button>`);
    }
    if (isVolunteer) {
        if (!buttons.length) return "";
        return `<div class="detail-manage">${buttons.join("")}</div>`;
    }
    if (p.deleted) {
        if (p.canDelete || forumIsAdmin()) {
            buttons.push(`<button type="button" class="pill-btn" onclick="forumDetailRestore()">恢复</button>`);
        }
        if (forumIsAdmin()) {
            buttons.push(`<button type="button" class="pill-btn danger" onclick="forumDetailPurge()">永久删除</button>`);
        }
    } else {
        if (forumIsAdmin()) {
            const topLabel = p.topped ? "取消置顶" : "置顶";
            buttons.push(`<button type="button" class="pill-btn" onclick="forumDetailToggleTop(${!p.topped})">${topLabel}</button>`);
        }
        if (p.canHide) {
            const hideLabel = p.hidden ? "恢复显示" : "隐藏";
            buttons.push(`<button type="button" class="pill-btn" onclick="forumDetailToggleHidden(${!p.hidden})">${hideLabel}</button>`);
        }
        if (p.canDelete) {
            buttons.push(`<button type="button" class="pill-btn danger" onclick="forumDetailDelete()">删除</button>`);
        }
    }
    if (!buttons.length) return "";
    return `<div class="detail-manage">${buttons.join("")}</div>`;
}

function forumRenderDetail(p) {
    const wrap = document.getElementById("forumDetail");
    const images = (p.imageUrls || []).map(url =>
        `<img src="${mediaUrl(url)}" alt="" onclick="window.open(this.src)">`
    ).join("");
    const activityHtml = p.activityId
        ? `<span class="activity-link" onclick="goActivityDetail(${p.activityId})">关联活动：${forumEscapeHtml(p.activityTitle)}</span>`
        : "";
    const comments = (p.comments || []).length
        ? p.comments.map(c => `<div class="comment-item"><div class="comment-head"><span class="comment-user">${forumEscapeHtml(c.authorName)}</span><span>${forumFormatDateTime(c.createTime)}</span></div><div>${forumEscapeHtml(c.content)}</div></div>`).join("")
        : '<div class="empty">暂无评论，快来发表第一条评论吧</div>';

    wrap.innerHTML = `
        <span class="back" onclick="location.href='${forumListPage()}'">← 返回论坛</span>
        <div class="detail-title-row">
            <h1 class="post-title">${forumEscapeHtml(p.title)}</h1>
            <div class="post-badges">${forumDetailBadges(p)}</div>
        </div>
        <div class="meta">
            <span>发布者：${forumEscapeHtml(p.authorName)}</span>
            <span>发布时间：${forumFormatDateTime(p.createTime)}</span>
            <span>浏览：${p.viewCount || 0}</span>
            <span>评论：${(p.comments || []).length}</span>
            ${activityHtml}
        </div>
        <article class="content">${forumEscapeHtml(p.content)}</article>
        ${images ? `<div class="img-gallery">${images}</div>` : ""}
        ${forumDetailManageHtml(p)}
        <div class="action-row">
            <button class="like-btn ${p.liked ? "liked" : ""}" onclick="forumDetailToggleLike()" ${p.deleted || p.hidden ? "disabled" : ""}>${p.liked ? "已点赞" : "点赞"}</button>
            <span class="count">${p.likeCount || 0} 人觉得有用</span>
        </div>
        <section class="comment-box">
            <h3>评论交流</h3>
            ${p.deleted || p.hidden ? '<div class="empty">当前帖子不可评论</div>' : `
            <div class="comment-input">
                <textarea id="forumCommentText" placeholder="写下你的评论..."></textarea>
                <button type="button" onclick="forumDetailAddComment()">发表评论</button>
            </div>`}
            <div id="forumComments">${comments}</div>
        </section>
    `;
}

async function forumLoadDetail() {
    const wrap = document.getElementById("forumDetail");
    const id = forumDetailId();
    if (!id) {
        wrap.innerHTML = '<div class="not-found">帖子不存在</div>';
        return;
    }
    try {
        const p = await api(`/api/v1/forum/posts/${id}`);
        forumRenderDetail(p);
    } catch (e) {
        wrap.innerHTML = `<div class="not-found">${forumEscapeHtml(e.message)}</div>`;
    }
}

async function forumDetailToggleLike() {
    try {
        const p = await api(`/api/v1/forum/posts/${forumDetailId()}/like`, { method: "POST", body: "{}" });
        forumRenderDetail(p);
    } catch (e) {
        alert(e.message);
    }
}

async function forumDetailAddComment() {
    const text = document.getElementById("forumCommentText").value.trim();
    if (!text) {
        alert("请输入评论内容");
        return;
    }
    try {
        const p = await api(`/api/v1/forum/posts/${forumDetailId()}/comments`, {
            method: "POST",
            body: JSON.stringify({ content: text })
        });
        forumRenderDetail(p);
    } catch (e) {
        alert(e.message);
    }
}

async function forumDetailToggleTop(topped) {
    const msg = topped ? "确认置顶该帖子？" : "确认取消置顶？";
    if (!confirm(msg)) return;
    try {
        await api(`/api/v1/forum/posts/${forumDetailId()}/manage`, { method: "PUT", body: JSON.stringify({ topped }) });
        await forumLoadDetail();
    } catch (e) {
        alert(e.message);
    }
}

async function forumDetailToggleHidden(hidden) {
    const msg = hidden ? "确认隐藏该帖子？" : "确认恢复显示该帖子？";
    if (!confirm(msg)) return;
    try {
        await api(`/api/v1/forum/posts/${forumDetailId()}/manage`, { method: "PUT", body: JSON.stringify({ hidden }) });
        await forumLoadDetail();
    } catch (e) {
        alert(e.message);
    }
}

async function forumDetailDelete() {
    if (!confirm("确认删除该帖子？删除后将移入回收站。")) return;
    try {
        await api(`/api/v1/forum/posts/${forumDetailId()}`, { method: "DELETE" });
        location.href = forumListPage();
    } catch (e) {
        alert(e.message);
    }
}

async function forumDetailRestore() {
    if (!confirm("确认恢复该帖子？")) return;
    try {
        await api(`/api/v1/forum/posts/${forumDetailId()}/restore`, { method: "POST", body: "{}" });
        await forumLoadDetail();
    } catch (e) {
        alert(e.message);
    }
}

async function forumDetailPurge() {
    if (!confirm("确认永久删除？此操作不可恢复。")) return;
    try {
        await api(`/api/v1/forum/posts/${forumDetailId()}/permanent`, { method: "DELETE" });
        location.href = forumListPage();
    } catch (e) {
        alert(e.message);
    }
}

async function forumDetailEdit() {
    try {
        const p = await api(`/api/v1/forum/posts/${forumDetailId()}`);
        if (p.deleted) {
            alert("已删除的帖子请先恢复后再编辑");
            return;
        }
        if (!p.canEdit) {
            alert("无权编辑该帖子");
            return;
        }
        if (p.postType === "NOTICE") forumOpenNoticeModal(p);
        else forumOpenPostModal(p);
    } catch (e) {
        alert(e.message);
    }
}

function forumInitDetailPage() {
    if (!token()) {
        requireLogin();
        return;
    }
    forumInitComposeModal();
    forumLoadDetail();
}
