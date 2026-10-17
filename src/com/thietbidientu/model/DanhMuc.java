package com.thietbidientu.model;

public class DanhMuc {

    private final int maDanhMuc;
    private String tenDanhMuc;

    public DanhMuc(int maDanhMuc, String tenDanhMuc) {
        this.maDanhMuc = maDanhMuc;
        this.tenDanhMuc = tenDanhMuc;
    }

    public int getMaDanhMuc() { return maDanhMuc; }
    public String getTenDanhMuc() { return tenDanhMuc; }
    public void setTenDanhMuc(String tenDanhMuc) { this.tenDanhMuc = tenDanhMuc; }

    @Override
    public String toString() {
        return tenDanhMuc;
    }
}
