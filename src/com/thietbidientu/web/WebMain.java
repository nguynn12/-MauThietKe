package com.thietbidientu.web;

import com.sun.net.httpserver.*;
import com.thietbidientu.dao.*;
import com.thietbidientu.factory.NguoiDungFactory;
import com.thietbidientu.model.*;
import com.thietbidientu.service.*;
import com.thietbidientu.singleton.KhoDuLieu;
import com.thietbidientu.strategy.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Tầng trình bày HTTP: các thao tác mua hàng gọi service/model của nhóm. */
public class WebMain {
    private static final KhoDuLieu db = KhoDuLieu.getInstance();
    private static final NguoiDungDAO users = new NguoiDungDAOImpl();
    private static final SanPhamService products = new SanPhamService();
    private static final GioHangService carts = new GioHangService();
    private static final DonHangService orders = new DonHangService();
    private static final Map<String, Session> sessions = new HashMap<>();
    private static int nextUser = 4, nextProduct = 7, nextCategory = 4;

    private static class Session {
        NguoiDung user;
        final String csrf = UUID.randomUUID().toString();
        String flash = "";
    }

    public static void main(String[] args) throws Exception {
        seed();
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", WebMain::handle);
        // Một luồng xử lý bảo vệ các collection và thao tác đặt hàng trong bộ nhớ.
        server.setExecutor(null);
        server.start();
        System.out.println("Giao dien: http://127.0.0.1:" + port + " | Ctrl+C de dung");
    }

    private static void seed() {
        String[] names = {"Điện thoại", "Laptop", "Phụ kiện"};
        for (int i = 0; i < names.length; i++) db.bangDanhMuc().put(i + 1, new DanhMuc(i + 1, names[i]));
        users.luu(NguoiDungFactory.taoNguoiDung(VaiTro.KHACH_HANG, 1, "Nguyễn Văn A", "a@gmail.com", "matkhau123", "0900000001", "12 Trần Phú, Đà Lạt"));
        users.luu(NguoiDungFactory.taoNguoiDung(VaiTro.NHAN_VIEN, 2, "Trần Thị B", "b@shop.vn", "matkhau456", "0900000002", "Đà Lạt"));
        users.luu(NguoiDungFactory.taoNguoiDung(VaiTro.QUAN_TRI_VIEN, 3, "Quản trị viên", "admin@shop.vn", "admin123", "0900000003", "Đà Lạt"));
        String[] titles = {"iPhone 15", "Samsung Galaxy S24", "MacBook Air M2", "ASUS Vivobook 15", "Tai nghe Bluetooth", "Chuột không dây"};
        long[] prices = {22000000, 18000000, 27000000, 15500000, 890000, 390000};
        int[] stock = {10, 5, 3, 8, 20, 15};
        String[] desc = {
            "Màn hình OLED Super Retina 6.1 inch, chip A16 Bionic hiệu năng cao, camera sắc nét.",
            "Thiết kế khung kim loại cao cấp, màn hình Dynamic AMOLED 2X và bộ xử lý hiện đại.",
            "Thiết kế siêu mỏng nhẹ, chip Apple M2 tiết kiệm năng lượng, thời lượng pin bền bỉ.",
            "Màn hình lớn 15.6 inch Full HD, bàn phím số đầy đủ, đáp ứng tốt học tập và văn phòng.",
            "Kết nối Bluetooth ổn định, đệm tai êm ái, âm bass rõ ràng và thời lượng pin cả ngày.",
            "Kết nối không dây 2.4GHz nhạy bén, thiết kế công thái học vừa vặn lòng bàn tay."
        };
        for (int i = 0; i < titles.length; i++) {
            products.themSanPham((NhanVien) users.timTheoMa(2), new SanPham(i + 1, titles[i], prices[i], stock[i], desc[i], db.bangDanhMuc().get(i / 2 + 1)));
        }
    }

    private static void handle(HttpExchange x) throws IOException {
        String path = x.getRequestURI().getPath();
        if (path.equals("/style.css")) {
            send(x, 200, "text/css", CSS);
            return;
        }
        Session s = session(x);
        try {
            if (x.getRequestMethod().equals("POST")) {
                byte[] bytes = x.getRequestBody().readNBytes(65537);
                require(bytes.length <= 65536, "Biểu mẫu quá lớn.");
                Map<String, String> f = parse(new String(bytes, StandardCharsets.UTF_8));
                require(s.csrf.equals(f.get("csrf")), "Phiên biểu mẫu đã hết hiệu lực. Vui lòng tải lại trang.");
                String target = action(s, f);
                redirect(x, target);
                return;
            }
            if (!x.getRequestMethod().equals("GET")) {
                send(x, 405, "text/plain", "Method not allowed");
                return;
            }
            Map<String, String> q = parse(x.getRequestURI().getRawQuery());
            String body = switch (path) {
                case "/" -> catalog(s, q);
                case "/product" -> detail(s, id(q));
                case "/login" -> login(s);
                case "/register" -> register(s);
                case "/cart" -> cart(s);
                case "/orders" -> orderList(s, q);
                case "/invoice" -> invoice(s, id(q));
                case "/profile" -> profile(s);
                case "/manage" -> manage(s, q);
                case "/categories" -> categories(s);
                case "/accounts" -> accounts(s);
                default -> throw new IllegalArgumentException("Không tìm thấy trang.");
            };
            send(x, 200, "text/html", layout(s, path, body));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            send(x, 400, "text/html", layout(s, path, "<section class='panel error-panel'><span class='eyebrow'>THÔNG BÁO HỆ THỐNG</span><h1>Không thể thực hiện yêu cầu</h1><p role='alert' class='alert-msg'>" + h(ex.getMessage()) + "</p><div class='panel-actions'><a class='button primary' href='/'>Về cửa hàng</a> <a class='button secondary' href='/cart'>Xem giỏ hàng</a></div></section>"));
        } catch (Exception ex) {
            ex.printStackTrace();
            send(x, 500, "text/html", layout(s, path, "<section class='panel error-panel'><h1>Có lỗi xử lý phía máy chủ</h1><p>Vui lòng thử lại hoặc xem thông tin trong cửa sổ chạy Java.</p><a class='button primary' href='/'>Về trang chủ</a></section>"));
        } finally {
            x.close();
        }
    }

    private static Session session(HttpExchange x) {
        String cookie = x.getRequestHeaders().getFirst("Cookie");
        String key = null;
        if (cookie != null) {
            for (String part : cookie.split(";")) {
                String p = part.trim();
                if (p.startsWith("sid=")) key = p.substring(4);
            }
        }
        if (key == null || !sessions.containsKey(key)) {
            key = UUID.randomUUID().toString();
            sessions.put(key, new Session());
            x.getResponseHeaders().add("Set-Cookie", "sid=" + key + "; Path=/; HttpOnly; SameSite=Strict");
        }
        return sessions.get(key);
    }

