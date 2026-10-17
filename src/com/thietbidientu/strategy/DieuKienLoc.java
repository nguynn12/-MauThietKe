package com.thietbidientu.strategy;

import com.thietbidientu.model.SanPham;

/**
 * MẪU THIẾT KẾ STRATEGY.
 *
 * Ứng với use case Tìm kiếm và lọc sản phẩm, mỗi tiêu chí lọc trong
 * D1 (từ khóa, danh mục, khoảng giá) được tách thành một chiến lược
 * riêng cài đặt giao diện này. Tầng dịch vụ nhận vào một danh sách
 * các chiến lược và áp dụng lần lượt, cho phép thêm tiêu chí lọc mới
 * trong tương lai mà không cần sửa lại logic tìm kiếm đã có.
 */
public interface DieuKienLoc {
    boolean thoaDieuKien(SanPham sanPham);
}
