# Chương 6 — Cài đặt minh họa

Website quản lý bán thiết bị điện tử — cài đặt minh họa bằng Java thuần
(không phụ thuộc thư viện ngoài, không cần cơ sở dữ liệu thật), bám sát
kiến trúc ba tầng và sơ đồ lớp đã chốt ở Chương 3, đồng thời áp dụng
sáu mẫu thiết kế GoF phục vụ luôn cho môn Mẫu thiết kế.

## Cấu trúc thư mục

```
src/com/thietbidientu/
  model/        Các lớp thực thể (NguoiDung, SanPham, DonHang...)
  state/        Mẫu State — vòng đời trạng thái đơn hàng
  observer/     Mẫu Observer — thông báo khi trạng thái đơn hàng đổi
  factory/      Mẫu Factory Method — khởi tạo đúng phân lớp người dùng
  builder/      Mẫu Builder — lắp ráp đơn hàng từ giỏ hàng
  strategy/     Mẫu Strategy — các tiêu chí lọc sản phẩm
  singleton/    Mẫu Singleton — kho dữ liệu dùng chung (mô phỏng CSDL)
  dao/          Tầng truy xuất dữ liệu (DAO / Repository)
  service/      Tầng xử lý nghiệp vụ
  Main.java     Chương trình minh họa (đóng vai trò tầng trình bày)
```

## Biên dịch và chạy

```bash
mkdir -p out
find src -name "*.java" > sources.txt
javac -d out -encoding UTF-8 @sources.txt
java -Dstdout.encoding=UTF-8 -cp out com.thietbidientu.Main
```

Yêu cầu JDK 17 trở lên (đã dùng cú pháp switch expression của Java 21).

## Bảng đối chiếu mẫu thiết kế và vị trí trong mã nguồn

| Mẫu thiết kế | Vị trí | Vai trò trong hệ thống |
|---|---|---|
| Singleton | `singleton/KhoDuLieu.java` | Một điểm lưu trữ dữ liệu dùng chung cho toàn bộ tầng DAO |
| Factory Method | `factory/NguoiDungFactory.java` | Khởi tạo đúng phân lớp Khách hàng / Nhân viên / Quản trị viên theo vai trò |
| Strategy | `strategy/*.java` | Các tiêu chí lọc sản phẩm độc lập, kết hợp linh hoạt |
| Builder | `builder/DonHangBuilder.java` | Lắp ráp đơn hàng qua nhiều bước đúng thứ tự nghiệp vụ |
| State | `state/*.java` | Quản lý vòng đời năm trạng thái của đơn hàng, chặn chuyển trạng thái không hợp lệ |
| Observer | `observer/*.java` | Thông báo cho khách hàng mỗi khi trạng thái đơn hàng thay đổi |
| DAO / Repository | `dao/*.java` | Tách biệt tầng truy xuất dữ liệu khỏi tầng xử lý nghiệp vụ |
