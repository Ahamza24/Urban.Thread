package com.urbanthreads.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TailorAdapter extends RecyclerView.Adapter<TailorAdapter.ViewHolder> {
    private List<models.User> tailors;
    private Map<models.User, String> tailorIds = new HashMap<>();

    public TailorAdapter(List<models.User> tailors) {
        this.tailors = tailors;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        models.User tailor = tailors.get(position);
        
        // Show Shop Name if available, otherwise User Name
        String displayName = (tailor.getShopName() != null && !tailor.getShopName().isEmpty()) 
                ? tailor.getShopName() : tailor.getName();
        holder.tvName.setText(displayName);
        
        // Load Profile Image or first Portfolio item as thumbnail
        String imageUrl = null;
        if (tailor.getProfileImage() != null && !tailor.getProfileImage().isEmpty()) {
            imageUrl = tailor.getProfileImage();
        } else if (tailor.getPortfolio() != null && !tailor.getPortfolio().isEmpty()) {
            imageUrl = tailor.getPortfolio().get(0);
        }

        if (imageUrl != null) {
            if (imageUrl.startsWith("content://")) {
                holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.stat_notify_error)
                        .into(holder.ivImage);
            }
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("tailor", tailor);
            bundle.putString("tailorId", tailorIds.get(tailor));
            Navigation.findNavController(v).navigate(R.id.tailorDetailFragment, bundle);
        });
    }

    @Override
    public int getItemCount() {
        return tailors.size();
    }

    public void updateList(List<models.User> newList) {
        this.tailors = newList;
        notifyDataSetChanged();
    }

    public void updateListWithIds(QuerySnapshot snapshots) {
        this.tailors = new ArrayList<>();
        this.tailorIds.clear();
        for (QueryDocumentSnapshot doc : snapshots) {
            models.User t = doc.toObject(models.User.class);
            this.tailors.add(t);
            this.tailorIds.put(t, doc.getId());
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivItemImage);
            tvName = itemView.findViewById(R.id.tvItemName);
        }
    }
    public String getTailorId(models.User tailor) {
        return tailorIds.get(tailor);
    }
}
