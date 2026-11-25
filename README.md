<<<<<<< HEAD
# 거미줄 서점 (BookMarketWeb)

Tomcat 10.1 + JSP/Servlet 프로젝트. 이번 버전에서 추가된 기능:

1. **Supabase DB 연동** – 회원·장바구니·주문(주문 시각 포함)이 서버를 꺼도 유지됩니다.
2. **카카오 연동** – 카카오 계정 로그인 + 카카오(다음) 도서 검색 API로 홈 화면에 소설/시/에세이 등 분야별 도서 목록 표시.
3. **쿠폰** – 마이페이지에서 코드 `1234` 등록 → 회원 전용, 15,000원 이상 구매 시 3,000원 할인 (1인 1회).

## 설정 방법 (처음 한 번)

### A. Supabase
1. https://supabase.com 에서 프로젝트 생성
2. **SQL Editor** 에 `supabase_schema.sql` 전체를 붙여넣고 Run
3. **Project Settings > API** 에서 `Project URL` 과 `service_role` 키를 복사
4. `src/main/webapp/WEB-INF/config.properties` 의 `supabase.url`, `supabase.key` 에 입력
   - service_role 키는 서버 전용입니다. GitHub 등에 올리지 마세요.

### B. 카카오
1. https://developers.kakao.com > 내 애플리케이션 > 애플리케이션 추가
2. **앱 키 > REST API 키** 를 `kakao.rest.key` 에 입력 (로그인·도서 검색 공용)
3. **제품 설정 > 카카오 로그인** 을 *활성화* 하고 **Redirect URI** 에
   `http://localhost:8080/BookMarketWeb/kakao/callback` 등록 (프로젝트 이름/포트가 다르면 그에 맞게)
4. **동의항목** 에서 `닉네임` 을 사용 설정
5. (선택) 보안 > Client Secret 을 켰다면 `kakao.client.secret` 에 입력

> 설정 후 Tomcat 을 재시작하세요. 키가 비어 있으면 Supabase 는 기존 로컬 파일 저장소로,
> 카카오 도서는 “키 미설정” 안내와 함께 서점 소장 도서만 보여주도록 안전하게 동작합니다.
> 환경변수(`SUPABASE_URL`, `SUPABASE_KEY`, `KAKAO_REST_KEY` …)로도 설정할 수 있습니다.

## 동작 요약
- **저장**: 회원 가입/수정/탈퇴, 장바구니 담기·수량·삭제, 주문·취소, 쿠폰 등록·사용이 즉시 Supabase 에 기록됩니다. (비로그인 장바구니는 세션에만 있다가 로그인 시 병합)
- **기존 데이터 이전**: Supabase 가 비어 있고 기존 파일 저장소에 회원/주문이 있으면 첫 실행 때 자동 이전됩니다.
- **홈 화면**: 서점 소장 도서 + 12개 분야(소설·시·에세이·자기계발·경제경영·인문·역사·과학·IT·만화·여행·어린이)를 카카오 도서 검색으로 30분 캐시하여 표시. `더보기` 로 분야별 전체/페이지/정렬, 상단 검색은 소장 도서 + 카카오 검색 결과. 분야 변경은 `kakao/BookCatalog.java` 의 `CATEGORIES`.
- **쿠폰**: 코드·금액은 `DataStore` 상단 상수(`COUPON_CODE`, `COUPON_MIN_ORDER`, `COUPON_DISCOUNT`). 주문 취소 시 쿠폰이 복구됩니다.
- **주문 시각**: `yyyy-MM-dd HH:mm:ss` (한국 시간) 로 저장·표시.
=======
# pj_java_BookMarket
>>>>>>> 4de248d965101ea3ef62fc9b5cafb92595d5369a
