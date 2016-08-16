package cn.bingo.myapp.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;

import java.util.UUID;

import cn.bingo.myapp.R;
import cn.bingo.myapp.SampleApplication;
import cn.bingo.myapp.utils.ByteKeyBoard;
import cn.bingo.myapp.utils.HexString;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * Created by Bingo on 16/8/16
 */
public class CharacteristicOperationActivity extends Activity implements View.OnClickListener {

    public static final String CHARACTERISTIC_1 = "characteristic_1";
    public static final String CHARACTERISTIC_2 = "00008002-0000-1000-8000-00805f9b34fb";

    private TextView title;
    private Button connectButton;
    private TextView readOutputView;
    private TextView readHexOutputView;
    private TextView writeInput;
    private Button readButton;
    private Button writeButton;
    private Button notifyButton;
    private LinearLayout main;
    private Button writeButton_2;

    private UUID characteristicUuid_1;
    private UUID characteristicUuid_2;
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;
    private RxBleDevice bleDevice;
    private String deviceName;
    private String macAddress;

    private PopupWindow PopupWindow;
    private View putView;

    private String sendText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characteristic_operation);
        deviceName = getIntent().getStringExtra(DiscoveryServiceActivity.EXTRA_NAME);
        macAddress = getIntent().getStringExtra(DiscoveryServiceActivity.EXTRA_MAC_ADDRESS);
        characteristicUuid_1 = (UUID) getIntent().getSerializableExtra(CHARACTERISTIC_1);
        characteristicUuid_2 = UUID.fromString(CHARACTERISTIC_2);

        initView();
        putView = getLayoutInflater().inflate(R.layout.shares_keyboard, null);

        bleDevice = SampleApplication.getRxBleClient(this).getBleDevice(macAddress);
        connectionObservable = bleDevice
                .establishConnection(this, false)
                .takeUntil(disconnectTriggerSubject)
                // .compose(bindUntilEvent(PAUSE))
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        clearSubscription();
                    }
                })
                .compose(new ConnectionSharingAdapter());
    }

    private void initView() {
        main = (LinearLayout) findViewById(R.id.main);
        title = (TextView) findViewById(R.id.title);
        title.setText(deviceName + "---" + macAddress);

        connectButton = (Button) findViewById(R.id.connect);
        readOutputView = (TextView) findViewById(R.id.read_output);
        readHexOutputView = (TextView) findViewById(R.id.read_hex_output);

        writeInput = (TextView) findViewById(R.id.write_input);
        // 屏蔽键盘
        writeInput.setInputType(InputType.TYPE_NULL);

        readButton = (Button) findViewById(R.id.read);
        writeButton = (Button) findViewById(R.id.write);
        writeButton_2 = (Button) findViewById(R.id.write_2);
        notifyButton = (Button) findViewById(R.id.notify);

        connectButton.setOnClickListener(this);
        writeInput.setOnClickListener(this);
        readButton.setOnClickListener(this);
        writeButton.setOnClickListener(this);
        writeButton_2.setOnClickListener(this);
        notifyButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.write_input:
                ByteKeyBoard.ShowKeyboard(PopupWindow, putView, CharacteristicOperationActivity.this, title,writeInput);
                break;
            case R.id.connect:
                onConnectToggleClick();
                break;
            case R.id.read:
                onReadClick();
                break;
            case R.id.write:
                onWriteClick();
                break;
            case R.id.write_2:
                onWriteClickTo_8002();
                break;
            case R.id.notify:
                onNotifyClick();
                break;
        }
    }

    public void onReadClick() {
        Log.e(getClass().getSimpleName(), "onReadClick");
        if (isConnected()) {
            connectionObservable.flatMap(new Func1<RxBleConnection, Observable<byte[]>>() {
                @Override
                public Observable<byte[]> call(RxBleConnection rxBleConnection) {
                    return rxBleConnection.readCharacteristic(characteristicUuid_2);
                }
            }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<byte[]>() {
                @Override
                public void call(byte[] bytes) {
                    readOutputView.setText(new String(bytes));
                    readHexOutputView.setText(HexString.bytesToHex(bytes));
                    writeInput.setText(HexString.bytesToHex(bytes));
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    onReadFailure(throwable);
                }
            });
        }
    }

    public void onWriteClick() {
        Log.e(getClass().getSimpleName(), "onWriteClick");
        if (isConnected()) {
            connectionObservable
                    .flatMap(new Func1<RxBleConnection, Observable<byte[]>>() {
                        @Override
                        public Observable<byte[]> call(RxBleConnection rxBleConnection) {
                            return rxBleConnection.writeCharacteristic(characteristicUuid_1, getInputBytes());
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<byte[]>() {
                        @Override
                        public void call(byte[] bytes) {
                            onWriteSuccess();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            onWriteFailure(throwable);
                        }
                    });
        }
    }

    public void onWriteClickTo_8002() {
        Log.e(getClass().getSimpleName(), "onWriteClickTo_8002");
        if (isConnected()) {
            connectionObservable
                    .flatMap(new Func1<RxBleConnection, Observable<byte[]>>() {
                        @Override
                        public Observable<byte[]> call(RxBleConnection rxBleConnection) {
                            return rxBleConnection.writeCharacteristic(characteristicUuid_2, getInputBytes());
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<byte[]>() {
                        @Override
                        public void call(byte[] bytes) {
                            onWriteSuccess();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            onWriteFailure(throwable);
                        }
                    });
        }
    }

    public void onNotifyClick() {
        Log.e(getClass().getSimpleName(), "onNotifyClick");
        if (isConnected()) {
            connectionObservable
                    .flatMap(new Func1<RxBleConnection, Observable<Observable<byte[]>>>() {
                        @Override
                        public Observable<Observable<byte[]>> call(RxBleConnection rxBleConnection) {
                            return rxBleConnection.setupNotification(characteristicUuid_2);
                        }
                    }).doOnNext(new Action1<Observable<byte[]>>() {
                @Override
                public void call(Observable<byte[]> observable) {
                    notificationHasBeenSetUp();
                }
            }).flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                @Override
                public Observable<byte[]> call(Observable<byte[]> notificationObservable) {
                    return notificationObservable;
                }
            }).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<byte[]>() {
                        @Override
                        public void call(byte[] bytes) {
                            onNotificationReceived(bytes);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            onNotificationSetupFailure(throwable);
                        }
                    });
        }
    }

    public void onConnectToggleClick() {
        Log.e(getClass().getSimpleName(), "onConnectToggleClick--" + isConnected());
        if (connectionObservable == null) {
            return;
        }
        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionObservable.subscribe(new Action1<RxBleConnection>() {
                @Override
                public void call(RxBleConnection rxBleConnection) {
                    Log.i(getClass().getSimpleName(), "Hey, connection has been established!");
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    onConnectionFailure(throwable);
                }
            });
        }
        updateUI();
    }

    /**
     * is connected
     *
     * @return connect status
     */
    private boolean isConnected() {
        Log.w(getClass().getSimpleName(), bleDevice.getConnectionState() + "----isConnected----" + RxBleConnection.RxBleConnectionState.CONNECTED);
        if (bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTING
                || bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

    private void onConnectionFailure(Throwable throwable) {
        // noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Connection error: " + throwable.toString());
    }

    private void onReadFailure(Throwable throwable) {
        // noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Read error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Read error: " + throwable.toString());
    }

    private void onWriteSuccess() {
        // noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write success", Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Write succes");
    }

    private void onWriteFailure(Throwable throwable) {
        // noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Write error" + throwable.toString());
    }

    private void onNotificationReceived(byte[] bytes) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Change: " + HexString.bytesToHex(bytes), Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Change" + HexString.bytesToHex(bytes));
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        // noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Notifications error: " + throwable);
    }

    private void notificationHasBeenSetUp() {
        // noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications has been set up", Snackbar.LENGTH_LONG).show();
        Log.w(getClass().getSimpleName(), "Notifications has been set up");
    }

    private void clearSubscription() {
        connectionObservable = null;
        updateUI();
    }

    private void triggerDisconnect() {
        disconnectTriggerSubject.onNext(null);
    }

    private void updateUI() {
        connectButton.setText(isConnected() ? getString(R.string.disconnect) : getString(R.string.connect));
        readButton.setEnabled(isConnected());
        writeButton.setEnabled(isConnected());
        writeButton_2.setEnabled(isConnected());
        notifyButton.setEnabled(isConnected());
    }

    private byte[] getInputBytes() {
        return HexString.hexToBytes(writeInput.getText().toString());
    }
}
