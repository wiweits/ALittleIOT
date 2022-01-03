#include <Arduino.h>
#include <U8g2lib.h>
#include <ArduinoJson.h>
#include <EduIntro.h>
#include <BH1750.h>
#include <Wire.h>

#define LED 7
#define BEE 8
#define BEE_BTN 11     //报警开关
#define LED_BTN 10      //灯光开关
#define DHT11_6 6       //温湿度传感器
#define ESP_IO0 4
#define ESP_IO2 3
#define MQ135   5
       
//#ifdef U8X8_HAVE_HW_SPI
//#include <SPI.h>
//#endif
//#ifdef U8X8_HAVE_HW_I2C
//#include <Wire.h>
//#endif

StaticJsonDocument<200> doc;
StaticJsonDocument<100> rsv;
String comdata = "";
String temp = "";
String humi = "";
String lux = "";
bool MQTT_Connected = false;
bool SND_Flag = false;
String bee_flag = "off";
String led_flag = "off";
bool out_flag = true;
unsigned int MQ135_delay = 100;

DHT11 dht11(DHT11_6);  // creating the object sensor on pin 'D6'

BH1750 lightMeter;


/*
  U8g2lib Example Overview:
    Frame Buffer Examples: clearBuffer/sendBuffer. Fast, but may not work with all Arduino boards because of RAM consumption
    Page Buffer Examples: firstPage/nextPage. Less RAM usage, should work with all Arduino boards.
    U8x8 Text Only Example: No RAM usage, direct communication with display controller. No graphics, 8x8 Text only.
    
  This is a page buffer example.    
*/

// Please UNCOMMENT one of the contructor lines below
// U8g2 Contructor List (Picture Loop Page Buffer)
// The complete list is available here: https://github.com/olikraus/u8g2/wiki/u8g2setupcpp
// Please update the pin numbers according to your setup. Use U8X8_PIN_NONE if the reset pin is not connected
//U8G2_NULL u8g2(U8G2_R0);  // null device, a 8x8 pixel display which does nothing
////U8G2_SSD1306_128X64_NONAME_1_4W_SW_SPI u8g2(U8G2_R0, /* clock=*/ 13, /* data=*/ 11, /* cs=*/ 10, /* dc=*/ 9, /* reset=*/ 8);
//U8G2_SSD1306_128X64_NONAME_1_4W_HW_SPI u8g2(U8G2_R0, /* cs=*/ 12, /* dc=*/ 4, /* reset=*/ 6); // Arduboy (Production, Kickstarter Edition)
//U8G2_SSD1306_128X64_NONAME_1_4W_HW_SPI u8g2(U8G2_R0, /* cs=*/ 10, /* dc=*/ 9, /* reset=*/ 8);
//U8G2_SSD1306_128X64_NONAME_1_3W_SW_SPI u8g2(U8G2_R0, /* clock=*/ 13, /* data=*/ 11, /* cs=*/ 10, /* reset=*/ 8);
U8G2_SSD1306_128X64_NONAME_1_HW_I2C u8g2(U8G2_R0, /* reset=*/ U8X8_PIN_NONE);
//U8G2_SSD1306_128X64_ALT0_1_HW_I2C u8g2(U8G2_R0, /* reset=*/ U8X8_PIN_NONE);   // same as the NONAME variant, but may solve the "every 2nd line skipped" 




// End of constructor list


void setup(void) {
  //初始化串口通信
  Serial.begin(9600);
  //初始化oled模块
  u8g2.begin();
  u8g2.enableUTF8Print();   // enable UTF8 support for the Arduino print() function
  
  //初始化光照传感器模块
  Wire.begin();
  // On esp8266 you can select SCL and SDA pins using Wire.begin(D4, D3);
  lightMeter.begin();

  //初始化功能引脚
  pinMode(ESP_IO2,INPUT);
  pinMode(ESP_IO0,INPUT);
  pinMode(MQ135,INPUT);
  pinMode(LED,OUTPUT);
  pinMode(BEE,OUTPUT);
  pinMode(LED_BTN,INPUT);
  pinMode(BEE_BTN,INPUT);
  //初始化输出
  digitalWrite(LED,HIGH);
  digitalWrite(BEE,HIGH);
}

