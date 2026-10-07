package com.example.iheaven.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iheaven.databinding.ItemOrderBinding;
import com.example.iheaven.model.Order;
import com.example.iheaven.model.Product;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private List<Order> orderList;
    private Map<String, Product> productMap;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(List<Order> orderList,
                        Map<String, Product> productMap,
                        OnOrderClickListener listener) {
        this.orderList  = orderList;
        this.productMap = productMap;
        this.listener   = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemOrderBinding binding;

        public ViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);

        holder.binding.orderIdText.setText("Order #" +
                order.getOrderId().substring(
                        Math.max(0, order.getOrderId().length() - 8)));

        SimpleDateFormat sdf = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a", Locale.getDefault());
        holder.binding.orderDateText.setText(
                sdf.format(new Date(order.getOrderDate())));

        holder.binding.orderStatusBadge.setText(
                order.getStatus().toUpperCase());
        setStatusColor(holder.binding.orderStatusBadge,
                order.getStatus());

        if (order.getOrderItems() != null &&
                !order.getOrderItems().isEmpty()) {
            Order.OrderItem firstItem = order.getOrderItems().get(0);
            Product product = productMap.get(firstItem.getProductId());

            if (product != null) {
                holder.binding.orderProductName.setText(
                        product.getTitle());
                holder.binding.orderProductPrice.setText(
                        String.format(Locale.US, "LKR %,.2f",
                                order.getTotalAmount()));

                if (product.getImages() != null &&
                        !product.getImages().isEmpty()) {
                    Glide.with(holder.itemView.getContext())
                            .load(product.getImages().get(0))
                            .fitCenter()
                            .into(holder.binding.orderProductImage);
                }
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    private void setStatusColor(TextView view, String status) {
        int color;
        switch (status.toLowerCase()) {
            case "paid":
                color = Color.parseColor("#4CAF50");
                break;
            case "pending":
                color = Color.parseColor("#FF9800");
                break;
            case "cancelled":
            case "payment_failed":
                color = Color.parseColor("#F44336");
                break;
            default:
                color = Color.parseColor("#2196F3");
                break;
        }
        view.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }
}