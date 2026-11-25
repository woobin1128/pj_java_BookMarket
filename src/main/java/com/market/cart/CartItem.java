package com.market.cart;

import java.io.Serializable;
import com.market.bookitem.Book;

public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Book itemBook;
    private String bookID;
    private int quantity;
    private int totalPrice;

    public CartItem() {
    }

    public CartItem(Book book) {
        setItemBook(book);
        this.quantity = 1;
        updateTotalPrice();
    }

    public CartItem(Book book, int quantity) {
        setItemBook(book);
        this.quantity = quantity;
        updateTotalPrice();
    }

    public Book getItemBook() {
        return itemBook;
    }

    public void setItemBook(Book itemBook) {
        this.itemBook = itemBook;
        if (itemBook != null && itemBook.getBookId() != null) {
            this.bookID = itemBook.getBookId();
        }
        updateTotalPrice();
    }

    public String getBookID() {
        if (bookID != null) return bookID;
        return (itemBook != null) ? itemBook.getBookId() : null;
    }

    public void setBookID(String bookID) {
        this.bookID = bookID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        updateTotalPrice();
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void updateTotalPrice() {
        if (itemBook == null) {
            this.totalPrice = 0;
        } else {
            this.totalPrice = itemBook.getUnitPrice() * this.quantity;
        }
    }
}