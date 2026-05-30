/** 首页轮播：第1项视频 + 最新2个活动封面 */
const HOME_CAROUSEL_VIDEO = "images/index%20video.mp4";

function ensureHomeCarouselStyles() {
    if (document.getElementById("home-carousel-style")) return;
    const style = document.createElement("style");
    style.id = "home-carousel-style";
    style.textContent = `
        .banner-container {
            width: 100%;
            height: clamp(440px, 50vh, 520px);
            position: relative;
            overflow: hidden;
            flex-shrink: 0;
        }
        .banner {
            position: relative;
            width: 100%;
            height: 100%;
            background: #222;
        }
        .banner .banner-slide {
            position: absolute;
            inset: 0;
            width: 100%;
            height: 100%;
            object-fit: cover;
            opacity: 0;
            transition: opacity 0.8s ease;
            border: 0;
        }
        .banner .banner-slide.active {
            opacity: 1;
            z-index: 1;
        }
        .banner-indicators {
            position: absolute;
            bottom: 24px;
            left: 50%;
            transform: translateX(-50%);
            display: flex;
            gap: 12px;
            z-index: 2;
        }
        .banner-indicators .indicator {
            width: 14px;
            height: 14px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.45);
            cursor: pointer;
            transition: background 0.3s;
        }
        .banner-indicators .indicator.active {
            background: #fff;
        }
    `;
    document.head.appendChild(style);
}

const _homeCarousel = { index: 0, total: 3, timer: null };

function homeCarouselChange(index) {
    const slides = document.querySelectorAll("#homeBanner .banner-slide");
    const indicators = document.querySelectorAll("#homeBannerIndicators .indicator");
    if (!slides.length) return;
    _homeCarousel.index = index;
    slides.forEach((slide, i) => {
        const active = i === index;
        slide.classList.toggle("active", active);
        if (slide.tagName === "VIDEO") {
            if (active) {
                slide.play().catch(() => {});
            } else {
                slide.pause();
                slide.currentTime = 0;
            }
        }
    });
    indicators.forEach((ind, i) => ind.classList.toggle("active", i === index));
}

function homeCarouselNext() {
    homeCarouselChange((_homeCarousel.index + 1) % _homeCarousel.total);
}

async function fetchBannerCovers() {
    try {
        const res = await fetch(`${API_BASE}/api/v1/activities/latest`);
        const json = await res.json();
        const list = (json && json.data) || [];
        return list.slice(0, 2).map((activity, i) => ({
            src: activity.imageUrl ? mediaUrl(activity.imageUrl) : lightweightImage(activity.id || i + 1, 1600, 520),
            alt: activity.title || `最新活动${i + 1}`
        }));
    } catch (error) {
        return [
            { src: lightweightImage(21, 1600, 520), alt: "志愿活动" },
            { src: lightweightImage(22, 1600, 520), alt: "志愿活动" }
        ];
    }
}

async function initHomeCarousel() {
    const banner = document.getElementById("homeBanner");
    if (!banner) return;
    ensureHomeCarouselStyles();

    const covers = await fetchBannerCovers();
    while (covers.length < 2) {
        covers.push({ src: lightweightImage(20 + covers.length, 1600, 520), alt: "志愿活动" });
    }

    banner.innerHTML = [
        `<video class="banner-slide active" autoplay muted loop playsinline disablePictureInPicture controlsList="nodownload nofullscreen noremoteplayback"><source src="${HOME_CAROUSEL_VIDEO}" type="video/mp4"></video>`,
        `<img class="banner-slide" src="${covers[0].src}" alt="${covers[0].alt}">`,
        `<img class="banner-slide" src="${covers[1].src}" alt="${covers[1].alt}">`
    ].join("");

    const indicators = document.getElementById("homeBannerIndicators");
    if (indicators) {
        indicators.innerHTML = [0, 1, 2].map((i) =>
            `<div class="indicator${i === 0 ? " active" : ""}" data-index="${i}"></div>`
        ).join("");
        indicators.querySelectorAll(".indicator").forEach((el) => {
            el.addEventListener("click", () => homeCarouselChange(Number(el.dataset.index)));
        });
    }

    if (_homeCarousel.timer) clearInterval(_homeCarousel.timer);
    _homeCarousel.timer = setInterval(homeCarouselNext, 5000);

    const video = banner.querySelector("video");
    if (video) video.play().catch(() => {});
}

document.addEventListener("DOMContentLoaded", initHomeCarousel);
