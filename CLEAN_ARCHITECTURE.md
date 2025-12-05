# Clean Architecture Implementation

## Tổng quan

App được triển khai theo **Clean Architecture** với 3 layers chính:

```
┌─────────────────────────────────────┐
│     Presentation Layer               │
│  (ViewModels, UI Components)        │
│  ↓ Depends on                        │
├─────────────────────────────────────┤
│     Domain Layer                     │
│  (Use Cases, Models, Interfaces)    │
│  ↓ Depends on                        │
├─────────────────────────────────────┤
│     Data Layer                       │
│  (Repository Impl, Data Sources)    │
└─────────────────────────────────────┘
```

## Cấu trúc Layers

### 1. **Presentation Layer** (`presentation/`)
- **Responsibility**: UI logic, state management
- **Dependencies**: Chỉ phụ thuộc vào Domain Layer (Use Cases)
- **Components**:
  - `MainViewModel` - Quản lý UI state, chỉ sử dụng Use Cases
  - `MainScreen` - UI components (Compose)

**Ví dụ MainViewModel:**
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    // Chỉ inject Use Cases - Domain Layer
    private val connectVpnUseCase: ConnectVpnUseCase,
    private val syncServersUseCase: SyncServersUseCase,
    // ...
) : ViewModel()
```

### 2. **Domain Layer** (`domain/`)
- **Responsibility**: Business logic, business rules
- **Dependencies**: Không phụ thuộc vào bất kỳ layer nào khác
- **Components**:
  - **Models** (`domain/model/`): Business entities
    - `Server`, `VpnConnection`, `TrafficStats`, etc.
  - **Use Cases** (`domain/usecase/`): Business operations
    - `ConnectVpnUseCase`
    - `SyncServersUseCase`
    - `GetServersUseCase`
    - `DisconnectVpnUseCase`
    - etc.
  - **Repository Interfaces** (`domain/repository/`): Contracts
    - `VpnRepository`
    - `ServerRepository`

**Ví dụ Use Case:**
```kotlin
class SyncServersUseCase @Inject constructor(
    private val serverRepository: ServerRepository // Interface, không phải implementation
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Int> {
        return serverRepository.syncServersFromApi(forceRefresh)
    }
}
```

### 3. **Data Layer** (`data/`)
- **Responsibility**: Data access, API calls, database operations
- **Dependencies**: Phụ thuộc vào Domain Layer (implement interfaces)
- **Components**:
  - **Repository Implementations** (`data/repository/`):
    - `VpnRepositoryImpl` - implements `VpnRepository`
    - `ServerRepositoryImpl` - implements `ServerRepository`
  - **Data Sources**:
    - `ServerDao` - Room database
    - `VpnApiClient` - API calls
    - `VpnServerSyncService` - Sync logic

**Ví dụ Repository Implementation:**
```kotlin
class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao,
    private val vpnServerSyncService: VpnServerSyncService
) : ServerRepository { // Implement interface từ Domain Layer
    override suspend fun syncServersFromApi(forceRefresh: Boolean): Result<Int> {
        return vpnServerSyncService.syncServersFromApi(forceRefresh)
    }
}
```

## Dependency Rules

### ✅ Đúng (Clean Architecture)
1. **Presentation → Domain**: ViewModel sử dụng Use Cases
2. **Data → Domain**: Repository implementations implement Domain interfaces
3. **Domain**: Không phụ thuộc vào bất kỳ layer nào

### ❌ Sai (Vi phạm Clean Architecture)
1. **Presentation → Data**: ViewModel không được inject Repository implementations trực tiếp
2. **Domain → Data**: Domain không được biết về Data layer
3. **Presentation → Data**: ViewModel không được inject Data services trực tiếp

## Use Cases

### Danh sách Use Cases hiện có:

1. **ConnectVpnUseCase**: Kết nối VPN
2. **DisconnectVpnUseCase**: Ngắt kết nối VPN
3. **ConnectFromOvpnFileUseCase**: Kết nối từ file .ovpn
4. **GetServersUseCase**: Lấy danh sách servers
5. **SyncServersUseCase**: Sync servers từ API ⭐ (mới thêm)
6. **InitializeDataUseCase**: Khởi tạo dữ liệu ⭐ (mới thêm)
7. **PingServerUseCase**: Ping server để test latency
8. **ObserveConnectionUseCase**: Observe connection state
9. **MonitorTrafficUseCase**: Monitor traffic stats
10. **TestSpeedUseCase**: Test tốc độ

## Dependency Injection

Sử dụng **Hilt** để quản lý dependencies:

- **RepositoryModule**: Bind repository implementations
- **NetworkModule**: Cấu hình Retrofit, OkHttp
- **DatabaseModule**: Cấu hình Room database
- **VpnModule**: Cấu hình VPN services

## Benefits của Clean Architecture

1. **Testability**: Dễ test từng layer độc lập
2. **Maintainability**: Code dễ maintain, dễ hiểu
3. **Scalability**: Dễ mở rộng, thêm features mới
4. **Separation of Concerns**: Mỗi layer có trách nhiệm rõ ràng
5. **Independence**: Domain layer không phụ thuộc vào framework

## Migration từ Old Code

### Trước (Vi phạm Clean Architecture):
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val sampleDataInitializer: SampleDataInitializer, // ❌ Data layer
    private val vpnServerSyncService: VpnServerSyncService,   // ❌ Data layer
    // ...
)
```

### Sau (Clean Architecture):
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val initializeDataUseCase: InitializeDataUseCase, // ✅ Domain layer
    private val syncServersUseCase: SyncServersUseCase,       // ✅ Domain layer
    // ...
)
```

## Best Practices

1. **ViewModel chỉ inject Use Cases**, không inject Repository hoặc Data services
2. **Use Cases chỉ inject Repository interfaces**, không inject implementations
3. **Domain layer không có dependencies** ngoài Kotlin standard library
4. **Mỗi Use Case có một trách nhiệm** duy nhất (Single Responsibility)
5. **Repository interfaces ở Domain layer**, implementations ở Data layer

## File Structure

```
app/src/main/java/com/amobear/freevpn/
├── domain/                    # Domain Layer
│   ├── model/                 # Business models
│   ├── repository/            # Repository interfaces
│   └── usecase/               # Use cases
├── data/                      # Data Layer
│   ├── repository/            # Repository implementations
│   ├── local/                 # Database, local storage
│   └── network/               # API clients
└── presentation/              # Presentation Layer
    └── main/                  # ViewModels, UI
```

## Testing Strategy

Với Clean Architecture, testing trở nên dễ dàng:

1. **Domain Layer**: Unit tests cho Use Cases (không cần Android)
2. **Data Layer**: Unit tests cho Repository implementations
3. **Presentation Layer**: ViewModel tests với mock Use Cases

## Kết luận

App hiện tại đã được refactor hoàn toàn theo Clean Architecture:
- ✅ ViewModel chỉ phụ thuộc vào Use Cases
- ✅ Use Cases chỉ phụ thuộc vào Repository interfaces
- ✅ Domain layer độc lập, không phụ thuộc vào framework
- ✅ Dễ test, dễ maintain, dễ mở rộng

