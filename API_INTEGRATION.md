# VPN API Integration - ProtonVPN

## Tổng quan

App đã được tích hợp với ProtonVPN API để tự động fetch danh sách VPN servers miễn phí. API này là public API và không cần authentication để lấy danh sách free servers.

## Cấu trúc tích hợp

### 1. API Service (`VpnApiService.kt`)
- Interface Retrofit để gọi ProtonVPN API
- Endpoints:
  - `GET /vpn/v1/logicals` - Lấy danh sách servers
  - `GET /vpn/v1/loads` - Lấy thông tin tải của servers

### 2. API Models (`VpnApiModels.kt`)
- `VpnServerListResponse` - Response từ API
- `LogicalServer` - Logical server model
- `PhysicalServer` - Physical server (entry point) model

### 3. API Client (`VpnApiClient.kt`)
- Xử lý API calls
- Convert API models sang domain models
- Filter chỉ lấy free tier servers (tier = 0)

### 4. Sync Service (`VpnServerSyncService.kt`)
- Sync servers từ API vào local database
- Hỗ trợ force refresh và merge mode

## Cách hoạt động

1. **App Startup**: Khi app khởi động, `VpnApplication` tự động gọi `syncServersFromApi()` trong background
2. **First Launch**: Nếu database trống, servers sẽ được fetch từ API
3. **Fallback**: Nếu API fail, app sẽ sử dụng sample data từ `SampleDataInitializer`

## API Endpoint

- **Base URL**: `https://api.protonvpn.net/`
- **Free Tier Endpoint**: `vpn/v1/logicals?Tier=0`
- **No Authentication Required**: API này là public, không cần login

## Cấu hình

### NetworkModule
- Retrofit được cấu hình với base URL: `https://api.protonvpn.net/`
- OkHttpClient với logging interceptor (BASIC level)
- Timeout: 30 seconds

### Auto Sync
- Sync tự động khi app khởi động (chỉ nếu database trống)
- Có thể force refresh bằng cách gọi `syncServersFromApi(forceRefresh = true)`

## Lưu ý

1. **ProtonVPN Certificate Auth**: ProtonVPN sử dụng certificate-based authentication, không phải username/password. Hiện tại app chưa implement certificate handling, cần thêm sau.

2. **Server Connection**: Để kết nối thực sự tới ProtonVPN servers, cần:
   - Certificate từ ProtonVPN
   - Proper OpenVPN configuration
   - Hoặc sử dụng ProtonVPN official app

3. **Free Tier Limitations**: 
   - Chỉ có thể lấy danh sách free servers
   - Cần account ProtonVPN để kết nối thực sự
   - Free tier có giới hạn về số lượng servers và tốc độ

## Testing

Để test API integration:

1. Clear app data để force sync từ API
2. Check logs với tag `VpnApiClient` và `VpnServerSyncService`
3. Verify servers được lưu vào database

## Future Improvements

- [ ] Implement certificate-based authentication
- [ ] Add server load information
- [ ] Cache API responses
- [ ] Periodic server list updates
- [ ] Support for premium tier (requires authentication)

