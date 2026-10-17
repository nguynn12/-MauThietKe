package com.thietbidientu.state;

import com.thietbidientu.model.DonHang;

public class ChoXacNhan implements TrangThaiDonHang {

    @Override
    public String tenTrangThai() { return "Chờ xác nhận"; }

    @Override
    public void xacNhan(DonHang donHang) {
        donHang.setTrangThaiHienTai(new DaXacNhan());
    }

    /** Đúng use case Hủy đơn hàng: chỉ được hủy khi còn ở trạng thái chờ xác nhận. */
    @Override
    public void huy(DonHang donHang) {
        donHang.hoanTraTonKho();
        donHang.setTrangThaiHienTai(new DaHuy());
    }
}
