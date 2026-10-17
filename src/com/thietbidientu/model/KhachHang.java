package com.thietbidientu.model;

/**
 * Khách hàng kế thừa toàn bộ thuộc tính và hành vi chung từ NguoiDung.
 * Các phương thức nghiệp vụ như timKiemSanPham(), datHang(), huyDonHang(),
 * xemLichSuDonHang() đã liệt kê ở Chương 3 được triển khai chi tiết tại
 * tầng xử lý nghiệp vụ (gói service), lớp này chỉ giữ vai trò định danh
 * đúng tinh thần bổ sung đối tượng điều khiển khi hiện thực hóa mô hình phân tích.
 */
public class KhachHang extends NguoiDung {

    public KhachHang(int maNguoiDung, String hoTen, String email,
                      String matKhau, String soDienThoai, String diaChi) {
        super(maNguoiDung, hoTen, email, matKhau, soDienThoai, diaChi);
    }

    @Override
    public VaiTro layVaiTro() {
        return VaiTro.KHACH_HANG;
    }
}
