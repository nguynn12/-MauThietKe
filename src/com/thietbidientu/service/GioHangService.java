package com.thietbidientu.service;

import com.thietbidientu.model.GioHang;
import com.thietbidientu.model.SanPham;

import java.util.HashMap;
import java.util.Map;

/**
 * Tầng xử lý nghiệp vụ cho use case Quản lý giỏ hàng. Trong bản minh
 * họa này, giỏ hàng được giữ trong bộ nhớ theo mã khách hàng, tương
 * đương bảng GIO_HANG với ràng buộc một khách hàng chỉ có tối đa một
 * giỏ hàng hiện hành (Chương 4).
 */
public class GioHangService {

    private final Map<Integer, GioHang> gioHangTheoKhachHang = new HashMap<>();
    private int demMaGioHang = 1;

    public GioHang layGioHang(int maKhachHang) {
        return gioHangTheoKhachHang.computeIfAbsent(maKhachHang,
                kh -> new GioHang(demMaGioHang++, kh));
    }

    public void themVaoGio(int maKhachHang, SanPham sanPham, int soLuong) {
        layGioHang(maKhachHang).themSanPham(sanPham, soLuong);
    }

    public void xoaKhoiGio(int maKhachHang, int maSanPham) {
        layGioHang(maKhachHang).xoaSanPham(maSanPham);
    }

    public void lamRongGio(int maKhachHang) {
        layGioHang(maKhachHang).lamRong();
    }
}
