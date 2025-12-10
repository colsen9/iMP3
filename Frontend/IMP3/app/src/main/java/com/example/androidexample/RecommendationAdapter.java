/** @author Cayden Olsen **/

package com.example.androidexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onEditClick(Recommendation rec);
        void onDeleteClick(Recommendation rec);
        void onSpotifyClick(Recommendation rec);
    }

    private final List<Recommendation> recommendationList;
    private final OnItemClickListener listener;

    public RecommendationAdapter(List<Recommendation> recommendationList, OnItemClickListener listener) {
        this.recommendationList = recommendationList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, type;
        Button btnEdit, btnDelete, btnSpotify;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            type = itemView.findViewById(R.id.text_type);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnSpotify = itemView.findViewById(R.id.btn_spotify_info);
        }
    }

    @NonNull
    @Override
    public RecommendationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationAdapter.ViewHolder holder, int position) {
        Recommendation rec = recommendationList.get(position);
        holder.title.setText(rec.title);
        holder.type.setText(rec.type);

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(rec));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(rec));
        holder.btnSpotify.setOnClickListener(v -> listener.onSpotifyClick(rec));
    }

    @Override
    public int getItemCount() {
        return recommendationList.size();
    }
}




