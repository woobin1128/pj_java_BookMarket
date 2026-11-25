<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, com.market.member.User, com.market.bookitem.Book, com.market.order.Order, com.market.order.OrderItem, com.market.store.DataStore, com.market.util.Html" %>
<%
	String ctx = request.getContextPath();
	if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
		response.sendRedirect(ctx + "/login.jsp");
		return;
	}
	DataStore store = DataStore.getInstance();
	String adminTab = request.getParameter("tab");
	if (adminTab == null || adminTab.isEmpty()) adminTab = "members";
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>관리자 센터 · 거미줄 서점</title>
<link rel="stylesheet" href="<%=ctx%>/css/style.css">
</head>
<body>
<jsp:include page="header.jsp" />
<main class="page">
<section class="panel">
	<div class="page-head">
		<h1>⚙ 관리자 센터</h1>
		<nav class="tabs">
			<a href="<%=ctx%>/admin.jsp?tab=members" class="<%= "members".equals(adminTab) ? "on" : "" %>">회원 관리</a>
			<a href="<%=ctx%>/admin.jsp?tab=books" class="<%= "books".equals(adminTab) ? "on" : "" %>">도서 관리</a>
			<a href="<%=ctx%>/admin.jsp?tab=orders" class="<%= "orders".equals(adminTab) ? "on" : "" %>">주문 관리</a>
		</nav>
	</div>

