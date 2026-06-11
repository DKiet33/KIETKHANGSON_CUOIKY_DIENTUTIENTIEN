/**
 * CHƯƠNG TRÌNH ĐIỀU KHIỂN HỆ THỐNG SMART HOME QUA ESP32-S3 (MÔ PHỎNG
 * BREADBOARD)
 *
 * Các thư viện cần cài đặt qua Arduino Library Manager:
 * 1. PubSubClient (bởi Nick O'Leary) - Để kết nối giao thức MQTT.
 * 2. ESP32Servo (bởi John K. Bennett) - Để điều khiển động cơ Servo trên chip
 * ESP32.
 *
 * Sơ đồ nối dây an toàn (GPIO Pinout - Tránh xung đột OSPI Flash/RAM):
 * - Đèn phòng khách: GPIO 4
 * - Đèn phòng ngủ: GPIO 5
 * - Đèn nhà bếp: GPIO 6
 * - Quạt phòng khách: GPIO 11
 * - Quạt phòng ngủ: GPIO 12
 * - Cửa chính: GPIO 13
 */

#include <ESP32Servo.h>
#include <Preferences.h>
#include <PubSubClient.h>
#include <WiFi.h>

// ==========================================
// THÔNG TIN CẤU HÌNH KẾT NỐI (THAY ĐỔI TẠI ĐÂY)
// ==========================================
const char *ssid = "H09";                      // Tên mạng Wi-Fi nhà bạn
const char *password = "hoilamgi";             // Mật khẩu Wi-Fi
const char *mqtt_server = "broker.hivemq.com"; // Địa chỉ MQTT Broker
const int mqtt_port = 1883;

// Định nghĩa Topic điều khiển (App -> ESP32) và phản hồi (ESP32 -> App) cho 6
// thiết bị
#define TOPIC_LIGHT_LIVING_SET "smarthome/device/light_living/set"
#define TOPIC_LIGHT_LIVING_STATE "smarthome/device/light_living/state"

#define TOPIC_LIGHT_BED_SET "smarthome/device/light_bed/set"
#define TOPIC_LIGHT_BED_STATE "smarthome/device/light_bed/state"

#define TOPIC_LIGHT_KITCHEN_SET "smarthome/device/light_kitchen/set"
#define TOPIC_LIGHT_KITCHEN_STATE "smarthome/device/light_kitchen/state"

#define TOPIC_FAN_LIVING_SET "smarthome/device/fan_living/set"
#define TOPIC_FAN_LIVING_STATE "smarthome/device/fan_living/state"

#define TOPIC_FAN_BED_SET "smarthome/device/fan_bed/set"
#define TOPIC_FAN_BED_STATE "smarthome/device/fan_bed/state"

#define TOPIC_DOOR_SET "smarthome/device/door_main/set"
#define TOPIC_DOOR_STATE "smarthome/device/door_main/state"

// Định nghĩa sơ đồ chân GPIO an toàn
const int PIN_LIGHT_LIVING = 4;
const int PIN_LIGHT_BED = 5;
const int PIN_LIGHT_KITCHEN = 6;
const int PIN_FAN_LIVING = 11;
const int PIN_FAN_BED = 12;
const int PIN_DOOR_MAIN = 13;

// Biến đối tượng
WiFiClient espClient;
PubSubClient client(espClient);
Servo mainDoorServo;
Preferences preferences;

// Hàm cấu hình và kết nối Wi-Fi
void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.printf("Connecting to WiFi SSID: %s\n", ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  int attempt = 0;
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    attempt++;
    if (attempt % 20 == 0) {
      Serial.printf("\n[WiFi] Still trying to connect. WiFi Status Code: %d\n",
                    WiFi.status());
    }
  }

  Serial.println("");
  Serial.println("[WiFi] Connected successfully!");
  Serial.print("[WiFi] IP address: ");
  Serial.println(WiFi.localIP());
  Serial.printf("[WiFi] Signal Strength (RSSI): %d dBm\n", WiFi.RSSI());
}

