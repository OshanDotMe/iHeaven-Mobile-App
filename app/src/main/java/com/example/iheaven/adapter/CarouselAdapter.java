package com.example.iheaven.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iheaven.R;

import java.util.List;

public class CarouselAdapter extends
        RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private List<String> imageUrls;
    private Context context;

    public CarouselAdapter(Context context, List<String> imageUrls) {
        this.context   = context;
        this.imageUrls = imageUrls;
    }

    public static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public CarouselViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carousel_image);
        }
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carousel, parent, false);

        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CarouselViewHolder holder, int position) {
        Glide.with(context)
                .load(imageUrls.get(position))
                .centerCrop()
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }
}