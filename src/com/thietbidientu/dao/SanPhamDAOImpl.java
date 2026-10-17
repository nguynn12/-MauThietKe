package com.thietbidientu.dao;

import com.thietbidientu.model.SanPham;
import com.thietbidientu.singleton.KhoDuLieu;

import java.util.ArrayList;
import java.util.List;

public class SanPhamDAOImpl implements SanPhamDAO {

    private final KhoDuLieu khoDuLieu = KhoDuLieu.getInstance();

    @Override
    public SanPham timTheoMa(int maSanPham) {
        return khoDuLieu.bangSanPham().get(maSanPham);
    }

    @Override
    public List<SanPham> layTatCa() {
        return new ArrayList<>(khoDuLieu.bangSanPham().values());
    }

    @Override
    public void luu(SanPham sanPham) {
        khoDuLieu.bangSanPham().put(sanPham.getMaSanPham(), sanPham);
    }
}
