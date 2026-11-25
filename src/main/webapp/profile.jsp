<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.market.member.User, com.market.store.DataStore, com.market.util.Html" %>
<%
	String ctx = request.getContextPath();
	User loginUser = (User) session.getAttribute("loginUser");
	if (loginUser == null) {
		response.sendRedirect(ctx + "/login.jsp");
		return;
	}
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>마이페이지 · 거미줄 서점</title>
<link rel="stylesheet" href="<%=ctx%>/css/style.css">
</head>
<body>
<jsp:include page="header.jsp" />
<main class="page">
<section class="panel medium">
	<div class="page-head"><h1>마이페이지</h1><span class="count"><%= loginUser.isKakao() ? "카카오 회원 · " : "" %>가입일 <%=Html.esc(loginUser.getJoinedAt())%></span></div>

	<%-- ===== 쿠폰 ===== --%>
	<h2 class="section-h">쿠폰</h2>
	<% if (loginUser.getCouponCode() != null) { boolean used = loginUser.isCouponUsed(); %>
	<div class="coupon-ticket <%= used ? "used" : "" %>">
		<div class="c-amount"><b><%=Html.won(DataStore.COUPON_DISCOUNT)%></b><span>원 할인</span></div>
		<div class="c-info">
			<strong><%=Html.esc(DataStore.COUPON_NAME)%></strong>
			<span><%=Html.won(DataStore.COUPON_MIN_ORDER)%>원 이상 구매 시 · 회원 전용 · 코드 <%=Html.esc(loginUser.getCouponCode())%></span>
		</div>
		<span class="c-state"><%= used ? "사용 완료" : "사용 가능" %></span>
	</div>
	<% } else { %>
	<form action="<%=ctx%>/action" method="post" class="coupon-form">
		<input type="hidden" name="cmd" value="registerCoupon">
		<input type="text" name="couponCode" placeholder="쿠폰 코드를 입력하세요" maxlength="30" required autocomplete="off" aria-label="쿠폰 코드">
		<button type="submit" class="btn">쿠폰 등록</button>
	</form>
	<p class="muted" style="font-size:.82rem; margin-top:8px;">회원 전용 · <%=Html.won(DataStore.COUPON_MIN_ORDER)%>원 이상 구매 시 <%=Html.won(DataStore.COUPON_DISCOUNT)%>원 할인 · 1인 1회 사용</p>
	<% } %>
	<hr class="divider">

	<h2 class="section-h">회원 정보</h2>
	<form action="<%=ctx%>/action" method="post">
		<input type="hidden" name="cmd" value="updateProfile">
		<div class="form-group"><label>아이디</label><input type="text" value="<%=Html.esc(loginUser.getUserId())%>" readonly></div>
		<div class="grid-3" style="grid-template-columns:1fr 1fr;">
			<div class="form-group"><label for="n">이름</label><input type="text" id="n" name="name" value="<%=Html.esc(loginUser.getName())%>" required></div>
			<div class="form-group"><label for="p">연락처</label><input type="text" id="p" name="phone" value="<%=Html.esc(loginUser.getPhone())%>" required></div>
		</div>
		<% if (!loginUser.isKakao()) { %>
		<hr class="divider">
		<h2 class="section-h">비밀번호 변경 <small class="muted" style="font-family:var(--font); font-size:.8rem; font-weight:500;">(변경 시에만 입력)</small></h2>
		<div class="form-group"><label for="cp">현재 비밀번호</label><input type="password" id="cp" name="currentPassword" autocomplete="current-password"></div>
		<div class="grid-3" style="grid-template-columns:1fr 1fr;">
			<div class="form-group"><label for="np">새 비밀번호</label><input type="password" id="np" name="newPassword" autocomplete="new-password"></div>
			<div class="form-group"><label for="cf">새 비밀번호 확인</label><input type="password" id="cf" name="confirmPassword" autocomplete="new-password"></div>
		</div>
		<% } %>
		<div style="display:flex; gap:10px;">
			<a href="<%=ctx%>/index.jsp" class="btn btn-outline" style="flex:1;">취소</a>
			<button type="submit" class="btn" style="flex:1;">저장하기</button>
		</div>
	</form>

	<hr class="divider">
	<div class="danger-zone">
		<p>회원 탈퇴 시 계정과 모든 주문 내역이 영구 삭제되며 되돌릴 수 없습니다.</p>
		<form action="<%=ctx%>/action" method="post" onsubmit="return confirm('정말 탈퇴하시겠습니까? 계정과 모든 주문 내역이 영구 삭제됩니다.');">
			<input type="hidden" name="cmd" value="withdraw">
			<button type="submit" class="btn btn-danger">회원 탈퇴</button>
		</form>
	</div>
</section>
</main>
<jsp:include page="footer.jsp" />
</body>
</html>
