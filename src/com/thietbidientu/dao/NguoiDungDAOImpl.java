package com.thietbidientu.dao;

import com.thietbidientu.model.NguoiDung;
import com.thietbidientu.singleton.KhoDuLieu;

import java.util.ArrayList;
import java.util.List;

public class NguoiDungDAOImpl implements NguoiDungDAO {

    private final KhoDuLieu khoDuLieu = KhoDuLieu.getInstance();

    @Override
    public NguoiDung timTheoMa(int maNguoiDung) {
        return khoDuLieu.bangNguoiDung().get(maNguoiDung);
    }

    @Override
    public NguoiDung timTheoEmail(String email) {
        for (NguoiDung nd : khoDuLieu.bangNguoiDung().values()) {
            if (nd.getEmail().equalsIgnoreCase(email)) {
                return nd;
            }
        }
        return null;
    }

    @Override
    public List<NguoiDung> layTatCa() {
        return new ArrayList<>(khoDuLieu.bangNguoiDung().values());
    }

    @Override
    public void luu(NguoiDung nguoiDung) {
        khoDuLieu.bangNguoiDung().put(nguoiDung.getMaNguoiDung(), nguoiDung);
    }
}
