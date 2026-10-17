package com.thietbidientu.model;

/** Quan hệ Aggregation với SanPham: chỉ tham chiếu, không sở hữu. */
public class ChiTietGioHang {

    private final SanPham sanPham;
    private int soLuong;

    public ChiTietGioHang(SanPham sanPham, int soLuong) {
        this.sanPham = sanPham;
        this.soLuong = soLuong;
    }

    public long tinhThanhTien() {
        return sanPham.getGia() * soLuong;
    }

    public SanPham getSanPham() { return sanPham; }
    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    @Override
    public String toString() {
        return String.format("%-20s x%-3d = %,10d đ", sanPham.getTenSanPham(), soLuong, tinhThanhTien());
    }
}