// Hàm bổ trợ điều khiển Đèn LED
void handleLight(String topicStr, String commandTopic, String stateTopic,
                 int pin, String prefKey, String message) {
  if (topicStr.equals(commandTopic)) {
    if (message.equals("OFF")) {
      ledcWrite(pin, 0); // Tắt LED
      preferences.putInt(prefKey.c_str(), 0);
      client.publish(stateTopic.c_str(), "OFF", true);
      Serial.printf("-> Light Pin %d turned OFF\n", pin);
    } else if (message.startsWith("ON")) {
      int brightness = 100; // Mặc định sáng tối đa
      if (message.indexOf("BRIGHTNESS=") > 0) {
        brightness =
            message.substring(message.indexOf("BRIGHTNESS=") + 11).toInt();
      }

      // Ánh xạ độ sáng 0-100% sang PWM 8-bit (0-255)
      int pwmVal = map(brightness, 0, 100, 0, 255);
      ledcWrite(pin, pwmVal);
      preferences.putInt(prefKey.c_str(), pwmVal);

      String response = "ON,BRIGHTNESS=" + String(brightness);
      client.publish(stateTopic.c_str(), response.c_str(), true);
      Serial.printf("-> Light Pin %d turned ON at brightness: %d%%\n", pin,
                    brightness);
    }
  }
}

// Hàm bổ trợ điều khiển Quạt kèm cơ chế Kickstart
void handleFan(String topicStr, String commandTopic, String stateTopic, int pin,
               String prefKey, String message) {
  if (topicStr.equals(commandTopic)) {
    if (message.equals("OFF")) {
      ledcWrite(pin, 0); // Tắt quạt
      preferences.putInt(prefKey.c_str(), 0);
      client.publish(stateTopic.c_str(), "OFF", true);
      Serial.printf("-> Fan Pin %d turned OFF\n", pin);
    } else if (message.startsWith("ON")) {
      int speed = 1; // Mặc định tốc độ thấp nhất
      if (message.indexOf("SPEED=") > 0) {
        speed = message.substring(message.indexOf("SPEED=") + 6).toInt();
      }

      // Ánh xạ tốc độ 1-3 sang giá trị PWM (được tăng để tránh quạt bị kẹt sau
      // khi kickstart)
      int pwmVal = 0;
      if (speed == 1)
        pwmVal = 135; // Mức 1 (Cân bằng, bắt đầu từ 135)
      else if (speed == 2)
        pwmVal = 195; // Mức 2
      else if (speed == 3)
        pwmVal = 255; // Mức 3 (100% công suất)

      // Kiểm tra xem quạt có đang tắt hay không để áp dụng kickstart
      int currentPwm = preferences.getInt(prefKey.c_str(), 0);
      if (currentPwm == 0 && pwmVal > 0) {
        // Cấp nguồn tối đa 100% trong 200ms để quạt thắng lực ma sát tĩnh khởi
        // điểm
        Serial.printf("-> Kickstarting Fan on Pin %d...\n", pin);
        ledcWrite(pin, 255);
        delay(200);
      }

      ledcWrite(pin, pwmVal);
      preferences.putInt(prefKey.c_str(), pwmVal);

      String response = "ON,SPEED=" + String(speed);
      client.publish(stateTopic.c_str(), response.c_str(), true);
      Serial.printf("-> Fan Pin %d turned ON at speed level: %d (PWM: %d)\n",
                    pin, speed, pwmVal);
    }
  }
}

