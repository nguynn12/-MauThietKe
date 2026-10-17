package com.thietbidientu.dao;

import com.thietbidientu.model.SanPham;
import java.util.List;

/**
 * MẪU THIẾT KẾ DAO (Data Access Object).
 *
 * Che giấu chi tiết cách dữ liệu sản phẩm thực sự được lưu trữ, đúng
 * vai trò tầng truy xuất dữ liệu đã mô tả ở Chương 3, mục 3.1. Tầng
 * dịch vụ chỉ thao tác qua giao diện này, nếu sau này đổi từ kho dữ
 * liệu mô phỏng sang một cơ sở dữ liệu SQL thật thì chỉ cần viết lại
 * lớp cài đặt mà không ảnh hưởng đến tầng xử lý nghiệp vụ.
 */
public interface SanPhamDAO {
    SanPham timTheoMa(int maSanPham);
    List<SanPham> layTatCa();
    void luu(SanPham sanPham);
}
