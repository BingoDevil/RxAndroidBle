package com.polidea.rxandroidble.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble.sample.example2_connection.ConnectionExampleActivity;
import com.polidea.rxandroidble.sample.example3_discovery.ServiceDiscoveryExampleActivity;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_MAC_ADDRESS = "extra_mac_address";
    @Bind(R.id.address)
    TextView address;
    @Bind(R.id.connect)
    Button connect;
    @Bind(R.id.discovery)
    Button discovery;
    private String macAddress;
    private String deviceName;

    @OnClick(R.id.connect)
    public void onConnectClick() {
        final Intent intent = new Intent(this, ConnectionExampleActivity.class);
        intent.putExtra(EXTRA_MAC_ADDRESS, macAddress);
        startActivity(intent);
    }

    @OnClick(R.id.discovery)
    public void onDiscoveryClick() {
        final Intent intent = new Intent(this, ServiceDiscoveryExampleActivity.class);
        intent.putExtra(EXTRA_MAC_ADDRESS, macAddress);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);
        deviceName = getIntent().getStringExtra(EXTRA_NAME);
        macAddress = getIntent().getStringExtra(EXTRA_MAC_ADDRESS);
        //noinspection ConstantConditions
        address.setText(deviceName);
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));
    }
}
