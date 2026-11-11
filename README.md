# ChiTieuApp – Quản lý chi tiêu cá nhân

Ứng dụng Android ghi thu/chi theo danh mục, đặt ngân sách tháng, xem biểu đồ thống kê và lưu ảnh hoá đơn.
Dữ liệu **dùng được khi offline** và **đồng bộ lên Firebase** khi có mạng.

> Đồ án môn lập trình Android: Jetpack Compose + Material 3, Navigation type-safe, MVVM, Hilt, Room, Firebase.

## Tính năng

| Màn hình | Chức năng |
|---|---|
| **Đăng nhập** | Đăng nhập Google qua Credential Manager + Firebase Auth |
| **Tổng quan** | Số dư, thu/chi của tháng, tiến độ ngân sách, 5 giao dịch gần nhất, đăng xuất |
| **Giao dịch** | Danh sách theo tháng, nhóm theo ngày, lọc theo danh mục, xoá (có xác nhận) |
| **Thêm/Sửa giao dịch** | Thu/chi, số tiền, danh mục, ngày, ghi chú, **chụp/chọn ảnh hoá đơn** |
| **Thống kê** | Biểu đồ donut theo danh mục, biểu đồ cột theo ngày, chuyển Chi/Thu, chọn tháng |
| **Ngân sách & danh mục** | Đặt hạn mức từng tháng, thanh tiến độ, thêm/xoá danh mục tuỳ chỉnh |

Ngoài ra:
- **Thông báo local** khi chi tiêu đạt 80% và 100% ngân sách (mỗi mốc/tháng báo một lần).
- **Runtime permission** `POST_NOTIFICATIONS` (Android 13+): giải thích lý do → xin quyền → nếu bị từ chối vĩnh viễn thì dẫn tới cài đặt thông báo.
- **WorkManager** đồng bộ nền (khi có mạng, tự retry với backoff; định kỳ 6 giờ).
- Ảnh hoá đơn dùng app camera hệ thống qua `FileProvider` và Photo Picker, nên **không cần quyền CAMERA/đọc bộ nhớ**.

## Kiến trúc

```
UI (Compose, Material 3)
  Screen  ──collectAsStateWithLifecycle──▶  ViewModel (StateFlow<UiState>)
                                                │
                                         Repository (interface)
                                                │
                    ┌───────────────────────────┴───────────────┐
                 Room (nguồn dữ liệu duy nhất cho UI)      SyncManager
                 transactions / categories / budgets        ├─ Cloud Firestore  users/{uid}/...
                                                            └─ Cloud Storage    users/{uid}/receipts/...
```

- **Offline-first**: UI chỉ đọc Room. Mọi thao tác ghi vào Room trước (`synced = false`), sau đó `SyncScheduler` xếp `SyncWorker` (WorkManager, ràng buộc có mạng).
- **SyncWorker/SyncManager**: (1) đẩy mọi thay đổi chưa sync (kể cả xoá mềm và upload ảnh), (2) kéo dữ liệu từ Firestore; bản ghi nào server mới hơn (`updatedAt`) và local không có thay đổi chờ đẩy thì ghi đè local.
- **DI**: Hilt (`@HiltViewModel`, `@HiltWorker`, module cung cấp Room/Firebase/Clock, `@Binds` repository).
- **Navigation**: Navigation Compose type-safe, route là `@Serializable` object/data class (`DashboardRoute`, `AddEditRoute(id)`…).

### Cấu trúc thư mục

```
app/src/main/java/com/example/chitieuapp/
├── data/
│   ├── local/        Room: Entities, Daos, AppDatabase, DefaultCategories
│   ├── repository/   Interface + implementation (Transaction, Category, Budget)
│   ├── sync/         SyncManager, SyncWorker, SyncScheduler, RemoteMappers
│   ├── auth/         AuthRepository (Credential Manager + Firebase Auth)
│   ├── notify/       BudgetNotifier (thông báo vượt ngân sách)
│   └── receipt/      ReceiptStore (ảnh hoá đơn, FileProvider)
├── di/               Hilt modules
├── domain/           Model hiển thị + hàm tiện ích
└── ui/
    ├── navigation/   Routes + NavHost + bottom bar
    ├── login/ dashboard/ transactions/ addedit/ stats/ budget/   (Screen + ViewModel)
    ├── components/   MonthSelector, TransactionRow, biểu đồ vẽ bằng Canvas
    └── theme/        Material 3 theme
```

## Công nghệ

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Hilt · Room (KSP) · Kotlin Coroutines/Flow ·
WorkManager · Firebase Auth / Firestore / Storage · Credential Manager · Coil 3 · JUnit4 + coroutines-test · Compose UI Test.

## Cài đặt & chạy

