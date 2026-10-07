package com.example.iheaven.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iheaven.R;
import com.example.iheaven.databinding.ItemSearchProductBinding;
import com.example.iheaven.model.Product;

import java.util.List;

public class SearchProductAdapter extends RecyclerView.Adapter<SearchProductAdapter.ViewHolder> {

    private List<Product> products;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public SearchProductAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemSearchProductBinding binding;

        public ViewHolder(ItemSearchProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemSearchProductBinding binding = ItemSearchProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Product product = products.get(position);

        holder.binding.searchProductName.setText(product.getTitle());
        holder.binding.searchProductPrice.setText(
                String.format("LKR %,.2f", product.getPrice())
        );

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImages().get(0))
                    .fitCenter()
                    .into(holder.binding.searchProductImage);
        }

        Animation animation = AnimationUtils.loadAnimation(
                holder.itemView.getContext(), R.anim.popup_enter);
        animation.setStartOffset(position * 60L);
        holder.itemView.startAnimation(animation);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }
}