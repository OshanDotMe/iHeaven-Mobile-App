package com.example.iheaven.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iheaven.R;
import com.example.iheaven.model.CartItem;
import com.example.iheaven.model.Product;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<CartItem> cartItems;
    private OnQuantityChangeListener changeListener;

    private OnRemoveListener removeListener;

    public CartAdapter(List<CartItem> cartItems){
        this.cartItems = cartItems;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.changeListener = listener;
    }
    public void setOnRemoveListener(OnRemoveListener listener) {
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public CartAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartAdapter.ViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products").whereEqualTo("productId", cartItem.getProductId()).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (!queryDocumentSnapshots.isEmpty()){
                    int currentPosition = holder.getAbsoluteAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION){
                        return;
                    }

                    Product product = queryDocumentSnapshots.getDocuments().get(0).toObject(Product.class);

                    holder.productTitle.setText(product.getTitle());
                    holder.productPrice.setText(String.format(Locale.US, "LKR %,.2f", product.getPrice()));
                    holder.productQuantity.setText(String.valueOf(cartItem.getQuantity()));

                    Glide.with(holder.itemView.getContext())
                            .load(product.getImages().get(0))
                            .centerCrop()
                            .into(holder.productImage);


                    holder.plusButton.setOnClickListener(v -> {
                        if (cartItem.getQuantity() < product.getStockCount()) {
                            cartItem.setQuantity(cartItem.getQuantity() + 1);
                            notifyItemChanged(currentPosition);
                            if (changeListener != null) {
                                changeListener.onChanged(cartItem);
                            }
                        }
                    });

                    holder.minusButton.setOnClickListener(v -> {
                        if (cartItem.getQuantity() > 1) {
                            cartItem.setQuantity(cartItem.getQuantity() - 1);
                            notifyItemChanged(currentPosition);
                            if (changeListener != null) {
                                changeListener.onChanged(cartItem);
                            }
                        }

                    });


                    holder.deleteButton.setOnClickListener(v -> {
                        int pos = holder.getAbsoluteAdapterPosition();
                        Log.i("Position", String.valueOf(pos));
                        if (pos != RecyclerView.NO_POSITION && removeListener != null) {
                            removeListener.onRemoved(currentPosition);
                        }
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView productImage;
        TextView productTitle;
        TextView productPrice;
        TextView productQuantity;
        ImageButton deleteButton;
        ImageButton minusButton;
        ImageButton plusButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.cart_item_image);
            productTitle = itemView.findViewById(R.id.cart_item_name);
            productPrice = itemView.findViewById(R.id.cart_item_price);
            productQuantity = itemView.findViewById(R.id.cart_item_quantity);
            deleteButton = itemView.findViewById(R.id.cart_item_delete);
            minusButton = itemView.findViewById(R.id.cart_item_minus);
            plusButton = itemView.findViewById(R.id.cart_item_plus);
        }
    }

    public interface OnQuantityChangeListener{
        void onChanged(CartItem cartItem);
    }

    public interface OnRemoveListener{
        void onRemoved(int position);
    }
}
