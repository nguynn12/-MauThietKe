package com.thietbidientu;

import com.thietbidientu.dao.NguoiDungDAO;
import com.thietbidientu.dao.NguoiDungDAOImpl;
import com.thietbidientu.factory.NguoiDungFactory;
import com.thietbidientu.model.*;
import com.thietbidientu.service.DonHangService;
import com.thietbidientu.service.GioHangService;
import com.thietbidientu.service.SanPhamService;
import com.thietbidientu.strategy.DieuKienLoc;
import com.thietbidientu.strategy.LocTheoDanhMuc;
import com.thietbidientu.strategy.LocTheoKhoangGia;

import java.util.List;

/**
 * Chương trình minh họa đóng vai trò tầng trình bày giản lược (console),
 * mô phỏng lại đúng chuỗi thao tác đã mô tả trong hai sơ đồ tuần tự
 * Đặt hàng và Xử lý đơn hàng ở Chương 2, đồng thời chứng minh sáu mẫu
 * thiết kế đã áp dụng: Singleton, Factory Method, Strategy, Builder,
 * State và Observer, cùng kiến trúc DAO ở tầng truy xuất dữ liệu.
 */
public class Main {

    public static void main(String[] args) {
        NguoiDungDAO nguoiDungDAO = new NguoiDungDAOImpl();
        SanPhamService sanPhamService = new SanPhamService();
        GioHangService gioHangService = new GioHangService();
        DonHangService donHangService = new DonHangService();

        chiaDong("BƯỚC 1: KHỞI TẠO DỮ LIỆU MẪU (Singleton KhoDuLieu)");
        DanhMuc dienThoai = new DanhMuc(1, "Điện thoại");
        DanhMuc laptop = new DanhMuc(2, "Laptop");

        SanPham sp1 = new SanPham(1, "iPhone 15", 22_000_000, 10, "Điện thoại Apple", dienThoai);
        SanPham sp2 = new SanPham(2, "Samsung Galaxy S24", 18_000_000, 5, "Điện thoại Samsung", dienThoai);
        SanPham sp3 = new SanPham(3, "MacBook Air M2", 27_000_000, 3, "Laptop Apple", laptop);
        sanPhamService.themSanPham(new NhanVien(99, "Nhân viên kho", "kho@shop.vn", "x", "0", ""), sp1);
        sanPhamService.themSanPham(new NhanVien(99, "Nhân viên kho", "kho@shop.vn", "x", "0", ""), sp2);
        sanPhamService.themSanPham(new NhanVien(99, "Nhân viên kho", "kho@shop.vn", "x", "0", ""), sp3);

        chiaDong("BƯỚC 2: TẠO NGƯỜI DÙNG QUA FACTORY METHOD");
        NguoiDung nd1 = NguoiDungFactory.taoNguoiDung(VaiTro.KHACH_HANG, 1, "Nguyễn Văn A",
                "a@gmail.com", "matkhau123", "0900000001", "Quận 1, TP.HCM");
        NguoiDung nd2 = NguoiDungFactory.taoNguoiDung(VaiTro.NHAN_VIEN, 2, "Trần Thị B",
                "b@shop.vn", "matkhau456", "0900000002", "Quận 3, TP.HCM");
        nguoiDungDAO.luu(nd1);
        nguoiDungDAO.luu(nd2);
        System.out.println("Đã tạo: " + nd1);
        System.out.println("Đã tạo: " + nd2);
        KhachHang khachHang = (KhachHang) nd1;
        NhanVien nhanVien = (NhanVien) nd2;

        chiaDong("BƯỚC 3: ĐĂNG NHẬP");
        boolean dangNhapOk = khachHang.dangNhap("a@gmail.com", "matkhau123");
        System.out.println("Khách hàng đăng nhập " + (dangNhapOk ? "thành công." : "thất bại."));

        chiaDong("BƯỚC 4: TÌM KIẾM SẢN PHẨM (mẫu Strategy)");
        List<DieuKienLoc> dieuKien = List.of(
                new LocTheoDanhMuc(dienThoai.getMaDanhMuc()),
                new LocTheoKhoangGia(15_000_000, 25_000_000)
        );
        List<SanPham> ketQuaTimKiem = sanPhamService.timKiemSanPham(dieuKien);
        System.out.println("Kết quả tìm điện thoại giá 15-25 triệu:");
        ketQuaTimKiem.forEach(sp -> System.out.println("   " + sp));

        chiaDong("BƯỚC 5: THÊM VÀO GIỎ HÀNG");
        gioHangService.themVaoGio(khachHang.getMaNguoiDung(), sp1, 1);
        gioHangService.themVaoGio(khachHang.getMaNguoiDung(), sp3, 1);
        GioHang gioHang = gioHangService.layGioHang(khachHang.getMaNguoiDung());
        gioHang.getDanhSachChiTiet().forEach(ct -> System.out.println("   " + ct));
        System.out.println("Tổng tiền tạm tính giỏ hàng: " + String.format("%,d đ", gioHang.tinhTongTien()));

        chiaDong("BƯỚC 6: ĐẶT HÀNG (mẫu Builder + Observer)");
        System.out.println("Tồn kho iPhone 15 trước khi đặt: " + sp1.getSoLuongTonKho());
        DonHang donHang = donHangService.datHang(khachHang, gioHang, khachHang.getDiaChi());
        System.out.println("Tồn kho iPhone 15 sau khi đặt: " + sp1.getSoLuongTonKho());

        chiaDong("BƯỚC 7: NHÂN VIÊN XỬ LÝ ĐƠN HÀNG (mẫu State + Observer)");
        donHangService.xacNhanDonHang(nhanVien, donHang.getMaDonHang());
        System.out.println("Trạng thái hiện tại: " + donHang.getTenTrangThai());

        System.out.println("Thử hoàn tất đơn khi chưa giao hàng (kỳ vọng bị chặn bởi mẫu State):");
        try {
            donHangService.hoanTatDonHang(donHang.getMaDonHang());
        } catch (IllegalStateException loi) {
            System.out.println("   >> Bị từ chối đúng như thiết kế: " + loi.getMessage());
        }

        donHangService.giaoHang(donHang.getMaDonHang());
        System.out.println("Trạng thái hiện tại: " + donHang.getTenTrangThai());
        donHangService.hoanTatDonHang(donHang.getMaDonHang());
        System.out.println("Trạng thái hiện tại: " + donHang.getTenTrangThai());

        System.out.println("Thử hủy một đơn đã hoàn tất (kỳ vọng bị chặn bởi mẫu State):");
        try {
            donHangService.huyDonHang(donHang.getMaDonHang());
        } catch (IllegalStateException loi) {
            System.out.println("   >> Bị từ chối đúng như thiết kế: " + loi.getMessage());
        }

        chiaDong("BƯỚC 8: MỘT ĐƠN HÀNG KHÁC BỊ HỦY KHI ĐANG CHỜ XÁC NHẬN (hoàn kho)");
        gioHangService.themVaoGio(khachHang.getMaNguoiDung(), sp2, 2);
        DonHang donHangThuHai = donHangService.datHang(khachHang,
                gioHangService.layGioHang(khachHang.getMaNguoiDung()), khachHang.getDiaChi());
        System.out.println("Tồn kho Samsung Galaxy S24 sau khi đặt: " + sp2.getSoLuongTonKho());
        donHangService.huyDonHang(donHangThuHai.getMaDonHang());
        System.out.println("Tồn kho Samsung Galaxy S24 sau khi hủy (đã hoàn kho): " + sp2.getSoLuongTonKho());

        chiaDong("BƯỚC 9: XEM LỊCH SỬ ĐƠN HÀNG CỦA KHÁCH HÀNG");
        List<DonHang> lichSu = donHangService.xemLichSuDonHang(khachHang);
        lichSu.forEach(dh -> System.out.println("   " + dh));
    }

    private static void chiaDong(String tieuDe) {
        System.out.println();
        System.out.println("=== " + tieuDe + " ===");
    }
}
