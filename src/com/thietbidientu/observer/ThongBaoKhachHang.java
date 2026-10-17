package com.thietbidientu.observer;

import com.thietbidientu.model.DonHang;

/**
 * Cài đặt minh họa: in ra màn hình console thay cho việc gửi email
 * hoặc thông báo đẩy trong một hệ thống thật. Nhờ mẫu Observer, nếu
 * sau này muốn đổi sang gửi email thật, chỉ cần viết thêm một lớp cài
 * đặt NguoiQuanSatDonHang khác mà không phải sửa lớp DonHang.
 */
public class ThongBaoKhachHang implements NguoiQuanSatDonHang {

    @Override
    public void capNhat(DonHang donHang) {
        System.out.println("   >> [Thông báo D6 gửi khách hàng] Đơn hàng #" + donHang.getMaDonHang()
                + " đã chuyển sang trạng thái: " + donHang.getTenTrangThai());
    }
}
