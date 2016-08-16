package cn.bingo.myapp.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ScanResultsAdapter extends RecyclerView.Adapter<ScanResultsAdapter.ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView line1;
        public TextView line2;

        public ViewHolder(View itemView) {
            super(itemView);
            line1 = (TextView) itemView.findViewById(android.R.id.text1);
            line2 = (TextView) itemView.findViewById(android.R.id.text2);
        }
    }

    private static final Comparator<RxBleScanResult> SORTING_COMPARATOR = new Comparator<RxBleScanResult>() {
        @Override
        public int compare(RxBleScanResult lhs, RxBleScanResult rhs) {
            return lhs.getBleDevice().getMacAddress().compareTo(rhs.getBleDevice().getMacAddress());
        }
    };

    private final List<RxBleScanResult> data = new ArrayList<>();

    public interface OnAdapterItemClickListener {
        void onAdapterViewClick(View view);
    }
    private OnAdapterItemClickListener onAdapterItemClickListener;
    public void setOnAdapterItemClickListener(OnAdapterItemClickListener onAdapterItemClickListener) {
        this.onAdapterItemClickListener = onAdapterItemClickListener;
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (onAdapterItemClickListener != null) {
                onAdapterItemClickListener.onAdapterViewClick(v);
            }
        }
    };

    public void addScanResult(RxBleScanResult bleScanResult) {
        // Not the best way to ensure distinct devices, just for sake on the demo.
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getBleDevice().equals(bleScanResult.getBleDevice())) {
                data.set(i, bleScanResult);
                notifyItemChanged(i);
                return;
            }
        }

        data.add(bleScanResult);
        Collections.sort(data, SORTING_COMPARATOR);
        notifyDataSetChanged();
    }

    public void clearScanResults() {
        data.clear();
        notifyDataSetChanged();
    }

    public RxBleScanResult getItemAtPosition(int childAdapterPosition) {
        return data.get(childAdapterPosition);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final RxBleScanResult rxBleScanResult = data.get(position);
        final RxBleDevice bleDevice = rxBleScanResult.getBleDevice();
        holder.line1.setText(String.format("%s (%s)", bleDevice.getMacAddress(), bleDevice.getName()));
        holder.line2.setText(String.format("rssi: %d", rxBleScanResult.getRssi()));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.two_line_list_item, parent, false);
        itemView.setOnClickListener(onClickListener);
        return new ViewHolder(itemView);
    }
}
