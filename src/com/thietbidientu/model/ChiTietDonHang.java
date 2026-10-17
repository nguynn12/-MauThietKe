package com.thietbidientu.model;

/**
 * Khác với ChiTietGioHang, lớp này lưu cố định đơn giá tại thời điểm
 * mua (D4 của DFD Đặt hàng), không phụ thuộc vào giá hiện hành của
 * sản phẩm về sau.
 */
public class ChiTietDonHang {

    private final SanPham sanPham;
    private final int soLuong;
    private final long donGia;

    public ChiTietDonHang(SanPham sanPham, int soLuong, long donGia) {
        this.sanPham = sanPham;
        this.soLuong = soLuong;
        this.donGia = donGia;
    }

    public long tinhThanhTien() {
        return donGia * soLuong;
    }

    public SanPham getSanPham() { return sanPham; }
    public int getSoLuong() { return soLuong; }
    public long getDonGia() { return donGia; }

    @Override
    public String toString() {
        return String.format("%-20s x%-3d @ %,10d đ = %,10d đ",
                sanPham.getTenSanPham(), soLuong, donGia, tinhThanhTien());
    }
}
