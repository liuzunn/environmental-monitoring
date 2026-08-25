package org.nep.nepsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT 硬件接入预留配置（当前版本以 HTTP 上报为主）。
 * 后续接入真实硬件（ESP32/Arduino）时：
 *   1. 在 pom.xml 添加 spring-integration-mqtt 依赖；
 *   2. 实现 MqttConfig（入站通道订阅 env/+/data，解析后调用 MonitorDataService.report）；
 *   3. 硬件向 MQTT Broker 的 env/{deviceCode}/data 主题发布与 /api/data/report 相同结构的 JSON。
 * 配置示例：
 *   mqtt.enabled=false
 *   mqtt.broker=tcp://localhost:1883
 *   mqtt.client-id=nepsystem
 *   mqtt.topic-prefix=env
 *   mqtt.username=
 *   mqtt.password=
 */
@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    /** 是否启用 MQTT 接入（默认 false，不依赖 MQTT Broker） */
    private boolean enabled = false;
    /** Broker 地址 */
    private String broker = "tcp://localhost:1883";
    /** 客户端 ID */
    private String clientId = "nepsystem";
    /** 主题前缀：env/{deviceCode}/data */
    private String topicPrefix = "env";
    private String username = "";
    private String password = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBroker() { return broker; }
    public void setBroker(String broker) { this.broker = broker; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getTopicPrefix() { return topicPrefix; }
    public void setTopicPrefix(String topicPrefix) { this.topicPrefix = topicPrefix; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
