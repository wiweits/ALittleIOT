### 0.项目简介

​		一个基于物联网的智能家居环境检测系统，该系统可以对家居环境（LED灯、室内温湿度、有害气体、光照感应、报警等）进行状态的监测和设置，以及必要的操作和反馈。系统分为三个主体。一是设备端部分：以Arduino Pro Mini为主控芯片，配置温湿度传感器实时测量温湿度，配置有害气体、光照传感器检测室内环境的变化并通过配置蜂鸣器进行报警反馈，配置报警器和LED灯实现通过物理开关控制，再加上以ESP-01S作为WIFI模块实现与云端设备网络连接，数据传输，指令下发等。二是云端部分：通过一款在线开源的物联网MQTT服务器实现硬件控制端与APP端的数据互联。三是APP端部分：在Android Studio集成开发环境下开发APP端的界面及功能。最后，通过PC端工具连接云端物联网MQTT服务器对虚拟设备和真实设备进行测试，验证系统的可靠性和功能性。

### 1.项目成品

#### 1.1安卓APP

![image-20220103163933408](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103163933408.png)

#### 1.2硬件电路

![image-20220103164052927](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103164052927.png)

#### 1.3测试结果

![image-20220103164155924](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103164155924.png)

### 2.技术简介

#### 2.1MQTT物联网通信云服务器平台

​	EMQ X MQTT 物联网云服务提供了一个在线的物联网MQTT 5.0服务器，可以将它们用于 MQTT 学习、测试或原型制作，服务端接入信息如下图所示。

![image-20220103165344200](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103165344200.png)

​	通过客户端工具可以向服务器订阅主题，发布或接受消息，在控制端和APP端软件开发的过程中调试相应主题下数据接受和发送的情况。

![image-20220103165655576](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103165655576.png)

#### 2.2安卓手机APP开发平台

​	Android Studio 是谷歌推出的一个Android集成开发工具，基于IntelliJ IDEA类似 Eclipse ADT，Android Studio 提供了集成的 Android 开发工具用于开发和调试，不仅可以使用虚拟安卓系统调试程序，还可以连接安卓手机进行实机调试，开发工具界面如下图所示。

![image-20220103165901231](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103165901231.png)

#### 2.3Arduino开源硬件开发平台

​	Arduino是一款便捷灵活、方便上手的开源电子原型平台。包含硬件（各种型号的Arduino板）和软件（ArduinoIDE）。由一个欧洲开发团队于2005年冬季开发。

![img](https://www.arduino.cn/data/attachment/addon_download/image/201701/09/203511k38cclpwaxgfc1h8.jpg)

### 3.硬件模型制作

#### 3.1绘制硬件原理图

​	硬件电路采用模块化设计，除按键、灯光和电源开关电路以外，其余各部分均使用独立模块实现，在很大程度上降低了电路设计的复杂度，提高模型的制作效率，电路以万能板为底板双面布线，原理图元件库均来自立创EDA。

![image-20220103171119435](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103171119435.png)

#### 3.2万能板布线

​	万能板电路布线图的绘制采用了LochMaster专业洞洞板布线工具，手工布线存在元件排布不合理，布线条理性不够，层次杂乱，元件密度低，占用面积大等问题，对比之下LochMaster软件具有许多优点。

![image-20220103171430050](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103171430050.png)

![image-20220103171440319](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103171440319.png)

### 4.软件模型设计

#### 4.1WIFI模块软件流程

​	该模块通过串口接收控制端检测的环境数据，并将数据打包成MQTT协议下传输的消息发送到物联网服务器，当接收到由APP端发来的消息时，对其进行解包并通过串口传回到控制端，该模块的软件流程如下图所示。

![image-20220103172615469](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103172615469.png)

#### 4.2主控模块软件流程

​	控制端驱动各传感器实时检测数据并显示在OLED屏上，同时通过串口发送给WIFI模块，当系统接收到来自APP 端的灯光及报警的控制指令时，执行相应的操作，灯光及报警的控制也可以通过物理按键来实现，模拟现实生活中的灯光开关，控制端的软件流程如下图所示。

![image-20220103172817622](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103172817622.png)

#### 4.3安卓手机APP开发

​	APP端不仅要实现与控制端的数据互联功能，还要可以通过互联网天气接口获取当地的天气数据进行展示，该APP是在Android Studio开发环境下连接安卓手机借助调试工具进行程序开发和调试，安卓APP的开发流程如下图所示。

![image-20220103173038643](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103173038643.png)

### 5.系统联机调试

#### 5.1硬件功能测试

​	将传感器实时检测到的温度、湿度、光照强度等数据以及MQTT服务器的连接状态显示在OLED屏幕上，当环境中其他气体超过设定检测浓度时，蜂鸣器报警，直到按下报警解除按钮系统停止报警。

​	按下灯光按键开启灯光，再次按下则关闭灯光，按下报警按键开启报警，再次按下则关闭报警，确保在按键按下过程中没有因为抖动而产生异常操作，且按下按键的时间不能太短否则没有响应。

![image-20220103173559260](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103173559260.png)

#### 5.2在线MQTT服务器调试

​	通过MQTT X客户端工具连接到在线MQTT服务器，分别订阅该系统的消息接收和发送的Topic，查看消息的发送和接受情况是否符合系统的设定，经过PC端模拟控制端和APP端的消息传递，调试成功该系统的数据互联功能。

![image-20220103173652190](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103173652190.png)

#### 5.3控制端与APP端数据互联功能调试

​	控制端上显示的数据同步到APP端显示，且可以通过APP端的灯光和报警开关下发控制指令，完成打开或关闭控制端灯光和报警的操作，实现两端数据互联，同时APP端可以通过点击天气信息展示窗格刷新天气数据。

![image-20220103173821422](C:\Users\张维\AppData\Roaming\Typora\typora-user-images\image-20220103173821422.png)

### 6.附录——元件模块清单

| ID   | Name                | Designator          | Footprint                  | Quantity |
| ---- | ------------------- | ------------------- | -------------------------- | -------- |
| 1    | DC005-2.0MM         | DC1                 | DC-IN-TH_DC005             | 1        |
| 2    | K2-3.6*6.1_SMD      | KEY1,KEY2           | KEY-SMD_2P-L6.2-W3.6-LS8.0 | 2        |
| 3    | 11-21/G6C-FN2P2B/2T | LED1,LED2,LED3,LED4 | LED1206-R-RD               | 4        |
| 4    | ESP-01S             | MK1                 | ESP-01S                    | 1        |
| 5    | 100R                | R1,R2,R3,R4,R5,R6   | R0603                      | 6        |
| 6    | DHT11               | S1                  | DHT11                      | 1        |
| 7    | K3-1235S-L1         | SW1                 | SW-TH_K3-1235S-L1          | 1        |
| 8    | ARDUINO PRO  MINI   | U1                  | ARDUINO PRO MINI           | 1        |
| 9    | OLED                | U3                  | IIC OLED 128*64            | 1        |
| 10   | AMS1117             | U4                  | AMS1117                    | 1        |
| 11   | MQ-135              | U5                  | MQ-135_NEW                 | 1        |
| 12   | BH1750光照度传感器  | U6                  | BH1750(GY-302)             | 1        |
| 13   | 蜂鸣器              | U7                  | 蜂鸣器模块                 | 1        |