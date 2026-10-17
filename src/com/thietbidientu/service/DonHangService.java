package com.thietbidientu.service;

import com.thietbidientu.builder.DonHangBuilder;
import com.thietbidientu.dao.DonHangDAO;
import com.thietbidientu.dao.DonHangDAOImpl;
import com.thietbidientu.model.DonHang;
import com.thietbidientu.model.GioHang;
import com.thietbidientu.model.KhachHang;
import com.thietbidientu.model.NhanVien;
import com.thietbidientu.observer.ThongBaoKhachHang;

import java.util.List;

/**
 * Tầng xử lý nghiệp vụ cho ba use case cốt lõi: Đặt hàng, Xử lý đơn
 * hàng và Hủy đơn hàng. Đây là nơi phối hợp cả bốn mẫu thiết kế còn
 * lại: Builder để tạo đơn, State để quản lý vòng đời, Observer để
 * thông báo, và DAO để lưu trữ.
 */
public class DonHangService {

    private final DonHangDAO donHangDAO = new DonHangDAOImpl();
    private int demMaDonHang = 1;

    /** Use case Đặt hàng, bước 1 đến bước 8 của dòng sự kiện chính (Chương 2). */
    public DonHang datHang(KhachHang khachHang, GioHang gioHang, String diaChiGiaoHang) {
        DonHang donHang = new DonHangBuilder(demMaDonHang++, khachHang.getMaNguoiDung(), diaChiGiaoHang)
                .tuGioHang(gioHang)
                .dangKyQuanSat(new ThongBaoKhachHang())
                .xayDung();
        donHangDAO.luu(donHang);
        gioHang.lamRong(); // bước 7: xóa các sản phẩm vừa đặt khỏi giỏ hàng
        System.out.println("[Đặt hàng] " + donHang);
        return donHang;
    }

    /** Nhánh hợp lệ của use case Xử lý đơn hàng. */
    public void xacNhanDonHang(NhanVien nhanVien, int maDonHang) {
        DonHang donHang = donHangDAO.timTheoMa(maDonHang);
        donHang.xacNhan(nhanVien.getMaNguoiDung());
        System.out.println("[" + nhanVien.getHoTen() + "] xác nhận đơn hàng #" + maDonHang);
    }

    /** Nhánh sự cố của use case Xử lý đơn hàng, hoặc use case Hủy đơn hàng của khách. */
    public void huyDonHang(int maDonHang) {
        DonHang donHang = donHangDAO.timTheoMa(maDonHang);
        donHang.huy();
        System.out.println("Đơn hàng #" + maDonHang + " đã được hủy, tồn kho đã được hoàn lại.");
    }

    public void giaoHang(int maDonHang) {
        donHangDAO.timTheoMa(maDonHang).giao();
    }

    public void hoanTatDonHang(int maDonHang) {
        donHangDAO.timTheoMa(maDonHang).hoanTat();
    }

    public List<DonHang> xemLichSuDonHang(KhachHang khachHang) {
        return donHangDAO.layTheoKhachHang(khachHang.getMaNguoiDung());
    }

    public List<DonHang> layDonHangChoXuLy() {
        return donHangDAO.layTatCa().stream()
                .filter(dh -> dh.getTenTrangThai().equals("Chờ xác nhận"))
                .toList();
    }
}
