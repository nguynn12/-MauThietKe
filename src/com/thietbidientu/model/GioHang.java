package com.thietbidientu.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Quan hệ Composition với ChiTietGioHang: các dòng chi tiết không có
 * ý nghĩa tồn tại độc lập nếu tách khỏi giỏ hàng này.
 */
public class GioHang {

    private final int maGioHang;
    private final int maKhachHang;
    private final List<ChiTietGioHang> danhSachChiTiet = new ArrayList<>();

    public GioHang(int maGioHang, int maKhachHang) {
        this.maGioHang = maGioHang;
        this.maKhachHang = maKhachHang;
    }

    public void themSanPham(SanPham sanPham, int soLuong) {
        if (!sanPham.kiemTraTonKho(soLuong)) {
            throw new IllegalArgumentException(
                "Số lượng tồn kho không đủ cho sản phẩm " + sanPham.getTenSanPham());
        }
        Optional<ChiTietGioHang> dongDaCo = timDong(sanPham);
        if (dongDaCo.isPresent()) {
            int tongSoLuong = dongDaCo.get().getSoLuong() + soLuong;
            if (!sanPham.kiemTraTonKho(tongSoLuong)) {
                throw new IllegalArgumentException("Tổng số lượng trong giỏ vượt quá tồn kho.");
            }
            dongDaCo.get().setSoLuong(tongSoLuong);
        } else {
            danhSachChiTiet.add(new ChiTietGioHang(sanPham, soLuong));
        }
    }

    public void xoaSanPham(int maSanPham) {
        danhSachChiTiet.removeIf(ct -> ct.getSanPham().getMaSanPham() == maSanPham);
    }

    public long tinhTongTien() {
        return danhSachChiTiet.stream().mapToLong(ChiTietGioHang::tinhThanhTien).sum();
    }

    public void lamRong() {
        danhSachChiTiet.clear();
    }

    private Optional<ChiTietGioHang> timDong(SanPham sanPham) {
        return danhSachChiTiet.stream()
                .filter(ct -> ct.getSanPham().getMaSanPham() == sanPham.getMaSanPham())
                .findFirst();
    }

    public int getMaGioHang() { return maGioHang; }
    public int getMaKhachHang() { return maKhachHang; }
    public List<ChiTietGioHang> getDanhSachChiTiet() { return danhSachChiTiet; }
}
