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

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    public interface OnAlbumClickListener {
        void onEdit(Album album);
        void onDelete(Album album);
    }

    private ArrayList<Album> albums;
    private OnAlbumClickListener listener;

    public AlbumAdapter(ArrayList<Album> albums, OnAlbumClickListener listener) {
        this.albums = albums;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.tvAlbumName.setText(album.getName());
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(album));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(album));
    }

    @Override
    public int getItemCount() { return albums.size(); }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlbumName;
        ImageButton btnEdit, btnDelete;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlbumName = itemView.findViewById(R.id.tvAlbumName);
            btnEdit = itemView.findViewById(R.id.btnEditAlbum);
            btnDelete = itemView.findViewById(R.id.btnDeleteAlbum);
        }
    }
}


