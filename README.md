# Hệ thống Nhà Thông Minh Chuẩn Matter

## Giới thiệu dự án
Dự án xây dựng hệ thống điều khiển và giám sát các thiết bị nhà thông minh tương thích chuẩn Matter, kết hợp giữa phần cứng thực tế sử dụng vi điều khiển ESP32-S3 và ứng dụng di động Android native. Hệ thống hỗ trợ điều khiển các thiết bị bao gồm đèn chiếu sáng, quạt gió và cửa chính thông qua giao thức MQTT. Ngoài giao diện đồ họa truyền thống, hệ thống tích hợp mô hình nhận diện cử chỉ tay dựa trên công nghệ MediaPipe để điều khiển thiết bị không tiếp xúc qua camera trước của điện thoại di động.

Thành viên thực hiện dự án gồm Trần Nguyễn An Sơn, Dương Chung Kiệt và Trương Nguyễn Duy Khang.

## Cấu trúc thư mục dự án
Mã nguồn của hệ thống được chia làm hai phần chính là ứng dụng di động chạy hệ điều hành Android và phần mềm nhúng chạy trên vi điều khiển ESP32-S3:
- Thư mục app chứa toàn bộ mã nguồn Kotlin của ứng dụng Android phát triển trên nền tảng Jetpack Compose, tích hợp thư viện CameraX và MediaPipe để nhận diện cử chỉ tay, cùng với dịch vụ kết nối và đồng bộ trạng thái thiết bị qua MQTT.
- Thư mục esp32_smarthome chứa mã nguồn C++ chạy trên vi điều khiển ESP32-S3 để nhận lệnh điều khiển, thực thi băm xung PWM điều chỉnh quạt và độ sáng đèn, điều khiển động cơ servo mở cửa, đồng thời lưu trữ trạng thái thiết bị vào bộ nhớ flash của chip.
- Tệp Wiring_Guide.md hướng dẫn chi tiết sơ đồ cắm dây, thông số điện trở, transistor và diode bảo vệ cho mạch điện thực tế trên breadboard.

## Sơ đồ kết nối phần cứng
Hệ thống sử dụng các chân GPIO an toàn trên vi điều khiển ESP32-S3 để kết nối với các thiết bị chấp hành nhằm tránh xung đột với phân vùng bộ nhớ OSPI Flash và RAM:
- Đèn phòng khách kết nối chân GPIO 4.
- Đèn phòng ngủ kết nối chân GPIO 5.
- Đèn nhà bếp kết nối chân GPIO 6.
- Quạt phòng khách kết nối chân GPIO 11.
- Quạt phòng ngủ kết nối chân GPIO 12.
- Động cơ servo cửa chính kết nối chân GPIO 13.

Chi tiết về cách đấu nối transistor 2N2222, diode 1N4007 bảo vệ dòng ngược và các điện trở hạn dòng được trình bày đầy đủ trong tài liệu hướng dẫn cắm dây kèm theo dự án.

## Cơ chế vận hành của thiết bị
Đèn led được điều chỉnh độ sáng từ 0% đến 100% bằng cách băm xung PWM 8 bit từ 0 đến 255.
Quạt gió DC được cấu hình chạy với 3 mức tốc độ cân bằng bao gồm tốc độ 1 tương ứng 140 PWM, tốc độ 2 tương ứng 198 PWM và tốc độ 3 tương ứng 255 PWM. Để khắc phục lực ma sát tĩnh ban đầu làm quạt bị nghẹt dòng, hệ thống áp dụng cơ chế khởi động nhanh bằng cách cấp điện áp tối đa 100% trong thời gian 200 miligiây mỗi khi kích hoạt quạt từ trạng thái tắt, sau đó mới hạ về mức PWM của tốc độ đã chọn.
Cửa chính sử dụng động cơ servo SG90 quay góc 90 độ khi mở và quay về góc 0 độ khi đóng.
Trạng thái bật tắt, độ sáng, tốc độ quạt và vị trí cửa được lưu trữ lâu dài vào bộ nhớ flash của ESP32-S3 thông qua thư viện Preferences và bộ nhớ điện thoại qua SharedPreferences để tránh mất dữ liệu khi mất nguồn điện hoặc mất kết nối mạng.

## Điều khiển bằng cử chỉ tay qua ứng dụng Android
Ứng dụng di động sử dụng camera trước của điện thoại và thư viện MediaPipe để thu thập 21 điểm tọa độ trên bàn tay. Hệ thống phân loại cử chỉ bằng các thuật toán hình học và thực hiện các lệnh điều khiển tương ứng:
- Xòe bàn tay để bật tất cả đèn trong nhà.
- Nắm bàn tay để tắt tất cả đèn trong nhà.
- Giơ ngón cái hướng lên để bật quạt phòng khách.
- Úp ngón cái hướng xuống để tắt quạt phòng khách.
- Chỉ ngón trỏ lên trời để tăng cấp tốc độ quạt phòng khách xoay vòng từ 1 đến 3.
- Ký hiệu chữ V để mở cửa chính.
- Giơ và khép chặt ngón trỏ cùng ngón giữa để đóng cửa chính.

Hệ thống áp dụng bộ đệm khử nhiễu tín hiệu trong thời gian 500 miligiây nhằm đảm bảo các cử chỉ được nhận diện chính xác trước khi gửi lệnh điều khiển qua giao thức MQTT đến vi điều khiển.