// Hàm xử lý gói tin MQTT nhận được từ App
void callback(char *topic, byte *payload, unsigned int length) {
  String message = "";
  for (unsigned int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  String topicStr = String(topic);
  Serial.printf("MQTT Message arrived [%s]: %s\n", topic, message.c_str());

  // 1. ĐIỀU KHIỂN ĐÈN
  handleLight(topicStr, TOPIC_LIGHT_LIVING_SET, TOPIC_LIGHT_LIVING_STATE,
              PIN_LIGHT_LIVING, "lt_living", message);
  handleLight(topicStr, TOPIC_LIGHT_BED_SET, TOPIC_LIGHT_BED_STATE,
              PIN_LIGHT_BED, "lt_bed", message);
  handleLight(topicStr, TOPIC_LIGHT_KITCHEN_SET, TOPIC_LIGHT_KITCHEN_STATE,
              PIN_LIGHT_KITCHEN, "lt_kitchen", message);

  // 2. ĐIỀU KHIỂN QUẠT
  handleFan(topicStr, TOPIC_FAN_LIVING_SET, TOPIC_FAN_LIVING_STATE,
            PIN_FAN_LIVING, "fan_living", message);
  handleFan(topicStr, TOPIC_FAN_BED_SET, TOPIC_FAN_BED_STATE, PIN_FAN_BED,
            "fan_bed", message);

  // 3. ĐIỀU KHIỂN CỬA/ĐỘNG CƠ SERVO
  if (topicStr.equals(TOPIC_DOOR_SET)) {
    if (message.equals("OPEN")) {
      mainDoorServo.write(90); // Mở cửa (Góc 90 độ)
      preferences.putInt("door_open", 1);
      client.publish(TOPIC_DOOR_STATE, "OPEN", true);
      Serial.println("-> Door OPENED");
    } else if (message.equals("CLOSE")) {
      mainDoorServo.write(0); // Đóng cửa (Về lại góc 0 độ)
      preferences.putInt("door_open", 0);
      client.publish(TOPIC_DOOR_STATE, "CLOSE", true);
      Serial.println("-> Door CLOSED");
    }
  }
}

// Hàm kết nối lại với MQTT Broker khi mất kết nối
void reconnect() {
  while (!client.connected()) {
    Serial.printf("[MQTT] Attempting connection to Broker: %s:%d\n",
                  mqtt_server, mqtt_port);

    String clientId = "ESP32S3_SmartHome_Client_" + String(random(0xffff), HEX);

    if (client.connect(clientId.c_str())) {
      Serial.println("[MQTT] Connected successfully to Broker!");

      // Đăng ký (Subscribe) nhận lệnh điều khiển thiết bị
      client.subscribe(TOPIC_LIGHT_LIVING_SET);
      client.subscribe(TOPIC_LIGHT_BED_SET);
      client.subscribe(TOPIC_LIGHT_KITCHEN_SET);
      client.subscribe(TOPIC_FAN_LIVING_SET);
      client.subscribe(TOPIC_FAN_BED_SET);
      client.subscribe(TOPIC_DOOR_SET);

      // Đọc trạng thái hiện tại từ Preferences để gửi lên MQTT đồng bộ trạng
      // thái thực tế
      int lightLivingVal = preferences.getInt("lt_living", 0);
      int lightBedVal = preferences.getInt("lt_bed", 0);
      int lightKitchenVal = preferences.getInt("lt_kitchen", 0);
      int fanLivingVal = preferences.getInt("fan_living", 0);
      int fanBedVal = preferences.getInt("fan_bed", 0);
      int doorOpen = preferences.getInt("door_open", 0);

      client.publish(
          TOPIC_LIGHT_LIVING_STATE,
          lightLivingVal > 0
              ? ("ON,BRIGHTNESS=" + String(map(lightLivingVal, 0, 255, 0, 100)))
                    .c_str()
              : "OFF",
          true);
      client.publish(
          TOPIC_LIGHT_BED_STATE,
          lightBedVal > 0
              ? ("ON,BRIGHTNESS=" + String(map(lightBedVal, 0, 255, 0, 100)))
                    .c_str()
              : "OFF",
          true);
      client.publish(TOPIC_LIGHT_KITCHEN_STATE,
                     lightKitchenVal > 0
                         ? ("ON,BRIGHTNESS=" +
                            String(map(lightKitchenVal, 0, 255, 0, 100)))
                               .c_str()
                         : "OFF",
                     true);

      // Phát trạng thái quạt dựa trên giá trị PWM lưu trữ
      String fanLivingPayload = "OFF";
      if (fanLivingVal > 0) {
        int speed = 1;
        if (fanLivingVal >= 225)
          speed = 3;
        else if (fanLivingVal >= 165)
          speed = 2;
        fanLivingPayload = "ON,SPEED=" + String(speed);
      }
      client.publish(TOPIC_FAN_LIVING_STATE, fanLivingPayload.c_str(), true);

      String fanBedPayload = "OFF";
      if (fanBedVal > 0) {
        int speed = 1;
        if (fanBedVal >= 225)
          speed = 3;
        else if (fanBedVal >= 165)
          speed = 2;
        fanBedPayload = "ON,SPEED=" + String(speed);
      }
      client.publish(TOPIC_FAN_BED_STATE, fanBedPayload.c_str(), true);

      client.publish(TOPIC_DOOR_STATE, doorOpen == 1 ? "OPEN" : "CLOSE", true);

      Serial.println("[MQTT] Current hardware states published & Subscribed to "
                     "command topics.");
    } else {
      int state = client.state();
      Serial.printf("[MQTT] Connection failed, State Code: %d\n", state);
      Serial.println("[MQTT] Retrying in 5 seconds...");
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(115200);

  // Cấu hình chân GPIO làm đầu ra
  pinMode(PIN_LIGHT_LIVING, OUTPUT);
  pinMode(PIN_LIGHT_BED, OUTPUT);
  pinMode(PIN_LIGHT_KITCHEN, OUTPUT);
  pinMode(PIN_FAN_LIVING, OUTPUT);
  pinMode(PIN_FAN_BED, OUTPUT);

  // Cấu hình LEDC (PWM) cho ESP32-S3
  // ledcAttachChannel(Pin, Tần số, Độ phân giải, Kênh)
  ledcAttachChannel(PIN_LIGHT_LIVING, 5000, 8, 1);
  ledcAttachChannel(PIN_LIGHT_BED, 5000, 8, 2);
  ledcAttachChannel(PIN_LIGHT_KITCHEN, 5000, 8, 3);
  ledcAttachChannel(PIN_FAN_LIVING, 5000, 8, 4);
  ledcAttachChannel(PIN_FAN_BED, 5000, 8, 5);

  // Khởi chạy Preferences lưu trạng thái
  preferences.begin("smarthome", false);

  // Đọc các giá trị đã lưu, nếu chưa có thì dùng mặc định
  int lightLivingVal = preferences.getInt("lt_living", 0);
  int lightBedVal = preferences.getInt("lt_bed", 0);
  int lightKitchenVal = preferences.getInt("lt_kitchen", 0);
  int fanLivingVal = preferences.getInt("fan_living", 0);
  int fanBedVal = preferences.getInt("fan_bed", 0);
  int doorOpen = preferences.getInt("door_open", 0);

  // Khôi phục trạng thái vật lý kèm cơ chế Kickstart để quạt không bị kẹt khi
  // khởi động từ trạng thái nguội (cold boot)
  if (fanLivingVal > 0) {
    ledcWrite(PIN_FAN_LIVING, 255);
  }
  if (fanBedVal > 0) {
    ledcWrite(PIN_FAN_BED, 255);
  }

  if (fanLivingVal > 0 || fanBedVal > 0) {
    delay(200); // Cấp điện áp tối đa 200ms để trục động cơ thắng ma sát quay
                // khởi hành
  }

  // Khôi phục trạng thái xuất ra chân vật lý thực tế
  ledcWrite(PIN_LIGHT_LIVING, lightLivingVal);
  ledcWrite(PIN_LIGHT_BED, lightBedVal);
  ledcWrite(PIN_LIGHT_KITCHEN, lightKitchenVal);
  ledcWrite(PIN_FAN_LIVING, fanLivingVal);
  ledcWrite(PIN_FAN_BED, fanBedVal);

  // Cài đặt chân điều khiển Servo cửa chính
  mainDoorServo.attach(PIN_DOOR_MAIN);
  if (doorOpen == 1) {
    mainDoorServo.write(90);
  } else {
    mainDoorServo.write(0);
  }

  // Kết nối Wi-Fi và cài đặt MQTT
  setup_wifi();
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
}
