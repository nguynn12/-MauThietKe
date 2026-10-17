package com.thietbidientu.model;

import com.thietbidientu.observer.NguoiQuanSatDonHang;
import com.thietbidientu.state.ChoXacNhan;
import com.thietbidientu.state.TrangThaiDonHang;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quan hệ Composition với ChiTietDonHang (1 : 1..*) và quan hệ
 * Dependency với SanPham (chỉ gọi tạm thời capNhatTonKho khi tạo và
 * khi hủy đơn, không giữ tham chiếu cố định), đúng như bảng quan hệ
 * đã lập ở Chương 3.
 */
public class DonHang {

    private final int maDonHang;
    private final int maKhachHang;
    private Integer maNhanVien; // 0..1, có thể null khi chưa được xử lý
    private final LocalDateTime ngayDatHang;
    private String diaChiGiaoHang;
    private final List<ChiTietDonHang> danhSachChiTiet = new ArrayList<>();
    private TrangThaiDonHang trangThaiHienTai = new ChoXacNhan();
    private final List<NguoiQuanSatDonHang> nguoiQuanSat = new ArrayList<>();

    public DonHang(int maDonHang, int maKhachHang, String diaChiGiaoHang) {
        this.maDonHang = maDonHang;
        this.maKhachHang = maKhachHang;
        this.diaChiGiaoHang = diaChiGiaoHang;
        this.ngayDatHang = LocalDateTime.now();
    }

    public void themQuanSat(NguoiQuanSatDonHang quanSat) {
        nguoiQuanSat.add(quanSat);
    }

    private void thongBaoQuanSat() {
        for (NguoiQuanSatDonHang qs : nguoiQuanSat) {
            qs.capNhat(this);
        }
    }

    /** Gọi bởi các lớp trạng thái, không dùng trực tiếp từ bên ngoài. */
    public void setTrangThaiHienTai(TrangThaiDonHang trangThaiMoi) {
        this.trangThaiHienTai = trangThaiMoi;
        thongBaoQuanSat();
    }

    public void themChiTiet(ChiTietDonHang chiTiet) {
        danhSachChiTiet.add(chiTiet);
    }

    public long tinhTongTien() {
        return danhSachChiTiet.stream().mapToLong(ChiTietDonHang::tinhThanhTien).sum();
    }

    /** Quan hệ Dependency: đơn hàng gọi tạm thời sang Sản phẩm khi cần hoàn kho. */
    public void hoanTraTonKho() {
        for (ChiTietDonHang ct : danhSachChiTiet) {
            ct.getSanPham().capNhatTonKho(ct.getSoLuong());
        }
    }

    // ----- Ủy quyền cho đối tượng trạng thái (mẫu State) -----
    public void xacNhan(int maNhanVienXuLy) {
        trangThaiHienTai.xacNhan(this);
        this.maNhanVien = maNhanVienXuLy;
    }

    public void giao() { trangThaiHienTai.giao(this); }

    public void hoanTat() { trangThaiHienTai.hoanTat(this); }

    public void huy() { trangThaiHienTai.huy(this); }

    public String getTenTrangThai() { return trangThaiHienTai.tenTrangThai(); }

    public int getMaDonHang() { return maDonHang; }
    public int getMaKhachHang() { return maKhachHang; }
    public Integer getMaNhanVien() { return maNhanVien; }
    public LocalDateTime getNgayDatHang() { return ngayDatHang; }
    public String getDiaChiGiaoHang() { return diaChiGiaoHang; }
    public List<ChiTietDonHang> getDanhSachChiTiet() { return Collections.unmodifiableList(danhSachChiTiet); }

    @Override
    public String toString() {
        return String.format("Đơn hàng #%d | khách hàng #%d | tổng tiền %,d đ | trạng thái: %s",
                maDonHang, maKhachHang, tinhTongTien(), getTenTrangThai());
    }
}
