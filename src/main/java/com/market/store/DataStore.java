package com.market.store;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

import com.market.bookitem.Book;
import com.market.cart.Cart;
import com.market.cart.CartItem;
import com.market.config.AppConfig;
import com.market.kakao.BookCatalog;
import com.market.kakao.KakaoClient;
import com.market.member.Admin;
import com.market.member.User;
import com.market.order.Order;
import com.market.order.OrderItem;

public class DataStore {
	private static final DataStore INSTANCE = new DataStore();

	/** 회원 전용 쿠폰: 코드 / 최소 주문 금액 / 할인 금액 */
	public static final String COUPON_CODE = "1234";
	public static final String COUPON_NAME = "회원전용 3,000원 할인 쿠폰";
	public static final int COUPON_MIN_ORDER = 15000;
	public static final int COUPON_DISCOUNT = 3000;
	public static final String KAKAO_ID_PREFIX = "kakao_";

	private final List<Book> books = new ArrayList<>();
	private final Map<String, User> users = new LinkedHashMap<>();
	private final List<Order> orders = new ArrayList<>();
	private final Admin admin = new Admin();
	/** 카카오 도서 검색으로 가져온 도서(ID -> Book). 장바구니/주문 시 조회용. */
	private final Map<String, Book> externalBooks = new java.util.concurrent.ConcurrentHashMap<>();

	private final SupabaseRepo repo = new SupabaseRepo();
	private final KakaoClient kakao = new KakaoClient();
	private final BookCatalog catalog = new BookCatalog(kakao);

	private Path dataDir;
	private Path stateFile;
	private Path uploadDir;
	private boolean initialized = false;

	private DataStore() {}

	public static DataStore getInstance() {
		return INSTANCE;
	}

