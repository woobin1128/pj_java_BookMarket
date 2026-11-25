<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, com.market.cart.Cart, com.market.cart.CartItem, com.market.member.User, com.market.store.DataStore, com.market.util.Html" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>장바구니 · 거미줄 서점</title>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
<jsp:include page="header.jsp" />
<%
	String ctx = request.getContextPath();
	Cart cart = (Cart) session.getAttribute("cart");
	List<CartItem> items = (cart != null) ? cart.getmCartItem() : Collections.<CartItem>emptyList();
	int total = (cart != null) ? cart.getTotalPrice() : 0;
	User cartUser = (User) session.getAttribute("loginUser");
	int couponDiscount = DataStore.getInstance().couponDiscountFor(cartUser, total);
%>
<main class="page">
<section class="panel">
	<div class="page-head">
		<h1>장바구니</h1>
		<% if (!items.isEmpty()) { %>
		<form action="<%=ctx%>/action" method="post" onsubmit="return confirm('장바구니를 모두 비우시겠습니까?');">
			<input type="hidden" name="cmd" value="clearCart">
			<button type="submit" class="btn btn-ghost btn-sm">전체 비우기</button>
		</form>
		<% } %>
	</div>

	<% if (items.isEmpty()) { %>
		<div class="empty">
			<div class="big">🛒</div>
			<p>장바구니가 비어 있습니다.<br>마음에 드는 책을 담아보세요.</p>
			<a href="<%=ctx%>/index.jsp" class="btn btn-dark">도서 둘러보기</a>
		</div>
	<% } else { %>
	<div class="cart-layout">
		<div>
			<% for (CartItem item : items) {
				String id = Html.esc(item.getBookID()); %>
			<div class="cart-item">
				<img src="<%=item.getItemBook().getImageWebPath(ctx)%>" alt="" onerror="this.onerror=null;this.src='<%=ctx%>/images/shop.jpg'">
				<div>
					<h3><%=Html.esc(item.getItemBook().getName())%></h3>
					<div class="sub"><%=Html.esc(item.getItemBook().getAuthor())%> · 단가 <%=Html.won(item.getItemBook().getUnitPrice())%>원</div>
				</div>
				<div class="cart-right">
					<div class="qty" role="group" aria-label="수량">
						<form action="<%=ctx%>/action" method="post"><input type="hidden" name="cmd" value="updateCartQty"><input type="hidden" name="bookId" value="<%=id%>"><input type="hidden" name="delta" value="-1"><button type="submit" aria-label="수량 감소">−</button></form>
						<span><%=item.getQuantity()%></span>
						<form action="<%=ctx%>/action" method="post"><input type="hidden" name="cmd" value="updateCartQty"><input type="hidden" name="bookId" value="<%=id%>"><input type="hidden" name="delta" value="1"><button type="submit" aria-label="수량 증가">+</button></form>
					</div>
					<span class="line-total"><%=Html.won(item.getTotalPrice())%>원</span>
					<form action="<%=ctx%>/action" method="post">
						<input type="hidden" name="cmd" value="removeCartItem"><input type="hidden" name="bookId" value="<%=id%>">
						<button type="submit" class="btn btn-ghost btn-sm">삭제</button>
					</form>
				</div>
			</div>
			<% } %>
			<div style="margin-top:18px;"><a href="<%=ctx%>/index.jsp" class="btn btn-outline">← 쇼핑 계속하기</a></div>
		</div>

		<aside class="summary">
			<h3>주문 요약</h3>
			<div class="sum-row"><span>상품 수량</span><span><%=cart.getTotalCount()%>권</span></div>
			<div class="sum-row"><span>상품 금액</span><span><%=Html.won(total)%>원</span></div>
			<% if (couponDiscount > 0) { %>
			<div class="sum-row coupon-row"><span>🎟 쿠폰 할인</span><span>-<%=Html.won(couponDiscount)%>원</span></div>
			<% } %>
			<div class="sum-row total"><span>결제 예정 금액</span><span><%=Html.won(total - couponDiscount)%>원</span></div>
			<% if (cartUser != null && cartUser.hasUsableCoupon() && couponDiscount == 0) { %>
			<p class="coupon-hint">🎟 보유 쿠폰: <%=Html.won(DataStore.COUPON_MIN_ORDER)%>원 이상 구매 시 <%=Html.won(DataStore.COUPON_DISCOUNT)%>원 할인 (<%=Html.won(DataStore.COUPON_MIN_ORDER - total)%>원 더 담으면 적용)</p>
			<% } else if (cartUser == null) { %>
			<p class="coupon-hint">회원이시라면 로그인 후 마이페이지에서 쿠폰을 등록해 보세요.</p>
			<% } %>
			<form action="<%=ctx%>/action" method="post" style="margin-top:18px;">
				<input type="hidden" name="cmd" value="checkoutStart">
				<button type="submit" class="btn btn-lg">주문하기</button>
			</form>
		</aside>
	</div>
	<% } %>
</section>
</main>
<jsp:include page="footer.jsp" />
</body>
</html>
