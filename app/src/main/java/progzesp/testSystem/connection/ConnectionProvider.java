package progzesp.testSystem.connection;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import progzesp.testSystem.R;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


public class ConnectionProvider {

    private static final UUID APP_UUID = UUID.fromString("5fc104c0-0fe6-448d-8a7d-06a9f16cef94");

    private BluetoothAdapter adapter;
    private Context context;
    private NewConnectionListener newConnectionListener;
    private ConnectionClient client;
    private ConnectionServer server;
    private DeviceFinder finder;


    public ConnectionProvider(Context context, BluetoothAdapter adapter) {
        super();
        this.context = context;
        this.adapter = adapter;
    }


    public synchronized void attemptConnection(final BluetoothDevice device, final int rssiG) {
        if (client != null) {
            return;
        }
        stopAcceptingConnections();
        stopFindingDevices();
        client = new ConnectionClient(device, APP_UUID, new NewConnectionListener() {
            @Override
            public void onNewConnection(BluetoothSocket socket, int rssi, int flag) {
                client = null;
                if (newConnectionListener != null) {
                    newConnectionListener.onNewConnection(socket, rssiG, 1); //flag 1 for incoming
                }
            }

        }, new FailureToConnectListener() {
            @Override
            public void onFailureToConnect() {
                client = null;
            }
        });
    }


    public synchronized void stopAttemptingConnection() {
        if (client != null) {
            client.terminate();
            client = null;
        }
    }


    public synchronized void acceptConnections() {
        if (server != null) {
            return;
        }
        stopAttemptingConnection();
        stopFindingDevices();
        server = new ConnectionServer(adapter, context, APP_UUID, new NewConnectionListener() {
            @Override
            public void onNewConnection(BluetoothSocket socket, int rssi, int flag) {
            server = null;
            if (newConnectionListener != null) {
                newConnectionListener.onNewConnection(socket, rssi, 0);  //flag 0 for accepting
            }
            }

        }, new FailureToConnectListener() {
            @Override
            public void onFailureToConnect() {
            server = null;
            }
        });
    }


    public synchronized void findDevices() {
        if (finder != null) {
            return;
        }
        stopAcceptingConnections();
        stopAttemptingConnection();
        closeDialog();
        finder = new DeviceFinder(adapter, context, new NewDeviceListener() {
            @Override
            public void onNewDevice(BluetoothDevice device, int rssi) {
                if (client == null) {
                    refreshDialog(device, rssi);
                }
            }
        }, new DiscoveryFinishedListener() {
            @Override
            public void onDiscoveryFinished() {
                Log.d("test", "finished discovery");
                finder.stopDiscovery();
                finder = null;
                synchronized (ConnectionProvider.this) {
                    dialogDevices.clear();
                }
            }
        });
    }


    public synchronized void stopFindingDevices() {
        if (finder != null) {
            finder.stopDiscovery();
            finder = null;
        }
    }


    public synchronized void stopAcceptingConnections() {
        if (server != null) {
            server.terminate();
            server = null;
        }
    }


    public synchronized void setNewConnectionListener(NewConnectionListener listener) {
        newConnectionListener = listener;
    }

    private AlertDialog deviceDialog;
    private List<BluetoothDevice> dialogDevices = new LinkedList<>();


    private synchronized void refreshDialog(BluetoothDevice device, final int rssi) {
        if (deviceDialog != null) {
            deviceDialog.dismiss();
        }
        dialogDevices.add(device);
        CharSequence[] deviceNames = new CharSequence[dialogDevices.size()];
        for (int i = 0; i < dialogDevices.size(); i++) {
            deviceNames[i] = dialogDevices.get(i).getName()+"  "+rssi;
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        deviceDialog = dialogBuilder.setTitle(context.getResources().getString(R.string.select_device))
                .setCancelable(false)
                .setItems(deviceNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothDevice device;
                synchronized (ConnectionProvider.this) {
                    device = dialogDevices.get(which);
                    deviceDialog = null;
                }
                attemptConnection(device, rssi);
            }
        }).create();
        deviceDialog.show();/*
        AlertDialog.Builder db = new AlertDialog.Builder(context);
        db.setTitle("test");
        db.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deviceDialog.dismiss();
            }
        });
        deviceDialog = db.create();
        deviceDialog.show();*/
    }


    private synchronized void closeDialog() {
        if (deviceDialog != null) {
            deviceDialog.dismiss();
            deviceDialog = null;
        }
        dialogDevices.clear();
    }

}