    private static String action(Session s, Map<String, String> f) {
        String op = f.getOrDefault("op", "");
        switch (op) {
            case "login": {
                NguoiDung u = users.timTheoEmail(required(f, "email"));
                require(u != null && u.dangNhap(f.get("email"), f.getOrDefault("password", "")), "Email hoặc mật khẩu không đúng.");
                s.user = u;
                s.flash = "Đăng nhập thành công. Chào mừng " + u.getHoTen() + "!";
                return "/";
            }
            case "register": {
                String name = required(f, "name"), email = email(f), password = required(f, "password"), phone = required(f, "phone"), address = required(f, "address");
                require(users.timTheoEmail(email) == null, "Email đã được đăng ký.");
                require(password.length() >= 6, "Mật khẩu cần ít nhất 6 ký tự.");
                NguoiDung u = NguoiDungFactory.taoNguoiDung(VaiTro.KHACH_HANG, nextUser++, name, email, password, phone, address);
                users.luu(u);
                s.user = u;
                s.flash = "Tạo tài khoản thành công. Bạn đã đăng nhập với vai trò Khách hàng.";
                return "/";
            }
            case "logout": {
                auth(s);
                s.user.dangXuat();
                s.user = null;
                s.flash = "Đã đăng xuất khỏi tài khoản.";
                return "/login";
            }
            case "profile": {
                auth(s);
                s.user.capNhatThongTin(required(f, "name"), required(f, "phone"), required(f, "address"));
                s.flash = "Đã cập nhật thông tin cá nhân.";
                return "/profile";
            }
            case "add": {
                KhachHang c = customer(s);
                SanPham p = product(id(f));
                int n = number(f, "quantity", 1, 9999);
                carts.themVaoGio(c.getMaNguoiDung(), p, n);
                s.flash = "Đã thêm " + p.getTenSanPham() + " (SL: " + n + ") vào giỏ hàng.";
                return "/cart";
            }
            case "quantity": {
                KhachHang c = customer(s);
                SanPham p = product(id(f));
                int n = number(f, "quantity", 1, 9999);
                require(p.kiemTraTonKho(n), "Số lượng vượt quá tồn kho.");
                var line = carts.layGioHang(c.getMaNguoiDung()).getDanhSachChiTiet().stream()
                        .filter(t -> t.getSanPham().getMaSanPham() == p.getMaSanPham())
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không có trong giỏ."));
                line.setSoLuong(n);
                s.flash = "Đã cập nhật số lượng sản phẩm trong giỏ.";
                return "/cart";
            }
            case "remove": {
                carts.xoaKhoiGio(customer(s).getMaNguoiDung(), id(f));
                s.flash = "Đã xóa sản phẩm khỏi giỏ hàng.";
                return "/cart";
            }
            case "checkout": {
                KhachHang c = customer(s);
                String address = required(f, "address");
                DonHang d = orders.datHang(c, carts.layGioHang(c.getMaNguoiDung()), address);
                s.flash = "Đặt hàng thành công! Mã đơn #" + d.getMaDonHang() + ". Thanh toán khi nhận hàng.";
                return "/orders";
            }
            case "cancel": {
                DonHang d = ownedOrder(s, id(f));
                require(staff(s) || d.getTenTrangThai().equals("Chờ xác nhận"), "Khách hàng chỉ được hủy đơn đang chờ xác nhận.");
                orders.huyDonHang(d.getMaDonHang());
                s.flash = "Đã hủy đơn #" + d.getMaDonHang() + " và hoàn trả tồn kho sản phẩm.";
                return "/orders";
            }
            case "confirm":
            case "ship":
            case "complete": {
                staffOnly(s);
                int id = id(f);
                ownedOrder(s, id);
                if (op.equals("confirm")) orders.xacNhanDonHang(employee(s), id);
                else if (op.equals("ship")) orders.giaoHang(id);
                else orders.hoanTatDonHang(id);
                s.flash = "Đã cập nhật trạng thái đơn #" + id + ".";
                return "/orders";
            }
            case "product-save": {
                staffOnly(s);
                int id = Integer.parseInt(f.getOrDefault("id", "0"));
                String name = required(f, "name"), description = required(f, "description");
                long price = Long.parseLong(required(f, "price"));
                require(price > 0 && price <= 1000000000L, "Giá phải lớn hơn 0 và không quá 1 tỷ đồng.");
                int stock = number(f, "stock", 0, 1000000);
                int cat = number(f, "category", 1, Integer.MAX_VALUE);
                DanhMuc category = db.bangDanhMuc().get(cat);
                require(category != null, "Danh mục không tồn tại.");
                if (id == 0) products.themSanPham(employee(s), new SanPham(nextProduct++, name, price, stock, description, category));
                else product(id).capNhatThongTin(name, price, stock, description, category);
                s.flash = "Đã lưu thông tin sản phẩm thành công.";
                return "/manage";
            }
            case "product-delete": {
                staffOnly(s);
                int id = id(f);
                product(id);
                boolean used = db.bangDonHang().values().stream()
                        .anyMatch(d -> d.getDanhSachChiTiet().stream().anyMatch(t -> t.getSanPham().getMaSanPham() == id));
                require(!used, "Sản phẩm đã có trong đơn hàng. Hãy cập nhật tồn kho về 0 thay vì xóa.");
                for (NguoiDung u : users.layTatCa()) {
                    if (u instanceof KhachHang) carts.xoaKhoiGio(u.getMaNguoiDung(), id);
                }
                db.bangSanPham().remove(id);
                s.flash = "Đã xóa sản phẩm khỏi kho hàng.";
                return "/manage";
            }
            case "category-save": {
                adminOnly(s);
                String name = required(f, "name");
                int id = Integer.parseInt(f.getOrDefault("id", "0"));
                require(db.bangDanhMuc().values().stream().noneMatch(c -> c.getMaDanhMuc() != id && c.getTenDanhMuc().equalsIgnoreCase(name)), "Tên danh mục đã tồn tại.");
                if (id == 0) {
                    int n = nextCategory++;
                    db.bangDanhMuc().put(n, new DanhMuc(n, name));
                } else {
                    DanhMuc c = db.bangDanhMuc().get(id);
                    require(c != null, "Không tìm thấy danh mục.");
                    c.setTenDanhMuc(name);
                }
                s.flash = "Đã lưu danh mục thành công.";
                return "/categories";
            }
            case "category-delete": {
                adminOnly(s);
                int id = id(f);
                require(products.layTatCa().stream().noneMatch(p -> p.getDanhMuc().getMaDanhMuc() == id), "Danh mục còn sản phẩm, không thể xóa.");
                db.bangDanhMuc().remove(id);
                s.flash = "Đã xóa danh mục.";
                return "/categories";
            }
            case "account-add": {
                adminOnly(s);
                String email = email(f);
                require(users.timTheoEmail(email) == null, "Email đã tồn tại trong hệ thống.");
                String password = required(f, "password");
                require(password.length() >= 6, "Mật khẩu cần ít nhất 6 ký tự.");
                users.luu(NguoiDungFactory.taoNguoiDung(VaiTro.valueOf(required(f, "role")), nextUser++, required(f, "name"), email, password, required(f, "phone"), required(f, "address")));
                s.flash = "Đã tạo tài khoản mới thành công.";
                return "/accounts";
            }
            default:
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
        }
    }

    private static String catalog(Session s, Map<String, String> q) {
        String keyword = q.getOrDefault("q", "").trim();
        int cat = Integer.parseInt(q.getOrDefault("category", "0"));
        List<DieuKienLoc> filters = new ArrayList<>();
        if (!keyword.isBlank()) filters.add(new LocTheoTuKhoa(keyword));
        if (cat > 0) filters.add(new LocTheoDanhMuc(cat));
        String max = q.getOrDefault("max", "");
        if (!max.isBlank()) {
            long limit = Long.parseLong(max);
            require(limit >= 0, "Giá tối đa không hợp lệ.");
            filters.add(new LocTheoKhoangGia(0, limit));
        }
        var list = products.timKiemSanPham(filters);

        StringBuilder b = new StringBuilder();
        b.append("<section class='store-banner'>");
        b.append("<div class='banner-content'>");
        b.append("<span class='banner-badge'>HỆ THỐNG THIẾT BỊ ĐIỆN TỬ TECHNEST</span>");
        b.append("<h1>Thiết Bị Công Nghệ Chính Hãng · Giá Tốt Mỗi Ngày</h1>");
        b.append("<p>Cung cấp điện thoại thông minh, máy tính xách tay và phụ kiện chính hãng phục vụ học tập, công việc và giải trí.</p>");
        b.append("</div>");
        b.append("<div class='banner-perks'>");
        b.append("<div class='perk-item'><span class='perk-icon'>✓</span><div><strong>100% Chính hãng</strong><small>Cam kết chất lượng</small></div></div>");
        b.append("<div class='perk-item'><span class='perk-icon'>✓</span><div><strong>Giao hàng COD</strong><small>Kiểm tra khi nhận</small></div></div>");
        b.append("<div class='perk-item'><span class='perk-icon'>✓</span><div><strong>Bảo hành uy tín</strong><small>Đổi trả linh hoạt</small></div></div>");
        b.append("</div>");
        b.append("</section>");

        b.append("<section id='catalog'>");
        b.append("<div class='panel filter-panel'>");
        b.append("<div class='filter-header'>");
        b.append("<div><span class='eyebrow'>DANH MỤC & TÌM KIẾM</span><h2 class='filter-title'>Bộ lọc sản phẩm</h2></div>");
        b.append("<span class='product-count'>Hiển thị <strong>").append(list.size()).append("</strong> sản phẩm</span>");
        b.append("</div>");
        b.append("<form class='filters' method='get' action='/#catalog'>");
        b.append("<label>Tìm theo tên sản phẩm<input name='q' placeholder='VD: iPhone, MacBook, Tai nghe…' value='").append(h(keyword)).append("'></label>");
        b.append("<label>Danh mục thiết bị<select name='category'><option value='0'>Tất cả danh mục</option>").append(categoryOptions(cat)).append("</select></label>");
        b.append("<label>Giá tối đa (₫)<input name='max' type='number' min='0' placeholder='Không giới hạn giá' value='").append(h(max)).append("'></label>");
        b.append("<div class='filter-buttons'><button type='submit' class='button primary'>Lọc sản phẩm</button><a class='button secondary' href='/#catalog'>Đặt lại</a></div>");
        b.append("</form>");
        b.append("</div>");

        b.append("<div class='grid'>");
        for (SanPham p : list) {
            b.append("<article class='product'>");
            b.append("<a class='product-art' href='/product?id=").append(p.getMaSanPham()).append("'>");
            b.append(productArt(p.getMaSanPham(), p.getDanhMuc().getTenDanhMuc(), false));
            b.append("</a>");
            b.append("<div class='product-info'>");
            b.append("<div class='product-meta'>");
            b.append("<span class='product-cat'>").append(h(p.getDanhMuc().getTenDanhMuc())).append("</span>");
            b.append("<span class='stock-tag ").append(p.getSoLuongTonKho() > 0 ? "in-stock" : "out-stock").append("'>")
             .append(p.getSoLuongTonKho() > 0 ? "Còn " + p.getSoLuongTonKho() + " sp" : "Hết hàng")
             .append("</span>");
            b.append("</div>");
            b.append("<h3><a href='/product?id=").append(p.getMaSanPham()).append("'>").append(h(p.getTenSanPham())).append("</a></h3>");
            b.append("<div class='price-row'>");
            b.append("<strong>").append(money(p.getGia())).append("</strong>");
            b.append("</div>");
            b.append("<a class='button secondary wide' href='/product?id=").append(p.getMaSanPham()).append("'>Xem chi tiết →</a>");
            b.append("</div>");
            b.append("</article>");
        }
        if (list.isEmpty()) {
            b.append("<div class='panel empty col-span-3'><div class='empty-icon-wrap'>🔍</div><h3>Không tìm thấy sản phẩm phù hợp</h3><p class='muted'>Hãy thử đổi từ khóa tìm kiếm hoặc bấm Đặt lại để xem tất cả sản phẩm.</p><a class='button primary' href='/#catalog'>Xem tất cả sản phẩm</a></div>");
        }
        b.append("</div></section>");
        return b.toString();
    }

