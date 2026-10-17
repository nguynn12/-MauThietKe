package com.thietbidientu.model;

public class SanPham {

    private final int maSanPham;
    private String tenSanPham;
    private long gia;
    private int soLuongTonKho;
    private String moTa;
    private DanhMuc danhMuc;

    public SanPham(int maSanPham, String tenSanPham, long gia,
                    int soLuongTonKho, String moTa, DanhMuc danhMuc) {
        this.maSanPham = maSanPham;
        this.tenSanPham = tenSanPham;
        this.gia = gia;
        this.soLuongTonKho = soLuongTonKho;
        this.moTa = moTa;
        this.danhMuc = danhMuc;
    }

    /** Quy tắc D3/D5 của DFD Đặt hàng: chỉ cho phép mua khi tồn kho đủ. */
    public boolean kiemTraTonKho(int soLuongCan) {
        return soLuongCan > 0 && soLuongCan <= soLuongTonKho;
    }

    /**
     * Trừ hoặc hoàn kho. Dùng số âm để trừ, số dương để hoàn,
     * tương ứng D4 của DFD Đặt hàng và DFD Xử lý đơn hàng (nhánh hủy).
     */
    public void capNhatTonKho(int delta) {
        int soLuongMoi = this.soLuongTonKho + delta;
        if (soLuongMoi < 0) {
            throw new IllegalStateException(
                "Không thể cập nhật tồn kho: số lượng sau khi cập nhật bị âm cho sản phẩm " + tenSanPham);
        }
        this.soLuongTonKho = soLuongMoi;
    }

    /** Cập nhật trên cùng đối tượng để giỏ hàng vẫn tham chiếu đúng sản phẩm. */
    public void capNhatThongTin(String ten, long giaMoi, int ton, String mota, DanhMuc dm) {
        if (ten == null || ten.isBlank() || giaMoi <= 0 || ton < 0 || dm == null)
            throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ.");
        tenSanPham = ten; gia = giaMoi; soLuongTonKho = ton; moTa = mota; danhMuc = dm;
    }

    public int getMaSanPham() { return maSanPham; }
    public String getTenSanPham() { return tenSanPham; }
    public long getGia() { return gia; }
    public int getSoLuongTonKho() { return soLuongTonKho; }
    public String getMoTa() { return moTa; }
    public DanhMuc getDanhMuc() { return danhMuc; }

    @Override
    public String toString() {
        return String.format("#%d %-20s | %,10d đ | tồn kho: %-3d | %s",
                maSanPham, tenSanPham, gia, soLuongTonKho, danhMuc);
    }
}
