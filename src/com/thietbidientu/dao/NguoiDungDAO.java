package com.thietbidientu.dao;

import com.thietbidientu.model.NguoiDung;
import java.util.List;

public interface NguoiDungDAO {
    NguoiDung timTheoMa(int maNguoiDung);
    NguoiDung timTheoEmail(String email);
    List<NguoiDung> layTatCa();
    void luu(NguoiDung nguoiDung);
}
