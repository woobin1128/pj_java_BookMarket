<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.text.SimpleDateFormat, java.util.Date, com.market.member.User, com.market.cart.Cart, com.market.store.DataStore, com.market.util.Html" %>
<%
	String ctx = request.getContextPath();
	User loginUser = (User) session.getAttribute("loginUser");
	if (loginUser == null) {
		session.setAttribute("redirectAfterLogin", ctx + "/shipping.jsp");
		response.sendRedirect(ctx + "/login.jsp");
		return;
	}
	Cart cart = (Cart) session.getAttribute("cart");
	if (cart == null || cart.getmCartItem().isEmpty()) {
		response.sendRedirect(ctx + "/cart.jsp");
		return;
	}
	int orderTotal = cart.getTotalPrice();
	int couponDiscount = DataStore.getInstance().couponDiscountFor(loginUser, orderTotal);
	String minDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	String defaultDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis() + 86400000L * 2));
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>배송 정보 · 거미줄 서점</title>
<link rel="stylesheet" href="<%=ctx%>/css/style.css">
</head>
<body>
<jsp:include page="header.jsp" />
<main class="page">
<section class="panel medium">
	<div class="steps" aria-label="주문 단계">
		<span><i>1</i>장바구니</span><em class="sep"></em><span class="on"><i>2</i>배송 정보</span><em class="sep"></em><span><i>3</i>주문 완료</span>
	</div>
	<div class="page-head"><h1>배송 정보</h1></div>
	<form action="<%=ctx%>/action" method="post">
		<input type="hidden" name="cmd" value="placeOrder">
		<div class="grid-3" style="grid-template-columns:1fr 1fr;">
			<div class="form-group"><label for="rn">받는 분</label><input type="text" id="rn" name="recipientName" value="<%=Html.esc(loginUser.getName())%>" required></div>
			<div class="form-group"><label for="rp">연락처</label><input type="text" id="rp" name="recipientPhone" value="<%=Html.esc(loginUser.getPhone())%>" required></div>
		</div>
		<div class="form-group"><label for="sa">배송 주소</label><input type="text" id="sa" name="shippingAddress" placeholder="배송받을 주소를 입력하세요" required autofocus></div>
		<div class="form-group"><label for="sd">배송 희망일</label><input type="date" id="sd" name="shippingDate" value="<%=defaultDate%>" min="<%=minDate%>" required></div>

		<% if (couponDiscount > 0) { %>
		<label class="coupon-check">
			<input type="checkbox" name="useCoupon" value="on" checked>
			<span><b>🎟 <%=Html.esc(DataStore.COUPON_NAME)%></b> 사용하기 <small>(-<%=Html.won(couponDiscount)%>원)</small></span>
		</label>
		<% } else if (loginUser.hasUsableCoupon()) { %>
		<p class="coupon-hint">🎟 쿠폰은 <%=Html.won(DataStore.COUPON_MIN_ORDER)%>원 이상 구매 시 사용할 수 있어요.</p>
		<% } %>
		<div class="summary" style="position:static; margin:6px 0 20px;">
			<div class="sum-row"><span>주문 수량</span><span><%=cart.getTotalCount()%>권</span></div>
			<div class="sum-row"><span>상품 금액</span><span><%=Html.won(orderTotal)%>원</span></div>
			<% if (couponDiscount > 0) { %>
			<div class="sum-row coupon-row"><span>🎟 쿠폰 할인</span><span>-<%=Html.won(couponDiscount)%>원</span></div>
			<% } %>
			<div class="sum-row total"><span>총 결제 금액</span><span><%=Html.won(orderTotal - couponDiscount)%>원</span></div>
		</div>
		<div style="display:flex; gap:10px; justify-content:space-between;">
			<a href="<%=ctx%>/cart.jsp" class="btn btn-outline">← 장바구니</a>
			<button type="submit" class="btn" style="flex:1; padding:12px;">주문 완료하기</button>
		</div>
	</form>
</section>
</main>
<jsp:include page="footer.jsp" />
</body>
</html>
