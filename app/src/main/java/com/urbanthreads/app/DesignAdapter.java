package com.urbanthreads.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class DesignAdapter extends RecyclerView.Adapter<DesignAdapter.ViewHolder> {
    private List<Design> designs;
    private OnDesignClickListener listener;

    public interface OnDesignClickListener {
        void onDesignClick(Design design);
    }

    public DesignAdapter(List<Design> designs, OnDesignClickListener listener) {
        this.designs = designs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_design, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Design design = designs.get(position);
        holder.txtTitle.setText(design.getTitle());
        holder.txtCategory.setText(design.getDescription());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDesignClick(design);
            }
        });

        if (design.getImageUrl() != null && !design.getImageUrl().isEmpty()) {
            String imageUrl = design.getImageUrl();
            if (imageUrl.startsWith("content://")) {
                // Fallback for local URIs that should not be in production Firestore
                holder.imgDesign.setImageResource(android.R.drawable.ic_menu_gallery);
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.stat_notify_error)
                        .centerCrop()
                        .into(holder.imgDesign);
            }
        } else {
            holder.imgDesign.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return (designs != null) ? designs.size() : 0;
    }

    public void updateList(List<Design> newList) {
        this.designs = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgDesign;
        TextView txtTitle, txtCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgDesign = itemView.findViewById(R.id.imgDesign);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtCategory = itemView.findViewById(R.id.txtCategory);
        }
    }
}
