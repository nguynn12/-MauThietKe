package com.thietbidientu.strategy;

import com.thietbidientu.model.SanPham;

public class LocTheoTuKhoa implements DieuKienLoc {

    private final String tuKhoa;

    public LocTheoTuKhoa(String tuKhoa) {
        this.tuKhoa = tuKhoa.toLowerCase();
    }

    @Override
    public boolean thoaDieuKien(SanPham sanPham) {
        return sanPham.getTenSanPham().toLowerCase().contains(tuKhoa);
    }
}
