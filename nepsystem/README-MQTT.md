# MQTT / 硬件接入说明（环境监测保护系统）

> 当前版本：**HTTP 上报已可用**（模拟器与真实硬件共用 `POST /api/data/report`），
> MQTT 通道已预留配置（`config/MqttProperties.java`），按下方步骤启用。

## 一、硬件接入方式（二选一）

### 方式 A：HTTP 直连上报（推荐，立即可用）

设备（ESP32/Arduino/树莓派）直接向后端发送：

```http
POST http://<服务器IP>:8080/api/data/report
Content-Type: application/json

{
  "deviceCode": "DEV-AIR-001",
  "items": [
    { "sensorCode": "TEMP", "value": 26.5 },
    { "sensorCode": "PM25", "value": 38.2 },
    { "sensorCode": "CO2",  "value": 620 }
  ],
  "reportTime": "2026-08-25 10:00:00"   // 可省略，省略取服务器当前时间
}
```

响应：`{"code":200,"message":"上报成功","data":null}` —— 上报即自动触发阈值告警。

设备编号必须在 `devices` 表中存在（device_code 唯一）。指标编码见 `sensors` 字典表。

### 方式 B：MQTT 接入（预留）

1. 后端 pom.xml 增加依赖：

```xml
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-mqtt</artifactId>
</dependency>
```

2. `application.properties` 启用并配置：

```properties
mqtt.enabled=true
mqtt.broker=tcp://<broker-ip>:1883
mqtt.client-id=nepsystem
mqtt.topic-prefix=env
mqtt.username=xxx
mqtt.password=xxx
```

3. 实现 `mqtt/MqttConfig`：
   - 出站：`MqttPahoClientFactory` + `@ServiceActivator(inputChannel="mqttOutboundChannel")`
   - 入站：订阅 `env/+/data`，收到 JSON 后调用 `MonitorDataService.report()`（与 HTTP 上报同一事务逻辑）

4. 硬件端（ESP32 示例，ArduinoJson 库）：

```cpp
// 伪代码：向主题 env/DEV-AIR-001/data 发布
char payload[256];
snprintf(payload, sizeof(payload),
  "{\"deviceCode\":\"DEV-AIR-001\",\"items\":["
  "{\"sensorCode\":\"TEMP\",\"value\":%.1f},"
  "{\"sensorCode\":\"PM25\",\"value\":%.1f}]}",
  temp, pm25);
client.publish("env/DEV-AIR-001/data", payload);
```

## 二、指标编码约定（sensors 表）

| sensor_code | 名称 | 单位 | 适用设备 |
|---|---|---|---|
| TEMP | 温度 | ℃ | 通用 |
| HUMI | 湿度 | % | 通用 |
| PM25 | PM2.5 | ug/m3 | AIR |
| CO2 | 二氧化碳 | ppm | AIR |
| PH | 酸碱度 | pH | WATER |
| TURBIDITY | 浊度 | NTU | WATER |
| DO | 溶解氧 | mg/L | WATER |
| NOISE | 噪声 | dB | NOISE |

## 三、告警规则

- 阈值配置在 `thresholds` 表（device_id 为空 = 全局默认，设备级优先）。
- 上报值超过 warn 区间 → 预警 WARN；超过 alarm 区间 → 报警 ALARM。
- 同设备同指标同级别 30 分钟内不重复告警。
- 当前内置：PM25 预警 75 / 报警 150；NOISE 预警 65 / 报警 75。
