package com.thietbidientu.model;

/**
 * Lớp cha trừu tượng cho ba nhóm người dùng, mang thuộc tính và
 * hành vi chung đã xác định ở sơ đồ lớp Chương 3, mục 3.2.
 * Đây là hiện thực Java của quan hệ Generalization giữa
 * KhachHang / NhanVien / QuanTriVien với NguoiDung.
 */
public abstract class NguoiDung {

    private final int maNguoiDung;
    private String hoTen;
    private String email;
    private String matKhau;
    private String soDienThoai;
    private String diaChi;

    protected NguoiDung(int maNguoiDung, String hoTen, String email,
                         String matKhau, String soDienThoai, String diaChi) {
        this.maNguoiDung = maNguoiDung;
        this.hoTen = hoTen;
        this.email = email;
        this.matKhau = matKhau;
        this.soDienThoai = soDienThoai;
        this.diaChi = diaChi;
    }

    public boolean dangNhap(String emailNhap, String matKhauNhap) {
        return this.email.equalsIgnoreCase(emailNhap) && this.matKhau.equals(matKhauNhap);
    }

    public void dangXuat() {
        System.out.println("[" + hoTen + "] đã đăng xuất khỏi hệ thống.");
    }

    public void capNhatThongTin(String hoTenMoi, String soDienThoaiMoi, String diaChiMoi) {
        this.hoTen = hoTenMoi;
        this.soDienThoai = soDienThoaiMoi;
        this.diaChi = diaChiMoi;
    }

    /** Mỗi lớp con phải khai báo đúng vai trò của mình. */
    public abstract VaiTro layVaiTro();

    public int getMaNguoiDung() { return maNguoiDung; }
    public String getHoTen() { return hoTen; }
    public String getEmail() { return email; }
    public String getSoDienThoai() { return soDienThoai; }
    public String getDiaChi() { return diaChi; }

    @Override
    public String toString() {
        return String.format("#%d %s (%s)", maNguoiDung, hoTen, layVaiTro());
    }
}
