#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

const char *ssid = "Album"; // Enter your WiFi name
const char *password = "zwheaing";  // Enter WiFi password
const char *mqtt_broker = "broker.emqx.io";
const int mqtt_port = 1883;
String led_flag;    //led状态标志
String bee_flag;    //蜂鸣器状态标志
String temp;
String humi;
String sun;


String comdata = "";
const char *data;

StaticJsonDocument<200> snd;//发送消息的json数据对象
StaticJsonDocument<200> rsv;//接受消息的json数据对象

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
    pinMode(2, OUTPUT);
    pinMode(0, OUTPUT);
    digitalWrite(2,LOW);
    digitalWrite(0,LOW);
    // Set software serial baud to 115200;
    Serial.begin(9600);
    // connecting to a WiFi network
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        //Serial.println("Connecting to WiFi..");
    }
    //Serial.println("Connected to the WiFi network");
    //connecting to a mqtt broker
    client.setServer(mqtt_broker, mqtt_port);
    client.setCallback(callback);
    while (!client.connected()) {
        //Serial.println("Connecting to public emqx mqtt broker.....");
        if (client.connect("esp8266-client")) {
            //Serial.println("Public emqx mqtt broker connected");
            digitalWrite(2,HIGH);
        } else {
            //Serial.print("failed with state ");
            //Serial.print(client.state());
            delay(2000);
        }
    }
    // publish and subscribe
    client.publish("esp8266/test", "hello emqx");
    client.subscribe("esp8266/test/snd");

    
}


void callback(char *topic, byte *payload, unsigned int length) {
    //打印接受到的消息
//    Serial.print("Message arrived in topic: ");
//    Serial.println(topic);
//    Serial.print("Message:");
//    for (int i = 0; i < length; i++) {
//        Serial.print((char) payload[i]);
//    }
//    Serial.println();
//    Serial.println("-----------------------");
    //处理接受到的消息
    //接收上位机的消息，判断是否为请求消息，是则输出请求信号，否则通过串口发送给主控芯片
    deserializeJson(rsv, payload);
    if (rsv["forFlag"] == "on") digitalWrite(0,HIGH);
    //else if (rsv["forFlag"] == "off") digitalWrite(0,LOW);
    else serializeJson(rsv, Serial);
    //rsv_pcs(payload);


}

void loop() {
    client.loop();
    //Serial.available() 的意思是：返回串口缓冲区中当前剩余的字符个数
    //Serial.read() 函数读取缓冲区中的一个Byte
    //当串口中存在字符数据时，将字符读入本地字符串变量
    while (Serial.available()>0)    
    {
        comdata += (char)Serial.read();
        delay(2);
    }
    if (comdata != "")
    {   
        //拉低数据请求引脚，等待响应下一次请求
        digitalWrite(0,LOW);
        //Serial.println(comdata);
        //由于publish函数只能传输字符指针，c_str()函数刚好返回字符串的指针
        client.publish("esp8266/test/rsv", comdata.c_str());
        //清空字符串变量，进行下一次传输
        comdata = "";
        
    }
    //如果连接丢失，自动重连
    if (!client.connected()) {
        digitalWrite(2,LOW);
        //Serial.println("Connecting to public emqx mqtt broker.....");
        if (client.connect("esp8266-client")){
            //Serial.println("Public emqx mqtt broker connected");
            client.subscribe("esp8266/test/snd");
            digitalWrite(2,HIGH);
        }   
    }
    //通过IO2告诉主控芯片MQTT服务器的连接状态
    //'''通过IO2告诉主控芯片是否向上位机传输数据
}
