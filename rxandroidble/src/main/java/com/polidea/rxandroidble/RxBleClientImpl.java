package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleDeviceProvider;
import com.polidea.rxandroidble.internal.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScan;
import com.polidea.rxandroidble.internal.radio.RxBleRadioImpl;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;
import com.polidea.rxandroidble.internal.util.LocationServicesStatus;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.internal.util.UUIDUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import rx.Observable;

class RxBleClientImpl extends RxBleClient {

    private final RxBleRadio rxBleRadio;
    private final UUIDUtil uuidUtil;
    private final RxBleDeviceProvider rxBleDeviceProvider;
    private final Map<Set<UUID>, Observable<RxBleScanResult>> queuedScanOperations = new HashMap<>();
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final Observable<BleAdapterState> rxBleAdapterStateObservable;
    private final LocationServicesStatus locationServicesStatus;

    RxBleClientImpl(RxBleAdapterWrapper rxBleAdapterWrapper,
                    RxBleRadio rxBleRadio,
                    Observable<BleAdapterState> adapterStateObservable,
                    UUIDUtil uuidUtil,
                    LocationServicesStatus locationServicesStatus,
                    RxBleDeviceProvider rxBleDeviceProvider) {
        this.uuidUtil = uuidUtil;
        this.rxBleRadio = rxBleRadio;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.rxBleAdapterStateObservable = adapterStateObservable;
        this.locationServicesStatus = locationServicesStatus;
        this.rxBleDeviceProvider = rxBleDeviceProvider;
    }

    public static RxBleClientImpl getInstance(@NonNull Context context) {
        final RxBleAdapterWrapper rxBleAdapterWrapper = new RxBleAdapterWrapper(BluetoothAdapter.getDefaultAdapter());
        final RxBleRadioImpl rxBleRadio = new RxBleRadioImpl();
        final RxBleAdapterStateObservable adapterStateObservable = new RxBleAdapterStateObservable(context.getApplicationContext());
        final BleConnectionCompat bleConnectionCompat = new BleConnectionCompat(context);
        return new RxBleClientImpl(
                rxBleAdapterWrapper,
                rxBleRadio,
                adapterStateObservable,
                new UUIDUtil(),
                new LocationServicesStatus(context, (LocationManager) context.getSystemService(Context.LOCATION_SERVICE)),
                new RxBleDeviceProvider(rxBleAdapterWrapper, rxBleRadio, bleConnectionCompat, adapterStateObservable));
    }

    @Override
    public RxBleDevice getBleDevice(@NonNull String macAddress) {
        return rxBleDeviceProvider.getBleDevice(macAddress);
    }

    @Override
    public Set<RxBleDevice> getBondedDevices() {
        Set<RxBleDevice> rxBleDevices = new HashSet<>();
        Set<BluetoothDevice> bluetoothDevices = rxBleAdapterWrapper.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            rxBleDevices.add(getBleDevice(bluetoothDevice.getAddress()));
        }

        return rxBleDevices;
    }

    @Override
    public Observable<RxBleScanResult> scanBleDevices(@Nullable UUID... filterServiceUUIDs) {

        if (!rxBleAdapterWrapper.hasBluetoothAdapter()) {
            return Observable.error(new BleScanException(BleScanException.BLUETOOTH_NOT_AVAILABLE));
        } else if (!rxBleAdapterWrapper.isBluetoothEnabled()) {
            return Observable.error(new BleScanException(BleScanException.BLUETOOTH_DISABLED));
        } else if (checkIfLocationPermissionIsGrantedIfRequired()) {
            return Observable.error(new BleScanException(BleScanException.LOCATION_PERMISSION_MISSING));
        } else if (checkIfLocationAccessIsEnabledIfRequired()) {
            return Observable.error(new BleScanException(BleScanException.LOCATION_SERVICES_DISABLED));
        } else {
            return initializeScan(filterServiceUUIDs);
        }
    }

    private Observable<RxBleScanResult> initializeScan(@Nullable UUID[] filterServiceUUIDs) {
        final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);

        synchronized (queuedScanOperations) {
            Observable<RxBleScanResult> matchingQueuedScan = queuedScanOperations.get(filteredUUIDs);

            if (matchingQueuedScan == null) {
                matchingQueuedScan = createScanOperation(filterServiceUUIDs);
                queuedScanOperations.put(filteredUUIDs, matchingQueuedScan);
            }

            return matchingQueuedScan;
        }
    }

    private boolean checkIfLocationAccessIsEnabledIfRequired() {
        return locationServicesStatus.isLocationProviderRequired() && !locationServicesStatus.isLocationProviderEnabled();
    }

    private boolean checkIfLocationPermissionIsGrantedIfRequired() {
        return locationServicesStatus.isLocationProviderRequired() && !locationServicesStatus.isLocationPermissionApproved();
    }

    private <T> Observable<T> bluetoothAdapterOffExceptionObservable() {
        return rxBleAdapterStateObservable
                .filter(state -> state != BleAdapterState.STATE_ON)
                .first()
                .flatMap(status -> Observable.error(new BleScanException(BleScanException.BLUETOOTH_DISABLED)));
    }

    private RxBleScanResult convertToPublicScanResult(RxBleInternalScanResult scanResult) {
        final BluetoothDevice bluetoothDevice = scanResult.getBluetoothDevice();
        final RxBleDevice bleDevice = getBleDevice(bluetoothDevice.getAddress());
        return new RxBleScanResult(bleDevice, scanResult.getRssi(), scanResult.getScanRecord());
    }

    private Observable<RxBleScanResult> createScanOperation(@Nullable UUID[] filterServiceUUIDs) {
        final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);
        final RxBleRadioOperationScan scanOperation = new RxBleRadioOperationScan(filterServiceUUIDs, rxBleAdapterWrapper, uuidUtil);
        return rxBleRadio.queue(scanOperation)
                .doOnUnsubscribe(() -> {

                    synchronized (queuedScanOperations) {
                        scanOperation.stop();
                        queuedScanOperations.remove(filteredUUIDs);
                    }
                })
                .mergeWith(bluetoothAdapterOffExceptionObservable())
                .map(this::convertToPublicScanResult)
                .share();
    }
}
