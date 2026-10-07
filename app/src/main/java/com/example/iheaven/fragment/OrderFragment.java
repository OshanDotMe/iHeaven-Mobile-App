package com.example.iheaven.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.iheaven.R;
import com.example.iheaven.adapter.OrderAdapter;
import com.example.iheaven.databinding.FragmentOrderBinding;
import com.example.iheaven.model.Order;
import com.example.iheaven.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderFragment extends Fragment {

    private FragmentOrderBinding binding;
    private List<Order> orderList = new ArrayList<>();
    private Map<String, Product> productMap = new HashMap<>();
    private OrderAdapter orderAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    binding.orderTitleBar.setPadding(
                            0, systemBars.top, 0, 0);
                    return WindowInsetsCompat.CONSUMED;
                });

        setupRecyclerView();
        fetchOrders();
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(
                orderList,
                productMap,
                order -> showOrderDetailsDialog(order)
        );
        binding.orderItems.setLayoutManager(
                new LinearLayoutManager(getContext()));
        binding.orderItems.setAdapter(orderAdapter);
    }

    private void fetchOrders() {
        if (binding == null) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String userId = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.orderItems.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("products")
                .get()
                .addOnSuccessListener(productSnapshots -> {
                    productMap.clear();
                    for (DocumentSnapshot ds : productSnapshots) {
                        Product product = ds.toObject(Product.class);
                        if (product != null) {
                            productMap.put(product.getProductId(),
                                    product);
                        }
                    }

                    FirebaseFirestore.getInstance()
                            .collection("orders")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(orderSnapshots -> {
                                if (binding == null) return;

                                orderList.clear();
                                for (DocumentSnapshot ds : orderSnapshots) {
                                    Order order = ds.toObject(Order.class);
                                    if (order != null) orderList.add(order);
                                }

                                orderList.sort((o1, o2) ->
                                        Long.compare(o2.getOrderDate(),
                                                o1.getOrderDate()));

                                orderAdapter.notifyDataSetChanged();
                                binding.progressBar.setVisibility(View.GONE);

                                if (orderList.isEmpty()) {
                                    binding.emptyState
                                            .setVisibility(View.VISIBLE);
                                    binding.orderItems
                                            .setVisibility(View.GONE);
                                } else {
                                    binding.emptyState
                                            .setVisibility(View.GONE);
                                    binding.orderItems
                                            .setVisibility(View.VISIBLE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null) return;
                                binding.progressBar
                                        .setVisibility(View.GONE);
                                Log.e("OrderFragment", "Error: ", e);
                            });
                });
    }

    private void showOrderDetailsDialog(Order order) {
        Dialog dialog = new Dialog(requireContext(),
                android.R.style.Theme_Material_Light_Dialog_NoActionBar);
        dialog.setContentView(R.layout.dialog_order_details);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        dialog.findViewById(R.id.close_button)
                .setOnClickListener(v -> dialog.dismiss());

        if (order.getOrderItems() != null &&
                !order.getOrderItems().isEmpty()) {

            Order.OrderItem firstItem = order.getOrderItems().get(0);
            Product product = productMap.get(firstItem.getProductId());

            ImageView productImage = dialog.findViewById(
                    R.id.dialog_product_image);
            TextView productName  = dialog.findViewById(
                    R.id.dialog_product_name);
            TextView productPrice = dialog.findViewById(
                    R.id.dialog_product_price);
            TextView qty          = dialog.findViewById(
                    R.id.dialog_quantity);

            if (product != null) {
                productName.setText(product.getTitle());
                productPrice.setText(String.format(Locale.US,
                        "LKR %,.2f", order.getTotalAmount()));
                qty.setText("Qty: " + firstItem.getQuantity());

                if (product.getImages() != null &&
                        !product.getImages().isEmpty()) {
                    Glide.with(requireContext())
                            .load(product.getImages().get(0))
                            .fitCenter()
                            .into(productImage);
                }
            }
        }

        setupTimeline(dialog, order);
        dialog.show();
    }

    private void setupTimeline(Dialog dialog, Order order) {
        long orderDate      = order.getOrderDate();
        long oneDayMs       = 24 * 60 * 60 * 1000L;
        long now            = System.currentTimeMillis();

        long packingDate    = orderDate;
        long pickingDate    = orderDate + oneDayMs;
        long deliveringDate = orderDate + (2 * oneDayMs);
        long deliveredDate  = orderDate + (4 * oneDayMs);

        SimpleDateFormat sdf = new SimpleDateFormat(
                "dd MMM yyyy", Locale.getDefault());

        ((TextView) dialog.findViewById(R.id.step1_date))
                .setText(sdf.format(new Date(orderDate)));
        ((TextView) dialog.findViewById(R.id.step2_date))
                .setText(sdf.format(new Date(packingDate)));
        ((TextView) dialog.findViewById(R.id.step3_date))
                .setText(sdf.format(new Date(pickingDate)));
        ((TextView) dialog.findViewById(R.id.step4_date))
                .setText(sdf.format(new Date(deliveringDate)));
        ((TextView) dialog.findViewById(R.id.step5_date))
                .setText(sdf.format(new Date(deliveredDate)));

        updateStep(dialog, R.id.step1_icon,
                R.id.step1_line, now >= orderDate);
        updateStep(dialog, R.id.step2_icon,
                R.id.step2_line, now >= packingDate);
        updateStep(dialog, R.id.step3_icon,
                R.id.step3_line, now >= pickingDate);
        updateStep(dialog, R.id.step4_icon,
                R.id.step4_line, now >= deliveringDate);
        updateStepLast(dialog, R.id.step5_icon,
                now >= deliveredDate);
    }

    private void updateStep(Dialog dialog, int iconId,
                            int lineId, boolean completed) {
        ImageView icon = dialog.findViewById(iconId);
        View line      = dialog.findViewById(lineId);

        if (completed) {
            icon.setImageResource(R.drawable.iphone);
            icon.setColorFilter(ContextCompat.getColor(
                    requireContext(), R.color.md_theme_primary));
            line.setBackgroundColor(ContextCompat.getColor(
                    requireContext(), R.color.md_theme_primary));
        } else {
            icon.setImageResource(R.drawable.iphone);
            icon.setColorFilter(Color.parseColor("#E0E0E0"));
            line.setBackgroundColor(Color.parseColor("#E0E0E0"));
        }
    }

    private void updateStepLast(Dialog dialog,
                                int iconId, boolean completed) {
        ImageView icon = dialog.findViewById(iconId);
        if (completed) {
            icon.setImageResource(R.drawable.iphone);
            icon.setColorFilter(ContextCompat.getColor(
                    requireContext(), R.color.md_theme_primary));
        } else {
            icon.setImageResource(R.drawable.iphone);
            icon.setColorFilter(Color.parseColor("#E0E0E0"));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}