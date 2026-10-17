package com.thietbidientu.observer;

import com.thietbidientu.model.DonHang;

/**
 * MẪU THIẾT KẾ OBSERVER.
 *
 * Mỗi khi trạng thái đơn hàng thay đổi, DonHang sẽ thông báo cho tất
 * cả các đối tượng quan sát đã đăng ký, tương ứng đúng luồng D6
 * "thông báo trả về cho người dùng" đã xuất hiện xuyên suốt các DFD
 * ở Chương 2, mà không cần DonHang biết cụ thể ai đang lắng nghe hay
 * họ thông báo bằng cách nào.
 */
public interface NguoiQuanSatDonHang {
    void capNhat(DonHang donHang);
}
