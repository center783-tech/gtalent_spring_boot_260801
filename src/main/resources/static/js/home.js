(function ($) {
    "use strict";
    const featuredBooks = [
        { id: 1, name: "Book A", tagline: "精選書籍，陪你展開閱讀旅程" },
        { id: 2, name: "Book B", tagline: "值得收藏，也值得慢慢閱讀" },
        { id: 3, name: "Book-26090900015", tagline: "探索不同故事，找到閱讀樂趣" },
        { id: 4, name: "Book-26090900014", tagline: "用一本好書，享受安靜時光" },
        { id: 5, name: "Book-26090900016", tagline: "讓文字成為生活中的陪伴" }
    ];
    function renderCovers() {
        const grid = $("#coverGrid").empty();
        featuredBooks.forEach(function (book) {
            const link = $("<a>", { href: "/page/book-detail?id=" + encodeURIComponent(book.id), "aria-label": book.name });
            $("<img>", { src: "/images/books/book-" + encodeURIComponent(book.id) + ".svg", alt: book.name + " 封面", width: 300, height: 400, loading: "lazy" }).appendTo(link);
            $("<div>", { class: "caption" }).append($("<h3>", { text: book.name })).append($("<p>", { text: book.tagline })).appendTo(link);
            $("<li>", { class: "cover-card" }).append(link).appendTo(grid);
        });
    }
    $(function () {
        renderCovers();
        const loggedIn = !!(localStorage.getItem("accessToken") && localStorage.getItem("refreshToken"));
        if (loggedIn) {
            $("#topLink, #primaryAction").text("開始購物").attr("href", "/page/books");
            $("#secondaryAction").hide();
            $("#moreLink").text("瀏覽全部書籍").attr("href", "/page/books");
        }
    });
})(jQuery);
