import subprocess, time, atexit, re, urllib.request, urllib.parse, urllib.error, http.cookiejar
from pathlib import Path

jar = Path(__file__).resolve().parents[1] / 'TechNest.jar'
log = open(Path(__file__).parent / 'test-thorough.log', 'w', encoding='utf-8')
server = subprocess.Popen(['java', '-jar', str(jar), '18082'], stdout=log, stderr=subprocess.STDOUT)

def cleanup():
    server.terminate()
    server.wait(timeout=10)
    log.close()

atexit.register(cleanup)
time.sleep(1.5)
if server.poll() is not None:
    raise RuntimeError('Khong chay duoc may chu TechNest.jar tren cong 18082. Xem tests/test-thorough.log')

class Response:
    def __init__(self, r):
        self.status_code = r.code
        self.text = r.read().decode('utf-8', errors='replace')

class Session:
    def __init__(self):
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def get(self, url):
        return self.request(url)
    def post(self, url, data):
        return self.request(url, urllib.parse.urlencode(data).encode('utf-8'))
    def request(self, url, data=None):
        try:
            return Response(self.opener.open(url, data))
        except urllib.error.HTTPError as e:
            return Response(e)

B = 'http://127.0.0.1:18082'

def get_csrf(s, url='/login'):
    r = s.get(B + url)
    m = re.search(r"name='csrf' value='([^']+)'", r.text)
    if not m:
        m = re.search(r'name="csrf" value="([^"]+)"', r.text)
    if not m:
        raise ValueError(f"Khong tim thay token CSRF tai {url}")
    return m.group(1)

def post(s, op, **data):
    token = get_csrf(s, '/login' if 'email' in data else '/cart')
    return s.post(B + '/action', {'csrf': token, 'op': op, **data})

def stock(s, pid):
    r = s.get(B + f'/product?id={pid}')
    m = re.search(r'Tồn kho:\s*(\d+)', r.text)
    if not m:
        raise ValueError(f"Khong tim thay thong tin ton kho cho san pham {pid}")
    return int(m.group(1))

print("=== 1. KIEM TRA GIAO DIEN CONG KHAI (KHACH VANG LAI) ===")
anon = Session()
r_home = anon.get(B + '/')
assert r_home.status_code == 200
assert 'TechNest' in r_home.text
assert 'Bộ lọc sản phẩm' in r_home.text
assert 'iPhone 15' in r_home.text
assert 'Samsung Galaxy S24' in r_home.text
assert 'MacBook Air M2' in r_home.text
assert 'Ảnh minh họa' in r_home.text

# Test search & filters
r_search = anon.get(B + '/?q=iPhone')
assert r_search.status_code == 200
assert 'iPhone 15' in r_search.text
assert 'Samsung Galaxy S24' not in r_search.text

r_cat = anon.get(B + '/?category=2')
assert r_cat.status_code == 200
assert 'MacBook Air M2' in r_cat.text
assert 'ASUS Vivobook 15' in r_cat.text
assert 'iPhone 15' not in r_cat.text

r_detail = anon.get(B + '/product?id=1')
assert r_detail.status_code == 200
assert 'iPhone 15' in r_detail.text
assert '22.000.000 ₫' in r_detail.text
assert 'Tồn kho: 10 sản phẩm' in r_detail.text
assert 'Đăng nhập để mua hàng' in r_detail.text
print("-> Khach vang lai: Xem danh sach, tim kiem, loc danh muc, xem chi tiet: OK")

print("=== 2. KIEM TRA LUONG KHACH HANG (DANG NHAP, GIO HANG, DAT HANG, HUY DON) ===")
cust = Session()
cust.get(B + '/login')
r_login = post(cust, 'login', email='a@gmail.com', password='matkhau123')
assert r_login.status_code == 200
assert 'Nguyễn Văn A' in cust.get(B + '/').text

# Add to cart
init_stock_1 = stock(cust, 1)
init_stock_3 = stock(cust, 3)
r_add1 = post(cust, 'add', id=1, quantity=2)
assert r_add1.status_code == 200
r_add3 = post(cust, 'add', id=3, quantity=1)
assert r_add3.status_code == 200

# View cart
r_cart = cust.get(B + '/cart')
assert r_cart.status_code == 200
assert 'iPhone 15' in r_cart.text
assert 'MacBook Air M2' in r_cart.text
assert '71.000.000 ₫' in r_cart.text  # 2*22tr + 27tr = 71tr

# Update quantity
r_qty = post(cust, 'quantity', id=1, quantity=1)
assert r_qty.status_code == 200
r_cart2 = cust.get(B + '/cart')
assert '49.000.000 ₫' in r_cart2.text  # 1*22tr + 27tr = 49tr

# Checkout
r_co = post(cust, 'checkout', address='12 Trần Phú, Phường 3, TP Đà Lạt')
assert r_co.status_code == 200
m_order = re.search(r'Mã đơn #(\d+)', r_co.text)
assert m_order is not None, "Khong tim thay ma don hang trong ket qua dat hang"
order_id = int(m_order.group(1))

# Check stock deduction
assert stock(cust, 1) == init_stock_1 - 1
assert stock(cust, 3) == init_stock_3 - 1

