# VPN Server Status

## ✅ Setup Complete!

VPN server đã được setup thành công trên Mac của bạn.

### Thông tin Server:
- **IP Address:** 192.168.100.241
- **Port:** 1194
- **Protocol:** UDP
- **Config Location:** `~/openvpn-server/server.conf`

### Để khởi động VPN server:

```bash
./start-vpn-server.sh
```

Hoặc chạy trực tiếp:
```bash
sudo /opt/homebrew/opt/openvpn/sbin/openvpn --config ~/openvpn-server/server.conf
```

**Lưu ý:** Cần nhập password của Mac để chạy với quyền sudo.

### Để dừng VPN server:

```bash
./stop-vpn-server.sh
```

Hoặc:
```bash
sudo pkill -f 'openvpn.*server.conf'
```

### Kiểm tra server đang chạy:

```bash
ps aux | grep openvpn | grep -v grep
```

### Mở Firewall (nếu cần):

1. System Preferences > Security & Privacy > Firewall
2. Click "Firewall Options"
3. Thêm rule cho OpenVPN

Hoặc chạy:
```bash
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /opt/homebrew/opt/openvpn/sbin/openvpn
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblockapp /opt/homebrew/opt/openvpn/sbin/openvpn
```

### Kết nối từ Android:

Server đã được thêm vào app với tên **"Local Mac Server"**.

1. Mở app trên điện thoại
2. Đảm bảo điện thoại và Mac cùng WiFi
3. Chọn server "Local Mac Server"
4. Click Connect

### Troubleshooting:

**Nếu không kết nối được:**

1. Kiểm tra server đang chạy:
   ```bash
   ps aux | grep openvpn
   ```

2. Kiểm tra port đang listen:
   ```bash
   lsof -i :1194
   ```

3. Kiểm tra firewall:
   ```bash
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --listapps
   ```

4. Xem logs (nếu server chạy với --log):
   ```bash
   tail -f ~/openvpn-server/server.log
   ```

**Nếu IP Mac thay đổi:**

Cập nhật IP trong file:
- `app/src/main/java/com/amobear/freevpn/data/local/SampleDataInitializer.kt`
- Thay `192.168.100.241` bằng IP mới

Lấy IP mới:
```bash
ipconfig getifaddr en0
```

