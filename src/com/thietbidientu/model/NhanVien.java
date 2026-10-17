package com.thietbidientu.model;

public class NhanVien extends NguoiDung {

    public NhanVien(int maNguoiDung, String hoTen, String email,
                     String matKhau, String soDienThoai, String diaChi) {
        super(maNguoiDung, hoTen, email, matKhau, soDienThoai, diaChi);
    }

    @Override
    public VaiTro layVaiTro() {
        return VaiTro.NHAN_VIEN;
    }
}
