<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, com.market.member.User, com.market.order.Order, com.market.order.OrderItem, com.market.store.DataStore, com.market.util.Html" %>
<%
	String ctx = request.getContextPath();
	User loginUser = (User) session.getAttribute("loginUser");
	if (loginUser == null) {
		response.sendRedirect(ctx + "/login.jsp");
		return;
	}
	List<Order> myOrders = DataStore.getInstance().getOrdersByUser(loginUser.getUserId());
	int totalAccumulated = 0;
	for (Order o : myOrders) totalAccumulated += o.getTotalAmount();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>주문 내역 · 거미줄 서점</title>
<link rel="stylesheet" href="<%=ctx%>/css/style.css">
</head>
<body>
<jsp:include page="header.jsp" />
<main class="page">
<section class="panel">
	<div class="page-head">
		<h1>주문 내역</h1>
		<span class="count">총 <%=myOrders.size()%>건</span>
	</div>

	<% if (myOrders.isEmpty()) { %>
		<div class="empty"><div class="big">📦</div><p>아직 주문 내역이 없습니다.</p><a href="<%=ctx%>/index.jsp" class="btn btn-dark">도서 둘러보기</a></div>
	<% } else { for (Order o : myOrders) { %>
	<article class="order-card">
		<div class="order-top">
			<div><span class="oid"><%=Html.esc(o.getOrderId())%></span> <span class="muted" style="font-size:.84rem;">· <%=Html.esc(o.getOrderedAt())%> 주문</span></div>
			<span class="pill">● <%=Html.esc(o.getStatus())%></span>
		</div>
		<dl class="order-meta">
			<div><dt>받는 분</dt><dd><%=Html.esc(o.getRecipientName())%> (<%=Html.esc(o.getRecipientPhone())%>)</dd></div>
			<div><dt>배송지</dt><dd><%=Html.esc(o.getShippingAddress())%></dd></div>
			<div><dt>배송 희망일</dt><dd><%=Html.esc(o.getShippingDate())%></dd></div>
		</dl>
		<div class="order-items">
			<% for (OrderItem oi : o.getItems()) { %>
			<div class="order-line">
				<img src="<%=Html.esc(oi.getImageWebPath())%>" alt="" onerror="this.onerror=null;this.src='<%=ctx%>/images/shop.jpg'">
				<strong><%=Html.esc(oi.getBookName())%></strong>
				<span class="q muted"><%=oi.getQuantity()%>권</span>
				<span class="line-total"><%=Html.won(oi.getTotalPrice())%>원</span>
			</div>
			<% } %>
		</div>
		<div class="order-bottom">
			<div>
				<% if (o.getDiscount() > 0) { %><span class="muted" style="font-size:.84rem; margin-right:10px;">상품 <%=Html.won(o.getGrandTotal())%>원 − 🎟 쿠폰 <%=Html.won(o.getDiscount())%>원</span><% } %>
				주문 합계 <span class="line-total" style="font-size:1.1rem; margin-left:6px;"><%=Html.won(o.getTotalAmount())%>원</span>
			</div>
			<form action="<%=ctx%>/action" method="post" onsubmit="return confirm('이 주문을 취소하고 내역에서 삭제하시겠습니까?');">
				<input type="hidden" name="cmd" value="cancelOrder"><input type="hidden" name="orderId" value="<%=Html.esc(o.getOrderId())%>">
				<button type="submit" class="btn btn-danger btn-sm">주문 취소</button>
			</form>
		</div>
	</article>
	<% } } %>

	<% if (!myOrders.isEmpty()) { %>
	<div class="grand"><span>누적 결제 금액</span><b><%=Html.won(totalAccumulated)%>원</b></div>
	<% } %>
</section>
</main>
<jsp:include page="footer.jsp" />
</body>
</html>
