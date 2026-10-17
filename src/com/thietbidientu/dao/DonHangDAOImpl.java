package com.thietbidientu.dao;

import com.thietbidientu.model.DonHang;
import com.thietbidientu.singleton.KhoDuLieu;

import java.util.ArrayList;
import java.util.List;

public class DonHangDAOImpl implements DonHangDAO {

    private final KhoDuLieu khoDuLieu = KhoDuLieu.getInstance();

    @Override
    public DonHang timTheoMa(int maDonHang) {
        return khoDuLieu.bangDonHang().get(maDonHang);
    }

    @Override
    public List<DonHang> layTatCa() {
        return new ArrayList<>(khoDuLieu.bangDonHang().values());
    }

    @Override
    public List<DonHang> layTheoKhachHang(int maKhachHang) {
        List<DonHang> ketQua = new ArrayList<>();
        for (DonHang donHang : khoDuLieu.bangDonHang().values()) {
            if (donHang.getMaKhachHang() == maKhachHang) {
                ketQua.add(donHang);
            }
        }
        return ketQua;
    }

    @Override
    public void luu(DonHang donHang) {
        khoDuLieu.bangDonHang().put(donHang.getMaDonHang(), donHang);
    }
}
