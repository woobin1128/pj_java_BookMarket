<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.market.cart.Cart, com.market.util.Html" %>
<%
	String ctx = request.getContextPath();
	String currentTab = request.getParameter("tab");
	if (currentTab == null || currentTab.trim().isEmpty()) currentTab = "all";
	currentTab = currentTab.toLowerCase();
	String q = request.getParameter("keyword");
	if (q == null) q = request.getParameter("q");   // 구 파라미터 호환
	if (q == null) q = "";

	boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
	boolean isLoggedIn = session.getAttribute("loginUser") != null;
	Cart hCart = (Cart) session.getAttribute("cart");
	int cartCount = (hCart != null) ? hCart.getTotalCount() : 0;
	boolean onIndex = request.getRequestURI().endsWith("index.jsp") || request.getRequestURI().endsWith("/");

	// 서블릿이 세션에 담아둔 1회성 안내 메시지 표시 후 제거
	String flashMsg = (String) session.getAttribute("flashMsg");
	String flashError = (String) session.getAttribute("flashError");
	session.removeAttribute("flashMsg");
	session.removeAttribute("flashError");
%>
<header class="site-header">
	<div class="header-inner">
		<a href="<%=ctx%>/index.jsp" class="brand" aria-label="거미줄 서점 홈">
			<img src="<%=ctx%>/images/shop.jpg" alt="">
			<span><b>거미줄 서점</b><small>SPIDERWEB BOOKS</small></span>
		</a>

		<form action="<%=ctx%>/index.jsp" method="get" class="search" role="search">
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/></svg>
			<input type="text" name="keyword" value="<%=Html.esc(q)%>" placeholder="도서명 또는 저자를 검색하세요" aria-label="도서 검색" autocomplete="off">
			<button type="submit">검색</button>
		</form>

		<div class="header-actions">
			<a href="<%=ctx%>/cart.jsp" class="hbtn cart" aria-label="장바구니 <%=cartCount%>권">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/></svg>
				<span class="lbl">장바구니</span><span class="badge"><%=cartCount%></span>
			</a>
			<% if (isAdmin) { %>
				<a href="<%=ctx%>/admin.jsp" class="hbtn admin">⚙ <span class="lbl">관리자 센터</span></a>
				<a href="<%=ctx%>/action?cmd=logout" class="hbtn"><span class="lbl">로그아웃</span></a>
			<% } else if (isLoggedIn) { %>
				<a href="<%=ctx%>/orders.jsp" class="hbtn">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3 8-8"/><path d="M20 12v7a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h9"/></svg>
					<span class="lbl">주문 내역</span>
				</a>
				<a href="<%=ctx%>/profile.jsp" class="hbtn" title="마이페이지">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-3.9 3.6-7 8-7s8 3.1 8 7"/></svg>
					<span class="lbl">마이페이지</span>
				</a>
				<a href="<%=ctx%>/action?cmd=logout" class="hbtn solid">로그아웃</a>
			<% } else { %>
				<a href="<%=ctx%>/register.jsp" class="hbtn">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M20 8v6M23 11h-6"/></svg>
					<span class="lbl">회원가입</span>
				</a>
				<a href="<%=ctx%>/login.jsp" class="hbtn solid">로그인</a>
			<% } %>
		</div>
	</div>

	<nav class="nav" aria-label="도서 카테고리">
		<div class="nav-inner">
			<a href="<%=ctx%>/index.jsp?tab=all" class="<%= onIndex && q.isEmpty() && "all".equals(currentTab) ? "active" : "" %>">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>도서 목록</a>
			<a href="<%=ctx%>/index.jsp?tab=best" class="<%= onIndex && q.isEmpty() && "best".equals(currentTab) ? "active" : "" %>">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linejoin="round"><polygon points="12 2 15.1 8.3 22 9.3 17 14.1 18.2 21 12 17.8 5.8 21 7 14.1 2 9.3 8.9 8.3 12 2"/></svg>베스트 도서</a>
			<a href="<%=ctx%>/index.jsp?tab=event" class="<%= onIndex && q.isEmpty() && "event".equals(currentTab) ? "active" : "" %>">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 12 20 22 4 22 4 12"/><rect x="2" y="7" width="20" height="5"/><line x1="12" y1="22" x2="12" y2="7"/><path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7zM12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"/></svg>이벤트 도서</a>
			<a href="<%=ctx%>/index.jsp?tab=new" class="<%= onIndex && q.isEmpty() && "new".equals(currentTab) ? "active" : "" %>">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linejoin="round"><path d="M12 3l1.9 5.8L20 10.7l-6.1 1.9L12 18.4l-1.9-5.8L4 10.7l6.1-1.9L12 3z"/></svg>신간 도서</a>
		</div>
	</nav>
</header>
<% if (flashMsg != null || flashError != null) { %>
<div class="flash-wrap" role="status" aria-live="polite">
	<% if (flashMsg != null) { %><div class="flash ok">✓ <span><%=Html.esc(flashMsg)%></span><button type="button" onclick="this.parentNode.remove()" aria-label="닫기">×</button></div><% } %>
	<% if (flashError != null) { %><div class="flash err">⚠ <span><%=Html.esc(flashError)%></span><button type="button" onclick="this.parentNode.remove()" aria-label="닫기">×</button></div><% } %>
</div>
<script>setTimeout(function(){document.querySelectorAll('.flash.ok').forEach(function(e){e.remove()})},4500);</script>
<% } %>
