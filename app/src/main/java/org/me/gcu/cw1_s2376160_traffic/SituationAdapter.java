package org.me.gcu.cw1_s2376160_traffic;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.ViewHolder> {

    public interface OnSituationClickListener {
        void onSituationClick(TrafficSituation situation);
    }

    private ArrayList<TrafficSituation> items = new ArrayList<>();
    private final OnSituationClickListener listener;

    public SituationAdapter(OnSituationClickListener listener) {
        this.listener = listener;
    }

    public void updateList(ArrayList<TrafficSituation> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrafficSituation item = items.get(position);
        String header = item.getRoadName() + " - " + item.getRecordType()
                + (item.getSpecificManagementType().isEmpty() ? "" : " (" + item.getSpecificManagementType() + ")");
        holder.titleView.setText(header);
        
        String commentText = item.getComment();
        if (item.getRecordType().toLowerCase().contains("maintenance") || item.getDurationInDays() > 0) {
            long durationLeft = item.getDurationLeftInDays();
            holder.commentView.setText(commentText + "\n\nDuration Left: " + durationLeft + " day(s)");
        } else {
            holder.commentView.setText(commentText);
        }
        
        holder.iconView.setImageResource(resolveSituationIcon(item));

        // Colour-code roadwork durations: green (<1 day), yellow (1-7 days), red (>7 days)
        long days = item.getDurationInDays();
        if (item.getRecordType().toLowerCase().contains("maintenance") || days > 0) {
            if (days > 7 || item.getDelayType().toLowerCase().contains("long")) {
                holder.rowContainer.setBackgroundColor(Color.parseColor("#FFCDD2")); // Red tint
            } else if (days >= 1) {
                holder.rowContainer.setBackgroundColor(Color.parseColor("#FFF9C4")); // Yellow tint
            } else {
                holder.rowContainer.setBackgroundColor(Color.parseColor("#C8E6C9")); // Green tint
            }
        } else {
            holder.rowContainer.setBackgroundColor(Color.parseColor("#F3F4F6"));
        }

        holder.itemView.setOnClickListener(v -> listener.onSituationClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int resolveSituationIcon(TrafficSituation s) {
        String subType = s.getSpecificManagementType().toLowerCase();
        String recType = s.getRecordType().toLowerCase();

        // TODO: Import 6 distinct Vector Assets (Apache License 2.0) via Android Studio Resource Manager
        // and replace the android.R.drawable placeholders below with your R.drawable.* resources:
        if (subType.contains("closed")) {
            return R.drawable.ic_delete;
        } else if (subType.contains("lane") || s.getImpactDetails().contains("Restricted")) {
            return R.drawable.ic_media_rew;
        } else if (subType.contains("queuing") || recType.contains("abnormaltraffic")) {
            return R.drawable.ic_popup_sync;
        } else if (recType.contains("maintenance") || subType.contains("roadworks")) {
            return R.drawable.ic_menu_manage;
        } else if (subType.contains("accident") || subType.contains("obstruction")) {
            return R.drawable.stat_sys_warning;
        } else {
            return R.drawable.ic_dialog_info;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout rowContainer;
        final ImageView iconView;
        final TextView titleView;
        final TextView commentView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rowContainer = itemView.findViewById(R.id.rowContainer);
            iconView = itemView.findViewById(R.id.situationIcon);
            titleView = itemView.findViewById(R.id.situationTypeTitle);
            commentView = itemView.findViewById(R.id.situationComment);
        }
    }
}