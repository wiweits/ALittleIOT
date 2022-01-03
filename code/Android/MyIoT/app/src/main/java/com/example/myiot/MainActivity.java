package com.example.myiot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.google.gson.Gson;
import com.qweather.sdk.bean.IndicesBean;
import com.qweather.sdk.bean.air.AirNowBean;
import com.qweather.sdk.bean.base.Code;
import com.qweather.sdk.bean.base.IndicesType;
import com.qweather.sdk.bean.base.Lang;
import com.qweather.sdk.bean.base.Range;
import com.qweather.sdk.bean.base.Unit;
import com.qweather.sdk.bean.geo.GeoBean;
import com.qweather.sdk.bean.weather.WeatherNowBean;
import com.qweather.sdk.view.HeConfig;
import com.qweather.sdk.view.QWeather;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String HOST = "tcp://broker.emqx.io:1883";
    private static final String MQTT_ID = "Android_IoT";
    private static final String MQTT_USER_NAME = "";
    private static final String MQTT_PASSWORD = "";
    private static final String MYTAG = "TestLog";
    private static final String MQTT_RSV = "esp8266/test/rsv";  //接收（订阅）
    private static final String MQTT_SND = "esp8266/test/snd";  //发送

    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private MqttConnectOptions options;
    private TextView tvShow;
    private String PublicId = "HE2010222016581593";
    private String AppKey = "2aca14305f4847afb521fb2b76bf14c0";
    private LocationManager locationManager;
    private String locationProvider;

    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    //private String Latiitude;       //纬度
    //private String Longitude;       //经度
    private String mLocation;
    private View updataWeather;
    private TextView tvWeather;
    private TextView tvAddress;
    private TextView tvAdvices;
    private TextView tvAir;
    private TextView tvAirIndex;
    private Switch stLight;
    private Switch stBee;
    private String led_flag = "off";
    private String bee_flag = "off";
    private TextView sunData;
    private TextView tempData;
    private TextView humiData;
    private String Weadther;
    private String Advices;
    private String City;
    private String Area;
    private String Air;
    private String AirIndex;

    private String time0;
    private SharedPreferences sharedPreferences;
    //    private TextView tvHumi;
//    private TextView tvTemp;
//    private TextView tvSun;
    //    private String weadther;
