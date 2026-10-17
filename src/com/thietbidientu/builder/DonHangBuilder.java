package com.thietbidientu.builder;

import com.thietbidientu.model.ChiTietDonHang;
import com.thietbidientu.model.DonHang;
import com.thietbidientu.model.GioHang;
import com.thietbidientu.model.SanPham;
import com.thietbidientu.observer.NguoiQuanSatDonHang;

import java.util.ArrayList;
import java.util.List;

/**
 * MẪU THIẾT KẾ BUILDER.
 *
 * Việc tạo một đơn hàng đúng theo dòng sự kiện chính của use case Đặt
 * hàng ở Chương 2 gồm nhiều bước phải thực hiện theo đúng thứ tự,
 * kiểm tra tồn kho, tạo từng dòng chi tiết với đơn giá cố định, trừ
 * kho, rồi mới hoàn tất đơn. Lớp builder này gói toàn bộ trình tự đó
 * lại thành một chuỗi lời gọi rõ ràng, tránh việc lớp gọi phải tự
 * nhớ đúng thứ tự thao tác.
 */
public class DonHangBuilder {

    private final DonHang donHang;
    private final List<NguoiQuanSatDonHang> nguoiQuanSat = new ArrayList<>();

    public DonHangBuilder(int maDonHang, int maKhachHang, String diaChiGiaoHang) {
        this.donHang = new DonHang(maDonHang, maKhachHang, diaChiGiaoHang);
    }

    public DonHangBuilder tuGioHang(GioHang gioHang) {
        gioHang.getDanhSachChiTiet().forEach(ctGioHang -> {
            SanPham sanPham = ctGioHang.getSanPham();
            int soLuong = ctGioHang.getSoLuong();
            // Bước 5 của dòng sự kiện chính: kiểm tra lại tồn kho thực tế.
            if (!sanPham.kiemTraTonKho(soLuong)) {
                throw new IllegalStateException(
                    "Sản phẩm " + sanPham.getTenSanPham() + " không đủ tồn kho để đặt hàng.");
            }
            donHang.themChiTiet(new ChiTietDonHang(sanPham, soLuong, sanPham.getGia()));
        });
        return this;
    }

    public DonHangBuilder dangKyQuanSat(NguoiQuanSatDonHang quanSat) {
        this.nguoiQuanSat.add(quanSat);
        return this;
    }

    public DonHang xayDung() {
        if (donHang.getDanhSachChiTiet().isEmpty()) {
            throw new IllegalStateException("Không thể tạo đơn hàng không có sản phẩm nào.");
        }
        // Bước 6: trừ kho ngay khi đơn hàng được tạo.
        donHang.getDanhSachChiTiet().forEach(ct -> ct.getSanPham().capNhatTonKho(-ct.getSoLuong()));
        nguoiQuanSat.forEach(donHang::themQuanSat);
        return donHang;
    }
}
