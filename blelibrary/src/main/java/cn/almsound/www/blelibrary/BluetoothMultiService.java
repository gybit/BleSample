package cn.almsound.www.blelibrary;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * @author alm
 * @date 2017/11/15
 */

@SuppressWarnings("SameParameterValue")
public class BluetoothMultiService extends Service {
    private static final String TAG = "BluetoothMultiService";

    private HashMap<String, BleConnectCallback> callbackHashMap = new HashMap<>();
    private HashMap<String, BluetoothGatt> gattCallbackHashMap = new HashMap<>();
    private BluetoothMultiServiceBinder bluetoothMultiServiceBinder;
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothGattCallback bleBluetoothMultiGattCallback = new BluetoothGattCallback() {


        /**
         * Callback triggered as result of {@link BluetoothGatt#setPreferredPhy}, or as a result of
         * remote device changing the PHY.
         *
         * @param gatt   GATT client
         * @param txPhy  the transmitter PHY in use. One of {@link BluetoothDevice#PHY_LE_1M},
         *               {@link BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
         * @param rxPhy  the receiver PHY in use. One of {@link BluetoothDevice#PHY_LE_1M},
         *               {@link BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
         * @param status Status of the PHY update operation.
         *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onPhyUpdate");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onPhyUpdate(gatt, txPhy, rxPhy);
            }
        }

        /**
         * Callback triggered as result of {@link BluetoothGatt#readPhy}
         *
         * @param gatt   GATT client
         * @param txPhy  the transmitter PHY in use. One of {@link BluetoothDevice#PHY_LE_1M},
         *               {@link BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
         * @param rxPhy  the receiver PHY in use. One of {@link BluetoothDevice#PHY_LE_1M},
         *               {@link BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}.
         * @param status Status of the PHY read operation.
         *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onPhyUpdate");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onPhyRead(gatt, txPhy, rxPhy);
            }
        }

        /**
         * BleConnectCallback indicating when GATT client has connected/disconnected to/from a remote
         * GATT server.
         *
         * @param gatt     GATT client
         * @param status   Status of the connect or disconnect operation.
         *                 {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         * @param newState Returns the new connection state. Can be one of
         *                 {@link BluetoothProfile#STATE_DISCONNECTED} or
         *                 {@link BluetoothProfile#STATE_CONNECTED}
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            String gattAddress;
            BleConnectCallback bleConnectCallback;


            switch (newState) {
                case BluetoothGatt.STATE_DISCONNECTED:
                    Tool.warnOut(TAG,gatt.getDevice().getAddress() + "STATE_DISCONNECTED");
                    gattAddress = gatt.getDevice().getAddress();
                    if (callbackHashMap.containsKey(gattAddress)) {
                        bleConnectCallback = callbackHashMap.get(gattAddress);
                        bleConnectCallback.onDisConnected(gatt);
                    }
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                    Tool.warnOut(TAG, gatt.getDevice().getAddress() + "STATE_CONNECTING");
                    gattAddress = gatt.getDevice().getAddress();
                    if (callbackHashMap.containsKey(gattAddress)) {
                        bleConnectCallback = callbackHashMap.get(gattAddress);
                        bleConnectCallback.onConnecting(gatt);
                    }
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    Tool.warnOut(TAG, gatt.getDevice().getAddress() + "STATE_CONNECTED");
                    gattAddress = gatt.getDevice().getAddress();
                    if (callbackHashMap.containsKey(gattAddress)) {
                        bleConnectCallback = callbackHashMap.get(gattAddress);
                        if (!gatt.discoverServices()) {
                            Tool.warnOut(TAG, "无法进行服务发现");
                            bleConnectCallback.onDiscoverServicesFailed(gatt);
                            return;
                        }
                        bleConnectCallback.onConnected(gatt);
                    }
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    Tool.warnOut(TAG, gatt.getDevice().getAddress() + "STATE_DISCONNECTING");
                    gattAddress = gatt.getDevice().getAddress();
                    if (callbackHashMap.containsKey(gattAddress)) {
                        bleConnectCallback = callbackHashMap.get(gattAddress);
                        bleConnectCallback.onDisconnecting(gatt);
                    }
                    break;
                default:
                    Tool.warnOut(TAG, gatt.getDevice().getAddress() + "other state");
                    break;
            }
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors
         * for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#discoverServices}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onServicesDiscovered");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onServicesDiscovered(gatt);
            }
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the associated
         *                       remote device.
         * @param status         {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onCharacteristicRead");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                byte[] values = characteristic.getValue();
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onCharacteristicRead(gatt, values);
            }
        }

        /**
         * Callback indicating the result of a characteristic write operation.
         * <p>
         * <p>If this callback is invoked while a reliable write transaction is
         * in progress, the value of the characteristic represents the value
         * reported by the remote device. An application should compare this
         * value to the desired value to be written. If the values don't match,
         * the application must abort the reliable write transaction.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#writeCharacteristic}
         * @param characteristic Characteristic that was written to the associated
         *                       remote device.
         * @param status         The result of the write operation
         *                       {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Tool.warnOut(TAG, "onCharacteristicWrite");
            String gattAddress =gatt.getDevice().getAddress() +  gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                byte[] values = characteristic.getValue();
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onCharacteristicWrite(gatt, values);
            }
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         *
         * @param gatt           GATT client the characteristic is associated with
         * @param characteristic Characteristic that has been updated as a result
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onCharacteristicWrite");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                byte[] values = characteristic.getValue();
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onReceivedNotification(gatt, values);
            }
        }

        /**
         * Callback reporting the result of a descriptor read operation.
         *
         * @param gatt       GATT client invoked {@link BluetoothGatt#readDescriptor}
         * @param descriptor Descriptor that was read from the associated
         *                   remote device.
         * @param status     {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onDescriptorRead");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                byte[] values = descriptor.getValue();
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onDescriptorRead(gatt, values);
            }
        }

        /**
         * Callback indicating the result of a descriptor write operation.
         *
         * @param gatt       GATT client invoked {@link BluetoothGatt#writeDescriptor}
         * @param descriptor Descriptor that was writte to the associated
         *                   remote device.
         * @param status     The result of the write operation
         *                   {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onDescriptorWrite");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                byte[] values = descriptor.getValue();
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onDescriptorWrite(gatt, values);
            }
        }

        /**
         * Callback invoked when a reliable write transaction has been completed.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#executeReliableWrite}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the reliable write
         */
        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onReliableWriteCompleted");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onReliableWriteCompleted(gatt);
            }
        }

        /**
         * Callback reporting the RSSI for a remote device connection.
         * <p>
         * This callback is triggered in response to the
         * {@link BluetoothGatt#readRemoteRssi} function.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#readRemoteRssi}
         * @param rssi   The RSSI value for the remote device
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the RSSI was read successfully
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onReadRemoteRssi");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onReadRemoteRssi(gatt, rssi);
            }
        }

        /**
         * Callback indicating the MTU for a given device connection has changed.
         * <p>
         * This callback is triggered in response to the
         * {@link BluetoothGatt#requestMtu} function, or in response to a connection
         * event.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#requestMtu}
         * @param mtu    The new MTU size
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the MTU has been changed successfully
         */
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Tool.warnOut(TAG, gatt.getDevice().getAddress() + "onMtuChanged");
            String gattAddress = gatt.getDevice().getAddress();
            if (callbackHashMap.containsKey(gattAddress)) {
                BleConnectCallback bleConnectCallback = callbackHashMap.get(gattAddress);
                bleConnectCallback.onMtuChanged(gatt, mtu);
            }
        }
    };

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothMultiServiceBinder = new BluetoothMultiServiceBinder(this);
    }

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * {@link IBinder} is usually for a complex interface
     * that has been <a href="{@docRoot}guide/components/aidl.html">described using
     * aidl</a>.
     * <p>
     * <p><em>Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process</em>.  More information about the main thread can be found in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
     * Threads</a>.</p>
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return bluetoothMultiServiceBinder;
    }

    public boolean initialize() {
        //使用单例模式获取蓝牙管理器
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager == null) {
            Tool.warnOut("BluetoothLeService", "get bluetoothManager failed!");
            return false;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Tool.warnOut("BluetoothLeService", "get bluetoothAdapter failed!");
            return false;
        }
        return true;
    }

    public boolean connectDevice(String address, BleConnectCallback bleConnectCallback, boolean autoConnect) {
        if (bluetoothAdapter == null) {
            return false;
        }
        //检查蓝牙地址是否符合规范
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }
        BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(address);
        if (remoteDevice == null) {
            return false;
        }

        BluetoothGatt bluetoothGatt = remoteDevice.connectGatt(this, autoConnect, bleBluetoothMultiGattCallback);
        if (autoConnect) {
            if (!callbackHashMap.containsKey(address)) {
                callbackHashMap.put(address, bleConnectCallback);
            }
            if (!gattCallbackHashMap.containsKey(address)) {
                gattCallbackHashMap.put(address, bluetoothGatt);
            }
            return true;
        }

        if (bluetoothGatt == null) {
            return false;
        }

        if (!callbackHashMap.containsKey(address)) {
            callbackHashMap.put(address, bleConnectCallback);
        }
        if (!gattCallbackHashMap.containsKey(address)) {
            gattCallbackHashMap.put(address, bluetoothGatt);
        }
        return bluetoothGatt.connect();
    }

    public boolean close(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }
        if (!gattCallbackHashMap.containsKey(address)) {
            return false;
        }
        BluetoothGatt gatt = gattCallbackHashMap.get(address);
        gatt.close();
        gattCallbackHashMap.remove(address);
        callbackHashMap.remove(address);
        return true;
    }

    public void closeAll() {
        for (Map.Entry<String, BluetoothGatt> entry : gattCallbackHashMap.entrySet()) {
            BluetoothGatt gatt = entry.getValue();
            gatt.disconnect();
            gatt.close();
        }
        for (Map.Entry<String, BleConnectCallback> entry : callbackHashMap.entrySet()) {
            BleConnectCallback bleConnectCallback = entry.getValue();
            bleConnectCallback.onGattClosed();
        }
        callbackHashMap.clear();
        gattCallbackHashMap.clear();
    }

    public boolean disconnect(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }
        if (!gattCallbackHashMap.containsKey(address)) {
            return false;
        }
        BluetoothGatt gatt = gattCallbackHashMap.get(address);
        gatt.disconnect();
        return true;
    }

    public boolean disconnectAll() {
        for (Map.Entry<String, BluetoothGatt> entry : gattCallbackHashMap.entrySet()) {
            BluetoothGatt gatt = entry.getValue();
            gatt.disconnect();
        }
        return true;
    }

    public boolean reConnect(String address) {
        return reConnect(address, false);
    }

    public boolean reConnect(String address, boolean autoConnect) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }
        if (!callbackHashMap.containsKey(address)) {
            return false;
        }
        if (gattCallbackHashMap.containsKey(address)) {
            BluetoothGatt gatt = gattCallbackHashMap.get(address);
            return gatt.connect();
        }
        BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(address);
        if (remoteDevice == null) {
            return false;
        }
        BluetoothGatt bluetoothGatt = remoteDevice.connectGatt(this, autoConnect, bleBluetoothMultiGattCallback);
        if (autoConnect){
            if (!gattCallbackHashMap.containsKey(address)) {
                gattCallbackHashMap.put(address, bluetoothGatt);
            }
            return true;
        }
        if (bluetoothGatt == null) {
            return false;
        }
        if (!gattCallbackHashMap.containsKey(address)) {
            gattCallbackHashMap.put(address, bluetoothGatt);
        }
        return bluetoothGatt.connect();
    }

    public List<BluetoothGattService> getServices(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return null;
        }
        if (gattCallbackHashMap.containsKey(address)) {
            BluetoothGatt gatt = gattCallbackHashMap.get(address);
            return gatt.getServices();
        }
        return null;
    }

    public BluetoothGattService getService(String address, UUID uuid) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return null;
        }
        if (uuid == null) {
            return null;
        }
        if (gattCallbackHashMap.containsKey(address)) {
            BluetoothGatt gatt = gattCallbackHashMap.get(address);
            return gatt.getService(uuid);
        }
        return null;
    }

    public boolean refreshGattCache(String address) {

        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }

        if (!gattCallbackHashMap.containsKey(address)) {
            return false;
        }

        BluetoothGatt bluetoothGatt = gattCallbackHashMap.get(address);

        if (bluetoothGatt == null) {
            return false;
        }

        try {
            Method refresh = bluetoothGatt.getClass().getMethod("refresh");
            if (refresh != null) {
                return (boolean) refresh.invoke(bluetoothGatt);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void refreshAllGattCache() {
        for (Map.Entry<String, BluetoothGatt> entries : gattCallbackHashMap.entrySet()) {
            String key = entries.getKey();
            refreshGattCache(key);
        }
    }

    public boolean writeData(String address, String serviceUUID, String characteristicUUID, byte[] values) {

        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }

        if (!gattCallbackHashMap.containsKey(address)) {
            return false;
        }

        BluetoothGatt bluetoothGatt = gattCallbackHashMap.get(address);

        if (serviceUUID == null || characteristicUUID == null || values == null || bluetoothGatt == null) {
            return false;
        }
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
        if (service == null) {
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
        if (characteristic == null) {
            return false;
        }


        if (!characteristic.setValue(values)) {
            return false;
        }

        Tool.warnOut(TAG, "values = " + Tool.bytesToHexStr(values));

        return bluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean readData(String address, String serviceUUID, String characteristicUUID) {

        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }

        if (!gattCallbackHashMap.containsKey(address)) {
            return false;
        }

        BluetoothGatt bluetoothGatt = gattCallbackHashMap.get(address);

        if (serviceUUID == null || characteristicUUID == null || bluetoothGatt == null) {
            return false;
        }
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
        if (service == null) {
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));

        return characteristic != null && bluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean openNotification(String address, String serviceUUID, String characteristicUUID) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }

        if (!gattCallbackHashMap.containsKey(address)) {
            return false;
        }

        BluetoothGatt bluetoothGatt = gattCallbackHashMap.get(address);
        if (serviceUUID == null || characteristicUUID == null) {
            return false;
        }
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(serviceUUID));
        if (bluetoothGattService == null) {
            return false;
        }
        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(characteristicUUID));
        if (!bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)) {
            return false;
        }
        BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BleConstants.CLIENT_CHARACTERISTIC_CONFIG));
        if (bluetoothGattDescriptor == null) {
            return false;
        } else {
            bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        }
        return bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
    }

    public boolean closeNotification(String address, String serviceUUID, String characteristicUUID) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            return false;
        }

        if (!gattCallbackHashMap.containsKey(address)) {
            return false;
        }

        BluetoothGatt bluetoothGatt = gattCallbackHashMap.get(address);
        if (serviceUUID == null || characteristicUUID == null) {
            return false;
        }
        BluetoothGattService bluetoothGattService = bluetoothGatt.getService(UUID.fromString(serviceUUID));
        if (bluetoothGattService == null) {
            return false;
        }
        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(characteristicUUID));
        if (!bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)) {
            return false;
        }
        BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BleConstants.CLIENT_CHARACTERISTIC_CONFIG));
        if (bluetoothGattDescriptor == null) {
            return false;
        } else {
            bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        return bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
    }

    boolean isConnected(String address) {
        return BluetoothAdapter.checkBluetoothAddress(address) && gattCallbackHashMap.containsKey(address);

    }
}
