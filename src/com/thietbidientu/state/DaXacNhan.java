package com.thietbidientu.state;

import com.thietbidientu.model.DonHang;

public class DaXacNhan implements TrangThaiDonHang {

    @Override
    public String tenTrangThai() { return "Đã xác nhận"; }

    @Override
    public void giao(DonHang donHang) {
        donHang.setTrangThaiHienTai(new DangGiao());
    }

    /**
     * Nhánh sự cố ở use case Xử lý đơn hàng: nhân viên vẫn có thể hủy
     * một đơn đã xác nhận nếu phát hiện sự cố, hệ thống hoàn kho tương tự.
     */
    @Override
    public void huy(DonHang donHang) {
        donHang.hoanTraTonKho();
        donHang.setTrangThaiHienTai(new DaHuy());
    }
}
