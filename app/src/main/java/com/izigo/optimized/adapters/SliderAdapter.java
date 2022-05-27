package com.izigo.optimized.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.viewpager.widget.PagerAdapter;

import com.izigo.optimized.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SliderAdapter extends PagerAdapter {
    private final Context mContext;

    @BindView(R.id.slide_desc)
    AppCompatTextView mSlideDesc;

    @BindView(R.id.slide_heading)
    AppCompatTextView mSlideHeading;

    @BindView(R.id.slide_image)
    AppCompatImageView mSlideImage;

    private final String[] slideDescriptions = new String[0];
    private final String[] slideHeadings = new String[0];
    private final int[] slideImages = new int[0];

    public SliderAdapter(@NonNull Context context) {
        mContext = context;
    }

    public int getCount() {
        return slideHeadings.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        final View view = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.layout_onboarding_slider, container, false);

        ButterKnife.bind(this, view);

        mSlideImage.setImageResource(this.slideImages[position]);
        mSlideHeading.setText(this.slideHeadings[position]);
        mSlideDesc.setText(this.slideDescriptions[position]);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.destroyItem(container, position, object);
        container.removeView((RelativeLayout) object);
    }
}