//    private String advices;
//    private String City;
//    private String Area;
//    private String Air;
//    private String AirIndex;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化控件
        initUI();
        //初始化mqtt连接
        initMQTT();
        //连接mqtt服务器
        connectToMQTT();
        //初始化天气api
        initWeather();
        
        //缺陷：当mqtt连接丢失后无法自动重连，需重启软件才能重连
    }

    private void initWeather() {
        //天气api账户初始化
        HeConfig.init(PublicId,AppKey);
        //切换至开发版服务
        HeConfig.switchToDevService();

        //获取之前的时间
        sharedPreferences = getSharedPreferences("data", Context .MODE_PRIVATE);
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");// HH:mm:ss
        //获取当前时间
        Date date = new Date(System.currentTimeMillis());
        //获取当前系统时间
        time0 = simpleDateFormat.format(date);
        Log.i(MYTAG,time0);
        if (time0 != sharedPreferences.getString("time","")) {
            //初始化高德获取经纬度的API
            initAmap();
        }
    }

    private void initAmap() {
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);

        //声明AMapLocationClientOption对象
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(true);

        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);

        //设置是否允许模拟位置,默认为true，允许模拟位置
        mLocationOption.setMockEnable(true);
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(20000);
        //关闭缓存机制
        mLocationOption.setLocationCacheEnable(false);

        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }


    //声明定位回调监听器,如果获取到了经纬度，则回调该监听函数
    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                    //可在其中解析amapLocation获取相应内容。
                    java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");
                    String Latiitude = df.format(aMapLocation.getLatitude());
                    String Longitude = df.format(aMapLocation.getLongitude());
                    Bundle bundle = new Bundle();
                    bundle.putString("Location",Longitude+","+Latiitude);
                    Message message = new Message();
                    message.what = 2;
                    message.setData(bundle);
                    mhandler.sendMessage(message);
                    //Latiitude = aMapLocation.getLatitude();//获取纬度
                    //Longitude = aMapLocation.getLongitude();//获取经度
                    Log.i("MainActivity","纬度："+Latiitude+"，经度："+Longitude);
                }else {
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError","location Error, ErrCode:"
                            + aMapLocation.getErrorCode() + ", errInfo:"
                            + aMapLocation.getErrorInfo());
                }
            }
        }
    };



    private void getWeather(String mLocation) {
        /**
         * 实况天气数据
         * @param location 所查询的地区，可通过该地区名称、ID、IP和经纬度进行查询经纬度格式：纬度,经度
         *                 （英文,分隔，十进制格式，北纬东经为正，南纬西经为负)
         * @param lang     (选填)多语言，可以不使用该参数，默认为简体中文
         * @param unit     (选填)单位选择，公制（m）或英制（i），默认为公制单位
         * @param listener 网络访问结果回调
         */
        //实况天气数据
        QWeather.getWeatherNow(MainActivity.this, mLocation, Lang.ZH_HANS, Unit.METRIC, new QWeather.OnResultWeatherNowListener() {
            @Override
            public void onError(Throwable e) {
                Log.i(MYTAG, "getWeather onError: " + e);
            }

            @Override
            public void onSuccess(WeatherNowBean weatherBean) {
                Log.i(MYTAG, "getWeather onSuccess: " + new Gson().toJson(weatherBean));
                //先判断返回的status是否正确，当status正确时获取数据，若status不正确，可查看status对应的Code值找到原因
                if (Code.OK.getCode().equalsIgnoreCase(weatherBean.getCode())) {
                    WeatherNowBean.NowBaseBean now = weatherBean.getNow();
                    Weadther = now.getText();
                    //将数据发送到主线程
                    Bundle bundle = new Bundle();
                    bundle.putString("Weather", Weadther);
                    Message message = new Message();
                    message.what = 21;
                    message.setData(bundle);
                    mhandler.sendMessage(message);
                    Log.i("weadher", Weadther);
                } else {
                    //在此查看返回数据失败的原因
                    String status = weatherBean.getCode();
                    Code code = Code.toEnum(status);
                    Log.i(MYTAG, "failed code: " + code);
                }
            }
        });

        //生活指数
        QWeather.getIndices1D(getApplicationContext(),mLocation,Lang.ZH_HANS, Collections.singletonList(IndicesType.DRSG), new QWeather.OnResultIndicesListener() {
            @Override
            public void onError(Throwable throwable) {
                Log.i(MYTAG, "getIndices onError: " + throwable);
            }

            @Override
            public void onSuccess(IndicesBean indicesBean) {
                Log.i(MYTAG, "getWeather onSuccess: " + new Gson().toJson(indicesBean));
                //先判断返回的status是否正确，当status正确时获取数据，若status不正确，可查看status对应的Code值找到原因
                if (Code.OK.getCode().equalsIgnoreCase(indicesBean.getCode())) {
                    List<IndicesBean.DailyBean> indices = indicesBean.getDailyList();
                    Advices = indices.get(0).getText();
                    Bundle bundle = new Bundle();
                    bundle.putString("Advices", Advices);
                    Message message = new Message();
                    message.what = 21;
                    message.setData(bundle);
                    mhandler.sendMessage(message);
                    Log.i("advice", Advices);

                    //Log.i(MYTAG,"Indices"+indicesBean.toString());
                } else {
                    //在此查看返回数据失败的原因
                    String status = indicesBean.getCode();
                    Code code = Code.toEnum(status);
                    Log.i(MYTAG, "failed code: " + code);
                }
            }
        });

        //城市信息查询
        QWeather.getGeoCityLookup(getApplicationContext(), mLocation, Range.CN, 1, Lang.ZH_HANS, new QWeather.OnResultGeoListener() {
            @Override
            public void onError(Throwable throwable) {
                Log.i(MYTAG, "getGeoCityLookup onError: " + throwable);
            }

            @Override
            public void onSuccess(GeoBean geoBean) {
                Log.i(MYTAG, "getWeather onSuccess: " + new Gson().toJson(geoBean));
                //先判断返回的status是否正确，当status正确时获取数据，若status不正确，可查看status对应的Code值找到原因
                if (Code.OK.getCode().equalsIgnoreCase(geoBean.getStatus())) {
                    List<GeoBean.LocationBean> city = geoBean.getLocationBean();
                    //城市
                    City = city.get(0).getAdm2();
                    //行政区
                    Area = city.get(0).getName();
                    Bundle bundle = new Bundle();
                    bundle.putString("Address", Area +"-"+ City);
                    Message message = new Message();
                    message.what = 21;
                    message.setData(bundle);
                    mhandler.sendMessage(message);
                    Log.i("city", City + Area);
                    //Log.i(MYTAG,"City"+geoBean.toString());
                } else {
                    //在此查看返回数据失败的原因
                    String status = geoBean.getStatus();
                    Code code = Code.toEnum(status);
                    Log.i(MYTAG, "failed code: " + code);
                }
            }
        });

        //空气质量实况
        QWeather.getAirNow(getApplicationContext(), mLocation, Lang.ZH_HANS, new QWeather.OnResultAirNowListener() {
            @Override
            public void onError(Throwable throwable) {
                Log.i(MYTAG, "getAirNow onError: " + throwable);
            }

            @Override
            public void onSuccess(AirNowBean airNowBean) {
                Log.i(MYTAG, "getWeather onSuccess: " + new Gson().toJson(airNowBean));
                //先判断返回的status是否正确，当status正确时获取数据，若status不正确，可查看status对应的Code值找到原因
                if (Code.OK.getCode().equalsIgnoreCase(airNowBean.getCode())) {
                    AirNowBean.NowBean AirNow = airNowBean.getNow();
                    Air = AirNow.getCategory();
                    AirIndex = AirNow.getAqi();
                    Bundle bundle = new Bundle();
                    bundle.putString("Air", Air);
                    bundle.putString("AirIndex", AirIndex);
                    Message message = new Message();
                    message.what = 21;
                    message.setData(bundle);
                    mhandler.sendMessage(message);
                    Log.i("air", Air + AirIndex);
                    //Log.i(MYTAG,"Air"+airNowBean.toString());
                } else {
                    //在此查看返回数据失败的原因
                    String status = airNowBean.getCode();
                    Code code = Code.toEnum(status);
                    Log.i(MYTAG, "failed code: " + code);
                }
            }
        });

    }


    private void initUI() {
        //传感器显示控件
        sunData = findViewById(R.id.sun_data);
        tempData = findViewById(R.id.temp_data);
        humiData = findViewById(R.id.humi_data);
        //天气显示控件
        tvWeather = findViewById(R.id.tv_weather);
        tvAddress = findViewById(R.id.tv_address);
        tvAdvices = findViewById(R.id.tv_advices);
        tvAir = findViewById(R.id.tv_air);
        tvAirIndex = findViewById(R.id.tv_airIndex);
        //“更新天气”控件，获取经纬度并更新天气数据
        updataWeather = findViewById(R.id.updata_weather);
        updataWeather.setOnClickListener(this);

        //测试用的
        //tvShow = findViewById(R.id.tv_show);

        //开关控件
        stLight = findViewById(R.id.st_light);
        stBee = findViewById(R.id.st_bee);
        //设置开关监听器
        setSwitchListener();

        //显示最近一次获取的数据
        SharedPreferences sharedPreferences= getSharedPreferences("data", Context .MODE_PRIVATE);
        if (sharedPreferences.getString("weather","") != null)
            tvWeather.setText(sharedPreferences.getString("weather",""));
        if (sharedPreferences.getString("air","") != null)
            tvAir.setText("空气质量-"+sharedPreferences.getString("air",""));
        if (sharedPreferences.getString("airIndex","") != null)
            tvAirIndex.setText(sharedPreferences.getString("airIndex",""));
        if (sharedPreferences.getString("advices","") != null)
            tvAdvices.setText(sharedPreferences.getString("advices",""));
        if (sharedPreferences.getString("address","") != null)
            tvAddress.setText(sharedPreferences.getString("address",""));
    }

    private void setSwitchListener() {
        stLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    //选中状态 可以做一些操作
                    led_flag = "on";
                }else {
                    //未选中状态 可以做一些操作
                    led_flag = "off";
                }
                //回传控制状态
                JSONObject flag = new JSONObject();
                try {
                    //添加
                    flag.put("led_flag", led_flag);
                    System.out.println(flag.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mPublish(MQTT_SND,flag.toString());
            }
        });
        stBee.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    //选中状态 可以做一些操作
                    bee_flag = "on";
                }else {
                    //未选中状态 可以做一些操作
                    bee_flag = "off";
                }
                //回传控制状态
                JSONObject flag = new JSONObject();
                try {
                    //添加
                    flag.put("bee_flag", bee_flag);
                    System.out.println(flag.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mPublish(MQTT_SND,flag.toString());
            }
        });

        //aSwitch.setChecked(true);   //设置选中
        //aSwitch.setChecked(false);   //设置取消
    }

    private void updateView(String rsv) {
        try {
            //将消息转换成json对象，获取相应的键值对
            JSONObject jsonObject = new JSONObject(rsv);
            //显示接收到的数据
            //stLight.setChecked(true);
            humiData.setText(jsonObject.getString("humi"));
            tempData.setText(jsonObject.getString("temp"));
            sunData.setText(jsonObject.getString("sun"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    Handler mhandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){

                case 3:     //收到消息回传
                    //通过主题取出消息
                    String rsv = msg.getData().getString(MQTT_RSV);
                    Log.i(MYTAG,"在mhandler中收到的消息是："+"MQTT_RSV"+":"+rsv);
                    updateView(rsv);
                    break;
                case 31:    //连接成功
                    Log.i(MYTAG,"MQTT服务器连接成功");
                    Toast.makeText(getApplicationContext(),"连接成功",Toast.LENGTH_SHORT).show();
                    //发出数据请求
                    forData();
                    //连接成功后订阅主题
                    try {
                        client.subscribe(MQTT_RSV,0);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    Log.i(MYTAG,MQTT_RSV+"---订阅成功");
                    //订阅成功后向下位机发送传输数据的请求
                    break;
                case 2:     //获取到经纬度
                    String mLocation = msg.getData().getString("Location");
                    Log.i(MYTAG,"经纬度是："+mLocation);
                    //获取天气数据
                    getWeather(mLocation);
                    break;
                case 21:
                    //显示天气数据
                    //msg.getData();
                    renewWeather(msg.getData());
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    //新开一个线程定时向下位机发送数据请求
    private void forData() {
        //新建并开启多线程，通过子线程连接
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject forFlag = new JSONObject();
                try {
                    forFlag.put("forFlag","on");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                while (true){
                    mPublish(MQTT_SND,forFlag.toString());
                    try {
                        Thread.sleep(900);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void renewWeather(Bundle data) {
        if (data.getString("Weather") != null)
            tvWeather.setText(data.getString("Weather"));
        if (data.getString("Air") != null)
            tvAir.setText("空气质量-"+data.getString("Air"));
        if (data.getString("AirIndex") != null)
            tvAirIndex.setText(data.getString("AirIndex"));
        if (data.getString("Advices") != null)
            tvAdvices.setText(data.getString("Advices"));
        if (data.getString("Address") != null)
            tvAddress.setText(data.getString("Address"));
    }


    private void initMQTT() {
        try {
            //连接到mqtt服务器，即在mqtt服务器新建一个客户端
            client = new MqttClient(HOST,MQTT_ID,
                                    new MemoryPersistence());
            //新建mqtt连接的配置对象
            options = new MqttConnectOptions();
            //是否清除客户端连接记录，清除后每次都已新的身份连接mqtt服务器
            options.setCleanSession(false);
            //设置连接的用户名和密码，公共mqtt服务器可以不用设置
            //options.setUserName(MQTT_USER_NAME);
            //options.setPassword(MQTT_PASSWORD.toCharArray());
            //设置连接超时时间，单位为秒
            options.setConnectionTimeout(10);
            //设置服务器判断客户端是否在线的时间间隔
            options.setKeepAliveInterval(20);
            //设置回调
            client.setCallback(new MqttCallback() {
                //如果连接丢失，系统回调执行connectionLost()函数
                @Override
                public void connectionLost(Throwable throwable) {
                    Log.i(MYTAG,"connectionLost");

                    //可以在此进行重连
                    startReconnect();
                }
                //收到服务器发出的相应主题"s"的消息后，系统回调messageArrived()函数
                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) {
                    Log.i(MYTAG,"messageArrived中收到的消息是："+s+":"+mqttMessage);
                    //将消息回传mhandler进行统一处理
                    Bundle bundle = new Bundle();
                    //将消息与主题绑定
                    bundle.putString(s,mqttMessage.toString());
                    Message msg = new Message();
                    msg.what = 3;
                    msg.setData(bundle);
                    mhandler.sendMessage(msg);
                }
                //向服务器发送消息后，系统回调deliveryComplete()函数
                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    //打印消息是否发送成功
                    Log.i(MYTAG,"deliveryComplete:"+ iMqttDeliveryToken.isComplete());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private void connectToMQTT() {
        //新建并开启多线程，通过子线程连接
        new Thread(new Runnable() {
            @Override
            public void run() {
                //如果没有连接，再新建一个连接，防止重复连接
                if (!(client.isConnected())){
                    try {
                        client.connect(options);
                        //回传到mhandler
                        Message msg = new Message();
                        msg.what = 31;
                        mhandler.sendMessage(msg);
                    } catch (MqttException e) {
                        e.printStackTrace();
                        //如果连接失败，也回传到mhandler
                        Message msg = new Message();
                        msg.what = 30;
                        mhandler.sendMessage(msg);
                    }
                }
            }
        }).start();
    }

    //掉线自动重连
    private void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    connectToMQTT();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    //重写mqtt消息发布函数
    private void mPublish(String topic, String msg) {
        //确定已经连接到mqtt服务器后，再发布消息
        if (client == null || !(client.isConnected())) {
            return;
        }
        //新建mqtt消息对象
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(msg.getBytes());
        try {
            client.publish(topic,mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        //获取经纬度并更新天气数据
        initAmap();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在程序退出时保存天气和传感器的数据
        //步骤1：创建一个SharedPreferences对象
        //被抽取成了全局变量不用每次创建对象
        //SharedPreferences sharedPreferences= getSharedPreferences("data",Context.MODE_PRIVATE);
        //步骤2： 实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //步骤3：将获取过来的值放入文件
        editor.putString("weather", Weadther);
        editor.putString("advices", Advices);
        editor.putString("address", Area+'-'+City);
        editor.putString("air", Air);
        editor.putString("airIndex", AirIndex);
        //在程序启动时获取时间，在程序销毁时保存时间，在初始化经纬度时判断时间是否相同
        editor.putString("time", time0);
        //editor.putInt("age", 28);
        //editor.putBoolean("marrid",false);
        //步骤4：提交
        editor.commit();
        //关闭程序后向下位机发送关闭数据传输的请求

        //关闭定位功能
        mLocationClient.onDestroy();//销毁定位客户端，同时销毁本地定位服务。
    }
}