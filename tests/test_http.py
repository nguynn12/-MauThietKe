import subprocess, time, atexit
from pathlib import Path
jar = Path(__file__).resolve().parents[1] / 'TechNest.jar'
log = open(Path(__file__).parent / 'test-server.log', 'w', encoding='utf-8')
server = subprocess.Popen(['java', '-jar', str(jar), '18080'], stdout=log, stderr=subprocess.STDOUT)
def cleanup():
 server.terminate()
 server.wait(timeout=10)
 log.close()
atexit.register(cleanup)
time.sleep(1)
if server.poll() is not None: raise RuntimeError('Khong chay duoc may chu. Xem tests/test-server.log')
import re, urllib.request, urllib.parse, urllib.error, http.cookiejar
class Response:
 def __init__(self,r):self.status_code=r.code;self.text=r.read().decode()
class Session:
 def __init__(self):self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
 def get(self,url):return self.request(url)
 def post(self,url,data):return self.request(url,urllib.parse.urlencode(data).encode())
 def request(self,url,data=None):
  try:return Response(self.opener.open(url,data))
  except urllib.error.HTTPError as e:return Response(e)
class requests:Session=Session
B='http://127.0.0.1:18080'
def session(email=None,password=None):
 s=requests.Session();s.get(B+'/login')
 if email: post(s,'login',email=email,password=password)
 return s
def post(s,op,**data):
 r=s.get(B+'/login'); token=re.search("name='csrf' value='([^']+)'",r.text)[1]
 return s.post(B+'/action',data={'csrf':token,'op':op,**data})
def stock(s,id):return int(re.search('Tồn kho: (\\d+)',s.get(B+f'/product?id={id}').text)[1])
a=session('a@gmail.com','matkhau123');n=session('b@shop.vn','matkhau456');anon=session();adm=session('admin@shop.vn','admin123')
assert anon.get(B+'/manage').status_code==400
assert a.get(B+'/manage').status_code==400
assert post(a,'login',email='a@gmail.com',password='wrong').status_code==400
assert a.post(B+'/action',data={'op':'checkout','csrf':'invalid','address':'x'}).status_code==400
assert post(a,'add',id=2,quantity=3).status_code==200
assert post(a,'add',id=2,quantity=3).status_code==400
assert post(a,'quantity',id=2,quantity=0).status_code==400
before=stock(a,2)
r=post(a,'checkout',address='12 Trần Phú, Đà Lạt');assert r.status_code==200
id=int(re.search('Mã đơn #(\\d+)',r.text)[1]);assert stock(a,2)==before-3
assert post(a,'cancel',id=id).status_code==200;assert stock(a,2)==before
assert post(a,'cancel',id=id).status_code==400;assert stock(a,2)==before
assert post(a,'checkout',address='Đà Lạt').status_code==400
post(a,'add',id=2,quantity=1);r=post(a,'checkout',address='Đà Lạt');id=int(re.search('Mã đơn #(\\d+)',r.text)[1])
assert post(n,'complete',id=id).status_code==400
assert post(a,'confirm',id=id).status_code==400
assert post(n,'confirm',id=id).status_code==200
assert post(a,'cancel',id=id).status_code==400
assert post(n,'ship',id=id).status_code==200
assert post(n,'complete',id=id).status_code==200
assert post(n,'cancel',id=id).status_code==400
other=session();assert post(other,'register',name='Người thử',email='test@example.com',password='123456',phone='0901234567',address='Đà Lạt').status_code==200
assert other.get(B+f'/invoice?id={id}').status_code==400
assert post(adm,'category-delete',id=1).status_code==400
assert post(adm,'category-save',id=0,name='Kiểm thử').status_code==200
assert post(n,'product-save',id=0,name='Sản phẩm thử',description='Mô tả',price=100000,stock=2,category=4).status_code==200
assert post(n,'product-save',id=7,name='Đã sửa',description='Mô tả mới',price=200000,stock=3,category=4).status_code==200
assert 'Đã sửa' in a.get(B+'/product?id=7').text
assert post(n,'product-delete',id=7).status_code==200
assert post(adm,'category-delete',id=4).status_code==200
print('PASS: HTTP checks: login, roles, CSRF, quantity, empty cart, stock deduction/refund, state transitions, ownership, category/product CRUD.')
