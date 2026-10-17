package com.thietbidientu.service;

import com.thietbidientu.dao.SanPhamDAO;
import com.thietbidientu.dao.SanPhamDAOImpl;
import com.thietbidientu.model.NhanVien;
import com.thietbidientu.model.SanPham;
import com.thietbidientu.strategy.DieuKienLoc;

import java.util.ArrayList;
import java.util.List;

/**
 * Tầng xử lý nghiệp vụ cho các use case Tìm kiếm và lọc sản phẩm,
 * Xem chi tiết sản phẩm, Quản lý sản phẩm và tồn kho.
 */
public class SanPhamService {

    private final SanPhamDAO sanPhamDAO = new SanPhamDAOImpl();

    /** Áp dụng lần lượt các chiến lược lọc (mẫu Strategy) theo kiểu AND. */
    public List<SanPham> timKiemSanPham(List<DieuKienLoc> dieuKiens) {
        List<SanPham> ketQua = new ArrayList<>();
        for (SanPham sanPham : sanPhamDAO.layTatCa()) {
            boolean thoaTatCa = true;
            for (DieuKienLoc dk : dieuKiens) {
                if (!dk.thoaDieuKien(sanPham)) {
                    thoaTatCa = false;
                    break;
                }
            }
            if (thoaTatCa) {
                ketQua.add(sanPham);
            }
        }
        return ketQua;
    }

    public void themSanPham(NhanVien nhanVien, SanPham sanPham) {
        sanPhamDAO.luu(sanPham);
        System.out.println("[" + nhanVien.getHoTen() + "] đã thêm sản phẩm mới: " + sanPham.getTenSanPham());
    }

    public SanPham timTheoMa(int maSanPham) {
        return sanPhamDAO.timTheoMa(maSanPham);
    }

    public List<SanPham> layTatCa() {
        return sanPhamDAO.layTatCa();
    }
}
