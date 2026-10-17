package com.thietbidientu.singleton;

import com.thietbidientu.model.DanhMuc;
import com.thietbidientu.model.DonHang;
import com.thietbidientu.model.NguoiDung;
import com.thietbidientu.model.SanPham;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MẪU THIẾT KẾ SINGLETON.
 *
 * Đóng vai trò bộ nhớ phụ mô phỏng trong bản minh họa này (thay cho
 * một hệ quản trị cơ sở dữ liệu thật như đã thiết kế ở Chương 4).
 * Toàn bộ hệ thống chỉ có đúng một thực thể kho dữ liệu được chia sẻ
 * giữa các lớp DAO, tương tự cách một ứng dụng thật chỉ nên duy trì
 * một điểm kết nối cơ sở dữ liệu dùng chung thay vì mỗi nơi tự mở
 * kết nối riêng.
 */
public final class KhoDuLieu {

    private static KhoDuLieu instance;

    private final Map<Integer, NguoiDung> bangNguoiDung = new LinkedHashMap<>();
    private final Map<Integer, DanhMuc> bangDanhMuc = new LinkedHashMap<>();
    private final Map<Integer, SanPham> bangSanPham = new LinkedHashMap<>();
    private final Map<Integer, DonHang> bangDonHang = new LinkedHashMap<>();

    private KhoDuLieu() {
        System.out.println("[Singleton] Khởi tạo kho dữ liệu dùng chung (chỉ xảy ra một lần duy nhất).");
    }

    public static synchronized KhoDuLieu getInstance() {
        if (instance == null) {
            instance = new KhoDuLieu();
        }
        return instance;
    }

    public Map<Integer, NguoiDung> bangNguoiDung() { return bangNguoiDung; }
    public Map<Integer, DanhMuc> bangDanhMuc() { return bangDanhMuc; }
    public Map<Integer, SanPham> bangSanPham() { return bangSanPham; }
    public Map<Integer, DonHang> bangDonHang() { return bangDonHang; }
}
