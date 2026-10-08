package com.example.iheaven.fragment;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.iheaven.service.NotificationReceiver;
import com.example.iheaven.util.NotificationHelper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.iheaven.R;
import com.example.iheaven.adapter.CartAdapter;
import com.example.iheaven.databinding.FragmentCartBinding;
import com.example.iheaven.model.CartItem;
import com.example.iheaven.model.Order;
import com.example.iheaven.model.Product;
import com.example.iheaven.util.ShakeDetector;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class CartFragment extends Fragment {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    private FragmentCartBinding binding;
    private List<CartItem> cartItems = new ArrayList<>();
    private CartAdapter adapter;
    private Order.Address savedShippingAddress;
    private Order.Address savedBillingAddress;
    private double deliveryFee = 800;
    private double currentTotal = 0;
    private String currentOrderId = null;

    private Order pendingOrder = null;
    private String pendingUserId = null;

    private ListenerRegistration cartListener;
    private ListenerRegistration productsListener;
    private Map<String, Product> latestProductMap = new HashMap<>();

    private final ActivityResultLauncher<Intent> payhereLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (binding == null) return;

                        Log.d("PayHere", "Result code: " + result.getResultCode());

                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {

                            Intent data = result.getData();
                            if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                                PHResponse<StatusResponse> response =
                                        (PHResponse<StatusResponse>)
                                                data.getSerializableExtra(
                                                        PHConstants.INTENT_EXTRA_RESULT);

                                if (response != null && response.isSuccess()) {
                                    Log.d("PayHere", "Payment Success");
                                    if (pendingOrder != null && pendingUserId != null) {
                                        saveOrderToFirebase(pendingOrder, pendingUserId);
                                    }
                                } else {
                                    String msg = response != null &&
                                            response.getData() != null ?
                                            response.getData().getMessage() :
                                            "Payment failed";
                                    Log.d("PayHere", "Payment Failed: " + msg);
                                    pendingOrder  = null;
                                    pendingUserId = null;
                                    if (binding != null) {
                                        binding.checkoutButton.setEnabled(true);
                                        binding.checkoutButton.setText("Checkout");
                                    }
                                    Toast.makeText(getContext(),
                                            "Payment failed: " + msg,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                            Log.d("PayHere", "Payment Cancelled");
                            pendingOrder  = null;
                            pendingUserId = null;
                            if (binding != null) {
                                binding.checkoutButton.setEnabled(true);
                                binding.checkoutButton.setText("Checkout");
                            }
                            Toast.makeText(getContext(),
                                    "Payment cancelled",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupShakeDetector();

        requireActivity().findViewById(R.id.toolbar)
                .setVisibility(View.GONE);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.cartTitleBar.setPadding(
                    binding.cartTitleBar.getPaddingLeft(),
                    systemBars.top,
                    binding.cartTitleBar.getPaddingRight(),
                    binding.cartTitleBar.getPaddingBottom()
            );
            binding.cartBottomBar.setPadding(
                    binding.cartBottomBar.getPaddingLeft(),
                    binding.cartBottomBar.getPaddingTop(),
                    binding.cartBottomBar.getPaddingRight(),
                    systemBars.bottom > 0 ? systemBars.bottom : 12
            );
            return WindowInsetsCompat.CONSUMED;
        });

        binding.cartBackButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        binding.addressCard.setOnClickListener(v -> {
            AddressFragment addressFragment = new AddressFragment();
            addressFragment.setOnAddressSavedListener((shipping, billing) -> {
                if (binding == null) return;
                savedShippingAddress = shipping;
                savedBillingAddress  = billing;
                binding.addressName.setText(shipping.getName());
                binding.addressDetail.setText(
                        shipping.getAddress() + ", " +
                                shipping.getCity() + " " +
                                shipping.getPostcode()
                );
            });
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.popup_enter, R.anim.popup_exit,
                            R.anim.popup_pop_enter, R.anim.popup_pop_exit
                    )
                    .add(R.id.container, addressFragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.checkoutButton.setOnClickListener(v -> placeOrder());

        setupAdapter();
        startCartListener();
        startProductsListener();
    }
    private void setupAdapter() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        adapter = new CartAdapter(cartItems);
        binding.cartRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext()));
        binding.cartRecyclerView.setAdapter(adapter);

        adapter.setOnQuantityChangeListener(cartItem ->
                FirebaseFirestore.getInstance()
                        .collection("users").document(userId)
                        .collection("cart")
                        .document(cartItem.getDocumentId())
                        .update("quantity", cartItem.getQuantity())
        );

        adapter.setOnRemoveListener(position -> {
            if (position >= cartItems.size()) return;
            String documentId = cartItems.get(position).getDocumentId();
            FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .collection("cart")
                    .document(documentId)
                    .delete();
        });
    }

    private void startCartListener() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        if (cartListener != null) {
            cartListener.remove();
            cartListener = null;
        }

        cartListener = FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("cart")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || binding == null) return;
                    if (snapshots == null) return;

                    cartItems.clear();
                    for (DocumentSnapshot ds : snapshots.getDocuments()) {
                        CartItem cartItem = ds.toObject(CartItem.class);
                        if (cartItem != null) {
                            cartItem.setDocumentId(ds.getId());
                            cartItems.add(cartItem);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    recalculateTotal();
                });
    }

    private void startProductsListener() {
        if (productsListener != null) {
            productsListener.remove();
            productsListener = null;
        }

        productsListener = FirebaseFirestore.getInstance()
                .collection("products")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || binding == null) return;
                    if (snapshots == null) return;

                    latestProductMap.clear();
                    for (DocumentSnapshot ds : snapshots.getDocuments()) {
                        Product product = ds.toObject(Product.class);
                        if (product != null) {
                            latestProductMap.put(product.getProductId(), product);
                        }
                    }

                    recalculateTotal();
                });
    }

    private void recalculateTotal() {
        if (binding == null) return;

        if (cartItems.isEmpty()) {
            currentTotal = deliveryFee;
            binding.subtotalText.setText("LKR 0.00");
            binding.deliveryFeeText.setText(
                    String.format(Locale.US, "LKR %,.2f", deliveryFee));
            binding.totalPriceText.setText(
                    String.format(Locale.US, "LKR %,.2f", deliveryFee));
            return;
        }

        if (latestProductMap.isEmpty()) return;

        double subtotal = 0;
        for (CartItem cartItem : cartItems) {
            Product product = latestProductMap.get(cartItem.getProductId());
            if (product != null) {
                double itemPrice = product.getPrice();

                // apply price modifier from selected attribute
                if (cartItem.getAttributes() != null
                        && product.getAttributes() != null) {
                    for (CartItem.Attribute selectedAttr : cartItem.getAttributes()) {
                        for (Product.Attribute productAttr : product.getAttributes()) {
                            if (productAttr.getName() != null
                                    && productAttr.getName().equals(
                                    selectedAttr.getName())
                                    && "text".equalsIgnoreCase(
                                    productAttr.getType())) {
                                List<String> values = productAttr.getValues();
                                List<Double> mods   = productAttr.getPriceModifiers();
                                if (values != null && mods != null
                                        && selectedAttr.getValue() != null) {
                                    int idx = values.indexOf(
                                            selectedAttr.getValue());
                                    if (idx >= 0 && idx < mods.size()) {
                                        itemPrice += mods.get(idx);
                                    }
                                }
                            }
                        }
                    }
                }
                subtotal += itemPrice * cartItem.getQuantity();
            }
        }

        currentTotal = subtotal + deliveryFee;
        binding.subtotalText.setText(
                String.format(Locale.US, "LKR %,.2f", subtotal));
        binding.deliveryFeeText.setText(
                String.format(Locale.US, "LKR %,.2f", deliveryFee));
        binding.totalPriceText.setText(
                String.format(Locale.US, "LKR %,.2f", currentTotal));
    }

    private void placeOrder() {
        if (savedShippingAddress == null) {
            Toast.makeText(getContext(),
                    "Please add a delivery address",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (cartItems.isEmpty()) {
            Toast.makeText(getContext(),
                    "Your cart is empty...",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        binding.checkoutButton.setEnabled(false);
        binding.checkoutButton.setText("Processing...");

        List<Order.OrderItem> orderItems = new ArrayList<>();
        double subtotal = 0;

        for (CartItem cartItem : cartItems) {
            Product product = latestProductMap.get(cartItem.getProductId());
            if (product != null) {
                double itemPrice = product.getPrice();

                // apply price modifier
                if (cartItem.getAttributes() != null
                        && product.getAttributes() != null) {
                    for (CartItem.Attribute selectedAttr : cartItem.getAttributes()) {
                        for (Product.Attribute productAttr : product.getAttributes()) {
                            if (productAttr.getName() != null
                                    && productAttr.getName().equals(
                                    selectedAttr.getName())
                                    && "text".equalsIgnoreCase(
                                    productAttr.getType())) {
                                List<String> values = productAttr.getValues();
                                List<Double> mods   = productAttr.getPriceModifiers();
                                if (values != null && mods != null
                                        && selectedAttr.getValue() != null) {
                                    int idx = values.indexOf(
                                            selectedAttr.getValue());
                                    if (idx >= 0 && idx < mods.size()) {
                                        itemPrice += mods.get(idx);
                                    }
                                }
                            }
                        }
                    }
                }

                List<Order.OrderItem.Attribute> attrs = new ArrayList<>();
                if (cartItem.getAttributes() != null) {
                    for (CartItem.Attribute a : cartItem.getAttributes()) {
                        attrs.add(new Order.OrderItem.Attribute(
                                a.getName(), a.getValue()));
                    }
                }

                orderItems.add(new Order.OrderItem(
                        product.getProductId(),
                        itemPrice,
                        cartItem.getQuantity(),
                        attrs
                ));
                subtotal += itemPrice * cartItem.getQuantity();
            }
        }

        if (orderItems.isEmpty()) {
            binding.checkoutButton.setEnabled(true);
            binding.checkoutButton.setText("Checkout");
            Toast.makeText(getContext(),
                    "Could not load product details",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double total   = subtotal + deliveryFee;
        currentTotal   = total;
        currentOrderId = String.valueOf(System.currentTimeMillis());

        pendingOrder = new Order(
                currentOrderId, userId, total, "paid",
                System.currentTimeMillis(), orderItems,
                savedShippingAddress, savedBillingAddress
        );
        pendingUserId = userId;

        launchPayHere(currentOrderId, total);
    }

    private void launchPayHere(String orderId, double total) {
        if (savedShippingAddress == null) return;

        InitRequest req = new InitRequest();
        req.setSandBox(true);
        req.setMerchantId("1226875");
        req.setMerchantSecret(
                "OTExNTM4MDczMTQ5ODY5MjIyNTI0NDQ3MTQxMjAyMDg3NzI3ODcy");
        req.setCurrency("LKR");

        String formattedAmount = String.format(Locale.US, "%.2f", total);
        double cleanAmount = Double.parseDouble(formattedAmount);
        req.setAmount(cleanAmount);

        String shortOrderId = "IH" + System.currentTimeMillis() % 100000;
        req.setOrderId(shortOrderId);
        req.setItemsDescription("iHeaven Order");

        String fullName = savedShippingAddress.getName() != null ?
                savedShippingAddress.getName() : "Customer";
        String[] parts = fullName.split(" ", 2);
        req.getCustomer().setFirstName(parts[0]);
        req.getCustomer().setLastName(parts.length > 1 ? parts[1] : "");
        req.getCustomer().setEmail(
                savedShippingAddress.getEmail() != null ?
                        savedShippingAddress.getEmail() : "customer@email.com");
        req.getCustomer().setPhone(
                savedShippingAddress.getContact() != null ?
                        savedShippingAddress.getContact() : "0771234567");
        req.getCustomer().getAddress().setAddress(
                savedShippingAddress.getAddress() != null ?
                        savedShippingAddress.getAddress() : "Colombo");
        req.getCustomer().getAddress().setCity(
                savedShippingAddress.getCity() != null ?
                        savedShippingAddress.getCity() : "Colombo");
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        try {
            Intent intent = new Intent(requireActivity(), PHMainActivity.class);
            intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
            payhereLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("PayHere", "Error launching PayHere", e);
            pendingOrder  = null;
            pendingUserId = null;
            Toast.makeText(getContext(),
                    "Error launching payment", Toast.LENGTH_SHORT).show();
            if (binding != null) {
                binding.checkoutButton.setEnabled(true);
                binding.checkoutButton.setText("Checkout");
            }
        }
    }

    private void saveOrderToFirebase(Order order, String userId) {
        FirebaseFirestore.getInstance()
                .collection("orders")
                .document(order.getOrderId())
                .set(order)
                .addOnSuccessListener(aVoid -> {
                    pendingOrder  = null;
                    pendingUserId = null;
                    NotificationHelper.showLocalNotification(
                            requireContext(),
                            "Payment Confirmed!",
                            "Your payment was successful. Order #" +
                                    order.getOrderId().substring(
                                            Math.max(0,
                                                    order.getOrderId().length() - 8)) +
                                    " has been placed."
                    );
                    scheduleOrderNotifications(order);
                    clearCart(userId);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.checkoutButton.setEnabled(true);
                    binding.checkoutButton.setText("Checkout");
                    Toast.makeText(getContext(),
                            "Failed to save order", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearCart(String userId) {
        FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(qds -> {
                    for (DocumentSnapshot ds : qds.getDocuments()) {
                        ds.getReference().delete();
                    }
                    if (binding == null) return;
                    binding.checkoutButton.setEnabled(true);
                    binding.checkoutButton.setText("Checkout");
                    Toast.makeText(getContext(),
                            "Order placed successfully!",
                            Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(
                                    R.anim.popup_enter, R.anim.popup_exit,
                                    R.anim.popup_pop_enter, R.anim.popup_pop_exit
                            )
                            .add(R.id.container, new OrderFragment())
                            .commit();
                })
                .addOnFailureListener(e ->
                        Log.e("CartFragment", "Error clearing cart", e));
    }

    private void setupShakeDetector() {
        sensorManager = (SensorManager) requireActivity()
                .getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER);
        }
        shakeDetector = new ShakeDetector();
        shakeDetector.setOnShakeListener(() ->
                requireActivity().runOnUiThread(() -> {
                    if (cartItems != null && !cartItems.isEmpty()) {
                        showClearCartDialog();
                    }
                }));
    }

    private void showClearCartDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear Cart")
                .setMessage("Shake detected! Do you want to clear your cart?")
                .setPositiveButton("Clear", (dialog, which) -> clearCartItems())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void clearCartItems() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(qds -> {
                    for (DocumentSnapshot ds : qds.getDocuments()) {
                        ds.getReference().delete();
                    }
                    Toast.makeText(getContext(),
                            "Cart cleared!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().findViewById(R.id.toolbar)
                .setVisibility(View.GONE);
        requireActivity().findViewById(R.id.bottom_navigation_view)
                .setVisibility(View.VISIBLE);
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(
                    shakeDetector, accelerometer,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (cartListener != null) {
            cartListener.remove();
            cartListener = null;
        }
        if (productsListener != null) {
            productsListener.remove();
            productsListener = null;
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
        requireActivity().findViewById(R.id.toolbar)
                .setVisibility(View.VISIBLE);
        binding = null;
    }

    private void scheduleOrderNotifications(Order order) {
        long oneDayMs = 24 * 60 * 60 * 1000L;
        long now      = System.currentTimeMillis();
        scheduleNotification(requireContext(), "Order Packing",
                "Your order #" + order.getOrderId().substring(
                        Math.max(0, order.getOrderId().length() - 8)) +
                        " is being packed.",
                now + (2 * 60 * 60 * 1000L));
        scheduleNotification(requireContext(), "Order Picked Up",
                "Your order has been picked up and is on its way!",
                now + oneDayMs);
        scheduleNotification(requireContext(), "Out for Delivery",
                "Your order is out for delivery. Expect it today!",
                now + (2 * oneDayMs));
        scheduleNotification(requireContext(), "Order Delivered!",
                "Your order has been delivered. Enjoy your purchase!",
                now + (4 * oneDayMs));
    }

    private void scheduleNotification(Context context, String title,
                                      String body, long triggerTime) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, (int) triggerTime, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        triggerTime, pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }
}