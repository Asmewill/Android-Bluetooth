package yc.bluetooth.androidbt;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.print.PrinterId;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import yc.bluetooth.androidbt.util.ClsUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "BtMain";

    private static final int CONNECT_SUCCESS = 0x01;
    private static final int CONNECT_FAILURE = 0x02;
    private static final int DISCONNECT_SUCCESS = 0x03;
    private static final int SEND_SUCCESS = 0x04;
    private static final int SEND_FAILURE= 0x05;
    private static final int RECEIVE_SUCCESS= 0x06;
    private static final int RECEIVE_FAILURE =0x07;
    private static final int START_DISCOVERY = 0x08;
    private static final int STOP_DISCOVERY = 0x09;
    private static final int DISCOVERY_DEVICE = 0x0A;
    private static final int DEVICE_BOND_NONE= 0x0B;
    private static final int DEVICE_BONDING = 0x0C;
    private static final int DEVICE_BONDED = 0x0D;

    private Button btSearch;
    private TextView tvCurConState;
    private TextView tvCurBondState;
    private TextView tvName;
    private TextView tvAddress;
    private Button btConnect;
    private Button btDisconnect;
    private Button btBound;
    private Button btDisBound;
    private EditText etSendMsg;
    private Button btSend;
    private TextView tvSendResult;
    private TextView tvReceive;
    private LinearLayout llDeviceList;
    private LinearLayout llDataSendReceive;
    private ListView lvDevices;
    private LVDevicesAdapter lvDevicesAdapter;

    //蓝牙
    private BluetoothAdapter bluetoothAdapter;
    private BtBroadcastReceiver btBroadcastReceiver;
    //连接设备的UUID
    public static final String MY_BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";  //蓝牙通讯
    //当前要连接的设备
    private BluetoothDevice curBluetoothDevice;
    //发起连接的线程
    private ConnectThread connectThread;
    //管理连接的线程
    private ConnectedThread connectedThread;
    //当前设备连接状态
    private boolean curConnState = false;
    //当前设备与系统配对状态
    private boolean curBondState = false;


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch(msg.what){
                case START_DISCOVERY:
                    Log.d(TAG, "开始搜索设备...");
                    break;

                case STOP_DISCOVERY:
                    Log.d(TAG, "停止搜索设备...");
                    break;

                case DISCOVERY_DEVICE:  //扫描到设备
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                    lvDevicesAdapter.addDevice(bluetoothDevice);

                    break;

                case CONNECT_FAILURE: //连接失败
                    Log.d(TAG, "连接失败");
                    tvCurConState.setText("连接失败");
                    curConnState = false;
                    break;

                case CONNECT_SUCCESS:  //连接成功
                    Log.d(TAG, "连接成功");
                    tvCurConState.setText("连接成功");
                    curConnState = true;
                    llDataSendReceive.setVisibility(View.VISIBLE);
                    llDeviceList.setVisibility(View.GONE);
                    break;

                case DISCONNECT_SUCCESS:
                    tvCurConState.setText("断开成功");
                    curConnState = false;

                    break;

                case SEND_FAILURE: //发送失败
                    Toast.makeText(MainActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                    break;

                case SEND_SUCCESS:  //发送成功
                    String sendResult = (String) msg.obj;
                    tvSendResult.setText(sendResult);
                    break;

                case RECEIVE_FAILURE: //接收失败
                    String receiveError = (String) msg.obj;
                    tvReceive.setText(receiveError);
                    break;

                case RECEIVE_SUCCESS:  //接收成功
                    String receiveResult = (String) msg.obj;
                    tvReceive.setText(receiveResult);
                    break;

                case DEVICE_BOND_NONE:  //已解除配对
                    tvCurBondState.setText("解除配对成功");
                    curBondState = false;

                    break;

                case DEVICE_BONDING:   //正在配对
                    tvCurBondState.setText("正在配对...");
                    break;

                case DEVICE_BONDED:   //已配对
                    tvCurBondState.setText("配对成功");
                    curBondState = true;
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化视图
        initView();
        //初始化监听
        iniListener();
        //初始化数据
        initData();
        //初始化蓝牙
        initBluetooth();
        //初始化蓝牙广播
        initBtBroadcast();
    }

    private void initData() {
        lvDevicesAdapter = new LVDevicesAdapter(MainActivity.this);
        lvDevices.setAdapter(lvDevicesAdapter);
    }

    private void initView() {
        btSearch = findViewById(R.id.bt_search);
        tvCurConState = findViewById(R.id.tv_cur_con_state);
        tvCurBondState = findViewById(R.id.tv_cur_bond_state);
        btConnect = findViewById(R.id.bt_connect);
        btDisconnect = findViewById(R.id.bt_disconnect);
        btBound = findViewById(R.id.bt_bound);
        btDisBound = findViewById(R.id.bt_disBound);
        tvName = findViewById(R.id.tv_name);
        tvAddress = findViewById(R.id.tv_address);
        etSendMsg = findViewById(R.id.et_send_msg);
        btSend = findViewById(R.id.bt_to_send);
        tvSendResult = findViewById(R.id.tv_send_result);
        tvReceive = findViewById(R.id.tv_receive_result);
        llDeviceList = findViewById(R.id.ll_device_list);
        llDataSendReceive  = findViewById(R.id.ll_data_send_receive);
        lvDevices = findViewById(R.id.lv_devices);
    }

    private void iniListener() {
        btSearch.setOnClickListener(this);
        btConnect.setOnClickListener(this);
        btDisconnect.setOnClickListener(this);
        btBound.setOnClickListener(this);
        btDisBound.setOnClickListener(this);
        btSend.setOnClickListener(this);

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) lvDevicesAdapter.getItem(i);
                tvName.setText(bluetoothDevice.getName());
                tvAddress.setText(bluetoothDevice.getAddress());
                curBluetoothDevice = bluetoothDevice;
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_search:  //搜索蓝牙
                llDataSendReceive.setVisibility(View.GONE);
                llDeviceList.setVisibility(View.VISIBLE);
                searchBtDevice();
                break;

            case R.id.bt_connect: //连接蓝牙
                if(!curConnState) {
                    startConnectDevice(curBluetoothDevice, MY_BLUETOOTH_UUID, 10000);
                }else{
                    Toast.makeText(this, "当前设备已连接", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.bt_disconnect: //断开连接
                if(curConnState) {
                    clearConnectedThread();
                }else{
                    Toast.makeText(this, "当前设备未连接", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.bt_bound:  //配对设备
                if(!curBondState) {
                    boundDevice(curBluetoothDevice);
                }else{
                    Toast.makeText(this, "当前设备已经与系统蓝牙建立配对", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.bt_disBound:  //解除配对
                if(curBondState) {
                    disBoundDevice(curBluetoothDevice);
                }else{
                    Toast.makeText(this, "当前设备尚未与系统蓝牙建立配对", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.bt_to_send: //发送数据
                if(curConnState){
                    String sendMsg = etSendMsg.getText().toString();
                    if(sendMsg.isEmpty()){
                        Toast.makeText(this, "发送数据为空！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendData(sendMsg,true);  //以16进制字符串形式发送数据
                }else{
                    Toast.makeText(this, "请先连接当前设备", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    //////////////////////////////////  搜索设备  /////////////////////////////////////////////////
    private void searchBtDevice() {
        if (bluetoothAdapter.isDiscovering()) { //当前正在搜索设备...
            return;
        }
        //开始搜索
        bluetoothAdapter.startDiscovery();
    }

    //////////////////////////////////  配对/接触配对设备  ////////////////////////////////////////////
    /**
     * 执行绑定 反射
     * @param bluetoothDevice 蓝牙设备
     * @return true 执行绑定 false 未执行绑定
     */
    public boolean boundDevice(BluetoothDevice bluetoothDevice){
        if(bluetoothDevice == null){
            Log.e(TAG,"boundDevice-->bluetoothDevice == null");
            return false;
        }

        try {
            return ClsUtils.createBond(BluetoothDevice.class,bluetoothDevice);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 执行解绑  反射
     * @param bluetoothDevice 蓝牙设备
     * @return  true 执行解绑  false未执行解绑
     */
    public boolean disBoundDevice(BluetoothDevice bluetoothDevice){
        if(bluetoothDevice == null){
            Log.e(TAG,"disBoundDevice-->bluetoothDevice == null");
            return false;
        }

        try {
            return ClsUtils.removeBond(BluetoothDevice.class,bluetoothDevice);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    //////////////////////////////////   连接设备   ///////////////////////////////////////////////
    /**
     * 开始连接设备
     * @param bluetoothDevice   蓝牙设备
     * @param uuid               发起连接的UUID
     * @param conOutTime        连接超时时间
     */
    public void startConnectDevice(final BluetoothDevice bluetoothDevice, String uuid, long conOutTime){
        if(bluetoothDevice == null){
            Log.e(TAG,"startConnectDevice-->bluetoothDevice == null");
            return;
        }
        if(bluetoothAdapter == null){
            Log.e(TAG,"startConnectDevice-->bluetooth3Adapter == null");
            return;
        }
        //发起连接
        connectThread = new ConnectThread(bluetoothAdapter,curBluetoothDevice,uuid);
        connectThread.setOnBluetoothConnectListener(new ConnectThread.OnBluetoothConnectListener() {
            @Override
            public void onStartConn() {
                Log.d(TAG,"startConnectDevice-->开始连接..." + bluetoothDevice.getName() + "-->" + bluetoothDevice.getAddress());
            }


            @Override
            public void onConnSuccess(BluetoothSocket bluetoothSocket) {
                //移除连接超时
                mHandler.removeCallbacks(connectOuttimeRunnable);
                Log.d(TAG,"startConnectDevice-->移除连接超时");
                Log.w(TAG,"startConnectDevice-->连接成功");

                Message message = new Message();
                message.what = CONNECT_SUCCESS;
                mHandler.sendMessage(message);

                //标记当前连接状态为true
                curConnState = true;
                //管理连接，收发数据
                managerConnectSendReceiveData(bluetoothSocket);
            }

            @Override
            public void onConnFailure(String errorMsg) {
                Log.e(TAG,"startConnectDevice-->" + errorMsg);

                Message message = new Message();
                message.what = CONNECT_FAILURE;
                mHandler.sendMessage(message);

                //标记当前连接状态为false
                curConnState = false;

                //断开管理连接
                clearConnectedThread();
            }
        });

        connectThread.start();
        //设置连接超时时间
        mHandler.postDelayed(connectOuttimeRunnable,conOutTime);

    }

    //连接超时
    private Runnable connectOuttimeRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG,"startConnectDevice-->连接超时" );

            Message message = new Message();
            message.what = CONNECT_FAILURE;
            mHandler.sendMessage(message);

            //标记当前连接状态为false
            curConnState = false;
            //断开管理连接
            clearConnectedThread();
        }
    };

    ////////////////////////////////////// 断开连接  //////////////////////////////////////////////
    /**
     * 断开已有的连接
     */
    public void clearConnectedThread(){
        Log.d(TAG,"clearConnectedThread-->即将断开");

        //connectedThread断开已有连接
        if(connectedThread == null){
            Log.e(TAG,"clearConnectedThread-->connectedThread == null");
            return;
        }
        connectedThread.terminalClose(connectThread);

        //等待线程运行完后再断开
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connectedThread.cancel();  //释放连接

                connectedThread = null;
            }
        },10);

        Log.w(TAG,"clearConnectedThread-->成功断开连接");
        Message message = new Message();
        message.what = DISCONNECT_SUCCESS;
        mHandler.sendMessage(message);

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////  管理已有连接、收发数据  //////////////////////////////////

    /**
     * 管理已建立的连接，收发数据
     * @param bluetoothSocket   已建立的连接
     */
    public void managerConnectSendReceiveData(BluetoothSocket bluetoothSocket){
        //管理已有连接
        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
        connectedThread.setOnSendReceiveDataListener(new ConnectedThread.OnSendReceiveDataListener() {
            @Override
            public void onSendDataSuccess(byte[] data) {
                Log.w(TAG,"发送数据成功,长度" + data.length + "->" + bytes2HexString(data,data.length));
                Message message = new Message();
                message.what = SEND_SUCCESS;
                message.obj = "发送数据成功,长度" + data.length + "->" + bytes2HexString(data,data.length);
                mHandler.sendMessage(message);
            }

            @Override
            public void onSendDataError(byte[] data,String errorMsg) {
                Log.e(TAG,"发送数据出错,长度" + data.length + "->" + bytes2HexString(data,data.length));
                Message message = new Message();
                message.what = SEND_FAILURE;
                message.obj = "发送数据出错,长度" + data.length + "->" + bytes2HexString(data,data.length);
                mHandler.sendMessage(message);
            }

            @Override
            public void onReceiveDataSuccess(byte[] buffer) {
                Log.w(TAG,"成功接收数据,长度" + buffer.length + "->" + bytes2HexString(buffer,buffer.length));
                Message message = new Message();
                message.what = RECEIVE_SUCCESS;
                message.obj = "成功接收数据,长度" + buffer.length + "->" + bytes2HexString(buffer,buffer.length);
                mHandler.sendMessage(message);
            }

            @Override
            public void onReceiveDataError(String errorMsg) {
                Log.e(TAG,"接收数据出错：" + errorMsg);
                Message message = new Message();
                message.what = RECEIVE_FAILURE;
                message.obj = "接收数据出错：" + errorMsg;
                mHandler.sendMessage(message);
            }
        });
    }

    /////////////////////////////////   发送数据  /////////////////////////////////////////////////

    /**
     * 发送数据
     * @param data      要发送的数据 字符串
     * @param isHex     是否是16进制字符串
     * @return   true 发送成功  false 发送失败
     */
    public boolean sendData(String data,boolean isHex){
        if(connectedThread == null){
            Log.e(TAG,"sendData:string -->connectedThread == null");
            return false;
        }
        if(data == null || data.length() == 0){
            Log.e(TAG,"sendData:string-->要发送的数据为空");
            return false;
        }

        if(isHex){  //是16进制字符串
            data.replace(" ","");  //取消空格
            //检查16进制数据是否合法
            if(data.length() % 2 != 0){
                //不合法，最后一位自动填充0
                String lasts = "0" + data.charAt(data.length() - 1);
                data = data.substring(0,data.length() - 2) + lasts;
            }
            Log.d(TAG,"sendData:string -->准备写入：" + data);  //加空格显示
            return connectedThread.write(hexString2Bytes(data));
        }

        //普通字符串
        Log.d(TAG,"sendData:string -->准备写入：" + data);
        return connectedThread.write(data.getBytes());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////   数据类型转换  //////////////////////////////////////////////
    /**
     * 字节数组-->16进制字符串
     * @param b   字节数组
     * @param length  字节数组长度
     * @return 16进制字符串 有空格类似“0A D5 CD 8F BD E5 F8”
     */
    public static String bytes2HexString(byte[] b, int length) {
        StringBuffer result = new StringBuffer();
        String hex;
        for (int i = 0; i < length; i++) {
            hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            result.append(hex.toUpperCase()).append(" ");
        }
        return result.toString();
    }

    /**
     * hexString2Bytes
     * 16进制字符串-->字节数组
     * @param src  16进制字符串
     * @return 字节数组
     */
    public static byte[] hexString2Bytes(String src) {
        int l = src.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            ret[i] = (byte) Integer
                    .valueOf(src.substring(i * 2, i * 2 + 2), 16).byteValue();
        }
        return ret;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 初始化蓝牙
     */
    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "当前手机设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            //手机设备支持蓝牙，判断蓝牙是否已开启
            if (bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "手机蓝牙已开启", Toast.LENGTH_SHORT).show();
            } else {
                //蓝牙没有打开，去打开蓝牙。推荐使用第二种打开蓝牙方式
                //第一种方式：直接打开手机蓝牙，没有任何提示
//                bluetoothAdapter.enable();  //BLUETOOTH_ADMIN权限
                //第二种方式：友好提示用户打开蓝牙
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
        }
    }

    /**
     * 初始化蓝牙广播
     */
    private void initBtBroadcast() {
        //注册广播接收
        btBroadcastReceiver = new BtBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //开始扫描
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//扫描结束
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);//搜索到设备
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //配对状态监听
        registerReceiver(btBroadcastReceiver,intentFilter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //注销广播接收
        unregisterReceiver(btBroadcastReceiver);
    }

    /**
     * 蓝牙广播接收器
     */
    private class BtBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_STARTED)) { //开启搜索
                Log.d(TAG,"开启搜索...");
                Message message = new Message();
                message.what = START_DISCOVERY;
                mHandler.sendMessage(message);

            } else if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {//完成搜素
                Log.d(TAG,"停止搜索...");
                Message message = new Message();
                message.what = STOP_DISCOVERY;
                mHandler.sendMessage(message);

            } else if (TextUtils.equals(action, BluetoothDevice.ACTION_FOUND)) {  //3.0搜索到设备
                //蓝牙设备
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //信号强度
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                Log.w(TAG, "扫描到设备：" + bluetoothDevice.getName() + "-->" + bluetoothDevice.getAddress());
                Message message = new Message();
                message.what = DISCOVERY_DEVICE;
                message.obj = bluetoothDevice;
                mHandler.sendMessage(message);

            }else if(TextUtils.equals(action,BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondSate = bluetoothDevice.getBondState();
                switch(bondSate) {
                    case BluetoothDevice.BOND_NONE:
                        Log.d(TAG, "已解除配对");
                        Message message1 = new Message();
                        message1.what = DEVICE_BOND_NONE;
                        mHandler.sendMessage(message1);
                        break;

                    case BluetoothDevice.BOND_BONDING:
                        Log.d(TAG, "正在配对...");
                        Message message2 = new Message();
                        message2.what = DEVICE_BONDING;
                        mHandler.sendMessage(message2);
                        break;

                    case BluetoothDevice.BOND_BONDED:
                        Log.d(TAG, "已配对");
                        Message message3 = new Message();
                        message3.what = DEVICE_BONDED;
                        mHandler.sendMessage(message3);
                        break;
                }
            }
        }
    }
}