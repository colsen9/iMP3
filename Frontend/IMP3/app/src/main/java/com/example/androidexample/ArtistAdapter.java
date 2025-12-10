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

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    public interface OnArtistClickListener {
        void onEdit(Artist artist);
        void onDelete(Artist artist);
    }

    private final ArrayList<Artist> artists;
    private final OnArtistClickListener listener;

    public ArtistAdapter(ArrayList<Artist> artists, OnArtistClickListener listener) {
        this.artists = artists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artists.get(position);
        holder.tvArtistName.setText(artist.getName());

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(artist));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(artist));
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        TextView tvArtistName, tvArtistBio, tvArtistAlbums, tvArtistTracks;
        ImageButton btnEdit, btnDelete;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            btnEdit = itemView.findViewById(R.id.btnEditArtist);
            btnDelete = itemView.findViewById(R.id.btnDeleteArtist);
        }
    }
}