void loop(void) {
  getData();
  showData();
  sndData();
  
  scan();
  getSerial();
  output();
}

void showData(){
  String temp1 = " T : "+temp+" C";
  String humi1 = " H : "+humi+" %RH";
  String lux1 = " L : "+lux+" LX";
  u8g2.firstPage();
  do {
    u8g2.setFont(u8g2_font_ncenB14_tr);
    //显示温度
    u8g2.drawStr(0,32,temp1.c_str());
    //显示湿度
    u8g2.drawStr(0,48,humi1.c_str());
    //显示光强
    u8g2.drawStr(0,64,lux1.c_str());
    //显示时间
    //u8g2.drawStr(0,16,TD.c_str());
    //显示MQTT连接状态
    if(MQTT_Connected) u8g2.drawStr(0,16,"mqtt online!");
    else u8g2.drawStr(0,16,"mqtt offline!");
  } while ( u8g2.nextPage() );
  delay(100);
  //Serial.println(TD);
}

void output(){
  if(out_flag == true){
    if(led_flag == "on") digitalWrite(LED,LOW); //点亮led
    if(led_flag == "off") digitalWrite(LED,HIGH);//熄灭led
    if(bee_flag == "on") digitalWrite(BEE,LOW); //开启报警
    if(bee_flag == "off") digitalWrite(BEE,HIGH);//关闭报警  
    out_flag = false;
  }
}

void getSerial(){
    while (Serial.available()>0)    
    {
        comdata += (char)Serial.read();
        delay(2);
    }
    if (comdata != "")
    {   
        //Serial.println(comdata);
        deserializeJson(rsv, comdata);
        if(rsv["led_flag"] == "on") led_flag = "on";
        if(rsv["led_flag"] == "off") led_flag = "off";
        if(rsv["bee_flag"] == "on") bee_flag = "on";
        if(rsv["bee_flag"] == "off") bee_flag = "off";
        out_flag = true;
        comdata = "";
    }
}

void getData(){
    int var = (int)lightMeter.readLightLevel();
    if ( var >= 1000 ){
      //报警光照过强
      lux = "---";
    } else lux = (String)var;
    dht11.update();
    temp = (String)dht11.readCelsius();       // Reading the temperature in Celsius degrees and store in the C variable
    humi = (String)dht11.readHumidity();     // Reading the humidity index    
    delay(100);
}


void sndData(){
    if((MQTT_Connected == true) && (SND_Flag == true)){
      doc["temp"] = temp;
      doc["humi"] = humi;
      doc["sun"] = lux;
      serializeJsonPretty(doc,Serial);
      delay(200);
      //esp8266在接收到数据后进行解包，将变化的数据发送到服务器，没有变化的数据截取不发送
      //这样可以减小对网络的占用以便在网络不好的时候保持正常的数据收发
    }   
}

void scan(){
  if(digitalRead(ESP_IO2) && (MQTT_Connected == false)) MQTT_Connected = true;
  if((!digitalRead(ESP_IO2)) && (MQTT_Connected == true)) MQTT_Connected = false;
  if(digitalRead(ESP_IO0) && (SND_Flag == false)) SND_Flag = true;
  if((!digitalRead(ESP_IO0)) && (SND_Flag == true)) SND_Flag = false;

  //灯光和报警按键
  if((!digitalRead(LED_BTN))||(!digitalRead(BEE_BTN))){
    //延时消抖
    delay(10);
    if(!digitalRead(LED_BTN)){
      if(led_flag == "on") led_flag = "off";
      else led_flag = "on";
      while(!digitalRead(LED_BTN));
    }
    if(!digitalRead(BEE_BTN)){
      if(bee_flag == "on") bee_flag = "off";
      else bee_flag = "on";
      while(!digitalRead(BEE_BTN));
    }
    out_flag = true;
//    rsv["bee_flag"] = bee_flag;
//    rsv["led_flag"] = led_flag;
//    serializeJsonPretty(rsv,Serial);
//    delay(100);
  }
  //有害气体传感器
    if(!digitalRead(MQ135) && (bee_flag == "off") && (MQ135_delay == 0)){
      bee_flag = "on";
      out_flag = true;
    } 
    if(MQ135_delay != 0)
      MQ135_delay--;
}
