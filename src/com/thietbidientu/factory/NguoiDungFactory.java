package com.thietbidientu.factory;

import com.thietbidientu.model.KhachHang;
import com.thietbidientu.model.NguoiDung;
import com.thietbidientu.model.NhanVien;
import com.thietbidientu.model.QuanTriVien;
import com.thietbidientu.model.VaiTro;

/**
 * MẪU THIẾT KẾ FACTORY METHOD.
 *
 * Che giấu việc phải biết chính xác lớp con nào (KhachHang, NhanVien
 * hay QuanTriVien) cần khởi tạo. Tầng dịch vụ chỉ cần cung cấp giá
 * trị VaiTro đọc được từ cột VaiTro của bảng NGUOI_DUNG (Chương 4),
 * factory sẽ trả về đúng đối tượng tương ứng mà không cần dùng
 * if/else hoặc switch rải rác ở nhiều nơi trong chương trình.
 */
public final class NguoiDungFactory {

    private NguoiDungFactory() { }

    public static NguoiDung taoNguoiDung(VaiTro vaiTro, int maNguoiDung, String hoTen,
                                          String email, String matKhau,
                                          String soDienThoai, String diaChi) {
        return switch (vaiTro) {
            case KHACH_HANG -> new KhachHang(maNguoiDung, hoTen, email, matKhau, soDienThoai, diaChi);
            case NHAN_VIEN -> new NhanVien(maNguoiDung, hoTen, email, matKhau, soDienThoai, diaChi);
            case QUAN_TRI_VIEN -> new QuanTriVien(maNguoiDung, hoTen, email, matKhau, soDienThoai, diaChi);
        };
    }
}
