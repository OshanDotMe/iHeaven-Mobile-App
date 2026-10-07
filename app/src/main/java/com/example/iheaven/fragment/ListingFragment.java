package com.example.iheaven.fragment;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.iheaven.R;
import com.example.iheaven.adapter.ListeningAdapter;
import com.example.iheaven.databinding.FragmentListingBinding;
import com.example.iheaven.model.Product;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;


public class ListingFragment extends Fragment {

    private FragmentListingBinding binding;
    private ListeningAdapter adapter;

    private String categoryId;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString("categoryId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentListingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.recyclerViewListening.setLayoutManager(new GridLayoutManager(getContext(), 2));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("products")
                .whereEqualTo("categoryId", categoryId)
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(ds ->{
                   if (!ds.isEmpty()){
                       List<Product> products = ds.toObjects(Product.class);

                       adapter = new ListeningAdapter(products, product -> {
                           Bundle bundle = new Bundle();
                           bundle.putString("productId", product.getProductId());

                           ProductDetailsFragment fragment = new ProductDetailsFragment();
                           fragment.setArguments(bundle);

                           getParentFragmentManager().beginTransaction()
                                   .add(R.id.container, fragment)
                                   .addToBackStack(null)
                                   .commit();
                       });

                       binding.recyclerViewListening.setAdapter(adapter);
                   }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("FireStore", "Error+ "+e.getMessage());
                    }
                });
        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }
}