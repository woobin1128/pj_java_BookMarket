<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>로그인 · 거미줄 서점</title>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
<jsp:include page="header.jsp" />
<% String ctx = request.getContextPath(); %>
<main class="page">
<section class="panel narrow">
	<div class="auth-title">
		<img src="<%=ctx%>/images/shop.jpg" alt="">
		<h1>다시 오셨군요</h1>
		<p>로그인하고 장바구니와 주문 내역을 확인하세요.</p>
	</div>
	<form action="<%=ctx%>/action" method="post">
		<input type="hidden" name="cmd" value="login">
		<div class="form-group">
			<label for="userId">아이디</label>
			<input type="text" id="userId" name="userId" placeholder="아이디 입력" required autofocus autocomplete="username">
		</div>
		<div class="form-group">
			<label for="pw">비밀번호</label>
			<input type="password" id="pw" name="password" placeholder="비밀번호 입력" required autocomplete="current-password">
		</div>
		<button type="submit" class="btn btn-lg">로그인</button>
	</form>
	<div class="or-divider"><span>또는</span></div>
	<a href="<%=ctx%>/kakao/login" class="btn-kakao" aria-label="카카오 계정으로 로그인">
		<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 3C6.48 3 2 6.58 2 11c0 2.86 1.87 5.37 4.68 6.8l-.95 3.5c-.08.3.26.54.52.37l4.16-2.76c.52.06 1.04.09 1.59.09 5.52 0 10-3.58 10-8s-4.48-8-10-8z"/></svg>
		카카오로 3초 만에 시작하기
	</a>
	<div class="auth-links">
		<a href="<%=ctx%>/register.jsp">아직 회원이 아니신가요? 회원가입</a>
		<a href="<%=ctx%>/index.jsp">메인으로</a>
	</div>
</section>
</main>
<jsp:include page="footer.jsp" />
</body>
</html>
