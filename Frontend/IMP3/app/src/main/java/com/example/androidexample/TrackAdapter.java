/** @author Cayden Olsen **/

package com.example.androidexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    public interface OnTrackClickListener {
        void onEdit(Track track);
        void onDelete(Track track);
    }

    private ArrayList<Track> tracks;
    private OnTrackClickListener listener;

    public TrackAdapter(ArrayList<Track> tracks, OnTrackClickListener listener) {
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.tvTrackName.setText(track.getName());
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(track));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(track));
    }

    @Override
    public int getItemCount() { return tracks.size(); }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView tvTrackName;
        ImageButton btnEdit, btnDelete;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTrackName = itemView.findViewById(R.id.tvTrackName);
            btnEdit = itemView.findViewById(R.id.btnEditTrack);
            btnDelete = itemView.findViewById(R.id.btnDeleteTrack);
        }
    }
}


