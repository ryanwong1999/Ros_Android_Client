package com.kgh.rosclient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;//Uri 代表要操作的数据，Android上可用的每种资源 - 图像、视频片段等都可以用Uri来表示
import android.os.Bundle;//Bundle主要用于传递数据
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.alibaba.fastjson.JSONObject;
import com.kgh.rosclient.RCApplication;
import com.jilk.ros.rosbridge.ROSBridgeClient;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
//import customView.BatteryView;
import de.greenrobot.event.EventBus;

//需要导入的包,不要用错包的MediaPlayer类
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

public class ShowIPCActivity extends Activity implements View.OnTouchListener, View.OnClickListener {
    private IVLCVout vlcVout;
    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;

    private VideoView videoView;
    private Button DC_Button_IPC;
    private Button DC_Button_RED;
    /*ROS端的数据*/
    ROSBridgeClient client;
    private PublishEvent mevent;

    //电量和充放电 0  放电   1充电 powers[0] 电量数据  power[1] 电量方向
    private static int powerData_number = 102;
    private static int powerData_dir = 3;
    //急停按钮
    private static int emergency_switch_flag = 3;
    //环境数据
    private static String Co2;
    private static String Voc;
    private static String Temp;
    private static String Hum;
    private static String Pm25;
    private static String Pm10;
    private static String Pm1_0;

    /*界面初始化*/
    private TextView DC_TextViewShowElectricPercentage;
    private TextView emergency_switch;
    private TextView envirment_co2;
    private TextView envirment_voc;
    private TextView envirment_temp;
    private TextView envirment_hum;
    private TextView envirment_pm25;
    private TextView envirment_pm10;
    private TextView envirment_pm1_0;
//    private BatteryView DC_Custom_Battery_view;
    private ImageButton DC_ImgBtn_Up1;
    private ImageButton DC_ImgBtn_Down1;
    private ImageButton DC_ImgBtn_Left1;
    private ImageButton DC_ImgBtn_Right1;
    private ImageButton DC_ImgBtn_HeadUp;
    private ImageButton DC_ImgBtn_HeadDown;
    private ImageButton DC_ImgBtn_HeadLeft;
    private ImageButton DC_ImgBtn_HeadRight;
    private ImageButton DC_ImgBtn_Reset;
    private ImageButton DC_ImgBtn_GoHome;

    String ipc_ip = "0.0.0.0";
    String red_ip = "0.0.0.0";
    String rtsp_ipc = "rtsp://0.0.0.0:554/user=admin&password=&channel=1&stream=0.sdp";
    String rtsp_red = "rtsp://0.0.0.0/webcam";
    private  String uri="rtsp://10.168.1.41:554/user=admin&password=&channel=1&stream=0.sdp";
    private  String uri1="rtsp://10.168.1.20/webcam";
//    private  String uri1="rtsp://10.168.1.20:9554/live?channel=1&subtype=0";
    private static final String HEAD = "rtsp://";
    private static final String SUFFIX = ":554/user=admin&password=&channel=1&stream=1.sdp";
//    private static final String SUFFIX1 = "/webcam";
//    private static final String SUFFIX = ":9554/live?channel=0&subtype=0";
    private static final String SUFFIX1 = ":9554/live?channel=1&subtype=0";

    int head_pitch = 90;
    int head_level = 90;

