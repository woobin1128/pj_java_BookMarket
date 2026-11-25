package com.market.util;

import com.market.bookitem.Book;

/** 도서 카드 / 가로 스크롤 없는 그리드에서 공통으로 쓰는 카드 HTML 생성기 */
public final class BookCards {
	private BookCards() {}

	public static String card(Book b, String ctx, String returnUrl, int order) {
		StringBuilder sb = new StringBuilder(1200);
		String cat = Html.esc(b.getCategory());
		sb.append("<article class=\"book-card\" data-price=\"").append(b.getUnitPrice())
				.append("\" data-name=\"").append(Html.esc(b.getName()))
				.append("\" data-date=\"").append(Html.esc(b.getReleaseDate()))
				.append("\" data-order=\"").append(order).append("\">");
		sb.append("<div class=\"cover-wrap\"><div class=\"tags\">");
		if (b.hasTab("BEST")) sb.append("<span class=\"tag best\">BEST</span>");
		if (b.hasTab("EVENT")) sb.append("<span class=\"tag event\">EVENT</span>");
		if (b.hasTab("NEW")) sb.append("<span class=\"tag new\">NEW</span>");
		sb.append("</div><img src=\"").append(Html.esc(b.getImageWebPath(ctx))).append("\" alt=\"")
				.append(Html.esc(b.getName())).append(" 표지\" loading=\"lazy\" referrerpolicy=\"no-referrer\" ")
				.append("onerror=\"this.onerror=null;this.src='").append(ctx).append("/images/shop.jpg'\"></div>");
		sb.append("<div class=\"book-body\"><span class=\"cat\">").append(cat).append("</span>");
		sb.append("<h3>").append(Html.esc(b.getName())).append("</h3>");
		sb.append("<span class=\"author\">").append(Html.esc(b.getAuthor()));
		if (b.getPublisher() != null && !b.getPublisher().isEmpty()) {
			sb.append(" · ").append(Html.esc(b.getPublisher()));
		}
		sb.append("</span>");
		sb.append("<p class=\"desc\">").append(Html.esc(b.getDescription())).append("</p>");
		sb.append("<span class=\"pub\">출판 ").append(Html.esc(b.getReleaseDate())).append("</span>");
		sb.append("<div class=\"book-foot\"><span class=\"price\">").append(Html.won(b.getUnitPrice()))
				.append("<small>원</small></span>");
		sb.append("<form action=\"").append(ctx).append("/action\" method=\"post\">")
				.append("<input type=\"hidden\" name=\"cmd\" value=\"addCart\">")
				.append("<input type=\"hidden\" name=\"bookId\" value=\"").append(Html.esc(b.getBookId())).append("\">")
				.append("<input type=\"hidden\" name=\"returnUrl\" value=\"").append(Html.esc(returnUrl)).append("\">")
				.append("<button type=\"submit\" class=\"btn\"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" ")
				.append("stroke-width=\"2.4\" stroke-linecap=\"round\"><path d=\"M12 5v14M5 12h14\"/></svg>담기</button></form>");
		sb.append("</div></div></article>");
		return sb.toString();
	}
}
