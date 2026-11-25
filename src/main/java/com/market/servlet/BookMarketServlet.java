package com.market.servlet;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

import com.market.bookitem.Book;
import com.market.cart.Cart;
import com.market.kakao.KakaoClient;
import com.market.member.User;
import com.market.order.Order;
import com.market.store.DataStore;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet(urlPatterns = { "/action", "/uploads/*", "/images/*", "/kakao/*" })
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 10 * 1024 * 1024)
public class BookMarketServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		DataStore.getInstance().init(getServletContext().getRealPath("/"));
	}

	private Cart getSessionCart(HttpSession session) {
		Cart cart = (Cart) session.getAttribute("cart");
		if (cart == null) {
			cart = new Cart();
			session.setAttribute("cart", cart);
		}
		return cart;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DataStore store = DataStore.getInstance();
		store.init(getServletContext().getRealPath("/"));

		// 업로드된 도서 표지 이미지 서빙
		if ("/uploads".equals(req.getServletPath())) {
			String pathInfo = req.getPathInfo();
			if (pathInfo != null && pathInfo.length() > 1) {
				Path imgPath = store.getUploadDir().resolve(pathInfo.substring(1)).normalize();
				if (imgPath.startsWith(store.getUploadDir()) && Files.exists(imgPath)) {
					String mime = getServletContext().getMimeType(imgPath.toString());
					resp.setContentType(mime != null ? mime : "image/jpeg");
					Files.copy(imgPath, resp.getOutputStream());
					return;
				}
			}
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		if ("/images".equals(req.getServletPath())) {
			String pathInfo = req.getPathInfo();
			if (pathInfo != null && pathInfo.length() > 1) {
				String realImagesDir = getServletContext().getRealPath("/images");
				if (realImagesDir != null) {
					Path baseDir = Paths.get(realImagesDir).normalize();
					Path imgPath = baseDir.resolve(pathInfo.substring(1)).normalize();
					if (imgPath.startsWith(baseDir) && Files.exists(imgPath)) {
						String mime = getServletContext().getMimeType(imgPath.toString());
						resp.setContentType(mime != null ? mime : "image/jpeg");
						Files.copy(imgPath, resp.getOutputStream());
						return;
					}
				}
			}
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String cmd = req.getParameter("cmd");
		HttpSession session = req.getSession();

		if ("/kakao".equals(req.getServletPath())) {
			handleKakao(req, resp, session, store);
			return;
		}

		if ("checkId".equals(cmd)) {
			String userId = req.getParameter("userId");
			boolean ok = store.isUserIdAvailable(userId);
			resp.setContentType("application/json; charset=UTF-8");
			resp.getWriter().write("{\"available\":" + ok + "}");
			return;
		}

		if ("logout".equals(cmd)) {
			session.removeAttribute("loginUser");
			session.removeAttribute("isAdmin");
			session.setAttribute("cart", new Cart());
			session.setAttribute("flashMsg", "로그아웃되었습니다.");
			resp.sendRedirect(req.getContextPath() + "/index.jsp");
			return;
		}

		resp.sendRedirect(req.getContextPath() + "/index.jsp");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		DataStore store = DataStore.getInstance();
		store.init(getServletContext().getRealPath("/"));

		String cmd = req.getParameter("cmd");
		HttpSession session = req.getSession();
		String ctx = req.getContextPath();

		if ("register".equals(cmd)) {
			String userId = req.getParameter("userId");
			String pw = req.getParameter("password");
			String name = req.getParameter("name");
			String phone = req.getParameter("phone");

			if (userId == null || userId.trim().isEmpty() || pw == null || pw.trim().isEmpty()
					|| name == null || name.trim().isEmpty() || phone == null || phone.trim().isEmpty()) {
				session.setAttribute("flashError", "모든 항목을 입력해 주세요.");
				resp.sendRedirect(ctx + "/register.jsp");
				return;
			}
			User created = store.registerUser(userId, pw, name, phone);
			if (created == null) {
				session.setAttribute("flashError", "이미 사용 중이거나 사용할 수 없는 아이디입니다.");
				resp.sendRedirect(ctx + "/register.jsp");
				return;
			}
			// 회원가입 완료 시 즉시 로그인 + 비로그인 임시 장바구니 유지
			Cart guestCart = getSessionCart(session);
			created.getSavedCart().mergeFrom(guestCart);
			store.saveCart(created);
			session.setAttribute("loginUser", created);
			session.setAttribute("cart", created.getSavedCart());
			session.removeAttribute("isAdmin");

			String redirectAfter = (String) session.getAttribute("redirectAfterLogin");
			session.removeAttribute("redirectAfterLogin");
			session.setAttribute("flashMsg", created.getName() + "님, 회원가입 및 로그인이 완료되었습니다.");
			resp.sendRedirect(redirectAfter != null ? redirectAfter : (ctx + "/index.jsp"));
			return;
		}

		if ("login".equals(cmd)) {
			String userId = req.getParameter("userId");
			String pw = req.getParameter("password");

			// 관리자 아이디/코드 입력 시 관리자 로그인
			if (store.authenticateAdmin(userId, pw)) {
				session.removeAttribute("loginUser");
				session.setAttribute("isAdmin", Boolean.TRUE);
				session.setAttribute("flashMsg", "관리자 모드로 로그인되었습니다.");
				resp.sendRedirect(ctx + "/admin.jsp");
				return;
			}

			User user = store.authenticateUser(userId, pw);
			if (user == null) {
				session.setAttribute("flashError", "아이디 또는 비밀번호가 일치하지 않습니다.");
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}

			// 로그인 전 임시 장바구니 병합 유지
			Cart guestCart = getSessionCart(session);
			user.getSavedCart().mergeFrom(guestCart);
			store.saveCart(user);
			session.setAttribute("loginUser", user);
			session.setAttribute("cart", user.getSavedCart());
			session.removeAttribute("isAdmin");

			String redirectAfter = (String) session.getAttribute("redirectAfterLogin");
			session.removeAttribute("redirectAfterLogin");
			session.setAttribute("flashMsg", user.getName() + "님 환영합니다.");
			resp.sendRedirect(redirectAfter != null ? redirectAfter : (ctx + "/index.jsp"));
			return;
		}

		if ("addCart".equals(cmd)) {
			String bookId = req.getParameter("bookId");
			String returnUrl = req.getParameter("returnUrl");
			Book book = store.getBookById(bookId);
			returnUrl = safeReturnUrl(returnUrl, ctx);
			if (book == null) {
				session.setAttribute("flashError", "도서 정보를 찾을 수 없습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.");
			} else {
				Cart cart = getSessionCart(session);
				cart.insertBook(book);
				User loginUser = (User) session.getAttribute("loginUser");
				if (loginUser != null) {
					loginUser.setSavedCart(cart);
					store.saveCart(loginUser);
				}
				session.setAttribute("flashMsg", "「" + book.getName() + "」 도서가 장바구니에 담겼습니다.");
			}
			resp.sendRedirect(returnUrl != null ? returnUrl : (ctx + "/index.jsp"));
			return;
		}

		if ("updateCartQty".equals(cmd)) {
			String bookId = req.getParameter("bookId");
			int delta = Integer.parseInt(req.getParameter("delta"));
			Cart cart = getSessionCart(session);
			cart.updateQuantity(bookId, delta);
			User loginUser = (User) session.getAttribute("loginUser");
			if (loginUser != null) {
				loginUser.setSavedCart(cart);
				store.saveCart(loginUser);
			}
			resp.sendRedirect(ctx + "/cart.jsp");
			return;
		}

		if ("removeCartItem".equals(cmd)) {
			String bookId = req.getParameter("bookId");
			Cart cart = getSessionCart(session);
			cart.removeByBookId(bookId);
			User loginUser = (User) session.getAttribute("loginUser");
			if (loginUser != null) {
				loginUser.setSavedCart(cart);
				store.saveCart(loginUser);
			}
			resp.sendRedirect(ctx + "/cart.jsp");
			return;
		}

		if ("clearCart".equals(cmd)) {
			Cart cart = getSessionCart(session);
			cart.deleteBook();
			User loginUser = (User) session.getAttribute("loginUser");
			if (loginUser != null) {
				loginUser.setSavedCart(cart);
				store.saveCart(loginUser);
			}
			session.setAttribute("flashMsg", "장바구니를 비웠습니다.");
			resp.sendRedirect(ctx + "/cart.jsp");
			return;
		}

		if ("checkoutStart".equals(cmd)) {
			Cart cart = getSessionCart(session);
			List<String> removed = store.cleanRemovedBooksFromCart(cart);
			if (!removed.isEmpty()) {
				session.setAttribute("flashError", "해당 도서는 판매가 종료되었습니다: " + String.join(", ", removed));
				resp.sendRedirect(ctx + "/cart.jsp");
				return;
			}
			if (cart.getmCartItem().isEmpty()) {
				session.setAttribute("flashError", "장바구니가 비어 있습니다.");
				resp.sendRedirect(ctx + "/cart.jsp");
				return;
			}
			User loginUser = (User) session.getAttribute("loginUser");
			if (loginUser == null) {
				session.setAttribute("redirectAfterLogin", ctx + "/shipping.jsp");
				session.setAttribute("flashMsg", "결제를 진행하려면 먼저 로그인해 주세요.");
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			resp.sendRedirect(ctx + "/shipping.jsp");
			return;
		}

		if ("placeOrder".equals(cmd)) {
			User loginUser = (User) session.getAttribute("loginUser");
			if (loginUser == null) {
				session.setAttribute("redirectAfterLogin", ctx + "/shipping.jsp");
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			Cart cart = getSessionCart(session);
			List<String> removed = store.cleanRemovedBooksFromCart(cart);
			if (!removed.isEmpty()) {
				session.setAttribute("flashError", "해당 도서는 판매가 종료되었습니다: " + String.join(", ", removed));
				resp.sendRedirect(ctx + "/cart.jsp");
				return;
			}
			if (cart.getmCartItem().isEmpty()) {
				session.setAttribute("flashError", "주문할 도서가 없습니다.");
				resp.sendRedirect(ctx + "/cart.jsp");
				return;
			}
			String recipientName = req.getParameter("recipientName");
			String shippingDate = req.getParameter("shippingDate");
			String recipientPhone = req.getParameter("recipientPhone");
			String shippingAddress = req.getParameter("shippingAddress");

			boolean useCoupon = "on".equals(req.getParameter("useCoupon"));
			Order placed = store.createOrder(loginUser, cart, recipientName, recipientPhone, shippingDate,
					shippingAddress, ctx, useCoupon);
			session.setAttribute("flashMsg", placed.getDiscount() > 0
					? "주문이 완료되었습니다. 쿠폰으로 " + String.format("%,d", placed.getDiscount()) + "원 할인되었어요."
					: "주문이 정상적으로 완료되었습니다.");
			resp.sendRedirect(ctx + "/orders.jsp");
			return;
		}

		if ("cancelOrder".equals(cmd)) {
			User loginUser = (User) session.getAttribute("loginUser");
			Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
			String orderId = req.getParameter("orderId");
			if (Boolean.TRUE.equals(isAdmin)) {
				store.cancelOrder(orderId, null);
				session.setAttribute("flashMsg", "주문 내역이 삭제되었습니다.");
				resp.sendRedirect(ctx + "/admin.jsp?tab=orders");
			} else if (loginUser != null) {
				store.cancelOrder(orderId, loginUser.getUserId());
				session.setAttribute("flashMsg", "해당 주문이 취소 및 삭제되었습니다.");
				resp.sendRedirect(ctx + "/orders.jsp");
			} else {
				resp.sendRedirect(ctx + "/login.jsp");
			}
			return;
		}

		if ("registerCoupon".equals(cmd)) {
			User loginUser = (User) session.getAttribute("loginUser");
			if (loginUser == null) {
				session.setAttribute("redirectAfterLogin", ctx + "/profile.jsp");
				session.setAttribute("flashError", "쿠폰은 회원 전용입니다. 로그인 후 등록해 주세요.");
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			DataStore.CouponResult r = store.registerCoupon(loginUser, req.getParameter("couponCode"));
			switch (r) {
			case OK:
				session.setAttribute("flashMsg", "쿠폰이 등록되었습니다! " + DataStore.COUPON_NAME + " ("
						+ String.format("%,d", DataStore.COUPON_MIN_ORDER) + "원 이상 구매 시)");
				break;
			case ALREADY_REGISTERED:
				session.setAttribute("flashError", "이미 쿠폰을 등록하셨습니다. (1인 1회)");
				break;
			default:
				session.setAttribute("flashError", "유효하지 않은 쿠폰 코드입니다.");
			}
			resp.sendRedirect(ctx + "/profile.jsp");
			return;
		}

		if ("updateProfile".equals(cmd)) {
			User loginUser = (User) session.getAttribute("loginUser");
			if (loginUser == null) {
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			String name = req.getParameter("name");
			String phone = req.getParameter("phone");
			String currentPw = req.getParameter("currentPassword");
			String newPw = req.getParameter("newPassword");
			String confirmPw = req.getParameter("confirmPassword");

			if (newPw != null && !newPw.trim().isEmpty() && !newPw.equals(confirmPw)) {
				session.setAttribute("flashError", "새 비밀번호 확인이 일치하지 않습니다.");
				resp.sendRedirect(ctx + "/profile.jsp");
				return;
			}
			boolean ok = store.updateUser(loginUser.getUserId(), name, phone, currentPw, newPw);
			if (!ok) {
				session.setAttribute("flashError", "현재 비밀번호가 올바르지 않습니다.");
			} else {
				session.setAttribute("loginUser", store.getUser(loginUser.getUserId()));
				session.setAttribute("flashMsg", "회원 정보가 수정되었습니다.");
			}
			resp.sendRedirect(ctx + "/profile.jsp");
			return;
		}

		if ("withdraw".equals(cmd)) {
			User loginUser = (User) session.getAttribute("loginUser");
			if (loginUser != null) {
				store.deleteUserAndOrders(loginUser.getUserId());
				session.invalidate();
				HttpSession newSession = req.getSession(true);
				newSession.setAttribute("flashMsg", "회원 탈퇴가 완료되어 계정 및 주문 내역이 모두 삭제되었습니다.");
			}
			resp.sendRedirect(ctx + "/index.jsp");
			return;
		}

		if ("adminAddBook".equals(cmd)) {
			if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			String bookId = req.getParameter("bookId");
			if (bookId == null || bookId.trim().isEmpty()) {
				bookId = "ISBN" + new SimpleDateFormat("yyMMddHHmmss").format(new Date());
			}
			String name = req.getParameter("name");
			int price = Integer.parseInt(req.getParameter("unitPrice"));
			String author = req.getParameter("author");
			String desc = req.getParameter("description");
			String category = req.getParameter("category");
			String releaseDate = req.getParameter("releaseDate");
			String imageFileName = saveUploadedImage(req.getPart("imageFile"), bookId, store);

			Book newBook = new Book(bookId.trim(), name.trim(), price, author.trim(), desc.trim(),
					category.trim(), releaseDate.trim(), imageFileName);
			if (!store.addBook(newBook)) {
				session.setAttribute("flashError", "해당 도서는 이미 판매 중인 도서입니다. (중복 등록 불가)");
			} else {
				session.setAttribute("flashMsg", "새 도서가 '도서 목록'과 '신간 도서'에 등록되었습니다.");
			}
			resp.sendRedirect(ctx + "/admin.jsp?tab=books");
			return;
		}

		if ("adminUpdateBook".equals(cmd)) {
			if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			String bookId = req.getParameter("bookId");
			String name = req.getParameter("name");
			int price = Integer.parseInt(req.getParameter("unitPrice"));
			String author = req.getParameter("author");
			String desc = req.getParameter("description");
			String category = req.getParameter("category");
			String releaseDate = req.getParameter("releaseDate");
			String uploaded = saveUploadedImage(req.getPart("imageFile"), bookId, store);

			Set<String> tabs = new LinkedHashSet<>();
			String[] selectedTabs = req.getParameterValues("tabs");
			if (selectedTabs != null) {
				for (String t : selectedTabs) {
					tabs.add(t.toUpperCase());
				}
			}
			store.updateBook(bookId, name.trim(), price, author.trim(), desc.trim(),
					category.trim(), releaseDate.trim(), uploaded, tabs);
			session.setAttribute("flashMsg", "도서 정보 및 탭 분류가 수정되었습니다.");
			resp.sendRedirect(ctx + "/admin.jsp?tab=books");
			return;
		}

		if ("adminDeleteBook".equals(cmd)) {
			if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			String bookId = req.getParameter("bookId");
			store.deleteBook(bookId);
			session.setAttribute("flashMsg", "도서가 삭제되었습니다.");
			resp.sendRedirect(ctx + "/admin.jsp?tab=books");
			return;
		}

		resp.sendRedirect(ctx + "/index.jsp");
	}

	/** 같은 사이트 내부 경로로만 되돌려 보냅니다(오픈 리다이렉트 방지). */
	private String safeReturnUrl(String url, String ctx) {
		if (url == null || url.isEmpty()) return null;
		String prefix = ctx.isEmpty() ? "/" : ctx + "/";
		if (url.startsWith(prefix) && !url.startsWith("//") && !url.contains("\r") && !url.contains("\n")) return url;
		return null;
	}

	private String kakaoRedirectUri(HttpServletRequest req) {
		String configured = com.market.config.AppConfig.get("kakao.redirect.uri");
		if (!configured.isEmpty()) return configured;
		StringBuilder sb = new StringBuilder(req.getScheme()).append("://").append(req.getServerName());
		int port = req.getServerPort();
		if (!(("http".equals(req.getScheme()) && port == 80) || ("https".equals(req.getScheme()) && port == 443))) {
			sb.append(':').append(port);
		}
		return sb.append(req.getContextPath()).append("/kakao/callback").toString();
	}

	/** 카카오 로그인: /kakao/login (인가 요청) -> /kakao/callback (코드 교환 후 로그인) */
	private void handleKakao(HttpServletRequest req, HttpServletResponse resp, HttpSession session, DataStore store)
			throws IOException {
		String ctx = req.getContextPath();
		KakaoClient kakao = store.getKakao();
		String path = req.getPathInfo() == null ? "" : req.getPathInfo();

		if ("/login".equals(path)) {
			if (!kakao.enabled()) {
				session.setAttribute("flashError", "카카오 로그인이 설정되지 않았습니다. (kakao.rest.key 확인)");
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			String state = UUID.randomUUID().toString();
			session.setAttribute("kakaoState", state);
			resp.sendRedirect(kakao.authorizeUrl(kakaoRedirectUri(req), state));
			return;
		}

		if ("/callback".equals(path)) {
			String expected = (String) session.getAttribute("kakaoState");
			session.removeAttribute("kakaoState");
			String code = req.getParameter("code");
			if (req.getParameter("error") != null || code == null) {
				session.setAttribute("flashError", "카카오 로그인이 취소되었습니다.");
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			if (expected == null || !expected.equals(req.getParameter("state"))) {
				session.setAttribute("flashError", "잘못된 로그인 요청입니다. 다시 시도해 주세요.");
				resp.sendRedirect(ctx + "/login.jsp");
				return;
			}
			try {
				String token = kakao.exchangeToken(code, kakaoRedirectUri(req));
				KakaoClient.KakaoUser ku = kakao.fetchUser(token);
				User user = store.findOrCreateKakaoUser(ku.id, ku.nickname);

				Cart guestCart = getSessionCart(session);
				if (guestCart != user.getSavedCart()) user.getSavedCart().mergeFrom(guestCart);
				store.saveCart(user);
				session.setAttribute("loginUser", user);
				session.setAttribute("cart", user.getSavedCart());
				session.removeAttribute("isAdmin");

				String redirectAfter = (String) session.getAttribute("redirectAfterLogin");
				session.removeAttribute("redirectAfterLogin");
				session.setAttribute("flashMsg", user.getName() + "님, 카카오 계정으로 로그인되었습니다.");
				resp.sendRedirect(redirectAfter != null ? redirectAfter : (ctx + "/index.jsp"));
			} catch (Exception e) {
				e.printStackTrace();
				session.setAttribute("flashError", "카카오 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.");
				resp.sendRedirect(ctx + "/login.jsp");
			}
			return;
		}
		resp.sendRedirect(ctx + "/index.jsp");
	}

	private String saveUploadedImage(Part filePart, String bookId, DataStore store) throws IOException {
		if (filePart == null || filePart.getSize() == 0) return null;
		String submitted = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
		if (submitted.isEmpty()) return null;
		String ext = ".jpg";
		int dot = submitted.lastIndexOf('.');
		if (dot >= 0) ext = submitted.substring(dot).toLowerCase();
		String savedName = bookId + "_" + System.currentTimeMillis() + ext;
		Path dest = store.getUploadDir().resolve(savedName);
		try (InputStream in = filePart.getInputStream()) {
			Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
		}
		return "uploads/" + savedName;
	}
}