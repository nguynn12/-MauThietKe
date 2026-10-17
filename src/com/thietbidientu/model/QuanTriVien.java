package com.thietbidientu.model;

public class QuanTriVien extends NguoiDung {

    public QuanTriVien(int maNguoiDung, String hoTen, String email,
                        String matKhau, String soDienThoai, String diaChi) {
        super(maNguoiDung, hoTen, email, matKhau, soDienThoai, diaChi);
    }

    @Override
    public VaiTro layVaiTro() {
        return VaiTro.QUAN_TRI_VIEN;
    }
}
