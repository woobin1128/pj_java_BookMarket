package com.market.kakao;

import java.util.*;
import java.util.concurrent.*;

import com.market.bookitem.Book;

/** 홈 화면 분야(키워드) 목록과 카카오 도서 검색 결과 캐시. */
public class BookCatalog {
	public static final class Category {
		public final String label;   // 화면 표시명
		public final String query;   // 카카오 검색어
		Category(String label, String query) { this.label = label; this.query = query; }
	}

	/** 홈에 노출할 분야. 필요하면 여기만 고치면 됩니다. */
	public static final List<Category> CATEGORIES = List.of(
			new Category("소설", "소설"),
			new Category("시", "시집"),
			new Category("에세이", "에세이"),
			new Category("자기계발", "자기계발"),
			new Category("경제·경영", "경제 경영"),
			new Category("인문", "인문학"),
			new Category("역사", "역사"),
			new Category("과학", "과학"),
			new Category("IT·컴퓨터", "프로그래밍"),
			new Category("만화", "만화"),
			new Category("여행", "여행"),
			new Category("어린이", "어린이 동화"));

	private static final long TTL_MS = 30 * 60 * 1000L;

	private static class Entry {
		final KakaoClient.BookPage page;
		final long at = System.currentTimeMillis();
		Entry(KakaoClient.BookPage p) { this.page = p; }
	}

	private final KakaoClient client;
	private final Map<String, Entry> cache = new ConcurrentHashMap<>();
	private final ExecutorService pool = Executors.newFixedThreadPool(6, r -> {
		Thread t = new Thread(r, "kakao-books");
		t.setDaemon(true);
		return t;
	});

	public BookCatalog(KakaoClient client) { this.client = client; }

	public static Category findCategory(String label) {
		if (label == null) return null;
		for (Category c : CATEGORIES) if (c.label.equals(label)) return c;
		return null;
	}

	/** 분야 한 페이지. 실패 시 만료된 캐시라도 반환, 없으면 null. */
	public KakaoClient.BookPage fetch(String query, String label, String sort, int page, int size) {
		if (!client.enabled()) return null;
		String key = query + "|" + sort + "|" + page + "|" + size;
		Entry e = cache.get(key);
		if (e != null && System.currentTimeMillis() - e.at < TTL_MS) return e.page;
		try {
			KakaoClient.BookPage bp = client.searchBooks(query, label, sort, page, size);
			cache.put(key, new Entry(bp));
			return bp;
		} catch (Exception ex) {
			System.err.println("[kakao] 도서 검색 실패(" + query + "): " + ex.getMessage());
			return (e != null) ? e.page : null;
		}
	}

	/** 홈 화면용: 모든 분야를 병렬로 가져옵니다. 순서는 CATEGORIES 와 동일, 실패한 분야는 null. */
	public Map<Category, KakaoClient.BookPage> fetchHome(int perCategory) {
		Map<Category, Future<KakaoClient.BookPage>> futures = new LinkedHashMap<>();
		for (Category c : CATEGORIES) {
			futures.put(c, pool.submit(() -> fetch(c.query, c.label, "accuracy", 1, perCategory)));
		}
		Map<Category, KakaoClient.BookPage> out = new LinkedHashMap<>();
		for (Map.Entry<Category, Future<KakaoClient.BookPage>> en : futures.entrySet()) {
			try {
				out.put(en.getKey(), en.getValue().get(15, TimeUnit.SECONDS));
			} catch (Exception ex) {
				out.put(en.getKey(), null);
			}
		}
		return out;
	}
}
