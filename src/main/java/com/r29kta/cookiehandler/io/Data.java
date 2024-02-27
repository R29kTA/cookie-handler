package com.r29kta.cookiehandler.io;

import java.io.Serializable;

public class Data implements Serializable {
    private String username;
    private String password;
    private String cookie;
    private String code;
    private String email;

    public Data(String username, String password, String cookie, String code, String email) {
        this.username = username;
        this.password = password;
        this.cookie = cookie;
        this.code = code;
        this.email = email;
    }

    public Data() {
    }

    @Override
    public String toString() {
        return "Data{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", cookie='" + cookie + '\'' +
                ", code='" + code + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
