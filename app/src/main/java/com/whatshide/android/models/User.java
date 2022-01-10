package com.whatshide.android.models;

public class User {
    String mobile,name,profile_url,status,lastSeen;

    public User() {
    }

    public User(String mobile, String name, String profile_url,String status) {
        this.mobile = mobile;
        this.name = name;
        this.profile_url = profile_url;
        this.status = status;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfile_url() {
        return profile_url;
    }

    public void setProfile_url(String profile_url) {
        this.profile_url = profile_url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
