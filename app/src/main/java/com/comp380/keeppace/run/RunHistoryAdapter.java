/**
 * Adapter for showing the list of runs in a RecyclerView.
 * Binds RunModel data to the run history layout.
 */
package com.comp380.keeppace.run;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.comp380.keeppace.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RunHistoryAdapter extends RecyclerView.Adapter<RunHistoryAdapter.RunViewHolder> {

    private List<RunModel> runList;

    public RunHistoryAdapter(List<RunModel> runList) {
        this.runList = runList;
    }

    @NonNull
    @Override
    public RunViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_run_history, parent, false);
        return new RunViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RunViewHolder holder, int position) {
        RunModel run = runList.get(position);

        // Format Date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        String dateStr = (run.getTimestamp() != null)
                ? sdf.format(run.getTimestamp().toDate())
                : "Unknown Date";

        // --- UPDATED: Meters to Miles ---
        double miles = run.getDistanceMeters() * 0.000621371;
        String distStr = String.format(Locale.getDefault(), "%.2f mi", miles);

        // Format Time
        long seconds = run.getTimeMillis() / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        // Set Text
        holder.textDate.setText(dateStr);
        holder.textDistance.setText(distStr);
        holder.textDuration.setText(timeStr);
        holder.textPoints.setText("+" + run.getPoints() + " pts");
    }

    @Override
    public int getItemCount() {
        return runList.size();
    }

    public static class RunViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textDistance, textDuration, textPoints;

        public RunViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.historyDate);
            textDistance = itemView.findViewById(R.id.historyDistance);
            textDuration = itemView.findViewById(R.id.historyDuration);
            textPoints = itemView.findViewById(R.id.historyPoints);
        }
    }
}