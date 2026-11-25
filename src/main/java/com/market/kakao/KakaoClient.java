package com.market.kakao;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import com.market.bookitem.Book;
import com.market.config.AppConfig;
import com.market.util.Json;

/**
 * 카카오 API 클라이언트.
 *  - 카카오 로그인(OAuth 2.0): kauth.kakao.com / kapi.kakao.com
 *  - 다음 책 검색 API: dapi.kakao.com/v3/search/book  (REST API 키 사용)
 */
public class KakaoClient {
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

	public String restKey() { return AppConfig.get("kakao.rest.key"); }
	public boolean enabled() { return !restKey().isEmpty(); }

	private String authBase() { return AppConfig.get("kakao.auth.base", "https://kauth.kakao.com"); }
	private String apiBase() { return AppConfig.get("kakao.api.base", "https://kapi.kakao.com"); }
	private String searchBase() { return AppConfig.get("kakao.search.base", "https://dapi.kakao.com"); }

	private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

	// ------------------------------------------------------------------ 카카오 로그인
	public String authorizeUrl(String redirectUri, String state) {
		return authBase() + "/oauth/authorize?response_type=code&client_id=" + enc(restKey())
				+ "&redirect_uri=" + enc(redirectUri) + "&state=" + enc(state)
				+ "&prompt=login";   // 카카오 세션이 남아 있어도 항상 ID/비밀번호 입력 화면을 표시
	}

	/** 인가 코드 -> 액세스 토큰 */
	public String exchangeToken(String code, String redirectUri) throws Exception {
		StringBuilder form = new StringBuilder("grant_type=authorization_code&client_id=" + enc(restKey())
				+ "&redirect_uri=" + enc(redirectUri) + "&code=" + enc(code));
		String secret = AppConfig.get("kakao.client.secret");
		if (!secret.isEmpty()) form.append("&client_secret=").append(enc(secret));
		HttpRequest req = HttpRequest.newBuilder(URI.create(authBase() + "/oauth/token"))
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
				.POST(HttpRequest.BodyPublishers.ofString(form.toString())).build();
		HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		Map<String, Object> j = Json.parseObject(r.body());
		String token = Json.str(j, "access_token");
		if (r.statusCode() / 100 != 2 || token == null) {
			throw new RuntimeException("카카오 토큰 발급 실패: HTTP " + r.statusCode() + " " + r.body());
		}
		return token;
	}

	public static class KakaoUser {
		public String id;
		public String nickname;
	}

	/** 액세스 토큰 -> 회원 정보(고유 id, 닉네임) */
	public KakaoUser fetchUser(String accessToken) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create(apiBase() + "/v2/user/me"))
				.timeout(Duration.ofSeconds(10))
				.header("Authorization", "Bearer " + accessToken).GET().build();
		HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		Map<String, Object> j = Json.parseObject(r.body());
		String id = Json.str(j, "id");
		if (r.statusCode() / 100 != 2 || id == null) {
			throw new RuntimeException("카카오 사용자 조회 실패: HTTP " + r.statusCode() + " " + r.body());
		}
		KakaoUser u = new KakaoUser();
		u.id = id;
		String nick = Json.str(Json.obj(Json.obj(j, "kakao_account"), "profile"), "nickname");
		if (nick == null) nick = Json.str(Json.obj(j, "properties"), "nickname");
		u.nickname = (nick == null || nick.trim().isEmpty()) ? "카카오회원" : nick.trim();
		return u;
	}

	// ------------------------------------------------------------------ 도서 검색
	public static class BookPage {
		public final List<Book> books = new ArrayList<>();
		public boolean isEnd = true;
		public int pageableCount = 0;
	}

	/**
	 * @param sort "accuracy"(정확도) | "latest"(발간일)
	 * @param category 화면에 표시할 분야명 (Book.category 로 저장)
	 */
	public BookPage searchBooks(String query, String category, String sort, int page, int size) throws Exception {
		String url = searchBase() + "/v3/search/book?query=" + enc(query) + "&sort=" + enc(sort)
				+ "&page=" + page + "&size=" + size;
		HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
				.header("Authorization", "KakaoAK " + restKey()).GET().build();
		HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (r.statusCode() / 100 != 2) {
			throw new RuntimeException("카카오 도서 검색 실패: HTTP " + r.statusCode() + " " + r.body());
		}
		Map<String, Object> j = Json.parseObject(r.body());
		BookPage bp = new BookPage();
		Map<String, Object> meta = Json.obj(j, "meta");
		bp.isEnd = Json.bool(meta, "is_end");
		bp.pageableCount = Json.num(meta, "pageable_count", 0);
		Set<String> seen = new HashSet<>();
		for (Object d : Json.list(j, "documents")) {
			Book b = toBook(d, category);
			if (b != null && seen.add(b.getBookId())) bp.books.add(b);
		}
		return bp;
	}

	static Book toBook(Object d, String category) {
		String title = Json.str(d, "title");
		if (title == null || title.trim().isEmpty()) return null;
		// isbn 은 "ISBN10 ISBN13" 형태 -> ISBN13 우선
		String isbnRaw = Json.str(d, "isbn");
		String isbn = "";
		if (isbnRaw != null) {
			for (String part : isbnRaw.trim().split("\\s+")) {
				if (!part.isEmpty() && (isbn.isEmpty() || part.length() == 13)) isbn = part;
			}
		}
		if (isbn.isEmpty()) return null;
		int price = Json.num(d, "sale_price", -1);
		if (price <= 0) price = Json.num(d, "price", 0);
		if (price <= 0) return null;   // 가격 미정(절판 등) 도서는 판매 불가

		StringBuilder authors = new StringBuilder();
		for (Object a : Json.list(d, "authors")) {
			if (authors.length() > 0) authors.append(", ");
			authors.append(a);
		}
		String author = authors.length() > 0 ? authors.toString() : "저자 미상";
		String publisher = Json.str(d, "publisher");
		String contents = Json.str(d, "contents");
		String dt = Json.str(d, "datetime");
		String release = (dt != null && dt.length() >= 10) ? dt.substring(0, 10) : "";
		String thumb = Json.str(d, "thumbnail");

		Book b = new Book(Book.EXTERNAL_PREFIX + isbn, title.trim(), price, author,
				(contents == null || contents.isEmpty()) ? "" : contents.trim() + (contents.length() >= 90 ? "…" : ""),
				category, release, thumb == null ? "" : thumb);
		b.setPublisher(publisher);
		return b;
	}
}