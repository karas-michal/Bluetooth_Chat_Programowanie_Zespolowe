package progzesp.btchat.connection;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import progzesp.btchat.communication.LostConnectionListener;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Krzysztof on 2016-11-15.
 */
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


    public void attemptConnection(BluetoothDevice device) {
        if (client != null) {
            return;
        }
        stopAcceptingConnections();
        stopFindingDevices();
        client = new ConnectionClient(device, APP_UUID, new NewConnectionListener() {
            @Override
            public void onNewConnection(BluetoothSocket socket) {
                client = null;
                if (newConnectionListener != null) {
                    newConnectionListener.onNewConnection(socket);
                }
            }
        }, new FailureToConnectListener() {
            @Override
            public void onFailureToConnect() {
                client = null;
            }
        });
    }


    public void stopAttemptingConnection() {
        if (client != null) {
            client.terminate();
            client = null;
        }
    }


    public void acceptConnections() {
        if (server != null) {
            return;
        }
        stopAttemptingConnection();
        stopFindingDevices();
        server = new ConnectionServer(adapter, context, APP_UUID, new NewConnectionListener() {
            @Override
            public void onNewConnection(BluetoothSocket socket) {
            server = null;
            if (newConnectionListener != null) {
                newConnectionListener.onNewConnection(socket);
            }
            }
        }, new FailureToConnectListener() {
            @Override
            public void onFailureToConnect() {
            server = null;
            }
        });
    }


    public void findDevices() {
        if (finder != null) {
            return;
        }
        stopAcceptingConnections();
        stopAttemptingConnection();
        closeDialog();
        finder = new DeviceFinder(adapter, context, new NewDeviceListener() {
            @Override
            public void onNewDevice(BluetoothDevice device) {
                finder = null;
                if (client == null) {
                    refreshDialog(device);
                }
            }
        }, new DiscoveryFinishedListener() {
            @Override
            public void onDiscoveryFinished() {
                synchronized (ConnectionProvider.this) {
                    dialogDevices.clear();
                }
            }
        });
    }


    public void stopFindingDevices() {
        if (finder != null) {
            finder.stopDiscovery();
            finder = null;
        }
    }


    public void stopAcceptingConnections() {
        if (server != null) {
            server.terminate();
            server = null;
        }
    }


    public void setNewConnectionListener(NewConnectionListener listener) {
        newConnectionListener = listener;
    }


    // TODO: REFACTOR

    private AlertDialog deviceDialog;
    private List<BluetoothDevice> dialogDevices = new LinkedList<>();


    private synchronized void refreshDialog(BluetoothDevice device) {
        if (deviceDialog != null) {
            deviceDialog.dismiss();
        }
        dialogDevices.add(device);
        CharSequence[] deviceNames = new CharSequence[dialogDevices.size()];
        for (int i = 0; i < dialogDevices.size(); i++) {
            deviceNames[i] = dialogDevices.get(i).getName();
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        deviceDialog = dialogBuilder.setTitle("Select device").setItems(deviceNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothDevice device;
                synchronized (ConnectionProvider.this) {
                    device = dialogDevices.get(which);
                    dialogDevices.clear();
                    deviceDialog = null;
                }
                attemptConnection(device);
            }
        }).create();
        deviceDialog.show();
    }


    private synchronized void closeDialog() {
        if (deviceDialog != null) {
            deviceDialog.dismiss();
            deviceDialog = null;
        }
        dialogDevices.clear();
    }

}