1. Clone repo, mở bằng Android Studio.
2. Tạo project Firebase, thêm app Android với package `com.example.chitieuapp`.
3. Thêm **SHA-1 và SHA-256** của keystore debug (`./gradlew signingReport`) vào Firebase.
4. Bật **Authentication → Google**, **Firestore Database**, (tuỳ chọn) **Storage**.
5. Tải `google-services.json` **sau khi** đã thêm SHA và bật Google Sign-In, đặt vào `../../Downloads/ChiTieuApp-code/ChiTieuApp/app`.
   File này sinh ra chuỗi `default_web_client_id` mà code đăng nhập cần.
6. Triển khai rules: nội dung `firestore.rules` và `storage.rules` dán vào Firebase Console (hoặc `firebase deploy --only firestore:rules,storage`).
7. Chạy app (`Run ▶`).

> Cloud Storage trên Firebase có thể yêu cầu gói Blaze. Nếu không bật Storage, app vẫn chạy bình thường:
> ảnh hoá đơn được giữ trên máy, việc upload thất bại sẽ được bỏ qua và thử lại ở lần đồng bộ sau.

## Chạy test

```bash
# Unit test ViewModel (JVM)
./gradlew testDebugUnitTest

# Compose UI test (cần emulator/máy thật đang chạy)
./gradlew connectedDebugAndroidTest
```

Unit test (4 lớp, dùng fake repository + `runTest`): `DashboardViewModelTest`, `AddEditViewModelTest`, `StatsViewModelTest`, `BudgetViewModelTest`.
Compose UI test: `AddEditContentTest` (validate form, nhập số tiền, chọn danh mục, bấm lưu).

## Build bản release (có ký, bật R8)

1. Tạo keystore (một lần):
   ```bash
   keytool -genkeypair -v -keystore keystore.jks -alias chitieu -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Sao chép `keystore.properties.example` thành `keystore.properties` và điền thông tin. **Không commit** `keystore.jks` và `keystore.properties`.
3. Lấy SHA-1/SHA-256 của keystore release (`keytool -list -v -keystore keystore.jks -alias chitieu`), thêm vào Firebase, **tải lại `google-services.json`** — nếu không, đăng nhập Google ở bản release sẽ lỗi.
4. Build:
   ```bash
   ./gradlew assembleRelease     # APK: app/build/outputs/apk/release/
   ./gradlew bundleRelease       # AAB: app/build/outputs/bundle/release/
   ```
   R8 được bật trong `buildTypes.release` (`optimization { enable = true }`).

## Các quyết định kỹ thuật (để bảo vệ)

- **Room là nguồn dữ liệu duy nhất cho UI**: app luôn dùng được offline; mất mạng chỉ làm chậm đồng bộ, không ảnh hưởng thao tác của người dùng.
- **Xoá mềm (`deleted`) + cờ `synced`**: biết chính xác bản ghi nào cần đẩy/xoá trên server, kể cả khi người dùng thao tác lúc offline.
- **Giải quyết xung đột bằng `updatedAt` (last-write-wins)**: đơn giản, đủ cho ứng dụng một người dùng nhiều thiết bị. `markSynced` so sánh `updatedAt` để không đánh dấu nhầm nếu bản ghi bị sửa trong lúc đang đồng bộ.
- **Kéo dữ liệu bằng `Source.SERVER`**: nếu offline sẽ ném lỗi thay vì trả cache thiếu, tránh xoá nhầm dữ liệu local.
- **Ánh xạ Firestore bằng `Map` thủ công**, không dùng reflection `toObject<T>()`: bản release bật R8 không làm hỏng dữ liệu vì đổi tên field.
- **`WorkManager` thay vì tự quản lý luồng**: được bảo đảm chạy lại sau khi tắt app/khởi động lại máy, có ràng buộc mạng và backoff.
- **Ảnh hoá đơn: `TakePicture` + `FileProvider` + Photo Picker**: không cần xin quyền nguy hiểm, giảm bề mặt quyền và đơn giản hoá luồng permission.
- **ID danh mục mặc định cố định** (`food`, `transport`…): giao dịch đồng bộ giữa các thiết bị vẫn khớp danh mục; danh mục mặc định không đồng bộ vì đã có trong code.
- **Repository là interface**: ViewModel test bằng fake, không cần Robolectric/emulator.
- **Biểu đồ tự vẽ bằng `Canvas`**: không thêm thư viện, kiểm soát hoàn toàn giao diện.
- **Số tiền lưu `Long` (đồng)**: tránh sai số số thực; VND không có phần thập phân.
- **Ngày lưu dạng `epochDay`**: không bị lệch do múi giờ khi đồng bộ giữa thiết bị.

## Hạn chế & hướng phát triển

- Đồng bộ last-write-wins (chưa merge theo từng trường).
- Chưa có tìm kiếm, xuất CSV, giao dịch định kỳ, đa tiền tệ (có sẵn Retrofit/DataStore trong dependency để mở rộng: tỷ giá).
- Chưa có FCM push; thông báo hiện là thông báo local.
