package com.market.order;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderId;
    private String userId;
    private String userName;
    private String recipientName;
    private String recipientPhone;
    private String shippingDate;
    private String shippingAddress;
    private String orderDate;
    private List<OrderItem> orderItems = new ArrayList<>();

    public Order() {
    }

    // DataStore.createOrder에서 호출하는 9인자 생성자
    public Order(String orderId, String userId, String userName, String recipientName,
                 String recipientPhone, String shippingDate, String shippingAddress,
                 String orderDate, List<OrderItem> orderItems) {
        this.orderId = orderId;
        this.userId = userId;
        this.userName = userName;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.shippingDate = shippingDate;
        this.shippingAddress = shippingAddress;
        this.orderDate = orderDate;
        this.orderItems = (orderItems != null) ? orderItems : new ArrayList<>();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getShippingDate() { return shippingDate; }
    public void setShippingDate(String shippingDate) { this.shippingDate = shippingDate; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public int getGrandTotal() {
        int sum = 0;
        if (orderItems != null) {
            for (OrderItem item : orderItems) {
                sum += item.getSubtotal();
            }
        }
        return sum;
    }
    
    private String status = "주문완료";
    /** 사용한 쿠폰 코드(없으면 null)와 할인 금액 */
    private String couponCode;
    private int discount;

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = (couponCode == null || couponCode.isEmpty()) ? null : couponCode; }
    public int getDiscount() { return discount; }
    public void setDiscount(int discount) { this.discount = Math.max(0, discount); }

    public String getOrderedAt() {
        return orderDate;
    }

    public void setOrderedAt(String orderedAt) {
        this.orderDate = orderedAt;
    }

    public String getStatus() {
        return (status != null) ? status : "주문완료";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return getOrderItems();
    }

    /** 쿠폰 할인 적용 후 실제 결제 금액 */
    public int getTotalAmount() {
        return Math.max(0, getGrandTotal() - discount);
    }
}