# Check invoice
r_inv = cust.get(B + f'/invoice?id={order_id}')
assert r_inv.status_code == 200
assert 'PHIẾU ĐƠN HÀNG' in r_inv.text
assert f'Mã đơn #{order_id}' in r_inv.text
assert '49.000.000 ₫' in r_inv.text
assert 'Nguyễn Văn A' in r_inv.text

# Cancel order
r_cancel = post(cust, 'cancel', id=order_id)
assert r_cancel.status_code == 200
assert stock(cust, 1) == init_stock_1
assert stock(cust, 3) == init_stock_3
print("-> Luong khach hang: Dang nhap, gio hang, cap nhat SL, dat hang, tru ton, xem hoa don, huy don hoan ton: OK")

print("=== 3. KIEM TRA LUONG NHAN VIEN (QUAN LY DON & QUAN LY SAN PHAM) ===")
# Create a new order for staff to process
post(cust, 'add', id=2, quantity=2)
r_co2 = post(cust, 'checkout', address='Đà Lạt')
order_id2 = int(re.search(r'Mã đơn #(\d+)', r_co2.text).group(1))

staff = Session()
staff.get(B + '/login')
r_staff_login = post(staff, 'login', email='b@shop.vn', password='matkhau456')
assert r_staff_login.status_code == 200

# View staff orders
r_staff_orders = staff.get(B + '/orders')
assert r_staff_orders.status_code == 200
assert 'Quản lý đơn hàng' in r_staff_orders.text
assert f'Mã đơn #{order_id2}' in r_staff_orders.text

# Staff confirms order
r_confirm = post(staff, 'confirm', id=order_id2)
assert r_confirm.status_code == 200
r_orders_conf = staff.get(B + '/orders')
assert 'Đã xác nhận' in r_orders_conf.text

# Staff ships order
r_ship = post(staff, 'ship', id=order_id2)
assert r_ship.status_code == 200
r_orders_ship = staff.get(B + '/orders')
assert 'Đang giao' in r_orders_ship.text

# Staff completes order
r_comp = post(staff, 'complete', id=order_id2)
assert r_comp.status_code == 200
r_orders_comp = staff.get(B + '/orders')
assert 'Hoàn tất' in r_orders_comp.text

# Staff product management
r_manage = staff.get(B + '/manage')
assert r_manage.status_code == 200
assert 'Quản lý sản phẩm' in r_manage.text
assert 'Danh sách sản phẩm trong kho' in r_manage.text

# Add new product
r_add_prod = post(staff, 'product-save', id=0, name='iPad Pro M4', description='Máy tính bảng cao cấp', price=28900000, stock=5, category=1)
assert r_add_prod.status_code == 200
assert 'iPad Pro M4' in staff.get(B + '/manage').text
assert '28.900.000 ₫' in staff.get(B + '/manage').text

# Edit product
new_prod_id = 7
r_edit_prod = post(staff, 'product-save', id=new_prod_id, name='iPad Pro M4 256GB', description='Cập nhật mô tả', price=29500000, stock=8, category=1)
assert r_edit_prod.status_code == 200
assert 'iPad Pro M4 256GB' in cust.get(B + f'/product?id={new_prod_id}').text
assert stock(cust, new_prod_id) == 8

# Delete product
r_del_prod = post(staff, 'product-delete', id=new_prod_id)
assert r_del_prod.status_code == 200
assert 'iPad Pro M4 256GB' not in staff.get(B + '/manage').text
print("-> Luong nhan vien: Quan ly don (Xac nhan -> Giao hang -> Hoan tat), Them/Sua/Xoa san pham: OK")

print("=== 4. KIEM TRA LUONG QUAN TRI VIEN (DANH MUC, TAI KHOAN) ===")
adm = Session()
adm.get(B + '/login')
post(adm, 'login', email='admin@shop.vn', password='admin123')

# Category CRUD
r_cat_page = adm.get(B + '/categories')
assert r_cat_page.status_code == 200
assert 'Danh mục sản phẩm' in r_cat_page.text

r_cat_add = post(adm, 'category-save', id=0, name='Máy chơi game')
assert r_cat_add.status_code == 200
assert 'Máy chơi game' in adm.get(B + '/categories').text

r_cat_del = post(adm, 'category-delete', id=4)
assert r_cat_del.status_code == 200
assert 'Máy chơi game' not in adm.get(B + '/categories').text

# Account CRUD
r_acc_page = adm.get(B + '/accounts')
assert r_acc_page.status_code == 200
assert 'Tài khoản người dùng' in r_acc_page.text
r_acc_add = post(adm, 'account-add', name='Nhân viên Mới', email='nvmoi@shop.vn', password='password123', phone='0911222333', address='Đà Lạt', role='NHAN_VIEN')
assert r_acc_add.status_code == 200
assert 'Nhân viên Mới' in adm.get(B + '/accounts').text
assert 'nvmoi@shop.vn' in adm.get(B + '/accounts').text
print("-> Luong quan tri vien: Quan ly danh muc, tao tai khoan theo phan quyen: OK")

print("\n>>> TAT CA KIEM TRA NGHIEP VU VA GIAO DIEN DEU DAT 100%! <<<")