    private static String detail(Session s, int id) {
        SanPham p = product(id);
        String action;
        if (s.user instanceof KhachHang) {
            action = form(s, "add", hidden("id", id) +
                    "<div class='detail-buy-row'><label class='qty-label'>Số lượng đặt mua<input name='quantity' type='number' min='1' max='" + p.getSoLuongTonKho() + "' value='1' required></label>" +
                    "<button class='button primary btn-add-cart' " + (p.getSoLuongTonKho() == 0 ? "disabled" : "") + ">" + (p.getSoLuongTonKho() == 0 ? "Tạm hết hàng" : "Thêm vào giỏ hàng") + "</button></div>");
        } else if (s.user == null) {
            action = "<div class='detail-action-prompt'><p class='muted'>Vui lòng đăng nhập tài khoản Khách hàng để chọn mua thiết bị.</p><a class='button primary' href='/login'>Đăng nhập để mua hàng</a></div>";
        } else {
            action = "<div class='detail-action-prompt'><p class='muted'>Bạn đang đăng nhập vai trò " + role(s.user) + ".</p><a class='button secondary' href='/manage?edit=" + id + "'>Chỉnh sửa sản phẩm này</a></div>";
        }

        StringBuilder b = new StringBuilder();
        b.append("<div class='breadcrumb-bar'><a class='back' href='/'>← Quay lại cửa hàng</a><span class='sep'>/</span><span class='crumb-cat'>").append(h(p.getDanhMuc().getTenDanhMuc())).append("</span><span class='sep'>/</span><span class='crumb-cur'>").append(h(p.getTenSanPham())).append("</span></div>");
        b.append("<section class='detail'>");
        b.append("<div class='detail-art'>");
        b.append(productArt(id, p.getDanhMuc().getTenDanhMuc(), true));
        b.append("</div>");
        b.append("<div class='panel detail-content'>");
        b.append("<div class='detail-header'>");
        b.append("<span class='product-cat'>").append(h(p.getDanhMuc().getTenDanhMuc())).append("</span>");
        b.append("<h1>").append(h(p.getTenSanPham())).append("</h1>");
        b.append("<div class='big-price'>").append(money(p.getGia())).append(" <span class='vat-note'>(Đã gồm VAT)</span></div>");
        b.append("</div>");
        b.append("<div class='detail-desc'><span class='desc-title'>Mô tả sản phẩm</span><p>").append(h(p.getMoTa())).append("</p></div>");
        b.append("<div class='stock-box'><span class='stock-dot ").append(p.getSoLuongTonKho() > 0 ? "available" : "unavailable").append("'></span><span class='stock'>Tồn kho: ").append(p.getSoLuongTonKho()).append(" sản phẩm</span></div>");
        b.append(action);
        b.append("<div class='trust-box'>");
        b.append("<div class='trust-item'><span class='trust-icon'>✓</span><span>Thanh toán COD khi nhận</span></div>");
        b.append("<div class='trust-item'><span class='trust-icon'>✓</span><span>Kiểm tra hàng trước khi nhận</span></div>");
        b.append("<div class='trust-item'><span class='trust-icon'>✓</span><span>Bảo hành theo tiêu chuẩn hãng</span></div>");
        b.append("</div>");
        b.append("</div>");
        b.append("</section>");
        return b.toString();
    }

    private static String login(Session s) {
        return "<section class='auth panel'><div class='auth-header'><span class='eyebrow'>ĐĂNG NHẬP HỆ THỐNG</span><h1>Đăng nhập TechNest</h1><p class='muted'>Truy cập để quản lý giỏ hàng, xem tiến độ đơn hàng và mua sắm.</p></div>" +
                form(s, "login", input("email", "Địa chỉ Email", "", "email", "placeholder='a@gmail.com, b@shop.vn…'") +
                        input("password", "Mật khẩu", "", "password", "placeholder='Nhập mật khẩu…'") +
                        "<button class='button primary wide btn-auth-submit'>Đăng nhập ngay</button>") +
                "<div class='auth-footer'><p>Chưa có tài khoản khách hàng? <a href='/register'>Đăng ký tại đây</a></p></div>" +
                "<div class='note demo-box'><div class='demo-title'>TÀI KHOẢN MẪU DÙNG ĐÁNH GIÁ ĐỒ ÁN</div>" +
                "<table class='demo-table'><thead><tr><th>Vai trò</th><th>Email</th><th>Mật khẩu</th></tr></thead><tbody>" +
                "<tr><td><span class='role-tag role-customer'>Khách hàng</span></td><td><code>a@gmail.com</code></td><td><code>matkhau123</code></td></tr>" +
                "<tr><td><span class='role-tag role-staff'>Nhân viên</span></td><td><code>b@shop.vn</code></td><td><code>matkhau456</code></td></tr>" +
                "<tr><td><span class='role-tag role-admin'>Quản trị viên</span></td><td><code>admin@shop.vn</code></td><td><code>admin123</code></td></tr>" +
                "</tbody></table></div></section>";
    }

    private static String register(Session s) {
        return "<section class='auth panel'><div class='auth-header'><span class='eyebrow'>TẠO TÀI KHOẢN MỚI</span><h1>Đăng ký Khách hàng</h1><p class='muted'>Đăng ký tài khoản để mua sắm thiết bị điện tử và theo dõi đơn hàng.</p></div>" +
                form(s, "register", input("name", "Họ và tên", "", "text", "placeholder='Nguyễn Văn A'") +
                        input("email", "Địa chỉ Email", "", "email", "placeholder='vidu@gmail.com'") +
                        input("password", "Mật khẩu", "", "password", "minlength='6' placeholder='Tối thiểu 6 ký tự'") +
                        input("phone", "Số điện thoại liên hệ", "", "tel", "placeholder='0900000001'") +
                        input("address", "Địa chỉ giao hàng", "", "text", "placeholder='Số nhà, tên đường, phường/xã, TP Đà Lạt'") +
                        "<button class='button primary wide btn-auth-submit'>Xác nhận đăng ký</button>") +
                "<div class='auth-footer'><p>Đã có tài khoản? <a href='/login'>Đăng nhập ngay</a></p></div></section>";
    }

    private static String profile(Session s) {
        auth(s);
        NguoiDung u = s.user;
        return heading("TÀI KHOẢN", "Thông tin cá nhân", "Cập nhật họ tên, số điện thoại và địa chỉ giao hàng mặc định.") +
                "<div class='profile-layout'><section class='panel profile-card'>" +
                "<div class='profile-header'><div class='profile-avatar'>" + h(u.getHoTen().substring(0, 1).toUpperCase()) + "</div><div><h2>" + h(u.getHoTen()) + "</h2><p class='muted'>" + h(u.getEmail()) + "</p>" + roleBadge(u) + "</div></div>" +
                form(s, "profile", input("name", "Họ và tên", u.getHoTen(), "text", "") +
                        input("phone", "Số điện thoại", u.getSoDienThoai(), "tel", "") +
                        input("address", "Địa chỉ giao hàng mặc định", u.getDiaChi(), "text", "") +
                        "<button class='button primary'>Lưu thay đổi thông tin</button>") +
                "</section></div>";
    }

    private static String cart(Session s) {
        KhachHang c = customer(s);
        GioHang g = carts.layGioHang(c.getMaNguoiDung());
        StringBuilder b = new StringBuilder(heading("MUA SẮM", "Giỏ hàng của bạn", "Kiểm tra danh sách thiết bị trước khi xác nhận đặt hàng (COD)."));
        if (g.getDanhSachChiTiet().isEmpty()) {
            return b + "<section class='panel empty'><div class='empty-icon-wrap'>🛒</div><h2>Giỏ hàng của bạn đang trống</h2><p class='muted'>Hãy chọn những thiết bị điện tử ưng ý từ cửa hàng để bắt đầu đặt hàng.</p><a class='button primary' href='/'>Khám phá sản phẩm ngay</a></section>";
        }
        b.append("<div class='checkout'>");
        b.append("<section class='panel'><div class='table-wrap'><table class='data-table cart-table'>");
        b.append("<thead><tr><th>Thiết bị điện tử</th><th class='right'>Đơn giá</th><th class='center'>Số lượng</th><th class='right'>Thành tiền</th><th class='center'>Thao tác</th></tr></thead><tbody>");
        for (var t : g.getDanhSachChiTiet()) {
            b.append("<tr>");
            b.append("<td><div class='cart-item-title'><strong>").append(h(t.getSanPham().getTenSanPham())).append("</strong></div>")
             .append("<span class='muted sub-meta'>").append(h(t.getSanPham().getDanhMuc().getTenDanhMuc())).append(" · Tồn kho: ").append(t.getSanPham().getSoLuongTonKho()).append(" sp</span></td>");
            b.append("<td class='right'>").append(money(t.getSanPham().getGia())).append("</td>");
            b.append("<td class='center'>").append(form(s, "quantity", hidden("id", t.getSanPham().getMaSanPham()) +
                    "<div class='qty-form-inline'><input aria-label='Số lượng' class='qty' type='number' name='quantity' min='1' max='" + t.getSanPham().getSoLuongTonKho() + "' value='" + t.getSoLuong() + "' required><button class='button small secondary'>Lưu</button></div>")).append("</td>");
            b.append("<td class='right font-bold'>").append(money(t.tinhThanhTien())).append("</td>");
            b.append("<td class='center'>").append(form(s, "remove", hidden("id", t.getSanPham().getMaSanPham()) + "<button class='button small danger'>Xóa</button>")).append("</td>");
            b.append("</tr>");
        }
        b.append("</tbody></table></div>");
        b.append("<div class='cart-foot-link'><a class='button secondary' href='/'>← Tiếp tục chọn sản phẩm khác</a></div>");
        b.append("</section>");

        b.append("<aside class='panel cart-summary-panel'>");
        b.append("<div class='summary-header'><h2>Tóm tắt đơn hàng</h2></div>");
        b.append("<div class='summary-lines'>");
        b.append("<div class='summary-row'><span>Tạm tính hàng:</span><strong>").append(money(g.tinhTongTien())).append("</strong></div>");
        b.append("<div class='summary-row'><span>Phí vận chuyển:</span><span class='badge green'>Miễn phí (COD)</span></div>");
        b.append("<div class='summary-row total-row'><span>Tổng thanh toán:</span><strong class='total-val'>").append(money(g.tinhTongTien())).append("</strong></div>");
        b.append("</div>");
        b.append(form(s, "checkout",
                input("address", "Địa chỉ giao hàng (COD)", c.getDiaChi(), "text", "placeholder='Số nhà, tên đường, khu vực…'") +
                "<div class='note checkout-note'><p><strong>Hình thức:</strong> Thanh toán tiền mặt khi nhận hàng (COD). Quý khách được quyền đồng kiểm tra ngoại quan thiết bị trước khi thanh toán.</p></div>" +
                "<button class='button primary wide btn-checkout'>Xác nhận đặt hàng →</button>"));
        b.append("</aside></div>");
        return b.toString();
    }

