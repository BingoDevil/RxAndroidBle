package cn.bingo.myapp.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;

import cn.bingo.myapp.R;
import cn.bingo.myapp.SampleApplication;
import cn.bingo.myapp.adapter.DiscoveryResultsAdapter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Bingo on 16/8/16
 */
public class DiscoveryServiceActivity extends Activity {

    public static final String EXTRA_NAME = "device_name";
    public static final String EXTRA_MAC_ADDRESS = "device_mac_address";

    private TextView current_mac;
    private Button connect;
    private RecyclerView recyclerView;
    private DiscoveryResultsAdapter adapter;
    private RxBleDevice bleDevice;

    private String macAddress;
    private String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery_service);
        deviceName = getIntent().getStringExtra(EXTRA_NAME);
        macAddress = getIntent().getStringExtra(EXTRA_MAC_ADDRESS);
        bleDevice = SampleApplication.getRxBleClient(this).getBleDevice(macAddress);
        initView();
        initClick();


        recyclerView.setHasFixedSize(true);
        LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerLayoutManager);
        adapter = new DiscoveryResultsAdapter(DiscoveryServiceActivity.this);
        recyclerView.setAdapter(adapter);

        adapter.setOnAdapterItemClickListener(new DiscoveryResultsAdapter.OnAdapterItemClickListener() {
            @Override
            public void onAdapterViewClick(View view) {
                final int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
                final DiscoveryResultsAdapter.AdapterItem itemAtPosition = adapter.getItem(childAdapterPosition);
                onAdapterItemClick(itemAtPosition);
            }
        });
    }

    /**
     * click
     *
     * @param item
     */
    private void onAdapterItemClick(DiscoveryResultsAdapter.AdapterItem item) {
        if (item.type == DiscoveryResultsAdapter.AdapterItem.CHARACTERISTIC) {
            final Intent intent = new Intent(this, CharacteristicOperationActivity.class);
            intent.putExtra(DiscoveryServiceActivity.EXTRA_NAME, deviceName);
            intent.putExtra(DiscoveryServiceActivity.EXTRA_MAC_ADDRESS, macAddress);
            intent.putExtra(CharacteristicOperationActivity.CHARACTERISTIC_1, item.uuid);
            startActivity(intent);
        } else {
            // noinspection ConstantConditions
            Snackbar.make(findViewById(android.R.id.content), R.string.not_clickable, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        current_mac = (TextView) findViewById(R.id.current_mac);
        connect = (Button) findViewById(R.id.connect);
        recyclerView = (RecyclerView) findViewById(R.id.scan_results);
        current_mac.setText(deviceName + "---" + macAddress);
    }

    private void initClick() {
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleDevice.establishConnection(DiscoveryServiceActivity.this, false)
                        .flatMap(new Func1<RxBleConnection, Observable<RxBleDeviceServices>>() {
                            @Override
                            public Observable<RxBleDeviceServices> call(RxBleConnection rxBleConnection) {
                                return rxBleConnection.discoverServices();
                            }
                        })
                        .first() // Disconnect automatically after discovery
                        // .compose(bindUntilEvent(PAUSE))
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnUnsubscribe(new Action0() {
                            @Override
                            public void call() {
                                updateUI();
                            }
                        })
                        .subscribe(new Action1<RxBleDeviceServices>() {
                            @Override
                            public void call(RxBleDeviceServices rxBleDeviceServices) {
                                adapter.swapScanResult(rxBleDeviceServices);
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                onConnectionFailure(throwable);
                            }
                        });
            }
        });
    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        // noinspection ConstantConditions
        Snackbar.make(findViewById(android.R.id.content), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void updateUI() {
        connect.setEnabled(!isConnected());
    }
}
