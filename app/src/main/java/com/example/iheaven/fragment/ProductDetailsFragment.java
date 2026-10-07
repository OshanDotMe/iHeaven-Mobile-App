package com.example.iheaven.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.iheaven.R;
import com.example.iheaven.activity.SignInActivity;
import com.example.iheaven.adapter.ProductSliderAdapter;
import com.example.iheaven.databinding.FragmentProductDetailsBinding;
import com.example.iheaven.model.CartItem;
import com.example.iheaven.model.Product;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ProductDetailsFragment extends Fragment {

    private FragmentProductDetailsBinding binding;
    private String productId;
    private int quantity = 1;
    private int abvQuantity;
    private double basePrice = 0;
    private double currentPrice = 0;
    private Map<String, ChipGroup> attributeGroups   = new HashMap<>();
    private Map<String, List<Double>> priceModifiersMap = new HashMap<>();
    private Map<String, List<String>> attributeValuesMap = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.backButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        View bottomBar = view.findViewById(R.id.bottomBar);
        ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.GONE);
        getActivity().findViewById(R.id.toolbar).setVisibility(View.GONE);

        getActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });

        fetchProduct();

        binding.productDetailsBtnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.productDetailsQuantity.setText(String.valueOf(quantity));
            }
        });

        binding.productDetailsBtnPlus.setOnClickListener(v -> {
            if (quantity < abvQuantity) {
                quantity++;
                binding.productDetailsQuantity.setText(String.valueOf(quantity));
            }
        });

        binding.productDetailsBtnAddCart.setOnClickListener(v->{

            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            if (firebaseAuth.getCurrentUser() == null) {
                Intent intent = new Intent(requireContext(), SignInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            }else{
                List<CartItem.Attribute> attributes = getFinalSelection();

                CartItem cartItem = new CartItem(productId, quantity, attributes);

                String uId = firebaseAuth.getCurrentUser().getUid();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").document(uId).collection("cart")
                        .document(productId).set(cartItem)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(getContext(), "Item added to cart", Toast.LENGTH_SHORT).show();
                            }
                        });
            }


        });

    }

    private void fetchProduct() {
        FirebaseFirestore.getInstance()
                .collection("products")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Product product = queryDocumentSnapshots
                                .getDocuments().get(0).toObject(Product.class);

                        if (product == null) return;

                        if (product.getImages() != null && !product.getImages().isEmpty()) {
                            ProductSliderAdapter adapter =
                                    new ProductSliderAdapter(product.getImages());
                            binding.productImageSlider.setAdapter(adapter);
                            binding.dotsIndicator.attachTo(binding.productImageSlider);
                        }

                        binding.productDetailsTitle.setText(product.getTitle());

                        NumberFormat formatter = NumberFormat.getNumberInstance();
                        String formattedPrice = "LKR " + formatter.format(product.getPrice()) + ".00";
                        binding.productDetailsPrice.setText(formattedPrice);

                        binding.productDetailsRating.setRating(product.getRating());

                        binding.ratingCount.setText("(" + product.getRating() + ")");

                        String description = product.getDescription();
                        if (description != null) {
                            description = description.replace("\"", "").trim();
                        }
                        binding.productDetailsDescription.setText(description);

                        abvQuantity = product.getStockCount();
                        binding.productDetailsAttributeContainer.removeAllViews();
                        if (product.getAttributes() != null) {
                            product.getAttributes().forEach(attribute ->
                                    renderAttribute(attribute,
                                            binding.productDetailsAttributeContainer)
                            );
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("ProductDetails", "Error fetching product", e)
                );
    }

    private void renderAttribute(Product.Attribute attribute, ViewGroup container) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 8, 0, 8);
        row.setLayoutParams(rowParams);

        TextView label = new TextView(getContext());
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                160,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );


        labelParams.gravity = Gravity.CENTER_VERTICAL;
        labelParams.setMarginEnd(12);
        label.setLayoutParams(labelParams);
        label.setText(attribute.getName());
        label.setTypeface(null, Typeface.BOLD);
        label.setTextSize(15);
        label.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.md_theme_tertiary));

        row.addView(label);

        ChipGroup group = new ChipGroup(getContext());
        group.setSelectionRequired(true);
        group.setSingleSelection(true);

        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        groupParams.weight = 1;
        group.setLayoutParams(groupParams);

        attribute.getValues().forEach(value -> {
            Chip chip = new Chip(getContext());
            chip.setCheckable(true);
            chip.setChipStrokeWidth(3f);

            chip.setTag(value);

            if ("color".equals(attribute.getType())) {
                chip.setText("");
                chip.setChipMinHeight(80f);
                try {
                    String colorValue = value.startsWith("#") ? value : "#" + value;
                    chip.setChipBackgroundColor(
                            ColorStateList.valueOf(Color.parseColor(colorValue))
                    );
                    chip.setChipStrokeColor(
                            ColorStateList.valueOf(Color.parseColor("#CCCCCC"))
                    );
                } catch (IllegalArgumentException e) {
                    Log.e("ProductDetails", "Invalid color: " + value);
                    chip.setChipBackgroundColor(
                            ColorStateList.valueOf(Color.GRAY)
                    );
                }
            } else {
                chip.setText(value);
            }

            group.addView(chip);
        });

        row.addView(group);
        container.addView(row);
        attributeGroups.put(attribute.getName(), group);

    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().findViewById(R.id.bottom_navigation_view).setVisibility(View.GONE);
        getActivity().findViewById(R.id.toolbar).setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private List<CartItem.Attribute> getFinalSelection(){
        List<CartItem.Attribute> attributes = new ArrayList<>();

        for (Map.Entry<String, ChipGroup> entry : attributeGroups.entrySet()){
            String attributeName = entry.getKey();
            ChipGroup chipGroup = entry.getValue();

            int checkedChipId = chipGroup.getCheckedChipId();
            if (checkedChipId != -1){
                Chip chip = getView().findViewById(checkedChipId);
                String value = chip.getTag().toString();

                CartItem.Attribute attribute = new CartItem.Attribute(attributeName, value);
                attributes.add(attribute);
            }
        }
        return attributes;
    }


}