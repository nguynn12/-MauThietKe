package com.thietbidientu.strategy;

import com.thietbidientu.model.SanPham;

public class LocTheoKhoangGia implements DieuKienLoc {

    private final long giaTu;
    private final long giaDen;

    public LocTheoKhoangGia(long giaTu, long giaDen) {
        this.giaTu = giaTu;
        this.giaDen = giaDen;
    }

    @Override
    public boolean thoaDieuKien(SanPham sanPham) {
        return sanPham.getGia() >= giaTu && sanPham.getGia() <= giaDen;
    }
}
