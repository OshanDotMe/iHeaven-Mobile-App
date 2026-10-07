package com.example.iheaven.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.iheaven.R;
import com.example.iheaven.adapter.SearchProductAdapter;
import com.example.iheaven.adapter.SectionAdapter;
import com.example.iheaven.databinding.FragmentSearchBinding;
import com.example.iheaven.model.Product;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchProductAdapter searchAdapter;
    private List<Product> searchResultList;
    private List<Product> allProducts;
    private boolean productsLoaded = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchResultList = new ArrayList<>();
        allProducts      = new ArrayList<>();

        setupRecyclerView();
        loadAllProducts();
    }

    public void updateSearch(String query) {
        if (productsLoaded) {
            filterProducts(query);
        } else {
            pendingQuery = query;
        }
    }

    private String pendingQuery = null;

    private void loadAllProducts() {
        if (binding == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;

                    allProducts.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Product product = doc.toObject(Product.class);
                        if (product != null) allProducts.add(product);
                    }

                    productsLoaded = true;
                    binding.progressBar.setVisibility(View.GONE);

                    if (pendingQuery != null) {
                        filterProducts(pendingQuery);
                        pendingQuery = null;
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("SearchFragment", "Error loading products", e);
                });
    }

    private void filterProducts(String query) {
        if (binding == null || query == null || query.isEmpty()) return;

        String queryLower = query.toLowerCase().trim();
        searchResultList.clear();

        for (Product product : allProducts) {
            boolean titleMatch = product.getTitle() != null &&
                    product.getTitle().toLowerCase().contains(queryLower);

            boolean descMatch = product.getDescription() != null &&
                    product.getDescription().toLowerCase().contains(queryLower);

            boolean categoryMatch = product.getCategoryId() != null &&
                    product.getCategoryId().toLowerCase().contains(queryLower);

            if (titleMatch || descMatch || categoryMatch) {
                searchResultList.add(product);
            }
        }

        searchAdapter.notifyDataSetChanged();

        if (searchResultList.isEmpty()) {
            binding.searchResultsRecycler.setVisibility(View.GONE);
            binding.searchEmptyState.setVisibility(View.VISIBLE);
            binding.searchResultCount.setText(
                    "No results for \"" + query + "\"");
        } else {
            binding.searchResultsRecycler.setVisibility(View.VISIBLE);
            binding.searchEmptyState.setVisibility(View.GONE);
            binding.searchResultCount.setText(
                    searchResultList.size() + " results for \"" + query + "\"");
        }
    }

    private void setupRecyclerView() {
        searchAdapter = new SearchProductAdapter(searchResultList, product -> {
            Bundle bundle = new Bundle();
            bundle.putString("productId", product.getProductId());

            ProductDetailsFragment fragment = new ProductDetailsFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.searchResultsRecycler.setLayoutManager(
                new GridLayoutManager(requireContext(), 2)
        );
        binding.searchResultsRecycler.setAdapter(searchAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}