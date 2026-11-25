package com.market.order;

import java.io.Serializable;

public class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bookId;
    private String bookName;
    private int unitPrice;
    private int quantity;
    private String imageWebPath;

    public OrderItem() {
    }

    // DataStore.createOrder에서 호출하는 5인자 생성자
    public OrderItem(String bookId, String bookName, int unitPrice, int quantity, String imageWebPath) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.imageWebPath = imageWebPath;
    }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }

    public int getUnitPrice() { return unitPrice; }
    public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getImageWebPath() { return imageWebPath; }
    public void setImageWebPath(String imageWebPath) { this.imageWebPath = imageWebPath; }

    public int getSubtotal() {
        return unitPrice * quantity;
    }
    
    public int getTotalPrice() {
        return getSubtotal();
    }
}