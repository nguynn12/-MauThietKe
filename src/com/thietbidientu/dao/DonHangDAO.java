package com.thietbidientu.dao;

import com.thietbidientu.model.DonHang;
import java.util.List;

public interface DonHangDAO {
    DonHang timTheoMa(int maDonHang);
    List<DonHang> layTatCa();
    List<DonHang> layTheoKhachHang(int maKhachHang);
    void luu(DonHang donHang);
}
