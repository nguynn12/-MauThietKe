package com.thietbidientu.state;

import com.thietbidientu.model.DonHang;

/**
 * MẪU THIẾT KẾ STATE.
 *
 * Mỗi trạng thái trong vòng đời đơn hàng (Chờ xác nhận, Đã xác nhận,
 * Đang giao, Hoàn tất, Đã hủy - đã liệt kê ở ràng buộc toàn vẹn
 * Chương 4) được hiện thực thành một lớp riêng cài đặt giao diện này.
 * Lớp DonHang chỉ giữ một tham chiếu đến trạng thái hiện tại và ủy
 * quyền toàn bộ logic chuyển trạng thái cho đối tượng trạng thái đó,
 * thay vì dùng chuỗi if/else kiểm tra trạng thái ở khắp nơi.
 */
public interface TrangThaiDonHang {

    String tenTrangThai();

    default void xacNhan(DonHang donHang) {
        throw new IllegalStateException(
            "Không thể xác nhận đơn hàng đang ở trạng thái " + tenTrangThai());
    }

    default void giao(DonHang donHang) {
        throw new IllegalStateException(
            "Không thể chuyển sang giao hàng khi đơn đang ở trạng thái " + tenTrangThai());
    }

    default void hoanTat(DonHang donHang) {
        throw new IllegalStateException(
            "Không thể hoàn tất đơn hàng đang ở trạng thái " + tenTrangThai());
    }

    default void huy(DonHang donHang) {
        throw new IllegalStateException(
            "Không thể hủy đơn hàng đang ở trạng thái " + tenTrangThai());
    }
}