    private static String orderList(Session s, Map<String, String> q) {
        auth(s);
        boolean staff = staff(s);
        var list = staff ? new ArrayList<>(db.bangDonHang().values()) : orders.xemLichSuDonHang(customer(s));
        Collections.reverse(list);
        String status = q.getOrDefault("status", "").trim();
        long pending = list.stream().filter(d -> d.getTenTrangThai().equals("Chờ xác nhận")).count();
        long completed = list.stream().filter(d -> d.getTenTrangThai().equals("Hoàn tất")).count();

        StringBuilder b = new StringBuilder(heading(staff ? "VẬN HÀNH CỬA HÀNG" : "TÀI KHOẢN CỦA BẠN", staff ? "Quản lý đơn hàng" : "Đơn hàng của tôi", "Theo dõi trạng thái, chi tiết các đơn hàng đã đặt và in phiếu đơn hàng."));
        b.append("<div class='stats'>");
        b.append("<div class='panel stat-card'><span>Tổng số đơn hàng</span><strong>").append(list.size()).append("</strong></div>");
        b.append("<div class='panel stat-card pending'><span>Đơn chờ xác nhận</span><strong>").append(pending).append("</strong></div>");
        b.append("<div class='panel stat-card completed'><span>Đơn đã hoàn tất</span><strong>").append(completed).append("</strong></div>");
        b.append("</div>");

        b.append("<div class='panel filter-bar-panel'>");
        b.append("<form class='filters order-filters' method='get'>");
        b.append("<label>Lọc theo trạng thái đơn<select name='status'><option value=''>Tất cả trạng thái</option>");
        for (String t : List.of("Chờ xác nhận", "Đã xác nhận", "Đang giao", "Hoàn tất", "Đã hủy")) {
            b.append("<option ").append(t.equals(status) ? "selected" : "").append(">").append(t).append("</option>");
        }
        b.append("</select></label>");
        b.append("<div class='filter-buttons'><button class='button primary'>Lọc đơn hàng</button>");
        if (!status.isEmpty()) b.append("<a class='button secondary' href='/orders'>Xem tất cả</a>");
        b.append("</div></form></div>");

        int count = 0;
        for (DonHang d : list) {
            if (!status.isEmpty() && !status.equals(d.getTenTrangThai())) continue;
            count++;
            String st = d.getTenTrangThai();
            b.append("<article class='panel order'>");
            b.append("<div class='order-header'>");
            b.append("<div>");
            b.append("<div class='order-id-meta'><span class='order-id-tag'>Mã đơn #").append(d.getMaDonHang()).append("</span>");
            b.append("<span class='order-date'>Ngày đặt: ").append(d.getNgayDatHang().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</span></div>");
            b.append("<h3 class='order-customer-title'>").append(staff ? "Khách hàng: " + h(users.timTheoMa(d.getMaKhachHang()).getHoTen()) : "Đơn hàng của bạn").append("</h3>");
            b.append("</div>");
            b.append("<div>").append(statusBadge(st)).append("</div>");
            b.append("</div>");

            b.append("<div class='order-address-box'><span class='muted'>Địa chỉ nhận hàng:</span> <strong>").append(h(d.getDiaChiGiaoHang())).append("</strong></div>");
            b.append("<div class='order-lines'>");
            for (var t : d.getDanhSachChiTiet()) {
                b.append("<div class='order-line-item'>");
                b.append("<div class='order-line-name'><strong>").append(h(t.getSanPham().getTenSanPham())).append("</strong> × ").append(t.getSoLuong()).append("</div>");
                b.append("<div class='order-line-price'>").append(money(t.tinhThanhTien())).append("</div>");
                b.append("</div>");
            }
            b.append("</div>");

            b.append("<div class='order-footer'>");
            b.append("<div class='order-total'><span>Tổng tiền đơn hàng:</span> <strong>").append(money(d.tinhTongTien())).append("</strong></div>");
            b.append("<div class='order-actions'>");
            b.append("<a class='button secondary small' href='/invoice?id=").append(d.getMaDonHang()).append("'>Xem phiếu đơn hàng</a>");
            if (staff && st.equals("Chờ xác nhận")) b.append(orderButton(s, d, "confirm", "Xác nhận đơn", "btn-confirm"));
            if (staff && st.equals("Đã xác nhận")) b.append(orderButton(s, d, "ship", "Giao hàng", "btn-ship"));
            if (staff && st.equals("Đang giao")) b.append(orderButton(s, d, "complete", "Hoàn tất đơn", "btn-complete"));
            if (st.equals("Chờ xác nhận") || (staff && st.equals("Đã xác nhận"))) b.append(orderButton(s, d, "cancel", "Hủy đơn", "danger"));
            b.append("</div>");
            b.append("</div>");
            b.append("</article>");
        }
        if (count == 0) {
            b.append("<section class='panel empty'><div class='empty-icon-wrap'>📋</div><h2>Chưa có đơn hàng nào</h2><p class='muted'>Các đơn hàng phù hợp với điều kiện lọc sẽ xuất hiện tại đây.</p></section>");
        }
        return b.toString();
    }

    private static String invoice(Session s, int id) {
        DonHang d = ownedOrder(s, id);
        NguoiDung kh = users.timTheoMa(d.getMaKhachHang());
        StringBuilder b = new StringBuilder();
        b.append("<div class='invoice-wrapper'>");
        b.append("<div class='invoice-nav-bar'><a class='button secondary small' href='/orders'>← Quay lại danh sách đơn hàng</a><button class='button primary small' onclick='window.print()'>In phiếu / Lưu PDF</button></div>");
        b.append("<section class='panel invoice'>");
        b.append("<div class='invoice-header'>");
        b.append("<div><div class='brand'><b>t<span>n</span></b> TechNest<span class='brand-dot'>.</span></div><span class='invoice-sub'>HỆ THỐNG THIẾT BỊ ĐIỆN TỬ CHÍNH HÃNG</span></div>");
        b.append("<div class='invoice-title-box'><h2>PHIẾU ĐƠN HÀNG</h2><span class='invoice-id'>Mã đơn #").append(id).append("</span></div>");
        b.append("</div>");

        b.append("<div class='invoice-meta-grid'>");
        b.append("<div><strong>Thông tin khách hàng:</strong><br>Họ tên: ").append(h(kh.getHoTen())).append("<br>Điện thoại: ").append(h(kh.getSoDienThoai())).append("<br>Địa chỉ: ").append(h(d.getDiaChiGiaoHang())).append("</div>");
        b.append("<div><strong>Thông tin đơn hàng:</strong><br>Ngày đặt: ").append(d.getNgayDatHang().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("<br>Hình thức: Thanh toán COD khi nhận<br>Trạng thái: ").append(statusBadge(d.getTenTrangThai())).append("</div>");
        b.append("</div>");

        b.append("<div class='table-wrap'><table class='data-table invoice-table'>");
        b.append("<thead><tr><th>STT</th><th>Thiết bị điện tử</th><th class='center'>SL</th><th class='right'>Đơn giá</th><th class='right'>Thành tiền</th></tr></thead><tbody>");
        int stt = 1;
        for (var t : d.getDanhSachChiTiet()) {
            b.append("<tr>");
            b.append("<td>").append(stt++).append("</td>");
            b.append("<td><strong>").append(h(t.getSanPham().getTenSanPham())).append("</strong><br><span class='muted'>").append(h(t.getSanPham().getDanhMuc().getTenDanhMuc())).append("</span></td>");
            b.append("<td class='center'>").append(t.getSoLuong()).append("</td>");
            b.append("<td class='right'>").append(money(t.getDonGia())).append("</td>");
            b.append("<td class='right font-bold'>").append(money(t.tinhThanhTien())).append("</td>");
            b.append("</tr>");
        }
        b.append("</tbody></table></div>");

        b.append("<div class='invoice-total-section'>");
        b.append("<div class='invoice-notes'><p><strong>Ghi chú:</strong> Phiếu phục vụ đồ án học phần Phân tích & Thiết kế Phần mềm. Khách hàng được quyền kiểm tra hàng trước khi nhận.</p></div>");
        b.append("<div class='invoice-total-box'><span class='muted'>Tổng cộng thanh toán:</span><strong class='invoice-total-price'>").append(money(d.tinhTongTien())).append("</strong></div>");
        b.append("</div>");
        b.append("</section></div>");
        return b.toString();
    }

    private static String manage(Session s, Map<String, String> q) {
        staffOnly(s);
        SanPham p = q.containsKey("edit") ? product(Integer.parseInt(q.get("edit"))) : null;
        StringBuilder b = new StringBuilder(heading("KHO HÀNG & SẢN PHẨM", "Quản lý sản phẩm", "Cập nhật giá bán, số lượng tồn kho và thông tin chi tiết thiết bị điện tử."));
        b.append("<div class='checkout'>");
        b.append("<section class='panel manage-table-panel'>");
        b.append("<div class='table-header'><h3>Danh sách sản phẩm trong kho</h3><span class='muted'>").append(products.layTatCa().size()).append(" sản phẩm</span></div>");
        b.append("<div class='table-wrap'><table class='data-table manage-table'>");
        b.append("<thead><tr><th>Mã SP</th><th>Tên sản phẩm & Danh mục</th><th class='right'>Giá bán</th><th class='center'>Tồn kho</th><th class='center'>Thao tác</th></tr></thead><tbody>");
        for (SanPham item : products.layTatCa()) {
            b.append("<tr>");
            b.append("<td><code>#").append(item.getMaSanPham()).append("</code></td>");
            b.append("<td><strong>").append(h(item.getTenSanPham())).append("</strong><br><span class='product-cat-sm'>").append(h(item.getDanhMuc().getTenDanhMuc())).append("</span></td>");
            b.append("<td class='right font-bold'>").append(money(item.getGia())).append("</td>");
            b.append("<td class='center'><span class='stock-tag ").append(item.getSoLuongTonKho() > 0 ? "in-stock" : "out-stock").append("'>").append(item.getSoLuongTonKho()).append(" sp</span></td>");
            b.append("<td class='center actions-cell'>");
            b.append("<a class='button small secondary' href='/manage?edit=").append(item.getMaSanPham()).append("'>Sửa</a> ");
            b.append(form(s, "product-delete", hidden("id", item.getMaSanPham()) + "<button class='button small danger'>Xóa</button>"));
            b.append("</td></tr>");
        }
        b.append("</tbody></table></div></section>");

        b.append("<aside class='panel manage-form-panel'>");
        b.append("<div class='form-header'><h2>").append(p == null ? "Thêm sản phẩm mới" : "Sửa sản phẩm #" + p.getMaSanPham()).append("</h2>");
        if (p != null) b.append("<a class='button secondary small' href='/manage'>Hủy sửa / Thêm mới</a>");
        b.append("</div>");
        b.append(form(s, "product-save",
                hidden("id", p == null ? 0 : p.getMaSanPham()) +
                input("name", "Tên thiết bị sản phẩm", p == null ? "" : p.getTenSanPham(), "text", "placeholder='VD: iPhone 15, iPad Pro…'") +
                input("price", "Giá bán (₫)", p == null ? "" : p.getGia() + "", "number", "min='1' max='1000000000' placeholder='VD: 22000000'") +
                input("stock", "Số lượng tồn kho", p == null ? "0" : p.getSoLuongTonKho() + "", "number", "min='0' max='1000000'") +
                "<label>Danh mục sản phẩm<select name='category'>" + categoryOptions(p == null ? 0 : p.getDanhMuc().getMaDanhMuc()) + "</select></label>" +
                input("description", "Mô tả cấu hình và tính năng", p == null ? "" : p.getMoTa(), "text", "placeholder='Mô tả thông số, màn hình, cấu hình chip…'") +
                "<div class='form-actions'><button class='button primary wide'>" + (p == null ? "Thêm sản phẩm vào kho" : "Lưu thay đổi sản phẩm") + "</button></div>"));
        b.append("</aside></div>");
        return b.toString();
    }

    private static String categories(Session s) {
        adminOnly(s);
        StringBuilder b = new StringBuilder(heading("QUẢN TRỊ HỆ THỐNG", "Danh mục sản phẩm", "Quản lý danh mục thiết bị điện tử. Danh mục đang có sản phẩm sẽ được bảo vệ khỏi thao tác xóa."));
        b.append("<div class='checkout'>");
        b.append("<section class='panel'><div class='table-header'><h3>Danh sách danh mục hiện có</h3></div>");
        b.append("<div class='category-list'>");
        for (DanhMuc c : db.bangDanhMuc().values()) {
            b.append("<div class='category-row'>");
            b.append(form(s, "category-save", hidden("id", c.getMaDanhMuc()) +
                    "<div class='cat-edit-inline'><label class='cat-lbl'>Mã #" + c.getMaDanhMuc() + "<input name='name' type='text' value='" + h(c.getTenDanhMuc()) + "' required></label><button class='button small primary'>Đổi tên</button></div>"));
            b.append(form(s, "category-delete", hidden("id", c.getMaDanhMuc()) + "<button class='button small danger'>Xóa</button>"));
            b.append("</div>");
        }
        b.append("</div></section>");

        b.append("<aside class='panel'><h2>Thêm danh mục mới</h2>");
        b.append(form(s, "category-save", hidden("id", 0) +
                input("name", "Tên danh mục", "", "text", "placeholder='VD: Máy tính bảng, Đồng hồ thông minh…'") +
                "<button class='button primary wide'>Thêm danh mục</button>"));
        b.append("</aside></div>");
        return b.toString();
    }

    private static String accounts(Session s) {
        adminOnly(s);
        StringBuilder b = new StringBuilder(heading("QUẢN TRỊ HỆ THỐNG", "Tài khoản người dùng", "Danh sách người dùng hệ thống và tạo tài khoản theo vai trò (Khách hàng, Nhân viên, Quản trị viên)."));
        b.append("<div class='checkout'>");
        b.append("<section class='panel'><div class='table-header'><h3>Danh sách người dùng</h3><span class='muted'>").append(users.layTatCa().size()).append(" tài khoản</span></div>");
        b.append("<div class='table-wrap'><table class='data-table'>");
        b.append("<thead><tr><th>Họ và tên</th><th>Email</th><th>Số điện thoại</th><th>Vai trò</th></tr></thead><tbody>");
        for (NguoiDung u : users.layTatCa()) {
            b.append("<tr>");
            b.append("<td><strong>").append(h(u.getHoTen())).append("</strong></td>");
            b.append("<td>").append(h(u.getEmail())).append("</td>");
            b.append("<td>").append(h(u.getSoDienThoai())).append("</td>");
            b.append("<td>").append(roleBadge(u)).append("</td>");
            b.append("</tr>");
        }
        b.append("</tbody></table></div></section>");

        b.append("<aside class='panel'><h2>Tạo tài khoản mới</h2>");
        b.append(form(s, "account-add",
                input("name", "Họ và tên", "", "text", "placeholder='Nguyễn Văn X'") +
                input("email", "Địa chỉ Email", "", "email", "placeholder='x@shop.vn'") +
                input("password", "Mật khẩu", "", "password", "minlength='6' placeholder='Tối thiểu 6 ký tự'") +
                input("phone", "Số điện thoại", "", "tel", "placeholder='0900000000'") +
                input("address", "Địa chỉ", "", "text", "placeholder='Đà Lạt'") +
                "<label>Vai trò phân quyền<select name='role'><option value='KHACH_HANG'>Khách hàng</option><option value='NHAN_VIEN'>Nhân viên</option><option value='QUAN_TRI_VIEN'>Quản trị viên</option></select></label>" +
                "<button class='button primary wide'>Tạo tài khoản</button>"));
        b.append("</aside></div>");
        return b.toString();
    }

    private static String layout(Session s, String path, String body) {
        String nav = nav(path, "/", "Cửa hàng");
        int cartCount = 0;
        if (s.user instanceof KhachHang) {
            cartCount = carts.layGioHang(s.user.getMaNguoiDung()).getDanhSachChiTiet().stream().mapToInt(ChiTietGioHang::getSoLuong).sum();
            nav += nav(path, "/cart", "Giỏ hàng (" + cartCount + ")");
        }
        if (s.user != null) {
            nav += nav(path, "/orders", staff(s) ? "Quản lý đơn hàng" : "Đơn hàng của tôi");
        }
        if (staff(s)) {
            nav += nav(path, "/manage", "Quản lý sản phẩm");
        }
        if (s.user instanceof QuanTriVien) {
            nav += nav(path, "/categories", "Danh mục");
            nav += nav(path, "/accounts", "Tài khoản");
        }
        if (s.user != null) {
            nav += nav(path, "/profile", "Thông tin cá nhân");
        }

        String cartHeaderBtn = (s.user instanceof KhachHang) ?
                "<a class='header-cart-btn' href='/cart'>" +
                "<svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><circle cx='9' cy='21' r='1'/><circle cx='20' cy='21' r='1'/><path d='M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6'/></svg>" +
                "<span>Giỏ hàng</span><span class='cart-count-badge'>" + cartCount + "</span></a>" : "";

        String account;
        if (s.user == null) {
            account = "<div class='auth-links'><a class='button small primary' href='/login'>Đăng nhập</a><a class='button small secondary' href='/register'>Đăng ký</a></div>";
        } else {
            account = "<div class='user-box'><a class='user-profile-link' href='/profile'><strong>" + h(s.user.getHoTen()) + "</strong>" + roleBadge(s.user) + "</a>" +
                    form(s, "logout", "<button class='button small secondary btn-logout'>Đăng xuất</button>") + "</div>";
        }

        String flash = s.flash.isEmpty() ? "" : "<div class='flash' role='status'>✓ " + h(s.flash) + "</div>";
        s.flash = "";

        return "<!doctype html><html lang='vi'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>TechNest · Hệ thống thiết bị điện tử</title><link rel='stylesheet' href='/style.css'></head><body>" +
                "<div class='topline'><div class='topline-inner'><span>TECHNEST — THIẾT BỊ ĐIỆN TỬ CHÍNH HÃNG</span><span class='topline-right'>Hotline: 1800 6868 · Giao hàng COD toàn quốc · Dữ liệu mẫu đồ án môn học</span></div></div>" +
                "<header><div class='header-inner'>" +
                "<a class='brand' href='/'><b>t<span>n</span></b> TechNest<span class='brand-dot'>.</span></a>" +
                "<form class='header-search' method='get' action='/'>" +
                "<input name='q' placeholder='Tìm kiếm điện thoại, laptop, tai nghe…' aria-label='Tìm kiếm sản phẩm'>" +
                "<button type='submit' aria-label='Tìm'><svg width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'><circle cx='11' cy='11' r='8'/><line x1='21' y1='21' x2='16.65' y2='16.65'/></svg><span>Tìm kiếm</span></button>" +
                "</form>" +
                "<div class='header-actions'>" + cartHeaderBtn + account + "</div>" +
                "</div></header>" +
                "<nav class='subnav-bar'><div class='subnav-inner'>" + nav + "</div></nav>" +
                "<main>" + flash + body + "</main>" +
                "<footer><div class='footer-inner'>" +
                "<div class='footer-brand'><a class='brand' href='/'>TechNest<span class='brand-dot'>.</span></a><p>Website thương mại điện tử quản lý và bán thiết bị điện tử phục vụ học phần Phân tích và thiết kế phần mềm.</p></div>" +
                "<div class='footer-col'><strong>Hỗ trợ khách hàng</strong><span>Chính hãng 100% · Đổi trả 7 ngày</span><span>Thanh toán khi nhận hàng (COD)</span></div>" +
                "<div class='footer-col'><strong>Môi trường thử nghiệm</strong><span>Kiến trúc mẫu thiết kế GoF & OOP</span><span>Dữ liệu In-Memory tự đặt lại khi tắt máy chủ</span></div>" +
                "</div></footer></body></html>";
    }

    private static String nav(String p, String href, String title) {
        return "<a class='nav-link " + (p.equals(href) ? "active" : "") + "' href='" + href + "'>" + title + "</a>";
    }

    private static String heading(String eyebrow, String title, String description) {
        return "<div class='page-title'><span class='eyebrow'>" + eyebrow + "</span><h1>" + title + "</h1><p class='muted'>" + description + "</p></div>";
    }

    private static String productArt(int id, String categoryName, boolean isLarge) {
        String cat = categoryName == null ? "" : categoryName.toLowerCase();
        int size = isLarge ? 56 : 38;
        String svg;
        if (cat.contains("thoại") || cat.contains("phone") || id <= 2) {
            svg = "<svg width='" + size + "' height='" + size + "' viewBox='0 0 24 24' fill='none' stroke='#1e3a8a' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><rect x='5' y='2' width='14' height='20' rx='3' ry='3'/><line x1='12' y1='18' x2='12.01' y2='18'/></svg>";
        } else if (cat.contains("laptop") || cat.contains("máy tính") || id <= 4) {
            svg = "<svg width='" + size + "' height='" + size + "' viewBox='0 0 24 24' fill='none' stroke='#1e3a8a' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><rect x='3' y='4' width='18' height='12' rx='2'/><line x1='2' y1='20' x2='22' y2='20'/></svg>";
        } else if (cat.contains("tai nghe") || id == 5) {
            svg = "<svg width='" + size + "' height='" + size + "' viewBox='0 0 24 24' fill='none' stroke='#1e3a8a' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><path d='M3 18v-6a9 9 0 0 1 18 0v6'/><path d='M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z'/></svg>";
        } else if (cat.contains("chuột") || id == 6) {
            svg = "<svg width='" + size + "' height='" + size + "' viewBox='0 0 24 24' fill='none' stroke='#1e3a8a' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><rect x='6' y='3' width='12' height='18' rx='6'/><line x1='12' y1='7' x2='12' y2='11'/></svg>";
        } else {
            svg = "<svg width='" + size + "' height='" + size + "' viewBox='0 0 24 24' fill='none' stroke='#1e3a8a' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><rect x='2' y='3' width='20' height='14' rx='2'/><line x1='8' y1='21' x2='16' y2='21'/><line x1='12' y1='17' x2='12' y2='21'/></svg>";
        }
        if (isLarge) {
            return "<div class='detail-placeholder-box'><div class='placeholder-icon'>" + svg + "</div><div class='placeholder-badge'>ẢNH MINH HỌA THIẾT BỊ</div><div class='placeholder-sub'>Mã sản phẩm #" + id + " · " + h(categoryName) + "</div></div>";
        }
        return "<div class='product-placeholder-box'><div class='placeholder-icon'>" + svg + "</div><span class='placeholder-badge'>Ảnh minh họa</span></div>";
    }

    private static String art(int id) {
        SanPham p = products.timTheoMa(id);
        String cat = (p != null && p.getDanhMuc() != null) ? p.getDanhMuc().getTenDanhMuc() : "";
        return productArt(id, cat, false);
    }

    private static String statusBadge(String st) {
        String cls = switch (st) {
            case "Chờ xác nhận" -> "status-pending";
            case "Đã xác nhận" -> "status-confirmed";
            case "Đang giao" -> "status-shipping";
            case "Hoàn tất" -> "status-completed green";
            case "Đã hủy" -> "status-cancelled red";
            default -> "status-default";
        };
        String symbol = switch (st) {
            case "Hoàn tất" -> "✓ ";
            case "Đã hủy" -> "✕ ";
            default -> "● ";
        };
        return "<span class='badge " + cls + "'><span class='status-dot'></span>" + symbol + h(st) + "</span>";
    }

    private static String roleBadge(NguoiDung u) {
        String roleText = role(u);
        String cls = switch (u.layVaiTro()) {
            case KHACH_HANG -> "role-customer";
            case NHAN_VIEN -> "role-staff";
            case QUAN_TRI_VIEN -> "role-admin";
        };
        return "<span class='role-tag " + cls + "'>" + roleText + "</span>";
    }

    private static String form(Session s, String op, String body) {
        return "<form method='post' action='/action'>" + hidden("csrf", s.csrf) + hidden("op", op) + body + "</form>";
    }

    private static String input(String name, String label, String value, String type, String attrs) {
        return "<label>" + h(label) + "<input name='" + name + "' type='" + type + "' value='" + h(value) + "' required maxlength='500' " + attrs + "></label>";
    }

    private static String hidden(String name, Object value) {
        return "<input type='hidden' name='" + name + "' value='" + h(String.valueOf(value)) + "'>";
    }

    private static String orderButton(Session s, DonHang d, String op, String label, String cls) {
        return form(s, op, hidden("id", d.getMaDonHang()) + "<button class='button small " + cls + "'>" + label + "</button>");
    }

    private static String categoryOptions(int selected) {
        StringBuilder b = new StringBuilder();
        for (DanhMuc c : db.bangDanhMuc().values()) {
            b.append("<option value='").append(c.getMaDanhMuc()).append("' ").append(selected == c.getMaDanhMuc() ? "selected" : "").append(">").append(h(c.getTenDanhMuc())).append("</option>");
        }
        return b.toString();
    }

    private static String h(String v) {
        return v == null ? "" : v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String money(long n) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(n) + " ₫";
    }

    private static String role(NguoiDung u) {
        return switch (u.layVaiTro()) {
            case KHACH_HANG -> "Khách hàng";
            case NHAN_VIEN -> "Nhân viên";
            case QUAN_TRI_VIEN -> "Quản trị viên";
        };
    }

    private static Map<String, String> parse(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw != null && !raw.isEmpty()) {
            for (String pair : raw.split("&")) {
                String[] kv = pair.split("=", 2);
                m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
            }
        }
        return m;
    }

    private static String required(Map<String, String> f, String key) {
        String v = f.getOrDefault(key, "").trim();
        require(!v.isEmpty() && v.length() <= 500, "Vui lòng nhập đầy đủ thông tin hợp lệ (tối đa 500 ký tự).");
        return v;
    }

    private static String email(Map<String, String> f) {
        String v = required(f, "email");
        require(v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Email không hợp lệ.");
        return v;
    }

    private static int number(Map<String, String> f, String key, int min, int max) {
        int n = Integer.parseInt(required(f, key));
        require(n >= min && n <= max, "Giá trị số không hợp lệ.");
        return n;
    }

    private static int id(Map<String, String> f) {
        return number(f, "id", 1, Integer.MAX_VALUE);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException(message);
    }

    private static void auth(Session s) {
        require(s.user != null, "Vui lòng đăng nhập để sử dụng chức năng này.");
    }

    private static boolean staff(Session s) {
        return s.user instanceof NhanVien || s.user instanceof QuanTriVien;
    }

    private static void staffOnly(Session s) {
        auth(s);
        require(staff(s), "Chức năng dành cho nhân viên hoặc quản trị viên.");
    }

    private static void adminOnly(Session s) {
        auth(s);
        require(s.user instanceof QuanTriVien, "Chức năng dành cho quản trị viên.");
    }

    private static KhachHang customer(Session s) {
        auth(s);
        require(s.user instanceof KhachHang, "Vui lòng dùng tài khoản khách hàng để mua sắm.");
        return (KhachHang) s.user;
    }

    private static NhanVien employee(Session s) {
        staffOnly(s);
        return s.user instanceof NhanVien ? (NhanVien) s.user : new NhanVien(s.user.getMaNguoiDung(), s.user.getHoTen(), s.user.getEmail(), "", s.user.getSoDienThoai(), s.user.getDiaChi());
    }

    private static SanPham product(int id) {
        SanPham p = products.timTheoMa(id);
        require(p != null, "Sản phẩm không tồn tại.");
        return p;
    }

    private static DonHang ownedOrder(Session s, int id) {
        auth(s);
        DonHang d = db.bangDonHang().get(id);
        require(d != null, "Đơn hàng không tồn tại.");
        require(staff(s) || d.getMaKhachHang() == s.user.getMaNguoiDung(), "Bạn không có quyền xem hoặc sửa đơn hàng này.");
        return d;
    }

    private static void redirect(HttpExchange x, String location) throws IOException {
        x.getResponseHeaders().set("Location", location);
        x.sendResponseHeaders(303, -1);
    }

    private static void send(HttpExchange x, int status, String type, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        x.getResponseHeaders().set("X-Frame-Options", "DENY");
        x.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = x.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static final String CSS = """
:root{--ink:#0f172a;--body:#1e293b;--muted:#64748b;--primary:#1e3a8a;--primary-hover:#1d4ed8;--navy:#0f2347;--line:#e2e8f0;--line-dark:#cbd5e1;--bg:#f1f5f9;--surface:#ffffff}
*{box-sizing:border-box}
html{scroll-behavior:smooth}
body{margin:0;color:var(--body);background:var(--bg);font:15px/1.55 system-ui,-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif}
a{color:var(--primary);text-decoration:none}
a:hover{text-decoration:underline}
h1,h2,h3,p{margin:0 0 14px}
h1{font-size:26px;color:var(--ink);letter-spacing:-.5px;font-weight:700}
h2{font-size:20px;color:var(--ink);font-weight:700}
h3{font-size:16px;color:var(--ink);font-weight:600}
code{background:#f1f5f9;padding:2px 6px;border-radius:4px;font-size:13px;border:1px solid #e2e8f0}
button,.button{border:1px solid transparent;border-radius:6px;background:var(--primary);color:#fff;padding:9px 16px;font:600 14px system-ui,-apple-system,sans-serif;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;gap:6px;text-align:center;line-height:1.4;transition:background-color .15s,border-color .15s}
button:hover,.button:hover{background:var(--primary-hover);text-decoration:none}
button:disabled{opacity:.5;cursor:not-allowed}
.button.secondary,button.secondary{background:#fff;border-color:var(--line-dark);color:#334155}
.button.secondary:hover,button.secondary:hover{background:#f8fafc;border-color:#94a3b8}
.button.danger,button.danger{background:#fef2f2;border-color:#fecaca;color:#dc2626}
.button.danger:hover,button.danger:hover{background:#fee2e2;border-color:#fca5a5}
.button.btn-confirm{background:#1d4ed8;color:#fff}
.button.btn-ship{background:#4338ca;color:#fff}
.button.btn-complete{background:#15803d;color:#fff}
.small{padding:6px 12px;font-size:13px}
.wide{width:100%}
.topline{background:#0a1931;color:#94a3b8;padding:6px 20px;font-size:12px;border-bottom:1px solid #132746}
.topline-inner{max-width:1240px;margin:0 auto;display:flex;justify-content:space-between;align-items:center}
header{background:var(--navy);border-bottom:1px solid #1e3a8a;padding:12px 20px}
.header-inner{max-width:1240px;margin:0 auto;display:flex;align-items:center;gap:24px}
.brand{font-size:22px;font-weight:800;letter-spacing:-.8px;color:#fff;display:inline-flex;align-items:center;gap:6px;text-decoration:none}
.brand b{display:inline-flex;align-items:center;justify-content:center;background:#1e3a8a;color:#fff;border-radius:6px;width:34px;height:34px;font-size:15px}
.brand b span{color:#60a5fa}
.brand-dot{color:#60a5fa}
.header-search{flex:1;max-width:480px;display:flex;position:relative}
.header-search input{width:100%;background:#fff;border:1px solid #cbd5e1;border-radius:6px 0 0 6px;padding:8px 12px;font-size:14px;color:var(--ink);margin:0}
.header-search button{background:#1e3a8a;border:1px solid #1e3a8a;border-radius:0 6px 6px 0;padding:8px 14px;font-size:13px;white-space:nowrap}
.header-actions{display:flex;align-items:center;gap:16px;margin-left:auto}
.header-cart-btn{display:inline-flex;align-items:center;gap:8px;background:rgba(255,255,255,.1);border:1px solid rgba(255,255,255,.2);color:#fff;padding:8px 14px;border-radius:6px;font-size:14px;font-weight:600;text-decoration:none}
.header-cart-btn:hover{background:rgba(255,255,255,.2);text-decoration:none}
.cart-count-badge{background:#ef4444;color:#fff;border-radius:999px;padding:1px 7px;font-size:12px;font-weight:700}
.auth-links{display:flex;gap:8px}
.user-box{display:flex;align-items:center;gap:12px}
.user-profile-link{color:#fff;text-align:right;text-decoration:none}
.user-profile-link strong{display:block;font-size:13px}
.subnav-bar{background:#fff;border-bottom:1px solid var(--line);box-shadow:0 1px 2px rgba(0,0,0,.03)}
.subnav-inner{max-width:1240px;margin:0 auto;padding:0 20px;display:flex;gap:4px;overflow-x:auto}
.nav-link{padding:11px 16px;font-size:14px;font-weight:500;color:#475569;white-space:nowrap;border-bottom:2px solid transparent;text-decoration:none}
.nav-link:hover{color:var(--primary);text-decoration:none}
.nav-link.active{color:var(--primary);font-weight:700;border-bottom-color:var(--primary)}
main{max-width:1240px;margin:0 auto;padding:22px 20px 48px;min-height:calc(100vh - 240px)}
.store-banner{background:#1e3a8a;color:#fff;border-radius:10px;padding:18px 24px;margin-bottom:22px;display:flex;justify-content:space-between;align-items:center;gap:20px}
.banner-badge{display:inline-block;font-size:11px;font-weight:700;letter-spacing:1px;background:rgba(255,255,255,.15);padding:3px 8px;border-radius:4px;margin-bottom:6px}
.banner-content h1{font-size:20px;color:#fff;margin:0 0 4px}
.banner-content p{margin:0;font-size:13px;color:#cbd5e1}
.banner-perks{display:flex;gap:16px;flex-shrink:0}
.perk-item{display:flex;align-items:center;gap:8px;background:rgba(255,255,255,.08);padding:8px 12px;border-radius:6px;border:1px solid rgba(255,255,255,.15)}
.perk-icon{width:20px;height:20px;border-radius:50%;background:#3b82f6;display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700}
.perk-item strong{display:block;font-size:12px;color:#fff}
.perk-item small{display:block;font-size:11px;color:#cbd5e1}
.panel{background:#fff;border:1px solid var(--line);border-radius:8px;padding:20px;box-shadow:0 1px 3px rgba(15,23,42,.04)}
.filter-panel{margin-bottom:22px}
.filter-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;padding-bottom:10px;border-bottom:1px solid #f1f5f9}
.filter-title{margin:0;font-size:18px}
.product-count{font-size:13px;color:var(--muted)}
.eyebrow{display:block;color:var(--primary);font-size:11px;letter-spacing:1.2px;font-weight:700;margin-bottom:4px}
.filters{display:flex;gap:14px;align-items:flex-end;flex-wrap:wrap}
.filters label{flex:1;min-width:180px;margin:0;font-size:13px;font-weight:600;color:#334155}
.filter-buttons{display:flex;gap:8px;align-items:center}
.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:20px}
.product{background:#fff;border:1px solid var(--line);border-radius:8px;overflow:hidden;display:flex;flex-direction:column;transition:border-color .15s,box-shadow .15s}
.product:hover{border-color:var(--line-dark);box-shadow:0 4px 12px rgba(15,23,42,.06);transform:translateY(-2px)}
.product-art{height:165px;background:#f8fafc;display:flex;align-items:center;justify-content:center;border-bottom:1px solid #f1f5f9;text-decoration:none}
.product-placeholder-box{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;width:100%;height:100%}
.placeholder-icon{color:var(--primary)}
.placeholder-badge{font-size:11px;color:var(--muted);background:#edf2f7;padding:2px 8px;border-radius:4px;font-weight:500}
.product-info{padding:16px;display:flex;flex-direction:column;flex:1}
.product-meta{display:flex;justify-content:space-between;align-items:center;margin-bottom:8px}
.product-cat{font-size:12px;font-weight:600;color:var(--primary);background:#eff6ff;padding:2px 8px;border-radius:4px}
.stock-tag{font-size:12px;font-weight:600;padding:2px 8px;border-radius:4px}
.stock-tag.in-stock{color:#15803d;background:#dcfce7}
.stock-tag.out-stock{color:#b91c1c;background:#fee2e2}
.product h3{font-size:16px;margin:4px 0 10px;line-height:1.4}
.product h3 a{color:var(--ink);text-decoration:none}
.product h3 a:hover{color:var(--primary);text-decoration:underline}
.price-row{display:flex;justify-content:space-between;align-items:baseline;margin:8px 0 14px;margin-top:auto}
.price-row strong{font-size:18px;color:var(--primary);font-weight:700}
.breadcrumb-bar{display:flex;align-items:center;gap:8px;font-size:13px;color:var(--muted);margin-bottom:18px}
.breadcrumb-bar .sep{color:#cbd5e1}
.breadcrumb-bar .crumb-cur{color:var(--ink);font-weight:600}
.detail{display:grid;grid-template-columns:1fr 1.25fr;gap:28px;align-items:start}
.detail-art{border-radius:8px;overflow:hidden}
.detail-placeholder-box{background:#f8fafc;border:1px solid var(--line);border-radius:8px;min-height:360px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px}
.detail-placeholder-box .placeholder-badge{font-size:12px;font-weight:600;letter-spacing:1px;color:#475569;background:#e2e8f0;padding:4px 10px;border-radius:4px}
.detail-placeholder-box .placeholder-sub{font-size:13px;color:var(--muted)}
.detail-content{padding:24px}
.detail-header{margin-bottom:16px}
.detail-header h1{font-size:24px;margin:8px 0 10px}
.big-price{font-size:26px;color:var(--primary);font-weight:700;margin:6px 0}
.vat-note{font-size:13px;font-weight:400;color:var(--muted)}
.detail-desc{margin:16px 0;padding:14px;background:#f8fafc;border-radius:6px;border:1px solid var(--line)}
.desc-title{display:block;font-size:12px;font-weight:700;color:#475569;margin-bottom:6px;text-transform:uppercase;letter-spacing:.5px}
.detail-desc p{margin:0;font-size:14px;line-height:1.6}
.stock-box{display:flex;align-items:center;gap:8px;padding:8px 12px;background:#fff;border-radius:6px;border:1px solid var(--line);margin-bottom:16px;font-size:14px}
.stock-dot{width:8px;height:8px;border-radius:50%}
.stock-dot.available{background:#16a34a}
.stock-dot.unavailable{background:#dc2626}
.detail-buy-row{display:flex;gap:12px;align-items:flex-end}
.qty-label{font-size:13px;font-weight:600;margin:0;flex-shrink:0}
.qty-label input{width:85px;margin-top:4px}
.btn-add-cart{flex:1;height:42px}
.detail-action-prompt{padding:14px;background:#f8fafc;border-radius:6px;border:1px solid var(--line)}
.detail-action-prompt p{margin-bottom:10px}
.trust-box{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:20px;padding-top:16px;border-top:1px solid var(--line);font-size:12px;color:#475569}
.trust-item{display:flex;align-items:center;gap:6px}
.trust-icon{color:#16a34a;font-weight:700}
.auth{max-width:460px;margin:28px auto}
.auth-header{margin-bottom:18px}
.auth-footer{margin-top:16px;padding-top:14px;border-top:1px solid var(--line);font-size:13px;text-align:center}
.btn-auth-submit{margin-top:8px}
label{display:block;font-size:13px;font-weight:600;color:#334155;margin-bottom:14px}
input,select,textarea{display:block;width:100%;padding:9px 12px;border:1px solid var(--line-dark);border-radius:6px;background:#fff;color:var(--ink);font:14px system-ui,-apple-system,sans-serif;margin-top:5px}
input:focus,select:focus,textarea:focus{border-color:var(--primary);outline:2px solid rgba(30,58,138,.15)}
input[type=hidden]{display:none}
.note{padding:14px;background:#f8fafc;border:1px solid var(--line);border-radius:6px;font-size:13px;color:#475569}
.demo-box{margin-top:20px}
.demo-title{font-size:11px;font-weight:700;color:var(--primary);letter-spacing:1px;margin-bottom:8px}
.demo-table{width:100%;border-collapse:collapse;font-size:12px}
.demo-table th{background:#eef2f6;padding:6px 8px;color:#334155}
.demo-table td{padding:6px 8px;border-bottom:1px solid #e2e8f0}
.flash{padding:12px 18px;border:1px solid #bbf7d0;background:#f0fdf4;border-radius:6px;margin-bottom:20px;color:#166534;font-size:14px;font-weight:500}
.error-panel{max-width:540px;margin:40px auto;text-align:center}
.error-panel h1{color:#b91c1c;margin-bottom:10px}
.alert-msg{font-size:15px;color:#475569;margin-bottom:20px}
.panel-actions{display:flex;gap:10px;justify-content:center}
.checkout{display:grid;grid-template-columns:1.8fr 1fr;gap:24px;align-items:start}
.data-table{border-collapse:collapse;width:100%;font-size:13px;text-align:left}
.data-table th{color:#475569;font-weight:600;padding:11px 12px;background:#f8fafc;border-bottom:1px solid var(--line);white-space:nowrap}
.data-table td{padding:12px;border-bottom:1px solid var(--line);vertical-align:middle}
.data-table tr:hover td{background:#fafafa}
.data-table td.right,.data-table th.right{text-align:right}
.data-table td.center,.data-table th.center{text-align:center}
.font-bold{font-weight:700;color:var(--ink)}
.qty-form-inline{display:inline-flex;gap:6px;align-items:center}
input.qty{width:64px;margin:0;padding:6px 8px}
.cart-foot-link{margin-top:16px;padding-top:14px;border-top:1px solid var(--line)}
.cart-summary-panel h2{font-size:18px;margin-bottom:14px}
.summary-lines{display:flex;flex-direction:column;gap:10px;margin-bottom:16px;padding-bottom:14px;border-bottom:1px solid var(--line)}
.summary-row{display:flex;justify-content:space-between;align-items:center;font-size:14px}
.total-row{padding-top:8px;border-top:1px solid #f1f5f9}
.total-val{font-size:20px;color:var(--primary);font-weight:700}
.checkout-note{margin:14px 0}
.stats{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:20px}
.stat-card{padding:16px 20px}
.stat-card span{color:var(--muted);font-size:13px;font-weight:500}
.stat-card strong{display:block;font-size:26px;color:var(--ink);margin-top:4px}
.stat-card.pending strong{color:#d97706}
.stat-card.completed strong{color:#16a34a}
.filter-bar-panel{margin-bottom:20px;padding:14px 20px}
.order-filters{align-items:center}
.order{margin-bottom:18px;padding:18px 20px}
.order-header{display:flex;justify-content:space-between;align-items:flex-start;padding-bottom:12px;border-bottom:1px solid #f1f5f9;margin-bottom:12px}
.order-id-meta{display:flex;align-items:center;gap:10px;margin-bottom:4px}
.order-id-tag{font-size:14px;font-weight:700;color:var(--primary)}
.order-date{font-size:12px;color:var(--muted)}
.order-customer-title{font-size:15px;margin:0}
.order-address-box{font-size:13px;padding:8px 12px;background:#f8fafc;border-radius:6px;border:1px solid var(--line);margin-bottom:12px}
.order-lines{padding:8px 0;margin-bottom:12px}
.order-line-item{display:flex;justify-content:space-between;font-size:13px;padding:4px 0}
.order-footer{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;padding-top:12px;border-top:1px solid var(--line)}
.order-total strong{font-size:17px;color:var(--primary)}
.order-actions{display:flex;gap:8px;align-items:center;flex-wrap:wrap}
.badge{font-size:12px;font-weight:600;padding:4px 10px;border-radius:999px;display:inline-flex;align-items:center;gap:6px;white-space:nowrap}
.status-dot{width:6px;height:6px;border-radius:50%;background:currentColor}
.status-pending{background:#fef3c7;color:#92400e;border:1px solid #fde68a}
.status-confirmed{background:#dbeafe;color:#1e40af;border:1px solid #bfdbfe}
.status-shipping{background:#ede9fe;color:#5b21b6;border:1px solid #ddd6fe}
.status-completed,.badge.green{background:#dcfce7;color:#166534;border:1px solid #bbf7d0}
.status-cancelled,.badge.red{background:#fee2e2;color:#991b1b;border:1px solid #fecaca}
.role-tag{font-size:11px;font-weight:600;padding:2px 8px;border-radius:4px;display:inline-block}
.role-customer{background:#eff6ff;color:#1d4ed8;border:1px solid #bfdbfe}
.role-staff{background:#fef3c7;color:#92400e;border:1px solid #fde68a}
.role-admin{background:#ede9fe;color:#5b21b6;border:1px solid #ddd6fe}
.invoice-wrapper{max-width:860px;margin:0 auto}
.invoice-nav-bar{display:flex;justify-content:space-between;margin-bottom:16px}
.invoice{padding:30px}
.invoice-header{display:flex;justify-content:space-between;align-items:flex-start;padding-bottom:18px;border-bottom:2px solid var(--line);margin-bottom:20px}
.invoice-sub{display:block;font-size:11px;color:var(--muted);letter-spacing:1px;margin-top:4px}
.invoice-title-box{text-align:right}
.invoice-title-box h2{font-size:20px;margin:0 0 4px}
.invoice-id{font-size:14px;color:var(--primary);font-weight:700}
.invoice-meta-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px;background:#f8fafc;padding:14px 18px;border-radius:6px;border:1px solid var(--line);margin-bottom:20px;font-size:13px;line-height:1.6}
.invoice-total-section{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;margin-top:20px;padding-top:16px;border-top:1px solid var(--line)}
.invoice-notes{max-width:440px;font-size:12px;color:var(--muted)}
.invoice-total-box{text-align:right}
.invoice-total-price{display:block;font-size:22px;color:var(--primary);font-weight:700}
.manage-table-panel h3{margin:0}
.table-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}
.product-cat-sm{font-size:11px;color:var(--primary);background:#eff6ff;padding:1px 6px;border-radius:3px}
.actions-cell{white-space:nowrap}
.manage-form-panel h2{font-size:18px;margin:0}
.form-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;padding-bottom:12px;border-bottom:1px solid #f1f5f9}
.category-row{display:flex;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid var(--line)}
.cat-edit-inline{display:flex;gap:10px;align-items:flex-end;flex:1}
.cat-lbl{flex:1;margin:0;font-size:12px}
.cat-lbl input{margin-top:2px}
.profile-card{max-width:540px;margin:0 auto}
.profile-header{display:flex;align-items:center;gap:16px;padding-bottom:16px;border-bottom:1px solid var(--line);margin-bottom:18px}
.profile-avatar{width:48px;height:48px;border-radius:50%;background:#1e3a8a;color:#fff;display:flex;align-items:center;justify-content:center;font-size:20px;font-weight:700}
.profile-header h2{margin:0 0 2px;font-size:18px}
footer{background:#fff;border-top:1px solid var(--line);padding:24px 20px;color:var(--muted);font-size:12px}
.footer-inner{max-width:1240px;margin:0 auto;display:flex;justify-content:space-between;gap:30px;align-items:flex-start}
.footer-brand p{margin:6px 0 0;max-width:380px;line-height:1.5}
.footer-col{display:flex;flex-direction:column;gap:4px}
.footer-col strong{color:var(--ink)}
.empty-icon-wrap{font-size:36px;margin-bottom:10px}
.empty{text-align:center;padding:48px 20px}
.col-span-3{grid-column:span 3}
@media(max-width:1050px){.grid{grid-template-columns:repeat(2,1fr)}.checkout{grid-template-columns:1fr}.detail{grid-template-columns:1fr}.store-banner{flex-direction:column;align-items:flex-start}}
@media(max-width:768px){.topline-right{display:none}.header-inner{flex-wrap:wrap}.header-search{order:3;max-width:100%}.grid{grid-template-columns:1fr}.stats{grid-template-columns:1fr}.banner-perks{flex-wrap:wrap}.footer-inner{flex-direction:column}}
@media print{header,footer,.topline,.subnav-bar,.flash,button,.invoice-nav-bar{display:none!important}body{background:#fff}main{padding:0}.invoice{border:0;padding:0}.invoice-wrapper{max-width:none}}
""";
}
