/* =========================================================
   書店購物網站共用 JavaScript（需要先載入 jQuery）
   放在 src/main/resources/static/js/shop-common.js
   提供：登入 token 處理、需要登入的 AJAX、頁首與手機底部導覽列、
        購物車數量、提示訊息、數量加減器、藍新付款表單送出
   ========================================================= */
(function (window, $) {
    "use strict";

    const Shop = {};

    // ---------- token ----------
    Shop.getAccessToken = function () { return localStorage.getItem("accessToken"); };
    Shop.getRefreshToken = function () { return localStorage.getItem("refreshToken"); };

    Shop.clearTokens = function () {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
    };

    // 後端若在 header 帶回新的 token，就存回 localStorage。
    Shop.saveNewTokens = function (xhr) {
        const newAccessToken = xhr.getResponseHeader("X-New-Access-Token");
        const newRefreshToken = xhr.getResponseHeader("X-New-Refresh-Token");
        if (newAccessToken) { localStorage.setItem("accessToken", newAccessToken); }
        if (newRefreshToken) { localStorage.setItem("refreshToken", newRefreshToken); }
    };

    // 沒有 token 就導回登入頁。回傳 true 代表已登入。
    Shop.requireLogin = function () {
        if (!Shop.getAccessToken() || !Shop.getRefreshToken()) {
            window.location.href = "/page/login";
            return false;
        }
        return true;
    };

    // 統一處理需要登入的 AJAX 請求，自動補上 access token 和 refresh token。
    // 沒有 token 時回傳 null（並已導向登入頁）。
    Shop.apiAjax = function (options) {
        if (!Shop.requireLogin()) {
            return null;
        }

        const ajaxOptions = $.extend({}, options);
        ajaxOptions.headers = $.extend({}, options.headers || {}, {
            Authorization: "Bearer " + Shop.getAccessToken(),
            "X-Refresh-Token": Shop.getRefreshToken()
        });

        return $.ajax(ajaxOptions);
    };

    // 401 代表登入失效，導回登入頁並回傳 true。
    Shop.handleAuthError = function (xhr) {
        if (xhr.status === 401) {
            window.location.href = "/page/login";
            return true;
        }
        return false;
    };

    // 從後端錯誤回應取出要顯示給使用者的訊息。
    Shop.getErrorMessage = function (xhr, fallback) {
        const data = xhr.responseJSON || {};
        if (data.errors && typeof data.errors === "object") {
            const first = Object.values(data.errors)[0];
            if (first) {
                return first;
            }
        }
        return data.message || fallback;
    };

    // ---------- 格式化 ----------
    Shop.formatMoney = function (value) {
        return "NT$ " + Number(value).toLocaleString();
    };

    // 後端時間格式是 2026-10-01T19:49:55.123，顯示成 2026-10-01 19:49
    Shop.formatTime = function (value) {
        return value ? String(value).replace("T", " ").slice(0, 16) : "-";
    };

    Shop.coverUrl = function (bookId) {
        return "/images/books/book-" + encodeURIComponent(bookId) + ".svg";
    };

    // 封面圖片載入失敗（例如新書還沒有圖片）時，改顯示灰底 No Image。
    Shop.bindImageFallback = function (img, wrapper) {
        img.on("error", function () {
            $(this).css("visibility", "hidden");
            if (wrapper) { wrapper.addClass("no-image"); }
        });
        return img;
    };

    // ---------- 提示訊息 ----------
    Shop.toast = function (message, isError) {
        $(".toast").remove();
        const element = $("<div>", {
            class: "toast" + (isError ? " error" : ""),
            role: isError ? "alert" : "status",
            text: message
        }).appendTo(document.body);
        setTimeout(function () {
            element.fadeOut(200, function () { element.remove(); });
        }, 2600);
    };

    // ---------- 登出 ----------
    Shop.logout = function () {
        const refreshToken = Shop.getRefreshToken();

        // 如果前端已經沒有 refresh token，就直接清除本地資料並回登入頁。
        if (!refreshToken) {
            Shop.clearTokens();
            window.location.href = "/page/login";
            return;
        }

        $.ajax({
            url: "/members/logout",
            method: "POST",
            contentType: "application/json",
            data: JSON.stringify({ refreshToken: refreshToken }),
            complete: function () {
                Shop.clearTokens();
                window.location.href = "/page/login";
            }
        });
    };

    // ---------- 購物車數量（頁首與手機底部導覽列的紅點） ----------
    Shop.setCartCount = function (count) {
        const total = Number(count) || 0;
        $(".js-cart-count")
            .text(total > 99 ? "99+" : String(total))
            .attr("data-zero", total === 0 ? "true" : "false");
    };

    Shop.refreshCartCount = function () {
        const request = Shop.apiAjax({ url: "/cart", method: "GET" });
        if (!request) {
            return;
        }
        request.done(function (cart, textStatus, xhr) {
            Shop.saveNewTokens(xhr);
            Shop.setCartCount(cart.totalQuantity);
        });
    };

    // ---------- 頁首與底部導覽列 ----------
    // 圖示都是固定的 SVG，不含使用者輸入。
    const ICONS = {
        brand: '<svg viewBox="0 0 32 32" aria-hidden="true"><rect x="4" y="3" width="24" height="26" rx="3" fill="#fff"/><path d="M10 3v26" stroke="#ee4d2d" stroke-width="2"/><path d="M14 10h9M14 15h9" stroke="#ee4d2d" stroke-width="2" stroke-linecap="round"/></svg>',
        search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="M15.5 15.5L21 21"/></svg>',
        cart: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 4h2.5l2.2 10.2a1.5 1.5 0 0 0 1.5 1.2h8.2a1.5 1.5 0 0 0 1.5-1.1L21 8H6.2"/><circle cx="9.5" cy="19.5" r="1.4"/><circle cx="17" cy="19.5" r="1.4"/></svg>',
        home: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 11l9-7 9 7"/><path d="M5.5 10v10h13V10"/><path d="M10 20v-6h4v6"/></svg>',
        orders: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 3h12v18l-3-2-3 2-3-2-3 2z"/><path d="M9 8h6M9 12h6"/></svg>',
        user: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4.2 3.6-7 8-7s8 2.8 8 7"/></svg>'
    };

    // options.active：目前頁面（books / cart / orders / profile），用來標示底部導覽列
    // options.bottomNav：是否顯示手機底部導覽列（預設顯示）
    // options.keyword：搜尋框預設文字
    Shop.renderHeader = function (options) {
        const opts = $.extend({ active: "", bottomNav: true, keyword: "" }, options);

        $("#siteHeader").html(
            '<header class="site-header">' +
              '<div class="header-inner">' +
                '<div class="header-top">' +
                  '<a href="/page/profile">會員資料</a>' +
                  '<a href="/page/orders">我的訂單</a>' +
                  '<button type="button" id="headerLogout">登出</button>' +
                '</div>' +
                '<div class="header-main">' +
                  '<a class="brand" href="/page/books" aria-label="回首頁">' + ICONS.brand + '<span>BookShop</span></a>' +
                  '<form class="search" id="searchForm" role="search">' +
                    '<input id="searchInput" type="search" maxlength="60" placeholder="搜尋書名" aria-label="搜尋書名">' +
                    '<button type="submit" aria-label="搜尋">' + ICONS.search + '</button>' +
                  '</form>' +
                  '<a class="cart-link" href="/page/cart" aria-label="購物車">' + ICONS.cart +
                    '<span class="badge-count js-cart-count" data-zero="true">0</span></a>' +
                '</div>' +
              '</div>' +
            '</header>'
        );

        $("#searchInput").val(opts.keyword);

        $("#searchForm").on("submit", function (event) {
            event.preventDefault();
            const keyword = $("#searchInput").val().trim();
            window.location.href = keyword
                ? "/page/books?keyword=" + encodeURIComponent(keyword)
                : "/page/books";
        });

        $("#headerLogout").on("click", Shop.logout);

        $("body").toggleClass("has-bottom-nav", !!opts.bottomNav);
        if (opts.bottomNav) {
            const items = [
                { key: "books", href: "/page/books", icon: ICONS.home, text: "首頁" },
                { key: "cart", href: "/page/cart", icon: ICONS.cart, text: "購物車", badge: true },
                { key: "orders", href: "/page/orders", icon: ICONS.orders, text: "我的訂單" },
                { key: "profile", href: "/page/profile", icon: ICONS.user, text: "我的" }
            ];
            const html = items.map(function (item) {
                return '<a href="' + item.href + '"' + (item.key === opts.active ? ' class="active" aria-current="page"' : '') + '>' +
                    item.icon + '<span>' + item.text + '</span>' +
                    (item.badge ? '<span class="badge-count js-cart-count" data-zero="true">0</span>' : '') +
                    '</a>';
            }).join("");
            $("#siteBottomNav").html('<nav class="bottom-nav" aria-label="主要導覽">' + html + '</nav>');
        }

        Shop.refreshCartCount();
    };

    // ---------- 數量加減器 ----------
    // 任何 <div class="stepper"> 裡的 <button data-step="-1|1"> 都會自動生效，
    // 數字被改變時會觸發 input 的 change 事件，由各頁面自己處理。
    $(document).on("click", ".stepper [data-step]", function () {
        const stepper = $(this).closest(".stepper");
        const input = stepper.find("input");
        const min = Number(input.attr("min")) || 1;
        const max = Number(input.attr("max")) || Infinity;
        const current = parseInt(input.val(), 10) || min;
        const next = Math.min(Math.max(current + Number($(this).attr("data-step")), min), max);

        if (next !== current) {
            input.val(next).trigger("change");
        }
    });

    // ---------- 藍新付款 ----------
    // 藍新 MPG 需要瀏覽器用 form POST 導向 gateway；表單資料由後端 API 提供。
    // 資料不完整時回傳 false。
    Shop.submitNewebPayForm = function (paymentForm) {
        if (!paymentForm || !paymentForm.gatewayUrl || !paymentForm.merchantId
                || !paymentForm.version || !paymentForm.tradeInfo || !paymentForm.tradeSha) {
            return false;
        }

        const form = $("<form>", {
            method: "POST",
            action: paymentForm.gatewayUrl
        }).css("display", "none");

        const fields = {
            MerchantID: paymentForm.merchantId,
            Version: paymentForm.version,
            TradeInfo: paymentForm.tradeInfo,
            TradeSha: paymentForm.tradeSha
        };

        $.each(fields, function (name, value) {
            form.append($("<input>", { type: "hidden", name: name, value: value }));
        });

        form.appendTo(document.body);
        form.trigger("submit");
        return true;
    };

    window.Shop = Shop;
})(window, jQuery);
