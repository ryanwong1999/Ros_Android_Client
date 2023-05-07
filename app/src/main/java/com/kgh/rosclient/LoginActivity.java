package com.kgh.rosclient;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.jilk.ros.ROSClient;
import com.jilk.ros.rosbridge.ROSBridgeClient;
import com.utils.ACache;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    ROSBridgeClient client;
    private Button DC_Button_Connect;
    private TextView tv;
    private ImageView img_con;
    private EditText DC_EditTextGetIP;
    private EditText DC_EditTextGetIPCIP;
    private EditText DC_EditTextGetREDIP;
    private String IP;
    private String ipc_ip;
    private String red_ip;
    private String port = "9090";
    private boolean Flag_Connect = false;
    private ProgressBar pgload;
    private ACache aCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        InitMenuShow();
    }

    //初始化界面的设计
    private void InitMenuShow() {
        tv =  findViewById(R.id.tv_connect);
        img_con = findViewById(R.id.img_connect);
        img_con.setImageDrawable(getResources().getDrawable(R.drawable.offline));
        tv.setText("未连接");
        DC_EditTextGetIP = (EditText) findViewById(R.id.DC_EditTextGetIP);
        DC_EditTextGetIPCIP = (EditText) findViewById(R.id.DC_EditTextGetIPCIP);
        DC_EditTextGetREDIP = (EditText) findViewById(R.id.DC_EditTextGetREDIP);
        pgload =  (ProgressBar)findViewById(R.id.progressBar);

        DC_Button_Connect = (Button) findViewById(R.id.DC_Button_Connect);
        DC_Button_Connect.setOnClickListener(this);

        aCache = ACache.get(this);
        if (aCache.getAsString("robot_ip") != null)
            DC_EditTextGetIP.setText(aCache.getAsString("robot_ip"));
        if (aCache.getAsString("ipc_ip") != null)
            DC_EditTextGetIPCIP.setText(aCache.getAsString("ipc_ip"));
        if (aCache.getAsString("red_ip") != null)
            DC_EditTextGetREDIP.setText(aCache.getAsString("red_ip"));
    }

    private Handler mHandlerMsg = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case 101:
                    tv.setText("已连接");
                    img_con.setImageResource(R.drawable.online);
                    pgload.setVisibility(View.GONE);
                    break;
                case 102:
                    tv.setText("未连接");
                    img_con.setImageResource(R.drawable.offline);
                    pgload.setVisibility(View.GONE);
                    break;
            }
        }
    };

    private void connect(String ip, String port, String ipc_ip, String red_ip) {
        client = new ROSBridgeClient("http://" + ip + ":" + port +"/");
        Log.i("LoginActivity","ip:"+ip+" port:"+port);
        Flag_Connect = client.connect(new ROSClient.ConnectionStatusListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onConnect() {
                client.setDebug(true);
                ((RCApplication) getApplication()).setRosClient(client);
                showTip("Connect ROS success");
                Log.d("Login", "Connect ROS success");
                Message msgtime = new Message();
                msgtime.what = 101;
                mHandlerMsg.sendMessage(msgtime);
                tv.setText("已连接");

//                Intent mIntent1 = new Intent();
//                mIntent1.putExtra("ipc_ip", ipc_ip);
//                mIntent1.putExtra("red_ip", red_ip);
//                mIntent1.setClass(LoginActivity.this, ShowIPCActivity.class);
//                startActivity(mIntent1);
            }
            @Override
            public void onDisconnect(boolean normal, String reason, int code) {
                Log.d("Login", "ROS Connect ERROR " + reason +" "+code);
                //startActivity(new Intent(LoginActivity.this, MainActivity.class));
                showTip("Connect ROS faild");
                Log.d("Login", "Connect ROS faild");
                Message msgtime = new Message();
                msgtime.what = 102;
                mHandlerMsg.sendMessage(msgtime);
            }
            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
                showTip("ROS communication error");
                Log.d("Login", "ROS communication error");
            }
        });
    }

    private void saveUserInfo() {
        if (DC_EditTextGetIP.getText().toString() != null
                && (!DC_EditTextGetIP.getText().toString()
                .equals(aCache.getAsString("robot_ip"))))
            aCache.put("robot_ip", DC_EditTextGetIP.getText().toString());
        if (DC_EditTextGetIPCIP.getText().toString() != null
                && (!DC_EditTextGetIPCIP.getText().toString()
                .equals(aCache.getAsString("ipc_ip"))))
            aCache.put("ipc_ip", DC_EditTextGetIPCIP.getText().toString());
        if (DC_EditTextGetREDIP.getText().toString() != null
                && (!DC_EditTextGetREDIP.getText().toString()
                .equals(aCache.getAsString("red_ip"))))
            aCache.put("red_ip", DC_EditTextGetREDIP.getText().toString());
    }

    private void showTip(final String tip) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, tip, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.DC_Button_Connect:
                if (DC_EditTextGetIP.getText().toString() == null
                        || DC_EditTextGetIP.getText().toString().trim().equals("")) {
                    showTip("请输入机器人IP");
                    return;
                }
                if (DC_EditTextGetIPCIP.getText().toString() == null
                        || DC_EditTextGetIPCIP.getText().toString().trim().equals("")) {
                    showTip("请输入可见光IP");
                    return;
                }
                if (DC_EditTextGetREDIP.getText().toString() == null
                        || DC_EditTextGetREDIP.getText().toString().trim().equals("")) {
                    showTip("请输入热像仪IP");
                    return;
                }
//                if (Flag_Connect == true) {
//                    showTip("已连接");
//                    Intent mIntent3 = new Intent();
//                    ipc_ip = DC_EditTextGetIPCIP.getText().toString().trim();
//                    red_ip = DC_EditTextGetREDIP.getText().toString().trim();
//                    mIntent3.putExtra("ipc_ip", ipc_ip);
//                    mIntent3.putExtra("red_ip", red_ip);
//                    mIntent3.setClass(LoginActivity.this, ShowIPCActivity.class);
//                    startActivity(mIntent3);
//                    break;
//                }
                pgload.setVisibility(View.VISIBLE);
                IP = DC_EditTextGetIP.getText().toString().trim();
                ipc_ip = DC_EditTextGetIPCIP.getText().toString().trim();
                red_ip = DC_EditTextGetREDIP.getText().toString().trim();
                //port = DC_EditTextGetPort.getText().toString().trim();
                saveUserInfo();
                Thread t = new Thread() {
                    public void run() {
                        connect(IP, port, ipc_ip, red_ip);
                    }
                };
                t.start();
                break;
            default:
                break;
        }
    }


}
