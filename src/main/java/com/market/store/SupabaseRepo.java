package com.market.store;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.market.bookitem.Book;
import com.market.cart.CartItem;
import com.market.config.AppConfig;
import com.market.member.User;
import com.market.order.Order;
import com.market.order.OrderItem;
import com.market.util.Json;

/**
 * Supabase(PostgREST) 저장소. 별도 JDBC 드라이버 없이 HTTPS REST 로 app_users / cart_items / orders / order_items 를 읽고 씁니다.
 * 설정(supabase.url, supabase.key)이 없으면 enabled()==false 이고 모든 호출은 무시됩니다.
 */
public class SupabaseRepo {
	public static final ZoneId KST = ZoneId.of("Asia/Seoul");
	public static final DateTimeFormatter KST_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final String baseUrl;
	private final String key;
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

	public SupabaseRepo() {
		String url = AppConfig.get("supabase.url");
		while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
		this.baseUrl = url;
		this.key = AppConfig.get("supabase.key");
	}

	public boolean enabled() {
		return !baseUrl.isEmpty() && !key.isEmpty();
	}

	// ------------------------------------------------------------------ HTTP
	private HttpResponse<String> send(String method, String path, String body, String prefer) throws Exception {
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + "/rest/v1/" + path))
				.timeout(Duration.ofSeconds(12))
				.header("apikey", key)
				.header("Authorization", "Bearer " + key)
				.header("Content-Type", "application/json; charset=utf-8")
				.header("Accept", "application/json");
		if (prefer != null) b.header("Prefer", prefer);
		HttpRequest.BodyPublisher pub = (body == null) ? HttpRequest.BodyPublishers.noBody()
				: HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
		b.method(method, pub);
		HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (r.statusCode() / 100 != 2) {
			throw new RuntimeException("Supabase " + method + " " + path + " -> HTTP " + r.statusCode() + " " + r.body());
		}
		return r;
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private List<Object> getList(String path) throws Exception {
		return Json.parseArray(send("GET", path, null, null).body());
	}

	/** 쓰기 실패가 사용자 요청을 깨뜨리지 않도록 로그만 남깁니다. */
	private void safely(String what, ThrowingRunnable r) {
		if (!enabled()) return;
		try {
			r.run();
		} catch (Exception e) {
			System.err.println("[supabase] " + what + " 실패: " + e.getMessage());
		}
	}

	private interface ThrowingRunnable { void run() throws Exception; }

	// ------------------------------------------------------------------ 읽기
	public static class Snapshot {
		public final Map<String, User> users = new LinkedHashMap<>();
		public final List<Order> orders = new ArrayList<>();
	}

	/** 서버 시작 시 회원/장바구니/주문 전체 로드. 실패하면 예외(호출 측에서 파일 저장소로 대체). */
	public Snapshot loadAll() throws Exception {
		Snapshot snap = new Snapshot();

		for (Object o : getList("app_users?select=*&order=created_at.asc")) {
			User u = new User(Json.str(o, "user_id"), nz(Json.str(o, "password_hash")), nz(Json.str(o, "name")),
					nz(Json.str(o, "phone")), nz(Json.str(o, "join_date")));
			u.setAddress(Json.str(o, "address"));
			u.setProvider(Json.str(o, "provider"));
			u.setCouponCode(Json.str(o, "coupon_code"));
			u.setCouponUsed(Json.bool(o, "coupon_used"));
			snap.users.put(u.getUserId(), u);
		}

		for (Object o : getList("cart_items?select=*&order=added_at.asc")) {
			User u = snap.users.get(Json.str(o, "user_id"));
			if (u == null) continue;
			Book b = new Book(Json.str(o, "book_id"), nz(Json.str(o, "title")), Json.num(o, "price", 0),
					nz(Json.str(o, "author")), nz(Json.str(o, "description")), nz(Json.str(o, "category")),
					nz(Json.str(o, "release_date")), nz(Json.str(o, "image")));
			CartItem ci = new CartItem(b, Math.max(1, Json.num(o, "quantity", 1)));
			u.getSavedCart().getmCartItem().add(ci);
		}

		Map<String, Order> byId = new LinkedHashMap<>();
		for (Object o : getList("orders?select=*&order=ordered_at.desc,order_id.desc")) {
			Order ord = new Order(Json.str(o, "order_id"), Json.str(o, "user_id"), nz(Json.str(o, "user_name")),
					nz(Json.str(o, "recipient_name")), nz(Json.str(o, "recipient_phone")),
					nz(Json.str(o, "shipping_date")), nz(Json.str(o, "shipping_address")),
					toKst(Json.str(o, "ordered_at")), new ArrayList<>());
			ord.setStatus(Json.str(o, "status"));
			ord.setCouponCode(Json.str(o, "coupon_code"));
			ord.setDiscount(Json.num(o, "discount", 0));
			byId.put(ord.getOrderId(), ord);
			snap.orders.add(ord);
		}
		for (Object o : getList("order_items?select=*&order=id.asc")) {
			Order ord = byId.get(Json.str(o, "order_id"));
			if (ord == null) continue;
			ord.getOrderItems().add(new OrderItem(Json.str(o, "book_id"), nz(Json.str(o, "title")),
					Json.num(o, "price", 0), Json.num(o, "quantity", 1), nz(Json.str(o, "image"))));
		}
		return snap;
	}

	// ------------------------------------------------------------------ 쓰기
	public void upsertUser(User u) {
		safely("회원 저장", () -> {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("user_id", u.getUserId());
			row.put("password_hash", u.getPasswordHash());
			row.put("name", u.getName());
			row.put("phone", u.getPhone() == null ? "" : u.getPhone());
			row.put("address", u.getAddress());
			row.put("join_date", u.getJoinedAt());
			row.put("provider", u.getProvider());
			row.put("coupon_code", u.getCouponCode());
			row.put("coupon_used", u.isCouponUsed());
			send("POST", "app_users?on_conflict=user_id", Json.write(List.of(row)),
					"resolution=merge-duplicates,return=minimal");
		});
	}

	/** 회원의 장바구니를 통째로 교체(삭제 후 재삽입). */
	public void replaceCart(String userId, List<CartItem> items) {
		safely("장바구니 저장", () -> {
			send("DELETE", "cart_items?user_id=eq." + enc(userId), null, "return=minimal");
			if (items.isEmpty()) return;
			List<Map<String, Object>> rows = new ArrayList<>();
			for (CartItem ci : items) {
				Book b = ci.getItemBook();
				if (b == null) continue;
				Map<String, Object> r = new LinkedHashMap<>();
				r.put("user_id", userId);
				r.put("book_id", ci.getBookID());
				r.put("title", b.getName());
				r.put("author", b.getAuthor());
				r.put("price", b.getUnitPrice());
				r.put("image", b.getImageFileName());
				r.put("category", b.getCategory());
				r.put("description", b.getDescription());
				r.put("release_date", b.getReleaseDate());
				r.put("quantity", ci.getQuantity());
				rows.add(r);
			}
			if (!rows.isEmpty()) send("POST", "cart_items", Json.write(rows), "return=minimal");
		});
	}

	public void insertOrder(Order o) {
		safely("주문 저장", () -> {
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("order_id", o.getOrderId());
			r.put("user_id", o.getUserId());
			r.put("user_name", o.getUserName());
			r.put("recipient_name", o.getRecipientName());
			r.put("recipient_phone", o.getRecipientPhone());
			r.put("shipping_date", o.getShippingDate());
			r.put("shipping_address", o.getShippingAddress());
			r.put("ordered_at", toIso(o.getOrderDate()));
			r.put("status", o.getStatus());
			r.put("coupon_code", o.getCouponCode());
			r.put("discount", o.getDiscount());
			r.put("total_amount", o.getTotalAmount());
			send("POST", "orders", Json.write(List.of(r)), "return=minimal");

			List<Map<String, Object>> rows = new ArrayList<>();
			for (OrderItem oi : o.getOrderItems()) {
				Map<String, Object> x = new LinkedHashMap<>();
				x.put("order_id", o.getOrderId());
				x.put("book_id", oi.getBookId());
				x.put("title", oi.getBookName());
				x.put("price", oi.getUnitPrice());
				x.put("quantity", oi.getQuantity());
				x.put("image", oi.getImageWebPath());
				rows.add(x);
			}
			if (!rows.isEmpty()) send("POST", "order_items", Json.write(rows), "return=minimal");
		});
	}

	public void deleteOrder(String orderId) {
		safely("주문 삭제", () -> send("DELETE", "orders?order_id=eq." + enc(orderId), null, "return=minimal"));
	}

	public void deleteUser(String userId) {
		safely("회원 삭제", () -> {
			send("DELETE", "orders?user_id=eq." + enc(userId), null, "return=minimal");
			send("DELETE", "app_users?user_id=eq." + enc(userId), null, "return=minimal");
		});
	}

	// ------------------------------------------------------------------ 시간 변환
	private static String nz(String s) {
		return s == null ? "" : s;
	}

	/** "yyyy-MM-dd HH:mm:ss"(KST) -> "2026-11-25T17:50:00+09:00" */
	static String toIso(String kst) {
		try {
			return java.time.LocalDateTime.parse(kst, KST_FMT).atZone(KST).toOffsetDateTime().toString();
		} catch (Exception e) {
			return OffsetDateTime.now(KST).toString();
		}
	}

	/** Supabase timestamptz 문자열 -> "yyyy-MM-dd HH:mm:ss"(KST) */
	static String toKst(String iso) {
		if (iso == null) return "";
		try {
			return OffsetDateTime.parse(iso).atZoneSameInstant(KST).format(KST_FMT);
		} catch (Exception e) {
			return iso;
		}
	}
}
