<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, com.market.bookitem.Book, com.market.store.DataStore, com.market.kakao.*, com.market.util.*" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>거미줄 서점 · 온라인 서점</title>
<link rel="icon" href="<%=request.getContextPath()%>/images/shop.jpg">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
<jsp:include page="header.jsp" />
<%
	String ctx = request.getContextPath();
	DataStore store = DataStore.getInstance();
	BookCatalog catalog = store.getCatalog();
	boolean kakaoOn = store.getKakao().enabled();

	String tab = request.getParameter("tab");
	if (tab == null || tab.isEmpty()) tab = "ALL";
	tab = tab.toUpperCase();
	String keyword = request.getParameter("keyword");
	if (keyword == null) keyword = request.getParameter("q");
	if (keyword == null) keyword = "";
	keyword = keyword.trim();
	boolean searching = !keyword.isEmpty();

	String catParam = request.getParameter("cat");
	BookCatalog.Category activeCat = searching ? null : BookCatalog.findCategory(catParam);

	String sort = "latest".equals(request.getParameter("sort")) ? "latest" : "accuracy";
	int pageNo = 1;
	try { pageNo = Math.max(1, Math.min(50, Integer.parseInt(request.getParameter("page")))); } catch (Exception ignore) {}
	final int PAGE_SIZE = 24;

	String qs = request.getQueryString();
	String currentUrl = ctx + "/index.jsp" + (qs != null && !qs.isEmpty() ? "?" + qs : "");

	boolean home = !searching && activeCat == null && "ALL".equals(tab);

	// 모드별 데이터
	List<Book> localBooks = store.getBooks(tab, keyword);   // 검색이면 전체에서 도서명/저자 검색
	Map<BookCatalog.Category, KakaoClient.BookPage> homeData = null;
	KakaoClient.BookPage pageData = null;
	boolean apiFailed = false;

	if (home && kakaoOn) {
		homeData = catalog.fetchHome(8);
		for (KakaoClient.BookPage p : homeData.values()) if (p != null) store.registerExternal(p.books);
	} else if (activeCat != null && kakaoOn) {
		pageData = catalog.fetch(activeCat.query, activeCat.label, sort, pageNo, PAGE_SIZE);
		if (pageData != null) store.registerExternal(pageData.books); else apiFailed = true;
	} else if (searching && kakaoOn) {
		pageData = catalog.fetch(keyword, "검색", sort, pageNo, PAGE_SIZE);
		if (pageData != null) store.registerExternal(pageData.books); else apiFailed = true;
	}
	if (homeData != null) {
		boolean allNull = true;
		for (KakaoClient.BookPage p : homeData.values()) if (p != null) { allNull = false; break; }
		apiFailed = allNull;
	}

	String heading = "도서 목록";
	String sub = "소설·시·에세이부터 IT까지, 분야별로 책을 골라보세요.";
	if (searching) { heading = "‘" + keyword + "’ 검색 결과"; sub = "서점 소장 도서와 카카오(다음) 도서 검색 결과입니다."; }
	else if (activeCat != null) { heading = activeCat.label; sub = "‘" + activeCat.label + "’ 분야의 책입니다."; }
	else if ("BEST".equals(tab)) { heading = "베스트 도서"; sub = "독자들이 가장 많이 찾는 책입니다."; }
	else if ("EVENT".equals(tab)) { heading = "이벤트 도서"; sub = "지금 놓치면 아쉬운 기획전 도서입니다."; }
	else if ("NEW".equals(tab)) { heading = "신간 도서"; sub = "막 입고된 따끈한 신간입니다."; }
	boolean showChips = (home || activeCat != null) && kakaoOn;
%>
<main class="page">
<% if (home) { %>
	<section class="hero">
		<small>WELCOME TO SPIDERWEB BOOKS</small>
		<h1>오래된 서가의 온기,<br>새로운 책 한 권의 설렘.</h1>
		<p>소설, 시, 에세이부터 IT 전문서까지. 거미줄 서점에서 읽고 싶은 책을 분야별로 찾고 바로 주문하세요.</p>
		<div class="hero-stats">
			<div><b><%=BookCatalog.CATEGORIES.size()%></b><span>도서 분야</span></div>
			<div><b><%=store.getAllBooks().size()%></b><span>서점 소장 도서</span></div>
			<div><b>2일</b><span>평균 배송</span></div>
		</div>
	</section>
<% } %>

