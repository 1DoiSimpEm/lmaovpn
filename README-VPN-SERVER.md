# Setup OpenVPN Server trên macOS

Hướng dẫn tạo VPN server trên laptop Mac để điện thoại Android kết nối.

## Bước 1: Chạy script setup

```bash
./setup-vpn-server.sh
```

Script sẽ:
- Cài đặt OpenVPN và easy-rsa (nếu chưa có)
- Tạo certificates (CA, server, client)
- Tạo file cấu hình server và client

## Bước 2: Lấy IP của Mac

Sau khi chạy script, nó sẽ hiển thị IP của Mac. Nếu không, chạy:

```bash
ipconfig getifaddr en0
# hoặc
ipconfig getifaddr en1
```

**Lưu ý quan trọng:** 
- Nếu Mac và điện thoại cùng WiFi: dùng IP local (ví dụ: 192.168.1.x)
- Nếu điện thoại ở mạng khác: cần cấu hình port forwarding trên router

## Bước 3: Mở Firewall

1. System Preferences > Security & Privacy > Firewall
2. Click "Firewall Options"
3. Thêm rule cho OpenVPN (port 1194 UDP)

Hoặc chạy lệnh:
```bash
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/local/sbin/openvpn
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblockapp /usr/local/sbin/openvpn
```

## Bước 4: Khởi động VPN Server

```bash
sudo openvpn --config ~/openvpn-server/server.conf
```

Server sẽ chạy và chờ kết nối từ client.

## Bước 5: Thêm server vào Android App

Có 2 cách:

### Cách 1: Thêm vào SampleDataInitializer

Mở file `app/src/main/java/com/amobear/freevpn/data/local/SampleDataInitializer.kt` và thêm:

```kotlin
ServerEntity(
    id = "local-mac-server",
    name = "Local Mac Server",
    countryCode = "VN", // hoặc mã quốc gia của bạn
    countryName = "Local",
    host = "YOUR_MAC_IP", // Thay bằng IP của Mac (ví dụ: "192.168.1.100")
    port = 1194,
    protocol = "udp",
    username = null, // Certificate-based, không cần username/password
    password = null,
    isPremium = false,
    latency = 0
)
```

### Cách 2: Sử dụng Quick Connect trong MainScreen

Sửa `testServer` trong `MainScreen.kt`:

```kotlin
val testServer = Server(
    id = "local-mac-server",
    name = "Local Mac Server",
    countryCode = "VN",
    countryName = "Local",
    host = "YOUR_MAC_IP", // Thay bằng IP của Mac
    port = 1194,
    protocol = "UDP",
    username = null,
    password = null,
    ovpnConfig = null,
    isPremium = false,
    latency = 0,
    speed = 0.0,
    isFavorite = false
)
```

## Bước 6: Test kết nối

1. Đảm bảo VPN server đang chạy trên Mac
2. Mở app trên điện thoại
3. Chọn server "Local Mac Server"
4. Click Connect

## Troubleshooting

### Không kết nối được

1. **Kiểm tra server đang chạy:**
   ```bash
   ps aux | grep openvpn
   ```

2. **Kiểm tra firewall:**
   ```bash
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --listapps
   ```

3. **Kiểm tra port đang listen:**
   ```bash
   lsof -i :1194
   ```

4. **Xem logs của OpenVPN server:**
   Server sẽ hiển thị logs trong terminal khi có client kết nối.

### Mac và điện thoại khác mạng

Nếu Mac và điện thoại ở mạng khác nhau:
1. Cần cấu hình port forwarding trên router
2. Forward UDP port 1194 đến IP của Mac
3. Dùng public IP của router thay vì local IP

### Tạo thêm client certificates

Nếu muốn tạo thêm client khác:

```bash
cd ~/openvpn-server/easy-rsa
./easyrsa build-client-full client-name nopass
```

Sau đó tạo file `.ovpn` tương tự như `android-client.ovpn`.