	public synchronized void init(String webappRealPath) {
		if (initialized) return;
		AppConfig.load(webappRealPath);
		this.dataDir = Paths.get(System.getProperty("user.home"), ".bookmarket_web_data");
		this.stateFile = dataDir.resolve("store_state.dat");
		this.uploadDir = dataDir.resolve("uploads");
		try {
			Files.createDirectories(dataDir);
			Files.createDirectories(uploadDir);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (Files.exists(stateFile)) {
			loadState();
		} else {
			loadSeedBooks(webappRealPath);
			saveState();
		}
		syncFromSupabase();
		initialized = true;
	}

	/** Supabase 가 설정되어 있으면 회원·장바구니·주문을 DB 기준으로 불러옵니다. (DB 가 비어 있으면 기존 파일 데이터를 이전) */
	private void syncFromSupabase() {
		if (!repo.enabled()) {
			System.out.println("[store] Supabase 미설정 → 로컬 파일 저장소를 사용합니다. (WEB-INF/config.properties 확인)");
			return;
		}
		try {
			SupabaseRepo.Snapshot snap = repo.loadAll();
			if (snap.users.isEmpty() && snap.orders.isEmpty() && (!users.isEmpty() || !orders.isEmpty())) {
				System.out.println("[store] Supabase 가 비어 있어 기존 회원/주문 데이터를 이전합니다.");
				for (User u : users.values()) {
					repo.upsertUser(u);
					repo.replaceCart(u.getUserId(), u.getSavedCart().getmCartItem());
				}
				for (Order o : orders) repo.insertOrder(o);
			} else {
				users.clear();
				users.putAll(snap.users);
				orders.clear();
				orders.addAll(snap.orders);
			}
			for (User u : users.values()) {
				for (CartItem ci : u.getSavedCart().getmCartItem()) registerExternal(ci.getItemBook());
			}
			System.out.println("[store] Supabase 연결 완료: 회원 " + users.size() + "명, 주문 " + orders.size() + "건");
		} catch (Exception e) {
			System.err.println("[store] Supabase 불러오기 실패 → 파일 저장소로 대체합니다: " + e.getMessage());
		}
	}

	public KakaoClient getKakao() { return kakao; }
	public BookCatalog getCatalog() { return catalog; }
	public boolean isSupabaseEnabled() { return repo.enabled(); }

	// ---------------------------------------------------------------- 외부(카카오) 도서
	public void registerExternal(Book b) {
		if (b != null && b.isExternal()) externalBooks.put(b.getBookId(), b);
	}

	public void registerExternal(Collection<Book> list) {
		for (Book b : list) registerExternal(b);
	}

	public Path getUploadDir() {
		return uploadDir;
	}

	private void loadSeedBooks(String webappRealPath) {
		books.clear();
		List<Path> candidates = new ArrayList<>();
		if (webappRealPath != null && !webappRealPath.isEmpty()) {
			candidates.add(Paths.get(webappRealPath, "WEB-INF", "data", "seed_book.txt"));
			candidates.add(Paths.get(webappRealPath, "WEB-INF", "data", "book.txt"));
			candidates.add(Paths.get(webappRealPath, "book.txt"));
		}
		candidates.add(Paths.get("src", "main", "webapp", "WEB-INF", "data", "book.txt"));

		Path seedPath = null;
		for (Path p : candidates) {
			if (p != null && Files.exists(p)) {
				seedPath = p;
				break;
			}
		}
		if (seedPath == null) return;

		try (BufferedReader reader = Files.newBufferedReader(seedPath, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) continue;
				if (line.contains("ISBN")) {
					String id = line;
					String name = readTrim(reader);
					int price = Integer.parseInt(readTrim(reader));
					String author = readTrim(reader);
					String desc = readTrim(reader);
					String category = readTrim(reader);
					String releaseDate = readTrim(reader);
					// 기존 BookMarketSwing 도서는 기본적으로 '도서 목록'에만 표시
					Book b = new Book(id, name, price, author, desc, category, releaseDate, id + ".jpg");
					books.add(b);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String readTrim(BufferedReader r) throws IOException {
		String s = r.readLine();
		return (s == null) ? "" : s.trim();
	}

	@SuppressWarnings("unchecked")
	private synchronized void loadState() {
		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(stateFile))) {
			List<Book> savedBooks = (List<Book>) ois.readObject();
			Map<String, User> savedUsers = (Map<String, User>) ois.readObject();
			List<Order> savedOrders = (List<Order>) ois.readObject();
			books.clear();
			books.addAll(savedBooks);
			users.clear();
			users.putAll(savedUsers);
			orders.clear();
			orders.addAll(savedOrders);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void saveState() {
		if (stateFile == null) return;
		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(stateFile))) {
			oos.writeObject(new ArrayList<>(books));
			oos.writeObject(new LinkedHashMap<>(users));
			oos.writeObject(new ArrayList<>(orders));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String sha256(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			return raw;
		}
	}

	public synchronized boolean isUserIdAvailable(String userId) {
		if (userId == null || userId.trim().isEmpty()) return false;
		String id = userId.trim();
		if (admin.getId().equalsIgnoreCase(id)) return false;
		if (id.toLowerCase().startsWith(KAKAO_ID_PREFIX)) return false;   // 카카오 회원 전용 접두어
		return !users.containsKey(id);
	}

	public synchronized User registerUser(String userId, String rawPassword, String name, String phone) {
		if (!isUserIdAvailable(userId)) return null;
		String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		User user = new User(userId.trim(), sha256(rawPassword), name.trim(), phone.trim(), today);
		users.put(user.getUserId(), user);
		saveState();
		repo.upsertUser(user);
		return user;
	}

	/** 카카오 로그인: 이미 가입된 카카오 회원이면 반환, 아니면 새로 만듭니다. */
	public synchronized User findOrCreateKakaoUser(String kakaoId, String nickname) {
		String id = KAKAO_ID_PREFIX + kakaoId;
		User u = users.get(id);
		if (u != null) return u;
		String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		u = new User(id, "", nickname, "", today);
		u.setProvider("kakao");
		users.put(id, u);
		saveState();
		repo.upsertUser(u);
		return u;
	}

	public synchronized User authenticateUser(String userId, String rawPassword) {
		if (userId == null || rawPassword == null) return null;
		User u = users.get(userId.trim());
		if (u != null && !u.isKakao() && u.getPasswordHash().equals(sha256(rawPassword))) {
			return u;
		}
		return null;
	}

	public boolean authenticateAdmin(String id, String code) {
		return id != null && code != null
				&& admin.getId().equals(id.trim())
				&& admin.getPassword().equals(code.trim());
	}

	public Admin getAdmin() {
		return admin;
	}

	public synchronized User getUser(String userId) {
		return users.get(userId);
	}

	public synchronized List<User> getAllUsers() {
		return new ArrayList<>(users.values());
	}

	public synchronized boolean updateUser(String userId, String newName, String newPhone, String currentPw, String newPw) {
		User u = users.get(userId);
		if (u == null) return false;
		if (!u.isKakao() && newPw != null && !newPw.trim().isEmpty()) {
			if (currentPw == null || !u.getPasswordHash().equals(sha256(currentPw))) {
				return false;
			}
			u.setPasswordHash(sha256(newPw.trim()));
		}
		u.setName(newName.trim());
		u.setPhone(newPhone.trim());
		saveState();
		repo.upsertUser(u);
		return true;
	}

	// 탈퇴 시 계정 정보 + 해당 회원의 모든 주문 내역(사용자/관리자 모두) 삭제 -> 동일 아이디 재가입 가능
	public synchronized void deleteUserAndOrders(String userId) {
		users.remove(userId);
		orders.removeIf(o -> o.getUserId().equals(userId));
		saveState();
		repo.deleteUser(userId);
	}

	// ---------------------------------------------------------------- 장바구니 저장
	/** 로그인 회원의 장바구니를 영구 저장(파일 + Supabase). */
	public synchronized void saveCart(User user) {
		if (user == null) return;
		saveState();
		repo.replaceCart(user.getUserId(), new ArrayList<>(user.getSavedCart().getmCartItem()));
	}

	// ---------------------------------------------------------------- 쿠폰
	public enum CouponResult { OK, INVALID, ALREADY_REGISTERED }

	/** 마이페이지에서 쿠폰 코드 등록 (회원 전용, 1인 1회) */
	public synchronized CouponResult registerCoupon(User user, String code) {
		if (code == null || !COUPON_CODE.equals(code.trim())) return CouponResult.INVALID;
		if (user.getCouponCode() != null) return CouponResult.ALREADY_REGISTERED;
		user.setCouponCode(COUPON_CODE);
		user.setCouponUsed(false);
		saveState();
		repo.upsertUser(user);
		return CouponResult.OK;
	}

	/** 이 회원이 이번 주문 금액에 쿠폰을 쓸 수 있으면 할인액, 아니면 0 */
	public int couponDiscountFor(User user, int orderTotal) {
		if (user != null && user.hasUsableCoupon() && orderTotal >= COUPON_MIN_ORDER) return COUPON_DISCOUNT;
		return 0;
	}

	// 도서 조회 (탭: ALL, BEST, EVENT, NEW / 검색: 탭 관계없이 전체 도서에서 도서명 or 저자 검색)
	public synchronized List<Book> getBooks(String tab, String keyword) {
		List<Book> result = new ArrayList<>();
		if (keyword != null && !keyword.trim().isEmpty()) {
			String kw = keyword.trim().toLowerCase();
			for (Book b : books) {
				if ((b.getName() != null && b.getName().toLowerCase().contains(kw))
						|| (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(kw))) {
					result.add(b);
				}
			}
			return result;
		}
		String targetTab = (tab == null || tab.isEmpty()) ? "ALL" : tab.toUpperCase();
		for (Book b : books) {
			if ("ALL".equals(targetTab) || b.hasTab(targetTab)) {
				result.add(b);
			}
		}
		return result;
	}

	public synchronized List<Book> getAllBooks() {
		return new ArrayList<>(books);
	}

	public synchronized Book getBookById(String bookId) {
		for (Book b : books) {
			if (b.getBookId().equals(bookId)) return b;
		}
		return (bookId == null) ? null : externalBooks.get(bookId);
	}

	// 중복 도서 검사 (도서 ID 또는 동일 도서명+저자)
	public synchronized boolean isDuplicateBook(String bookId, String name, String author) {
		for (Book b : books) {
			if (bookId != null && !bookId.trim().isEmpty() && b.getBookId().equalsIgnoreCase(bookId.trim())) {
				return true;
			}
			if (b.getName().equalsIgnoreCase(name.trim()) && b.getAuthor().equalsIgnoreCase(author.trim())) {
				return true;
			}
		}
		return false;
	}

	// 새 도서 추가 시 '도서 목록(ALL)'과 '신간 도서(NEW)'에 동시 등록
	public synchronized boolean addBook(Book book) {
		if (isDuplicateBook(book.getBookId(), book.getName(), book.getAuthor())) {
			return false;
		}
		book.addTab("NEW");
		books.add(book);
		saveState();
		return true;
	}

	public synchronized void updateBook(String bookId, String name, int price, String author, String desc,
			String category, String releaseDate, String newImageFileName, Set<String> tabs) {
		Book b = getBookById(bookId);
		if (b == null) return;
		b.setName(name);
		b.setUnitPrice(price);
		b.setAuthor(author);
		b.setDescription(desc);
		b.setCategory(category);
		b.setReleaseDate(releaseDate);
		if (newImageFileName != null && !newImageFileName.isEmpty()) {
			b.setImageFileName(newImageFileName);
		}
		b.setTabs(tabs);
		// 모든 사용자 저장 장바구니의 도서 단가/정보 동기화
		for (User u : users.values()) {
			for (CartItem ci : u.getSavedCart().getmCartItem()) {
				if (ci.getBookID().equals(bookId)) {
					ci.setItemBook(b);
					ci.updateTotalPrice();
				}
			}
		}
		saveState();
		for (User u : users.values()) repo.replaceCart(u.getUserId(), new ArrayList<>(u.getSavedCart().getmCartItem()));
	}

	// 도서 삭제 시 저장된 회원 장바구니에서도 제거 (이미 완료된 주문 내역은 유지)
	public synchronized void deleteBook(String bookId) {
		books.removeIf(b -> b.getBookId().equals(bookId));
		for (User u : users.values()) {
			u.getSavedCart().removeByBookId(bookId);
			repo.replaceCart(u.getUserId(), new ArrayList<>(u.getSavedCart().getmCartItem()));
		}
		saveState();
	}

	// 장바구니 내 삭제된 도서 정리 및 안내 메시지 반환
	public synchronized List<String> cleanRemovedBooksFromCart(Cart cart) {
		List<String> removedNames = new ArrayList<>();
		if (cart == null) return removedNames;
		Iterator<CartItem> it = cart.getmCartItem().iterator();
		while (it.hasNext()) {
			CartItem item = it.next();
			Book live = getBookById(item.getBookID());
			if (live == null && item.getItemBook() != null && item.getItemBook().isExternal()) {
				registerExternal(item.getItemBook());   // 카카오 도서는 담을 때의 정보를 유지
			} else if (live == null) {
				removedNames.add(item.getItemBook() != null ? item.getItemBook().getName() : item.getBookID());
				it.remove();
			} else {
				item.setItemBook(live);
				item.updateTotalPrice();
			}
		}
		return removedNames;
	}

	public synchronized Order createOrder(User user, Cart cart, String recipientName, String recipientPhone,
			String shippingDate, String shippingAddress, String contextPath, boolean useCoupon) {
		List<OrderItem> orderItems = new ArrayList<>();
		for (CartItem ci : cart.getmCartItem()) {
			Book b = ci.getItemBook();
			orderItems.add(new OrderItem(
					ci.getBookID(),
					b.getName(),
					b.getUnitPrice(),
					ci.getQuantity(),
					b.getImageWebPath(contextPath)));
		}
		String orderId = "ORD-" + System.currentTimeMillis();
		String now = java.time.ZonedDateTime.now(SupabaseRepo.KST).format(SupabaseRepo.KST_FMT);   // 주문 시각(KST, 초 단위)
		Order order = new Order(orderId, user.getUserId(), user.getName(), recipientName, recipientPhone,
				shippingDate, shippingAddress, now, orderItems);
		int discount = useCoupon ? couponDiscountFor(user, order.getGrandTotal()) : 0;
		if (discount > 0) {
			order.setCouponCode(user.getCouponCode());
			order.setDiscount(discount);
			user.setCouponUsed(true);
		}
		orders.add(0, order);
		cart.deleteBook();
		user.getSavedCart().deleteBook();
		saveState();
		repo.insertOrder(order);
		repo.replaceCart(user.getUserId(), new ArrayList<>());
		if (discount > 0) repo.upsertUser(user);
		return order;
	}

	public synchronized List<Order> getOrdersByUser(String userId) {
		List<Order> list = new ArrayList<>();
		for (Order o : orders) {
			if (o.getUserId().equals(userId)) list.add(o);
		}
		return list;
	}

	public synchronized List<Order> getAllOrders() {
		return new ArrayList<>(orders);
	}

	// 개별 주문 카드 한 건 전체 취소 -> 목록에서 즉시 삭제 (누적 금액 자동 차감)
	public synchronized void cancelOrder(String orderId, String userIdOrNull) {
		Order target = null;
		for (Order o : orders) {
			if (o.getOrderId().equals(orderId) && (userIdOrNull == null || o.getUserId().equals(userIdOrNull))) {
				target = o;
				break;
			}
		}
		if (target == null) return;
		orders.remove(target);
		// 취소된 주문에 쓴 쿠폰은 되돌려 줍니다.
		if (target.getCouponCode() != null) {
			User owner = users.get(target.getUserId());
			if (owner != null && owner.isCouponUsed()) {
				owner.setCouponUsed(false);
				repo.upsertUser(owner);
			}
		}
		saveState();
		repo.deleteOrder(orderId);
	}
}