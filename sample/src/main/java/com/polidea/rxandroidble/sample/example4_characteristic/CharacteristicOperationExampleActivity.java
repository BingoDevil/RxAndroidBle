package com.polidea.rxandroidble.sample.example4_characteristic;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.sample.DeviceActivity;
import com.polidea.rxandroidble.sample.R;
import com.polidea.rxandroidble.sample.SampleApplication;
import com.polidea.rxandroidble.sample.util.HexString;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import static com.trello.rxlifecycle.ActivityEvent.PAUSE;

public class CharacteristicOperationExampleActivity extends RxAppCompatActivity {

    public static final String EXTRA_CHARACTERISTIC_UUID = "extra_uuid";
    @Bind(R.id.connect)
    Button connectButton;
    @Bind(R.id.read_output)
    TextView readOutputView;
    @Bind(R.id.read_hex_output)
    TextView readHexOutputView;
    @Bind(R.id.write_input)
    TextView writeInput;
    @Bind(R.id.read)
    Button readButton;
    @Bind(R.id.write)
    Button writeButton;
    @Bind(R.id.notify)
    Button notifyButton;
    @Bind(R.id.uuid)
    TextView uuid;
    @Bind(R.id.main)
    LinearLayout main;
    private UUID characteristicUuid_1;
    private UUID characteristicUuid_2;
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;
    private RxBleDevice bleDevice;

    @OnClick(R.id.read)
    public void onReadClick() {
        Log.e(getClass().getSimpleName(), "onReadClick");
        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUuid_2))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bytes -> {
                        readOutputView.setText(new String(bytes));
                        readHexOutputView.setText(HexString.bytesToHex(bytes));
                        writeInput.setText(HexString.bytesToHex(bytes));
                    }, this::onReadFailure);
        }
    }

    @OnClick(R.id.write)
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
                    }, this::onWriteFailure);
        }
    }

    @OnClick(R.id.notify)
    public void onNotifyClick() {
        Log.e(getClass().getSimpleName(), "onNotifyClick");
        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(characteristicUuid_2))
                    .doOnNext(notificationObservable -> runOnUiThread(this::notificationHasBeenSetUp))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example4);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        characteristicUuid_1 = (UUID) getIntent().getSerializableExtra(EXTRA_CHARACTERISTIC_UUID);
        characteristicUuid_2 = UUID.fromString("00008002-0000-1000-8000-00805f9b34fb");
        uuid.setText(characteristicUuid_1.toString());
        bleDevice = SampleApplication.getRxBleClient(this).getBleDevice(macAddress);
        connectionObservable = bleDevice
                .establishConnection(this, false)
                .takeUntil(disconnectTriggerSubject)
                .compose(bindUntilEvent(PAUSE))
                .doOnUnsubscribe(this::clearSubscription)
                .compose(new ConnectionSharingAdapter());
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));
    }

    @OnClick(R.id.connect)
    public void onConnectToggleClick() {
        Log.e(getClass().getSimpleName(), "onConnectToggleClick--" + isConnected());
        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionObservable.subscribe(new Action1<RxBleConnection>() {
                @Override
                public void call(RxBleConnection rxBleConnection) {
                    Log.i(getClass().getSimpleName(), "Hey, connection has been established!");
                }
            }, this::onConnectionFailure);
        }
        updateUI();
    }

    private boolean isConnected() {
        Log.w(getClass().getSimpleName(), bleDevice.getConnectionState() + "----isConnected----" + RxBleConnection.RxBleConnectionState.CONNECTED);
        if (bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTING
                || bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED) {
            return true;
        } else {
            return false;
        }
        // return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Connection error: " + throwable.toString());
    }

    private void onReadFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Read error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Read error: " + throwable.toString());
    }

    private void onWriteSuccess() {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write success", Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Write succes");
    }

    private void onWriteFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Write error" + throwable.toString());
    }

    private void onNotificationReceived(byte[] bytes) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Change: " + HexString.bytesToHex(bytes), Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Change" + HexString.bytesToHex(bytes));
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications error: " + throwable, Snackbar.LENGTH_SHORT).show();
        Log.w(getClass().getSimpleName(), "Notifications error: " + throwable);
    }

    private void notificationHasBeenSetUp() {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications has been set up", Snackbar.LENGTH_SHORT).show();
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
        notifyButton.setEnabled(isConnected());
    }

    private byte[] getInputBytes() {
        return HexString.hexToBytes(writeInput.getText().toString());
    }
}
