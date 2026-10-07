package com.example.iheaven.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iheaven.R;
import com.example.iheaven.databinding.ItemProductRecyclerBinding;
import com.example.iheaven.model.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.listener    = listener;
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ItemProductRecyclerBinding binding;

        public ProductViewHolder(ItemProductRecyclerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemProductRecyclerBinding binding = ItemProductRecyclerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.binding.productName.setText(product.getTitle());

        holder.binding.productPrice.setText(
                String.format("$%.2f", product.getPrice())
        );

        Glide.with(holder.itemView.getContext())
                .load(product.getImages())
//                .placeholder(R.drawable.ic_placeholder)
                .centerCrop()
                .into(holder.binding.productImage);

        Animation animation = AnimationUtils.loadAnimation(
                holder.itemView.getContext(), R.anim.click_animation
        );
        animation.setStartOffset(position * 80L);
        holder.itemView.startAnimation(animation);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }
}
