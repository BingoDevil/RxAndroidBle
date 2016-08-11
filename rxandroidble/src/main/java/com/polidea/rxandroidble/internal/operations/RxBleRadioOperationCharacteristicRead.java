package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import rx.Subscription;

public class RxBleRadioOperationCharacteristicRead extends RxBleRadioOperation<byte[]> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    public RxBleRadioOperationCharacteristicRead(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                                 BluetoothGattCharacteristic bluetoothGattCharacteristicObservable) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristicObservable;
    }

    @Override
    protected void protectedRun() {
        //noinspection Convert2MethodRef
        final Subscription subscription = rxBleGattCallback
                .getOnCharacteristicRead()
                .filter(uuidPair -> uuidPair.first.equals(bluetoothGattCharacteristic.getUuid()))
                .take(1)
                .map(uuidPair -> uuidPair.second)
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(BleGattOperationType.CHARACTERISTIC_READ));
        }
    }
}
