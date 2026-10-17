package com.thietbidientu.strategy;

import com.thietbidientu.model.SanPham;

public class LocTheoDanhMuc implements DieuKienLoc {

    private final int maDanhMuc;

    public LocTheoDanhMuc(int maDanhMuc) {
        this.maDanhMuc = maDanhMuc;
    }

    @Override
    public boolean thoaDieuKien(SanPham sanPham) {
        return sanPham.getDanhMuc().getMaDanhMuc() == maDanhMuc;
    }
}