<section class="panel">
	<div class="page-head">
		<div>
			<h1><%=Html.esc(heading)%></h1>
			<span class="muted" style="font-size:.88rem;"><%=Html.esc(sub)%></span>
		</div>
		<% if (home) { %><span class="count">총 <b><%=homeBookCount(homeData, localBooks)%></b>권 전시 중</span>
		<% } else if (pageData != null) { %><span class="count">검색된 도서 <b><%=Html.won(pageData.pageableCount)%></b>권</span>
		<% } else { %><span class="count">총 <b id="shownCount"><%=localBooks.size()%></b>권</span><% } %>
	</div>

	<% if (showChips) { %>
	<div class="chips cat-chips" role="navigation" aria-label="분야">
		<a class="chip <%= activeCat == null ? "on" : "" %>" href="<%=ctx%>/index.jsp">전체</a>
		<% int ci = 0; for (BookCatalog.Category c : BookCatalog.CATEGORIES) { %>
			<a class="chip <%= (activeCat != null && activeCat.label.equals(c.label)) ? "on" : "" %>"
				href="<%= home ? ("#sec-" + ci) : (ctx + "/index.jsp?cat=" + java.net.URLEncoder.encode(c.label, "UTF-8")) %>"><%=Html.esc(c.label)%></a>
		<% ci++; } %>
	</div>
	<% } %>

	<% if (!kakaoOn && (home || activeCat != null || searching)) { %>
		<div class="notice">카카오 도서 API 키가 설정되지 않아 서점 소장 도서만 표시됩니다. <code>WEB-INF/config.properties</code>의 <code>kakao.rest.key</code>를 입력해 주세요.</div>
	<% } else if (apiFailed) { %>
		<div class="notice warn">카카오 도서 목록을 불러오지 못했습니다. 잠시 후 새로고침해 주세요. (API 키·네트워크 확인)</div>
	<% } %>

	<%-- ============ 홈: 소장 도서 + 분야별 섹션 ============ --%>
	<% if (home) {
		if (!localBooks.isEmpty()) { %>
		<h2 class="sec-title">거미줄 서점 소장 도서 <small><%=localBooks.size()%>권</small></h2>
		<div class="book-grid">
			<% int o = 0; for (Book b : localBooks) { %><%=BookCards.card(b, ctx, currentUrl, o++)%><% } %>
		</div>
		<% }
		if (homeData != null) { int idx = 0; for (Map.Entry<BookCatalog.Category, KakaoClient.BookPage> en : homeData.entrySet()) {
			BookCatalog.Category c = en.getKey(); KakaoClient.BookPage p = en.getValue();
			if (p != null && !p.books.isEmpty()) { %>
		<h2 class="sec-title" id="sec-<%=idx%>"><%=Html.esc(c.label)%>
			<a class="more" href="<%=ctx%>/index.jsp?cat=<%=java.net.URLEncoder.encode(c.label, "UTF-8")%>">더보기 →</a></h2>
		<div class="book-grid">
			<% int o = 0; for (Book b : p.books) { %><%=BookCards.card(b, ctx, currentUrl, o++)%><% } %>
		</div>
		<% } idx++; } } %>

	<%-- ============ 분야 / 검색 (카카오) ============ --%>
	<% } else if (activeCat != null || searching) {
		if (searching && !localBooks.isEmpty()) { %>
		<h2 class="sec-title">서점 소장 도서 <small><%=localBooks.size()%>권</small></h2>
		<div class="book-grid">
			<% int o = 0; for (Book b : localBooks) { %><%=BookCards.card(b, ctx, currentUrl, o++)%><% } %>
		</div>
		<% }
		if (pageData != null) {
			String base = ctx + "/index.jsp?" + (searching ? "keyword=" + java.net.URLEncoder.encode(keyword, "UTF-8")
					: "cat=" + java.net.URLEncoder.encode(activeCat.label, "UTF-8")); %>
		<div class="toolbar" style="margin-top:20px;">
			<h2 class="sec-title" style="margin:0;"><%= searching ? "카카오 도서 검색" : Html.esc(activeCat.label) %></h2>
			<div class="chips">
				<a class="chip <%= "accuracy".equals(sort) ? "on" : "" %>" href="<%=base%>&sort=accuracy">인기·정확도순</a>
				<a class="chip <%= "latest".equals(sort) ? "on" : "" %>" href="<%=base%>&sort=latest">최신 출간순</a>
			</div>
		</div>
		<% if (pageData.books.isEmpty()) { %>
			<div class="empty"><div class="big">📚</div><p>표시할 도서가 없습니다.</p></div>
		<% } else { %>
		<div class="book-grid">
			<% int o = 0; for (Book b : pageData.books) { %><%=BookCards.card(b, ctx, currentUrl, o++)%><% } %>
		</div>
		<% } %>
		<%
			int maxPage = Math.max(1, Math.min(50, (int) Math.ceil(pageData.pageableCount / (double) PAGE_SIZE)));
			if (maxPage > 1) {
				int from = Math.max(1, pageNo - 3), to = Math.min(maxPage, from + 6);
				from = Math.max(1, to - 6);
				String pb = base + "&sort=" + sort + "&page=";
		%>
		<nav class="pager" aria-label="페이지">
			<% if (pageNo > 1) { %><a href="<%=pb + (pageNo - 1)%>">‹ 이전</a><% } %>
			<% for (int pg = from; pg <= to; pg++) { %><a class="<%= pg == pageNo ? "on" : "" %>" href="<%=pb + pg%>"><%=pg%></a><% } %>
			<% if (!pageData.isEnd && pageNo < maxPage) { %><a href="<%=pb + (pageNo + 1)%>">다음 ›</a><% } %>
		</nav>
		<% } %>
		<% } else if (searching && localBooks.isEmpty() && !apiFailed) { %>
			<div class="empty"><div class="big">📚</div><p>검색 결과가 없습니다. 다른 검색어를 입력해 보세요.</p></div>
		<% } %>

	<%-- ============ 베스트 / 이벤트 / 신간 (서점 소장 도서) ============ --%>
	<% } else { %>
		<% if (localBooks.isEmpty()) { %>
			<div class="empty">
				<div class="big">📚</div>
				<p>표시할 도서가 없습니다.</p>
				<a href="<%=ctx%>/index.jsp" class="btn btn-dark">전체 도서 보기</a>
			</div>
		<% } else { %>
		<div class="toolbar">
			<span></span>
			<select id="sort" aria-label="정렬">
				<option value="default">기본 정렬</option>
				<option value="new">최신 출판순</option>
				<option value="low">낮은 가격순</option>
				<option value="high">높은 가격순</option>
				<option value="name">가나다순</option>
			</select>
		</div>
		<div class="book-grid" id="grid">
			<% int o = 0; for (Book b : localBooks) { %><%=BookCards.card(b, ctx, currentUrl, o++)%><% } %>
		</div>
		<% } %>
	<% } %>
</section>
</main>
<jsp:include page="footer.jsp" />
<script>
(function () {
	var grid = document.getElementById('grid'), sel = document.getElementById('sort');
	if (!grid || !sel) return;
	var cards = Array.prototype.slice.call(grid.children);
	sel.addEventListener('change', function () {
		var mode = sel.value;
		cards.sort(function (a, b) {
			var A = a.dataset, B = b.dataset;
			if (mode === 'low') return A.price - B.price;
			if (mode === 'high') return B.price - A.price;
			if (mode === 'name') return A.name.localeCompare(B.name, 'ko');
			if (mode === 'new') return B.date.localeCompare(A.date);
			return A.order - B.order;
		});
		cards.forEach(function (c) { grid.appendChild(c); });
	});
})();
</script>
</body>
</html>
<%!
	private static int homeBookCount(Map<BookCatalog.Category, KakaoClient.BookPage> data, List<Book> local) {
		int n = local.size();
		if (data != null) for (KakaoClient.BookPage p : data.values()) if (p != null) n += p.books.size();
		return n;
	}
%>
