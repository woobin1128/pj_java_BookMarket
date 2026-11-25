package com.market.cart;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.market.bookitem.Book;

public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;

    private ArrayList<CartItem> mCartItem = new ArrayList<>();

    public Cart() {
    }

    public ArrayList<CartItem> getmCartItem() {
        if (mCartItem == null) {
            mCartItem = new ArrayList<>();
        }
        return mCartItem;
    }

    public void setmCartItem(ArrayList<CartItem> mCartItem) {
        this.mCartItem = mCartItem;
    }

    public List<CartItem> getItems() {
        return getmCartItem();
    }

    // 도서 1권 추가 (이미 있으면 수량 +1)
    public void insertBook(Book book) {
        addBook(book, 1);
    }

    public void addBook(Book book, int qty) {
        if (book == null || qty <= 0) return;
        for (CartItem item : getmCartItem()) {
            if (book.getBookId() != null && book.getBookId().equals(item.getBookID())) {
                item.setQuantity(item.getQuantity() + qty);
                return;
            }
        }
        getmCartItem().add(new CartItem(book, qty));
    }

    // 수량 증감 (0 이하가 되면 항목 제거)
    public void updateQuantity(String bookId, int delta) {
        if (bookId == null) return;
        Iterator<CartItem> it = getmCartItem().iterator();
        while (it.hasNext()) {
            CartItem item = it.next();
            if (bookId.equals(item.getBookID())) {
                int nextQty = item.getQuantity() + delta;
                if (nextQty <= 0) {
                    it.remove();
                } else {
                    item.setQuantity(nextQty);
                }
                return;
            }
        }
    }

    // 다른 장바구니(비로그인 임시 장바구니 등)의 항목 병합
    public void mergeFrom(Cart other) {
        if (other == null || other == this) return;
        for (CartItem item : other.getmCartItem()) {
            if (item != null && item.getItemBook() != null) {
                addBook(item.getItemBook(), item.getQuantity());
            }
        }
    }

    public void removeByBookId(String bookId) {
        if (bookId == null) return;
        Iterator<CartItem> it = getmCartItem().iterator();
        while (it.hasNext()) {
            CartItem item = it.next();
            if (bookId.equals(item.getBookID())) {
                it.remove();
            }
        }
    }

    public void deleteBook() {
        getmCartItem().clear();
    }

    // 장바구니 총 도서 권수
    public int getTotalCount() {
        int count = 0;
        for (CartItem item : getmCartItem()) {
            count += item.getQuantity();
        }
        return count;
    }

    // 장바구니 총 결제 금액
    public int getTotalPrice() {
        int sum = 0;
        for (CartItem item : getmCartItem()) {
            sum += item.getTotalPrice();
        }
        return sum;
    }

    public int getGrandTotal() {
        return getTotalPrice();
    }
}