package yc.bluetooth.androidbt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索到的设备列表适配器
 */
public class LVDevicesAdapter extends BaseAdapter {

    private Context context;
    public static List<BluetoothDevice> list;

    public LVDevicesAdapter(Context context) {
        this.context = context;
        list = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return list == null ?  0 : list.size();
    }

    @Override
    public Object getItem(int i) {
        if(list == null){
            return null;
        }
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DeviceViewHolder viewHolder;
        if(view == null){
            view = LayoutInflater.from(context).inflate(R.layout.layout_lv_devices_item,null);
            viewHolder = new DeviceViewHolder();
            viewHolder.tvDeviceName = view.findViewById(R.id.tv_device_name);
            viewHolder.tvDeviceAddress = view.findViewById(R.id.tv_device_address);
            view.setTag(viewHolder);
        }else{
            viewHolder = (DeviceViewHolder) view.getTag();
        }

        if(list.get(i).getName() == null){
            viewHolder.tvDeviceName.setText("NULL");
        }else{
            viewHolder.tvDeviceName.setText(list.get(i).getName());
        }

        viewHolder.tvDeviceAddress.setText(list.get(i).getAddress());

        return view;
    }

    /**
     * 初始化所有设备列表
     * @param bluetoothDevices
     */
    public void addAllDevice(List<BluetoothDevice> bluetoothDevices){
        if(list != null){
            list.clear();
        }
        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            list.add(bluetoothDevice);
        }
        notifyDataSetChanged();
    }

    /**
     * 添加列表子项
     * @param bluetoothDevice
     */
    public void addDevice(BluetoothDevice bluetoothDevice){
        if(list == null){
            return;
        }
        if(!list.contains(bluetoothDevice)){
            list.add(bluetoothDevice);
        }
        notifyDataSetChanged();   //刷新
    }

    /**
     * 清空列表
     */
    public void clear(){
        if(list != null){
            list.clear();
        }
        notifyDataSetChanged(); //刷新
    }

    class DeviceViewHolder {

        TextView tvDeviceName;
        TextView tvDeviceAddress;
    }

}
