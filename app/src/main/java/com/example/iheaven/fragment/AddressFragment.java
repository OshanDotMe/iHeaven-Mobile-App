package com.example.iheaven.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.iheaven.R;
import com.example.iheaven.activity.MainActivity;
import com.example.iheaven.databinding.FragmentAddressBinding;
import com.example.iheaven.model.Order;


public class AddressFragment extends Fragment {

    private FragmentAddressBinding binding;
    private OnAddressSavedListener addressSavedListener;

    public interface OnAddressSavedListener {
        void onAddressSaved(
                Order.Address shippingAddress,
                Order.Address billingAddress
        );
    }

    public void setOnAddressSavedListener(OnAddressSavedListener listener) {
        this.addressSavedListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().findViewById(R.id.toolbar)
                .setVisibility(View.GONE);
        requireActivity().findViewById(R.id.bottom_navigation_view)
                .setVisibility(View.GONE);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.addressTitleBar.setPadding(
                    binding.addressTitleBar.getPaddingLeft(),
                    systemBars.top,
                    binding.addressTitleBar.getPaddingRight(),
                    binding.addressTitleBar.getPaddingBottom()
            );
            return WindowInsetsCompat.CONSUMED;
        });

        binding.addressBackButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        binding.sameAsShippingCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.billingFullName.setText(binding.shippingFullName.getText());
                binding.billingEmail.setText(binding.shippingEmail.getText());
                binding.billingPhone.setText(binding.shippingPhone.getText());
                binding.billingAddress.setText(binding.shippingAddress.getText());
                binding.billingCity.setText(binding.shippingCity.getText());
                binding.billingZip.setText(binding.shippingZip.getText());
                setBillingFieldsEnabled(false);
            } else {
                setBillingFieldsEnabled(true);
            }
        });

        binding.saveAddressButton.setOnClickListener(v -> {
            if (!validateFields()) return;

            Order.Address shipping = new Order.Address(
                    binding.shippingFullName.getText().toString().trim(),
                    binding.shippingEmail.getText().toString().trim(),
                    binding.shippingPhone.getText().toString().trim(),
                    binding.shippingAddress.getText().toString().trim(),
                    binding.shippingCity.getText().toString().trim(),
                    binding.shippingZip.getText().toString().trim()
            );

            Order.Address billing;
            if (binding.sameAsShippingCheckbox.isChecked()) {
                billing = new Order.Address(
                        shipping.getName(),
                        shipping.getEmail(),
                        shipping.getContact(),
                        shipping.getAddress(),
                        shipping.getCity(),
                        shipping.getPostcode()
                );
            } else {
                billing = new Order.Address(
                        binding.billingFullName.getText().toString().trim(),
                        binding.billingEmail.getText().toString().trim(),
                        binding.billingPhone.getText().toString().trim(),
                        binding.billingAddress.getText().toString().trim(),
                        binding.billingCity.getText().toString().trim(),
                        binding.billingZip.getText().toString().trim()
                );
            }

            if (addressSavedListener != null) {
                addressSavedListener.onAddressSaved(shipping, billing);
            }

            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void setBillingFieldsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.5f;
        binding.billingFullName.setEnabled(enabled);
        binding.billingEmail.setEnabled(enabled);
        binding.billingPhone.setEnabled(enabled);
        binding.billingAddress.setEnabled(enabled);
        binding.billingCity.setEnabled(enabled);
        binding.billingZip.setEnabled(enabled);
        binding.billingFullName.setAlpha(alpha);
        binding.billingEmail.setAlpha(alpha);
        binding.billingPhone.setAlpha(alpha);
        binding.billingAddress.setAlpha(alpha);
        binding.billingCity.setAlpha(alpha);
        binding.billingZip.setAlpha(alpha);
    }

    private boolean validateFields() {
        if (binding.shippingFullName.getText().toString().trim().isEmpty()) {
            binding.shippingFullName.setError("Required");
            binding.shippingFullName.requestFocus();
            return false;
        }
        if (binding.shippingEmail.getText().toString().trim().isEmpty()) {
            binding.shippingEmail.setError("Required");
            binding.shippingEmail.requestFocus();
            return false;
        }
        if (binding.shippingPhone.getText().toString().trim().isEmpty()) {
            binding.shippingPhone.setError("Required");
            binding.shippingPhone.requestFocus();
            return false;
        }
        if (binding.shippingAddress.getText().toString().trim().isEmpty()) {
            binding.shippingAddress.setError("Required");
            binding.shippingAddress.requestFocus();
            return false;
        }
        if (binding.shippingCity.getText().toString().trim().isEmpty()) {
            binding.shippingCity.setError("Required");
            binding.shippingCity.requestFocus();
            return false;
        }
        if (binding.shippingZip.getText().toString().trim().isEmpty()) {
            binding.shippingZip.setError("Required");
            binding.shippingZip.requestFocus();
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().findViewById(R.id.toolbar)
                .setVisibility(View.GONE);
        requireActivity().findViewById(R.id.bottom_navigation_view)
                .setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        requireActivity().findViewById(R.id.bottom_navigation_view)
                .setVisibility(View.VISIBLE);
    }
}