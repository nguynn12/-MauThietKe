package com.thietbidientu.state;

import com.thietbidientu.model.DonHang;

public class DangGiao implements TrangThaiDonHang {

    @Override
    public String tenTrangThai() { return "Đang giao"; }

    @Override
    public void hoanTat(DonHang donHang) {
        donHang.setTrangThaiHienTai(new HoanTat());
    }
}