    public ShowIPCActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_ipc);//导入一个布局
        InitMenuShow();
        Intent intent = getIntent(); //Activity2
        ipc_ip = intent.getStringExtra("ipc_ip");
        red_ip = intent.getStringExtra("red_ip");
        Log.i("ShowIPCActivity","mylog ipc_ip"+ipc_ip + " red_ip:"+red_ip);

        rtsp_ipc = HEAD + ipc_ip + SUFFIX;
        rtsp_red = HEAD + red_ip + SUFFIX1;

        EventBus.getDefault().register(this);  //必须注册 ROS端的处理  除非重新创建
        //获取ROS 端的实例
        client = ((RCApplication) getApplication()).getRosClient();
        //向ROS端订阅数据
        SubscribleRos();

        new Thread(new Runnable() {
            @Override
            public void run() {
                RtspPlay(rtsp_ipc);
            }
        }).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void InitMenuShow() {
        DC_TextViewShowElectricPercentage = (TextView) findViewById(R.id.DC_TextViewShowElectricPercentage);
        emergency_switch = (TextView) findViewById(R.id.emergency_switch);
        envirment_co2 = (TextView) findViewById(R.id.envirment_co2);
        envirment_voc = (TextView) findViewById(R.id.envirment_voc);
        envirment_temp = (TextView) findViewById(R.id.envirment_temp);
        envirment_hum = (TextView) findViewById(R.id.envirment_hum);
        envirment_pm25 = (TextView) findViewById(R.id.envirment_pm25);
        envirment_pm10 = (TextView) findViewById(R.id.envirment_pm10);
        envirment_pm1_0 = (TextView) findViewById(R.id.envirment_pm1_0);
        DC_Button_IPC = (Button) findViewById(R.id.DC_Button_IPC);
        DC_Button_RED = (Button) findViewById(R.id.DC_Button_RED);
        DC_Button_IPC.setOnClickListener(this);
        DC_Button_RED.setOnClickListener(this);
        //方向运动
        DC_ImgBtn_Up1 = (ImageButton) findViewById(R.id.DC_ImgBtn_Up1);
        DC_ImgBtn_Up1.setOnTouchListener(new ShowIPCActivity.ComponentOnLongTouch());
        DC_ImgBtn_Down1 = (ImageButton) findViewById(R.id.DC_ImgBtn_Down1);
        DC_ImgBtn_Down1.setOnTouchListener(new ShowIPCActivity.ComponentOnLongTouch());
        DC_ImgBtn_Left1 = (ImageButton) findViewById(R.id.DC_ImgBtn_Left1);
        DC_ImgBtn_Left1.setOnTouchListener(new ShowIPCActivity.ComponentOnLongTouch());
        DC_ImgBtn_Right1 = (ImageButton) findViewById(R.id.DC_ImgBtn_Right1);
        DC_ImgBtn_Right1.setOnTouchListener(new ShowIPCActivity.ComponentOnLongTouch());
        //云台
        DC_ImgBtn_HeadUp = (ImageButton) findViewById(R.id.DC_ImgBtn_HeadUp);
        DC_ImgBtn_HeadUp.setOnTouchListener(new ShowIPCActivity.ComponentOnLongTouch());
        DC_ImgBtn_HeadDown = (ImageButton) findViewById(R.id.DC_ImgBtn_HeadDown);
        DC_ImgBtn_HeadDown.setOnTouchListener(new ShowIPCActivity.ComponentOnLongTouch());
        DC_ImgBtn_HeadLeft = (ImageButton) findViewById(R.id.DC_ImgBtn_HeadLeft);
        DC_ImgBtn_HeadLeft.setOnTouchListener(new ShowIPCActivity.ComponentOnLongTouch());
        DC_ImgBtn_HeadRight = (ImageButton) findViewById(R.id.DC_ImgBtn_HeadRight);
        DC_ImgBtn_HeadRight.setOnTouchListener(new ShowIPCActivity.ComponentOnLongTouch());
        DC_ImgBtn_Reset = (ImageButton) findViewById(R.id.DC_ImgBtn_Reset);
        DC_ImgBtn_Reset.setOnClickListener(this);
        DC_ImgBtn_GoHome = (ImageButton) findViewById(R.id.DC_ImgBtn_GoHome);
        DC_ImgBtn_GoHome.setOnClickListener(this);
    }

    private void RtspPlay(String rtsp_uri) {
        SurfaceView srfc = (SurfaceView) findViewById(R.id.surface_view);
        ArrayList<String> options = new ArrayList<>();
        LibVLC libVLC = new LibVLC(getApplication(), options);
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer(libVLC);
            mediaPlayer.getVLCVout().setVideoSurface(srfc.getHolder().getSurface(), srfc.getHolder());
            //播放前还要调用这个方法
            mediaPlayer.getVLCVout().attachViews();
            Media media = new Media(libVLC, Uri.parse(rtsp_uri));
            mediaPlayer.setMedia(media);
            mediaPlayer.play();
            surfaceView.setOnTouchListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @Function: 获取电量信息
     * @Return: 0   毫安时  1 电量   2     3 电压    4：0 放电 1充电   5 电流的大小
     */
    private void parsePowerInfo(PublishEvent event) {
        //还是阿里的包好用
        JSONObject jsonObject = JSONObject.parseObject(event.msg);
        Log.i("ShowIPCActivity" ,  "PMS--------------->"+jsonObject.toJSONString());
        powerData_dir = Integer.valueOf(jsonObject.getString("pms_charging_flag"));
        powerData_number = Integer.valueOf(jsonObject.getString("pms_battary_level"));
    }

    private void get_Wheel_Switch(PublishEvent event) {
        JSONObject jsonObject = JSONObject.parseObject(event.msg);
        Log.i("ShowIPCActivity" ,  "Wheel_Switch------------>"+jsonObject.toJSONString());
        emergency_switch_flag = Integer.valueOf(jsonObject.getString("Switch"));
        if(emergency_switch_flag == 0){
            emergency_switch.setText("松开");
        }
        if(emergency_switch_flag == 1){
            emergency_switch.setText("按下");
        }
    }

    private void get_Envirment_data(PublishEvent event) {
        JSONObject jsonObject = JSONObject.parseObject(event.msg);
        Log.i("ShowIPCActivity" ,  "Envirment_data------------>"+jsonObject.toJSONString());
        Co2 = String.valueOf(jsonObject.getString("Co2"));
        Voc = String.valueOf(jsonObject.getString("Voc"));
        Temp = String.valueOf(jsonObject.getString("Temp"));
        Hum = String.valueOf(jsonObject.getString("Hum"));
        Pm25 = String.valueOf(jsonObject.getString("Pm25"));
        Pm10 = String.valueOf(jsonObject.getString("Pm10"));
        Pm1_0 = String.valueOf(jsonObject.getString("Pm1_0"));
        envirment_co2.setText(Co2);
        envirment_voc.setText(Voc);
        envirment_temp.setText(Temp);
        envirment_hum.setText(Hum);
        envirment_pm25.setText(Pm25);
        envirment_pm10.setText(Pm10);
        envirment_pm1_0.setText(Pm1_0);
    }

    //向ROS端订阅数据
    private void SubscribleRos() {
        //获取电量信息
        String RosMegPMSDetailName = "/PMS_get_status";
        String RosMegPower = "{\"op\":\"subscribe\",\"topic\":\""+RosMegPMSDetailName+ "\"}";
        client.send(RosMegPower);
        //订阅急停信息
        String RosMegSwitchDetailName = "/Wheel_Switch";
        String RosMegSwitch = "{\"op\":\"subscribe\",\"topic\":\"" + RosMegSwitchDetailName + "\"}";
        client.send(RosMegSwitch);
        //订阅环境信息
        String RosMegEnvirmentDetailName = "/Envirment_data";
        String RosMegEnvirment = "{\"op\":\"subscribe\",\"topic\":\"" + RosMegEnvirmentDetailName + "\"}";
        client.send(RosMegEnvirment);
    }

    /**
     * @Function: 接受订阅的消息
     * @Return:
     */
    public void onEvent(final PublishEvent event) {
        //接受电量信息
        if ("/PMS_get_status".equals(event.name)) {
            parsePowerInfo(event);
            return;
        }
        //接收急停
        if ("/Wheel_Switch".equals(event.name)) {
            get_Wheel_Switch(event);
            return;
        }
        //接收环境
        if ("/Envirment_data".equals(event.name)) {
            get_Envirment_data(event);
            return;
        }
    }

    /**
     * @Function: 控制前后左右的运动的长按 触发
     * @Return:
     */
    private class ComponentOnLongTouch implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (v.getId()) {
                case R.id.DC_ImgBtn_Up1:    //前进
                    onTouchChange("Up", event.getAction());
                    break;
                case R.id.DC_ImgBtn_Down1:  //后退
                    onTouchChange("Down", event.getAction());
                    break;
                case R.id.DC_ImgBtn_Left1:  //左转
                    onTouchChange("Left", event.getAction());
                    break;
                case R.id.DC_ImgBtn_Right1: //右转
                    onTouchChange("Right", event.getAction());
                    break;

                case R.id.DC_ImgBtn_HeadUp: //头上
                    onTouchChange("HeadUp", event.getAction());
                    break;
                case R.id.DC_ImgBtn_HeadDown: //头下
                    onTouchChange("HeadDown", event.getAction());
                    break;
                case R.id.DC_ImgBtn_HeadLeft: //头左
                    onTouchChange("HeadLeft", event.getAction());
                    break;
                case R.id.DC_ImgBtn_HeadRight: //头右
                    onTouchChange("HeadRight", event.getAction());
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private boolean Btn_LongPressUp = false;
    private boolean Btn_LongPressDown = false;
    private boolean Btn_LongPressLeft = false;
    private boolean Btn_LongPressRight = false;
    private boolean Btn_LongPressHeadUp = false;
    private boolean Btn_LongPressHeadDown = false;
    private boolean Btn_LongPressHeadLeft = false;
    private boolean Btn_LongPressHeadRight = false;

    //控制方向的转动
    class ControlMoveThread extends Thread {
        @Override
        public void run() {
            while (Btn_LongPressUp) {
                TurnUp();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (Btn_LongPressDown) {
                TurnDown();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (Btn_LongPressLeft) {
                TurnLeft();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (Btn_LongPressRight) {
                TurnRight();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            while (Btn_LongPressHeadUp) {
                head_pitch += 1;
                if(head_pitch >= 135){
                    head_pitch = 135;
                }
                HeadUp(head_pitch);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (Btn_LongPressHeadDown) {
                head_pitch -= 1;
                if(head_pitch <= 45){
                    head_pitch = 45;
                }
                HeadDown(head_pitch);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (Btn_LongPressHeadLeft) {
                head_level -= 1;
                if(head_level <= 0){
                    head_level = 0;
                }
                HeadLeft(head_level);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (Btn_LongPressHeadRight) {
                head_level += 1;
                if(head_level >= 180){
                    head_level = 180;
                }
                HeadRight(head_level);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //判断事件
    private void onTouchChange(String methodName, int eventAction) {
        //接触误按，多个按键一起按下
        if (Btn_LongPressUp && Btn_LongPressDown && Btn_LongPressLeft && Btn_LongPressRight) {
            Btn_LongPressUp = false;
            Btn_LongPressDown = false;
            Btn_LongPressLeft = false;
            Btn_LongPressRight = false;
        }
        if (Btn_LongPressHeadUp && Btn_LongPressHeadDown && Btn_LongPressHeadLeft && Btn_LongPressHeadRight) {
            Btn_LongPressHeadUp = false;
            Btn_LongPressHeadDown = false;
            Btn_LongPressHeadLeft = false;
            Btn_LongPressHeadRight = false;
        }
        ShowIPCActivity.ControlMoveThread controlMoveThread = new ControlMoveThread();
        //判断按下了哪个
        if ("Up".equals(methodName)) {  //前进
            if (eventAction == MotionEvent.ACTION_DOWN) {
                controlMoveThread.start();
                Btn_LongPressUp = true;
                DC_ImgBtn_Up1.setImageResource(R.drawable.move_up_press1);
            } else if (eventAction == MotionEvent.ACTION_UP) {
                TurnStop();
                if (controlMoveThread != null)
                    Btn_LongPressUp = false;
                DC_ImgBtn_Up1.setImageResource(R.drawable.move_up1);
            }
        }
        if ("Down".equals(methodName)) {  //后退
            if (eventAction == MotionEvent.ACTION_DOWN) {
                controlMoveThread.start();
                Btn_LongPressDown = true;
                DC_ImgBtn_Down1.setImageResource(R.drawable.move_down_press1);
            } else if (eventAction == MotionEvent.ACTION_UP) {
                TurnStop();
                if (controlMoveThread != null)
                    Btn_LongPressDown = false;
                DC_ImgBtn_Down1.setImageResource(R.drawable.move_down1);
            }
        }
        if ("Left".equals(methodName)) {  //左转
            if (eventAction == MotionEvent.ACTION_DOWN) {
                controlMoveThread.start();
                Btn_LongPressLeft = true;
                DC_ImgBtn_Left1.setImageResource(R.drawable.move_left_press1);
            } else if (eventAction == MotionEvent.ACTION_UP) {
                TurnStop();
                if (controlMoveThread != null)
                    Btn_LongPressLeft = false;
                DC_ImgBtn_Left1.setImageResource(R.drawable.move_left1);
            }
        }
        if ("Right".equals(methodName)) {  //右转
            if (eventAction == MotionEvent.ACTION_DOWN) {
                controlMoveThread.start();
                Btn_LongPressRight = true;
                DC_ImgBtn_Right1.setImageResource(R.drawable.move_right_press1);
            } else if (eventAction == MotionEvent.ACTION_UP) {
                TurnStop();
                if (controlMoveThread != null)
                    Btn_LongPressRight = false;
                DC_ImgBtn_Right1.setImageResource(R.drawable.move_right1);
            }
        }

        if ("HeadUp".equals(methodName)) {  //头上
            if (eventAction == MotionEvent.ACTION_DOWN) {
                controlMoveThread.start();
                Btn_LongPressHeadUp = true;
                DC_ImgBtn_HeadUp.setImageResource(R.drawable.move_up_press1);
            } else if (eventAction == MotionEvent.ACTION_UP) {
                if (controlMoveThread != null)
                    Btn_LongPressHeadUp = false;
                DC_ImgBtn_HeadUp.setImageResource(R.drawable.move_up1);
            }
        }
        if ("HeadDown".equals(methodName)) {  //头下
            if (eventAction == MotionEvent.ACTION_DOWN) {
                controlMoveThread.start();
                Btn_LongPressHeadDown = true;
                DC_ImgBtn_HeadDown.setImageResource(R.drawable.move_down_press1);
            } else if (eventAction == MotionEvent.ACTION_UP) {
                if (controlMoveThread != null)
                    Btn_LongPressHeadDown = false;
                DC_ImgBtn_HeadDown.setImageResource(R.drawable.move_down1);
            }
        }
        if ("HeadLeft".equals(methodName)) {  //头左
            if (eventAction == MotionEvent.ACTION_DOWN) {
                controlMoveThread.start();
                Btn_LongPressHeadLeft = true;
                DC_ImgBtn_HeadLeft.setImageResource(R.drawable.move_left_press1);
            } else if (eventAction == MotionEvent.ACTION_UP) {
                if (controlMoveThread != null)
                    Btn_LongPressHeadLeft = false;
                DC_ImgBtn_HeadLeft.setImageResource(R.drawable.move_left1);
            }
        }
        if ("HeadRight".equals(methodName)) {  //头右
            if (eventAction == MotionEvent.ACTION_DOWN) {
                controlMoveThread.start();
                Btn_LongPressHeadRight = true;
                DC_ImgBtn_HeadRight.setImageResource(R.drawable.move_right_press1);
            } else if (eventAction == MotionEvent.ACTION_UP) {
                if (controlMoveThread != null)
                    Btn_LongPressHeadRight = false;
                DC_ImgBtn_HeadRight.setImageResource(R.drawable.move_right1);
            }
        }
    }

    //  前进
    private void TurnUp() {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/cmd_vel\",\"msg\":{\"linear\":{\"x\":" + 0.4 + ",\"y\":" +
                0 + ",\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":" + 0 + "}}}";
        client.send(MegRight);
        Log.i("ShowIPCActivity", "在前进");
    }
    //后退
    private void TurnDown() {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/cmd_vel\",\"msg\":{\"linear\":{\"x\":" + -0.2 + ",\"y\":" +
                0 + ",\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":" + 0 + "}}}";
        client.send(MegRight);
        Log.i("ShowIPCActivity", "在后退");
    }
    //左转
    private void TurnLeft() {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/cmd_vel\",\"msg\":{\"linear\":{\"x\":" + 0 + ",\"y\":" +
                0 + ",\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":" + (0.4) + "}}}";
        client.send(MegRight);
        Log.i("ShowIPCActivity", "在左转");
    }
    //右转
    private void TurnRight() {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/cmd_vel\",\"msg\":{\"linear\":{\"x\":" + 0 + ",\"y\":" +
                0 + ",\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":" + (-0.4) + "}}}";
        client.send(MegRight);
        Log.i("ShowIPCActivity", "在右转");
    }
    //停止
    private void TurnStop() {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/cmd_vel\",\"msg\":{\"linear\":{\"x\":" + 0 + ",\"y\":" +
                0 + ",\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":" + 0 + "}}}";
        client.send(MegRight);
        Log.i("ShowIPCActivity", "不想转了");
    }

    //  头上
    private void HeadUp(int head_pitch) {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/PSC_angle_control\",\"msg\":{\"set_level\":" + head_level + "," +
                "\"set_pitch\":" + head_pitch +"}}";
        client.send(MegRight);
        Log.i("ShowIPCActivity", "头上");
    }
    //  头下
    private void HeadDown(int head_pitch) {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/PSC_angle_control\",\"msg\":{\"set_level\":" + head_level + "," +
                "\"set_pitch\":" + head_pitch +"}}";
        client.send(MegRight);
        Log.i("ShowIPCActivity", "头下");
    }
    //  头左
    private void HeadLeft(int head_level) {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/PSC_angle_control\",\"msg\":{\"set_level\":" + head_level + "," +
                "\"set_pitch\":" + head_pitch +"}}";
        client.send(MegRight);
        Log.i("ShowIPCActivity", "头左");
    }
    //  头右
    private void HeadRight(int head_level) {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/PSC_angle_control\",\"msg\":{\"set_level\":" + head_level + "," +
                "\"set_pitch\":" + head_pitch +"}}";
        client.send(MegRight);
        Log.i("ShowIPCActivity", "头右");
    }
    //  头正
    private void HeadReset() {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/PSC_angle_control\",\"msg\":{\"set_level\":90," +
                "\"set_pitch\":90}}";
        client.send(MegRight);
        Toast.makeText(ShowIPCActivity.this,"云台复位",Toast.LENGTH_LONG).show();
        Log.i("ShowIPCActivity", "头正");
    }
    //  回充电桩
    private void GoHonme() {
        String MegRight = "{\"op\":\"publish\",\"topic\":\"/Robot_move\",\"msg\":{\"type\":407}}";
        client.send(MegRight);
        Toast.makeText(ShowIPCActivity.this,"回充电桩",Toast.LENGTH_LONG).show();
        Log.i("ShowIPCActivity", "回充电桩");
    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.DC_Button_IPC:
//                Toast toast = Toast.makeText(this, "切换可见光中，切换完成前请勿操作", Toast.LENGTH_LONG);
//                //参数1：gravity,显示的位置，如上下左右，参数2:xOffset,x的距离，参数3：yOffset,y的距离
//                toast.setGravity(Gravity.TOP, 0, 150);
//                toast.show();
                ProgressDialog dialog_ipc = new ProgressDialog(this); //1.创建一个ProgressDialog的实例
                dialog_ipc.setMessage("切换可见光中，请稍等...");
                dialog_ipc.show();//5.将ProgessDialog显示出来
                Log.i("ShowIPCActivity" ,  "切换可见光");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        RtspPlay(rtsp_ipc);
                        dialog_ipc.dismiss();
                    }
                }).start();
                break;
            case R.id.DC_Button_RED:
                ProgressDialog dialog_red = new ProgressDialog(this); //1.创建一个ProgressDialog的实例
                dialog_red.setMessage("切换热像仪中，请稍等...");
                dialog_red.show();//5.将ProgessDialog显示出来
                Log.i("ShowIPCActivity" ,  "切换热像仪");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        RtspPlay(rtsp_red);
                        dialog_red.dismiss();
                    }
                }).start();
                break;
            case R.id.DC_ImgBtn_Reset: //头正
                Thread t6 = new Thread() {
                    public void run() {
                        head_pitch = 90;
                        head_level = 90;
                        HeadReset();
                    }
                };
                t6.run();
                break;
            case R.id.DC_ImgBtn_GoHome:
                GoHonme();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            if(mediaPlayer != null && mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.d("vlc-destroy", e.toString());
        }
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {    //实现onTouch接口

        switch (view.getId()){
            case R.id.surface_view:
//                Toast.makeText(ShowIPCActivity.this,"IPC",Toast.LENGTH_LONG).show();
                break;
        }
        return false;
    }
}

