/** @author Cayden Olsen **/

package com.example.androidexample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onPerms(User user);
        void onBan(User user);
    }

    private List<User> userList;
    private OnUserActionListener listener;

    public UserAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        Button btnPerms, btnBan;
        ImageView ivProfilePicture;

        public UserViewHolder(View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            btnPerms = itemView.findViewById(R.id.btnPerms);
            btnBan = itemView.findViewById(R.id.btnBan);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
        }
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUsername.setText(user.getUsername());

        if (user.getType().equalsIgnoreCase("banned")) {
            holder.btnBan.setText("Unban User");
        } else {
            holder.btnBan.setText("Ban User");
        }

        holder.btnBan.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBan(user);
            }
        });

        byte[] pictureBytes = user.getPicture();
        if (pictureBytes != null && pictureBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.length);
            holder.ivProfilePicture.setImageBitmap(bitmap);
        } else {
            holder.ivProfilePicture.setImageResource(R.drawable.imp3);
        }

        holder.ivProfilePicture.setBackgroundResource(R.drawable.circle);
        holder.ivProfilePicture.setClipToOutline(true);

        holder.btnPerms.setOnClickListener(v -> listener.onPerms(user));
        holder.btnBan.setOnClickListener(v -> listener.onBan(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }
}

