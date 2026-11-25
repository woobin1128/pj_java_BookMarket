<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>회원가입 · 거미줄 서점</title>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
<jsp:include page="header.jsp" />
<% String ctx = request.getContextPath(); %>
<main class="page">
<section class="panel narrow">
	<div class="auth-title">
		<img src="<%=ctx%>/images/shop.jpg" alt="">
		<h1>거미줄 서점 회원가입</h1>
		<p>가입 즉시 로그인되며 담아둔 장바구니는 유지됩니다.</p>
	</div>
	<form action="<%=ctx%>/action" method="post" id="regForm">
		<input type="hidden" name="cmd" value="register">
		<div class="form-group">
			<label for="userIdInput">아이디</label>
			<div class="input-row">
				<input type="text" id="userIdInput" name="userId" placeholder="아이디 입력" required autocomplete="username">
				<button type="button" class="btn btn-dark" onclick="checkDuplicateId()">중복확인</button>
			</div>
			<small id="idCheckMsg" class="hint" aria-live="polite"></small>
		</div>
		<div class="form-group">
			<label for="pw">비밀번호</label>
			<input type="password" id="pw" name="password" placeholder="비밀번호 입력" required autocomplete="new-password">
		</div>
		<div class="form-group">
			<label for="name">이름</label>
			<input type="text" id="name" name="name" placeholder="이름 입력" required autocomplete="name">
		</div>
		<div class="form-group">
			<label for="phone">연락처</label>
			<input type="text" id="phone" name="phone" placeholder="숫자만 입력 (01012345678)" inputmode="numeric" pattern="[0-9\-]{9,13}" title="숫자(와 하이픈)만 입력하세요" required autocomplete="tel">
		</div>
		<button type="submit" class="btn btn-lg">가입 완료</button>
	</form>
	<div class="or-divider"><span>또는</span></div>
	<a href="<%=ctx%>/kakao/login" class="btn-kakao" aria-label="카카오 계정으로 시작하기">
		<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 3C6.48 3 2 6.58 2 11c0 2.86 1.87 5.37 4.68 6.8l-.95 3.5c-.08.3.26.54.52.37l4.16-2.76c.52.06 1.04.09 1.59.09 5.52 0 10-3.58 10-8s-4.48-8-10-8z"/></svg>
		카카오로 3초 만에 시작하기
	</a>
	<div class="auth-links">
		<a href="<%=ctx%>/login.jsp">이미 계정이 있으신가요? 로그인</a>
		<a href="<%=ctx%>/index.jsp">메인으로</a>
	</div>
</section>
</main>
<jsp:include page="footer.jsp" />
<script>
function checkDuplicateId() {
	var id = document.getElementById('userIdInput').value.trim();
	var msg = document.getElementById('idCheckMsg');
	if (!id) { msg.style.color = '#b3261e'; msg.textContent = '아이디를 먼저 입력하세요.'; return; }
	fetch('<%=ctx%>/action?cmd=checkId&userId=' + encodeURIComponent(id))
		.then(function (r) { return r.json(); })
		.then(function (d) {
			msg.style.color = d.available ? '#25594a' : '#b3261e';
			msg.textContent = d.available ? '✓ 사용 가능한 아이디입니다.' : '이미 사용 중이거나 사용할 수 없는 아이디입니다.';
		})
		.catch(function () { msg.style.color = '#b3261e'; msg.textContent = '확인 중 오류가 발생했습니다.'; });
}
document.getElementById('userIdInput').addEventListener('input', function () { document.getElementById('idCheckMsg').textContent = ''; });
</script>
</body>
</html>
