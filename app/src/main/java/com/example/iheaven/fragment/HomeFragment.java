package com.example.iheaven.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.iheaven.R;
import com.example.iheaven.adapter.CarouselAdapter;
import com.example.iheaven.adapter.CategoryAdapter;
import com.example.iheaven.adapter.ProductAdapter;
import com.example.iheaven.adapter.SectionAdapter;
import com.example.iheaven.databinding.FragmentHomeBinding;
import com.example.iheaven.model.Category;
import com.example.iheaven.model.Product;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList;
    private String productId;

    // carousel
    private CarouselAdapter carouselAdapter;
    private Handler carouselHandler = new Handler(Looper.getMainLooper());
    private Runnable carouselRunnable;
    private int currentCarouselPage = 0;

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
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categoryList = new ArrayList<>();
        setupCategoryRecycler();
        fetchCategories();
        loadLatestProducts();
        loadCarouselImages();
        loadAllProducts();
    }

    private void setupCategoryRecycler() {
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            Bundle bundle = new Bundle();
            bundle.putString("categoryId", category.getCategoryId());

            ListingFragment fragment = new ListingFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.popup_enter,
                            R.anim.popup_exit,
                            R.anim.popup_pop_enter,
                            R.anim.popup_pop_exit
                    )
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.itemSectionCategory.itemSectionCategoryRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(),
                        LinearLayoutManager.HORIZONTAL, false)
        );
        binding.itemSectionCategory.itemSectionCategoryRecycler.setAdapter(categoryAdapter);
        binding.itemSectionCategory.itemSectionCategoryRecycler.setNestedScrollingEnabled(false);
    }

    private void fetchCategories() {
        FirebaseFirestore.getInstance()
                .collection("categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Category category = doc.toObject(Category.class);
                        categoryList.add(category);
                    }
                    categoryAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error fetching categories", e);
                });
    }

    private void loadLatestProducts() {
        FirebaseFirestore.getInstance()
                .collection("products")
                .whereEqualTo("latest", true)
                .whereEqualTo("status", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;

                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Product> products = queryDocumentSnapshots
                                .toObjects(Product.class);
                        Collections.reverse(products);

                        binding.productLatestSection.itemSectionContainer
                                .setLayoutManager(new GridLayoutManager(getContext(), 2));

                        SectionAdapter adapter = new SectionAdapter(products, product -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("productId", product.getProductId());

                            ProductDetailsFragment fragment = new ProductDetailsFragment();
                            fragment.setArguments(bundle);

                            getParentFragmentManager().beginTransaction()
                                    .add(R.id.container, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        binding.productLatestSection.itemSectionContainer
                                .setAdapter(adapter);

                    } else {
                        binding.productLatestSection.getRoot().setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    Log.e("HomeFragment", "Error loading latest products", e);
                });
    }



    private void loadAllProducts() {
    FirebaseFirestore.getInstance()
            .collection("products")
            .whereEqualTo("status", true)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (binding == null) return;

                if (!queryDocumentSnapshots.isEmpty()) {
                    List<Product> products = queryDocumentSnapshots
                            .toObjects(Product.class);

                    binding.allProductsCount.setText(
                            products.size() + " products");

                    binding.allProductsRecycler.setLayoutManager(
                            new GridLayoutManager(getContext(), 2));

                    binding.allProductsRecycler
                            .setNestedScrollingEnabled(false);

                    SectionAdapter adapter = new SectionAdapter(
                            products, product -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("productId",
                                product.getProductId());

                        ProductDetailsFragment fragment =
                                new ProductDetailsFragment();
                        fragment.setArguments(bundle);

                        getParentFragmentManager()
                                .beginTransaction()
                                .add(R.id.container, fragment)
                                .addToBackStack(null)
                                .commit();
                    });

                    binding.allProductsRecycler.setAdapter(adapter);

                } else {
                    binding.allProductsCount.setText("No products");
                }
            })
            .addOnFailureListener(e -> {
                if (binding == null) return;
                Log.e("HomeFragment", "Error loading all products", e);
            });
}
    private void loadCarouselImages() {
        Log.d("Carousel", "Fetching carousel images...");

        FirebaseFirestore.getInstance()
                .collection("carousel")
                .orderBy("order")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("Carousel", "Docs found: " +
                            queryDocumentSnapshots.size());

                    if (binding == null) return;

                    List<String> imageUrls = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String url = doc.getString("imageUrl");
                        Log.d("Carousel", "URL: " + url);
                        if (url != null && !url.isEmpty()) {
                            imageUrls.add(url);
                        }
                    }

                    if (!imageUrls.isEmpty()) {
                        setupCarousel(imageUrls);
                    } else {
                        Log.d("Carousel", "No images found!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Carousel", "Error: " + e.getMessage());
                });
    }

    private void setupCarousel(List<String> imageUrls) {
        if (binding == null) return;

        Log.d("Carousel", "Setting up carousel with " +
                imageUrls.size() + " images");

        carouselAdapter = new CarouselAdapter(requireContext(), imageUrls);
        binding.carouselViewpager.setAdapter(carouselAdapter);
        binding.carouselViewpager.setOffscreenPageLimit(1);

        binding.carouselDots.attachTo(binding.carouselViewpager);

        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null || carouselAdapter == null) return;
                int total = carouselAdapter.getItemCount();
                if (total == 0) return;
                currentCarouselPage = (currentCarouselPage + 1) % total;
                binding.carouselViewpager.setCurrentItem(
                        currentCarouselPage, true);
                carouselHandler.postDelayed(this, 3000);
            }
        };
        carouselHandler.postDelayed(carouselRunnable, 3000);

        binding.carouselViewpager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        currentCarouselPage = position;
                        carouselHandler.removeCallbacks(carouselRunnable);
                        carouselHandler.postDelayed(carouselRunnable, 3000);
                    }
                });
    }

    private void startAutoSwipe() {
        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null || carouselAdapter == null) return;
                int total = carouselAdapter.getItemCount();
                if (total == 0) return;
                currentCarouselPage = (currentCarouselPage + 1) % total;
                binding.carouselViewpager.setCurrentItem(
                        currentCarouselPage, true);
                carouselHandler.postDelayed(this, 3000);
            }
        };
        carouselHandler.postDelayed(carouselRunnable, 3000);
    }



    @Override
    public void onPause() {
        super.onPause();
        carouselHandler.removeCallbacks(carouselRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (carouselRunnable != null) {
            carouselHandler.postDelayed(carouselRunnable, 3000);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        carouselHandler.removeCallbacks(carouselRunnable);
        binding = null;
    }
}
