package com.example.iheaven.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iheaven.R;
import com.example.iheaven.databinding.ItemCategoryBinding;
import com.example.iheaven.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private OnCategoryClickListener listener;
    public interface OnCategoryClickListener {
        void onItemClick(Category category);
    }

    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener     = listener;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ItemCategoryBinding binding;

        public CategoryViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @Override
    public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.binding.categoryName.setText(category.getCategoryName());

        int iconRes = getIconResource(holder.itemView.getContext(), category.getCategoryPic());
        holder.binding.categoryIcon.setImageResource(iconRes);

        holder.binding.categoryIcon.setColorFilter(
                Color.parseColor("#FFFFFF"),
                PorterDuff.Mode.SRC_IN
        );

        holder.itemView.setOnClickListener(v -> {
            Animation animation = AnimationUtils.loadAnimation(
                    holder.itemView.getContext(), R.anim.click_animation);
            v.startAnimation(animation);
            if (listener != null) {
                listener.onItemClick(category);
            }
        });
    }

    private int getIconResource(Context context, String iconName) {
        int resId = context.getResources().getIdentifier(
                iconName, "drawable", context.getPackageName()
        );
        return resId != 0 ? resId : R.drawable.iphone;
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }
}