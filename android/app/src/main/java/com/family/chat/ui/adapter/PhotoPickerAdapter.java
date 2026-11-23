package com.family.chat.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // 建议引入 Glide
import com.family.chat.R;
import java.util.ArrayList;
import java.util.List;

public class PhotoPickerAdapter extends RecyclerView.Adapter<PhotoPickerAdapter.PhotoViewHolder> {

    private List<String> photoPaths = new ArrayList<>();
    private final List<String> selectedPhotos = new ArrayList<>();
    private OnPhotoSelectListener listener;

    public interface OnPhotoSelectListener {
        void onPhotoSelectChanged(int selectedCount, List<String> selectedPaths);
    }

    public void setOnPhotoSelectListener(OnPhotoSelectListener listener) {
        this.listener = listener;
    }

    public void setData(List<String> paths) {
        this.photoPaths = paths != null ? paths : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_picker, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String path = photoPaths.get(position);
        Glide.with(holder.itemView.getContext())
                .load(path)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivPhoto);
        int selectedIndex = selectedPhotos.indexOf(path);
        if (selectedIndex != -1) {
            holder.tvSelectIndex.setBackgroundResource(R.drawable.bg_photo_selected);
            holder.tvSelectIndex.setText(String.valueOf(selectedIndex + 1));
        } else {
            holder.tvSelectIndex.setBackgroundResource(R.drawable.bg_photo_unselected);
            holder.tvSelectIndex.setText("");
        }
        holder.tvSelectIndex.setOnClickListener(v -> {
            int index = selectedPhotos.indexOf(path);
            if (index != -1) {
                selectedPhotos.remove(path);
                notifyDataSetChanged();
            } else {
                selectedPhotos.add(path);
                notifyDataSetChanged();
            }
            if (listener != null) {
                listener.onPhotoSelectChanged(selectedPhotos.size(), selectedPhotos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoPaths != null ? photoPaths.size() : 0;
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvSelectIndex;
        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            tvSelectIndex = itemView.findViewById(R.id.tv_select_index);
        }
    }
}