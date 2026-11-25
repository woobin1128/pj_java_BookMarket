package com.market.member;

import java.io.Serializable;
import com.market.cart.Cart;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String passwordHash;
    private String name;
    private String phone;
    private String joinDate;
    private String address;
    private Cart savedCart = new Cart();
    /** 가입 방식: "local"(아이디/비밀번호) 또는 "kakao" */
    private String provider = "local";
    /** 등록된 쿠폰 코드(없으면 null) 와 사용 여부 */
    private String couponCode;
    private boolean couponUsed;

    public User() {
    }

    // DataStore.registerUser에서 호출하는 5인자 생성자
    public User(String userId, String passwordHash, String name, String phone, String joinDate) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.joinDate = joinDate;
        this.savedCart = new Cart();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Cart getSavedCart() {
        if (savedCart == null) {
            savedCart = new Cart();
        }
        return savedCart;
    }

    public void setSavedCart(Cart savedCart) {
        this.savedCart = savedCart;
    }
    
    public String getProvider() {
        return (provider == null || provider.isEmpty()) ? "local" : provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isKakao() {
        return "kakao".equals(getProvider());
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = (couponCode == null || couponCode.isEmpty()) ? null : couponCode;
    }

    public boolean isCouponUsed() {
        return couponUsed;
    }

    public void setCouponUsed(boolean couponUsed) {
        this.couponUsed = couponUsed;
    }

    /** 등록되어 있고 아직 쓰지 않은 쿠폰이 있는가 */
    public boolean hasUsableCoupon() {
        return couponCode != null && !couponUsed;
    }

    public String getJoinedAt() {
        return joinDate;
    }

    public void setJoinedAt(String joinedAt) {
        this.joinDate = joinedAt;
    }
}