<% if ("members".equals(adminTab)) {
	List<User> users = store.getAllUsers(); %>
	<h2 class="section-h">일반 회원 <span class="muted">(<%=users.size()%>명)</span></h2>
	<div class="table-scroll">
	<table class="data">
		<thead><tr><th>#</th><th>아이디</th><th>이름</th><th>연락처</th><th>가입일</th><th>비밀번호 해시 (SHA-256)</th></tr></thead>
		<tbody>
		<% if (users.isEmpty()) { %>
			<tr><td colspan="6" class="empty" style="padding:30px;">가입된 일반 회원이 없습니다.</td></tr>
		<% } else { int idx = 1; for (User u : users) { %>
			<tr>
				<td><%=idx++%></td><td><strong><%=Html.esc(u.getUserId())%></strong></td><td><%=Html.esc(u.getName())%></td>
				<td><%=Html.esc(u.getPhone())%></td><td><%=Html.esc(u.getJoinedAt())%></td><td class="mono"><%=Html.esc(u.getPasswordHash())%></td>
			</tr>
		<% } } %>
		</tbody>
	</table>
	</div>

<% } else if ("books".equals(adminTab)) {
	List<Book> books = store.getAllBooks(); %>
	<form action="<%=ctx%>/action" method="post" enctype="multipart/form-data" class="admin-form">
		<h3>➕ 새 도서 등록 <small class="muted" style="font-family:var(--font); font-size:.78rem; font-weight:500;">등록 시 ‘도서 목록’과 ‘신간 도서’에 자동 노출</small></h3>
		<input type="hidden" name="cmd" value="adminAddBook">
		<div class="grid-3">
			<div class="form-group"><label>도서 ID (비우면 자동생성)</label><input type="text" name="bookId" placeholder="예: ISBN1237"></div>
			<div class="form-group"><label>도서명 *</label><input type="text" name="name" required></div>
			<div class="form-group"><label>가격(원) *</label><input type="number" name="unitPrice" min="0" required></div>
			<div class="form-group"><label>저자 *</label><input type="text" name="author" required></div>
			<div class="form-group"><label>분야 *</label><input type="text" name="category" placeholder="예: IT전문서" required></div>
			<div class="form-group"><label>출판일 *</label><input type="text" name="releaseDate" placeholder="예: 2026/11/25" required></div>
		</div>
		<div class="grid-2">
			<div class="form-group"><label>도서 설명 *</label><input type="text" name="description" required></div>
			<div class="form-group"><label>표지 이미지</label><input type="file" name="imageFile" accept="image/*"></div>
		</div>
		<button type="submit" class="btn">새 도서 등록</button>
	</form>

	<h2 class="section-h">등록 도서 <span class="muted">(<%=books.size()%>권)</span> · 수정 / 탭 노출 / 삭제</h2>
	<% for (Book b : books) { String fid = "editForm_" + Html.esc(b.getBookId()); %>
	<div class="admin-book">
		<img src="<%=b.getImageWebPath(ctx)%>" alt="" onerror="this.onerror=null;this.src='<%=ctx%>/images/shop.jpg'">
		<form id="<%=fid%>" action="<%=ctx%>/action" method="post" enctype="multipart/form-data">
			<input type="hidden" name="cmd" value="adminUpdateBook">
			<input type="hidden" name="bookId" value="<%=Html.esc(b.getBookId())%>">
			<div class="fields">
				<input type="text" name="name" value="<%=Html.esc(b.getName())%>" placeholder="도서명" required aria-label="도서명">
				<input type="number" name="unitPrice" value="<%=b.getUnitPrice()%>" placeholder="가격" min="0" required aria-label="가격">
				<input type="text" name="author" value="<%=Html.esc(b.getAuthor())%>" placeholder="저자" required aria-label="저자">
				<input type="text" name="category" value="<%=Html.esc(b.getCategory())%>" placeholder="분야" required aria-label="분야">
				<input type="text" name="releaseDate" value="<%=Html.esc(b.getReleaseDate())%>" placeholder="출판일" required aria-label="출판일">
				<input type="file" name="imageFile" accept="image/*" aria-label="표지 변경">
				<input type="text" name="description" class="full" value="<%=Html.esc(b.getDescription())%>" placeholder="설명" required aria-label="설명">
			</div>
		</form>
		<div>
			<strong style="font-size:.78rem; color:var(--ink-3);">노출 탭</strong>
			<label class="check"><input type="checkbox" form="<%=fid%>" name="tabs" value="BEST" <%=b.hasTab("BEST") ? "checked" : ""%>>베스트</label>
			<label class="check"><input type="checkbox" form="<%=fid%>" name="tabs" value="EVENT" <%=b.hasTab("EVENT") ? "checked" : ""%>>이벤트</label>
			<label class="check"><input type="checkbox" form="<%=fid%>" name="tabs" value="NEW" <%=b.hasTab("NEW") ? "checked" : ""%>>신간</label>
		</div>
		<div class="stack">
			<button type="submit" form="<%=fid%>" class="btn btn-sm">저장</button>
			<form action="<%=ctx%>/action" method="post" onsubmit="return confirm('이 도서를 삭제하시겠습니까?');">
				<input type="hidden" name="cmd" value="adminDeleteBook"><input type="hidden" name="bookId" value="<%=Html.esc(b.getBookId())%>">
				<button type="submit" class="btn btn-danger btn-sm" style="width:100%;">삭제</button>
			</form>
		</div>
	</div>
	<% } %>

<% } else if ("orders".equals(adminTab)) {
	List<Order> allOrders = store.getAllOrders(); %>
	<h2 class="section-h">전체 주문 <span class="muted">(<%=allOrders.size()%>건)</span></h2>
	<% if (allOrders.isEmpty()) { %>
		<div class="empty"><div class="big">📭</div><p>접수된 주문이 없습니다.</p></div>
	<% } else { for (Order o : allOrders) { %>
	<article class="order-card">
		<div class="order-top">
			<div><span class="oid"><%=Html.esc(o.getOrderId())%></span> <span class="muted" style="font-size:.84rem;">· <%=Html.esc(o.getOrderedAt())%></span></div>
			<form action="<%=ctx%>/action" method="post" onsubmit="return confirm('이 주문을 삭제하시겠습니까?');">
				<input type="hidden" name="cmd" value="cancelOrder"><input type="hidden" name="orderId" value="<%=Html.esc(o.getOrderId())%>">
				<button type="submit" class="btn btn-danger btn-sm">주문 삭제</button>
			</form>
		</div>
		<dl class="order-meta">
			<div><dt>회원</dt><dd><%=Html.esc(o.getUserName())%> (<%=Html.esc(o.getUserId())%>)</dd></div>
			<div><dt>받는 분</dt><dd><%=Html.esc(o.getRecipientName())%> (<%=Html.esc(o.getRecipientPhone())%>)</dd></div>
			<div><dt>배송지</dt><dd><%=Html.esc(o.getShippingAddress())%> · <%=Html.esc(o.getShippingDate())%></dd></div>
		</dl>
		<div class="order-items">
			<% for (OrderItem oi : o.getItems()) { %>
			<div class="order-line">
				<img src="<%=Html.esc(oi.getImageWebPath())%>" alt="" onerror="this.onerror=null;this.src='<%=ctx%>/images/shop.jpg'">
				<span><strong><%=Html.esc(oi.getBookName())%></strong> <span class="mono">(<%=Html.esc(oi.getBookId())%>)</span></span>
				<span class="q muted"><%=Html.won(oi.getUnitPrice())%>원 × <%=oi.getQuantity()%></span>
				<span class="line-total"><%=Html.won(oi.getTotalPrice())%>원</span>
			</div>
			<% } %>
		</div>
		<div class="order-bottom"><span class="muted">결제 합계</span><span class="line-total" style="font-size:1.1rem;"><%=Html.won(o.getTotalAmount())%>원</span></div>
	</article>
	<% } } %>
<% } %>
</section>
</main>
<jsp:include page="footer.jsp" />
</body>
</html>
