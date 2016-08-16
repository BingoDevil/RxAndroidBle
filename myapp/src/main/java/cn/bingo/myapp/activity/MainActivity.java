package cn.bingo.myapp.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleScanResult;
import com.polidea.rxandroidble.exceptions.BleScanException;

import cn.bingo.myapp.R;
import cn.bingo.myapp.SampleApplication;
import cn.bingo.myapp.adapter.ScanResultsAdapter;
import cn.bingo.myapp.utils.MyPixle;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;

public class MainActivity extends Activity {

    private RxBleClient rxBleClient;
    private Subscription subscription;
    private EditText scan_text;
    private Button scan_toggle_btn;
    private RecyclerView recyclerView;
    private ScanResultsAdapter resultsAdapter;
    private static final int REQUEST_FINE_LOCATION = 0;

    /**
     * android 6.0必须动态申请权限
     */
    private void MayRequestLocation() {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要 向用户解释，为什么要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    // Toast.makeText(context,R.string.ble_need, 1).show();
                }
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_FINE_LOCATION);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rxBleClient = SampleApplication.getRxBleClient(MainActivity.this);
        MayRequestLocation();
        initView();
        initClick();

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerLayoutManager);
        resultsAdapter = new ScanResultsAdapter();
        recyclerView.setAdapter(resultsAdapter);
        resultsAdapter.setOnAdapterItemClickListener(new ScanResultsAdapter.OnAdapterItemClickListener() {
            @Override
            public void onAdapterViewClick(View view) {
                final int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
                final RxBleScanResult itemAtPosition = resultsAdapter.getItemAtPosition(childAdapterPosition);
                onAdapterItemClick(itemAtPosition);
            }
        });
    }

    /**
     * item click
     *
     * @param scanResults
     */
    private void onAdapterItemClick(RxBleScanResult scanResults) {
        final String macAddress = scanResults.getBleDevice().getMacAddress();
        final String deviceName = scanResults.getBleDevice().getName();
        Toast.makeText(MainActivity.this, macAddress + "--" + deviceName, Toast.LENGTH_SHORT).show();
        final Intent intent = new Intent(this, DiscoveryServiceActivity.class);
        intent.putExtra(DiscoveryServiceActivity.EXTRA_NAME, deviceName);
        intent.putExtra(DiscoveryServiceActivity.EXTRA_MAC_ADDRESS, macAddress);
        startActivity(intent);
    }

    private boolean isScanning() {
        return subscription != null;
    }

    private void handleBleScanException(BleScanException bleScanException) {
        switch (bleScanException.getReason()) {
            case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                Toast.makeText(MainActivity.this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                break;
            case BleScanException.BLUETOOTH_DISABLED:
                Toast.makeText(MainActivity.this, "Enable bluetooth and try again", Toast.LENGTH_SHORT).show();
                break;
            case BleScanException.LOCATION_PERMISSION_MISSING:
                Toast.makeText(MainActivity.this, "On Android 6.0 location permission is required. Implement Runtime Permissions", Toast.LENGTH_SHORT).show();
                break;
            case BleScanException.LOCATION_SERVICES_DISABLED:
                Toast.makeText(MainActivity.this, "Location services needs to be enabled on Android 6.0", Toast.LENGTH_SHORT).show();
                break;
            case BleScanException.BLUETOOTH_CANNOT_START:
            default:
                Toast.makeText(MainActivity.this, "Unable to start scanning", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void initClick() {
        scan_toggle_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScanning()) {
                    subscription.unsubscribe();
                } else {
                    subscription = rxBleClient
                            .scanBleDevices()
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnUnsubscribe(new Action0() {
                                @Override
                                public void call() {
                                    // 清空缓存,释放内存
                                    subscription = null;
                                    resultsAdapter.clearScanResults();
                                    scan_toggle_btn.setText(isScanning() ? R.string.stop_scan : R.string.start_scan);
                                }
                            })
                            .subscribe(new Action1<RxBleScanResult>() {
                                @Override
                                public void call(RxBleScanResult rxBleScanResult) {
                                    // has device return
                                    resultsAdapter.addScanResult(rxBleScanResult);
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    // handle error
                                    if (throwable instanceof BleScanException) {
                                        handleBleScanException((BleScanException) throwable);
                                    }
                                }
                            });
                }
                scan_toggle_btn.setText(isScanning() ? R.string.stop_scan : R.string.start_scan);
            }
        });
    }

    private void initView() {
        scan_text = (EditText) findViewById(R.id.scan_text);
        // 屏蔽键盘
        scan_text.setInputType(InputType.TYPE_NULL);
        scan_toggle_btn = (Button) findViewById(R.id.scan_toggle_btn);
        recyclerView = (RecyclerView) findViewById(R.id.scan_results);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isScanning()) {
            /*
             * Stop scanning in onPause callback. You can use rxlifecycle for convenience. Examples are provided later.
             */
            subscription.unsubscribe();
        }
    }

}
