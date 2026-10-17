package com.thietbidientu.state;

/** Trạng thái kết thúc, không còn chuyển tiếp nào hợp lệ nữa. */
public class DaHuy implements TrangThaiDonHang {

    @Override
    public String tenTrangThai() { return "Đã hủy"; }
}
