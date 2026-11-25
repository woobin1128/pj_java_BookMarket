package com.market.member;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private String id = "admin";
    private String password = "1234";

    public Admin() {
        setUserId(this.id);
        setName("관리자");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        setUserId(id);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}