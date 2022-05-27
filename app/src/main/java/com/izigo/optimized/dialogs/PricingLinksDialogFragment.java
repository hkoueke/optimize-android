package com.izigo.optimized.dialogs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.widget.TextViewCompat;

import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.izigo.optimized.R;
import com.izigo.optimized.models.entities.Provider;
import com.izigo.optimized.utils.Constants;
import com.izigo.optimized.utils.Utils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class PricingLinksDialogFragment extends BottomSheetDialogFragment {
    private static final String TAG = PricingLinksDialogFragment.class.getSimpleName();
    private ArrayList<Provider> mProviders;

    @NonNull
    public static PricingLinksDialogFragment newInstance(@NonNull final ArrayList<Provider> providers) {
        final PricingLinksDialogFragment fragment = new PricingLinksDialogFragment();
        final Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.KEY_FRAGMENT_PARCEL_PROVIDERS, providers);
        fragment.setArguments(bundle);
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = requireArguments();
        mProviders = args.getParcelableArrayList(Constants.KEY_FRAGMENT_PARCEL_PROVIDERS);
        if (mProviders == null)
            throw new NullPointerException(TAG.concat(": You must provide a non-null ArrayList<Providers>"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return Utils.buildLinearLayout(requireContext(),
                LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                LinearLayoutCompat.VERTICAL);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        for (Provider provider : mProviders)
            generateLinkItem(view, provider);
    }

    private void generateLinkItem(@NonNull final View view, @NonNull final Provider provider) {
        new ThreadUtils.SimpleTask<View>() {

            @Override
            public View doInBackground() {
                final View linkItem = View.inflate(requireContext(), R.layout.pricing_link_item, null);
                final AppCompatTextView title = linkItem.findViewById(R.id.title_text);
                final AppCompatTextView subTitle = linkItem.findViewById(R.id.subtitle_text);

                Picasso.get().load(provider.getIconUri()).into((CircleImageView) linkItem.findViewById(R.id.avatar_image));

                title.setTextFuture(PrecomputedTextCompat.getTextFuture(provider.getName(),
                        TextViewCompat.getTextMetricsParams(title),
                        null));

                subTitle.setTextFuture(PrecomputedTextCompat.getTextFuture(provider.getPricingUri(),
                        TextViewCompat.getTextMetricsParams(subTitle),
                        null));

                subTitle.setSelected(true);
                return linkItem;
            }

            @Override
            public void onSuccess(View result) {
                result.setOnClickListener(pricingLinkClickListener);
                ((LinearLayoutCompat) view).addView(result);
            }
        }.run();
    }

    private final View.OnClickListener pricingLinkClickListener = view -> {
        dismiss();

        final Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(((AppCompatTextView) view.findViewById(R.id.subtitle_text)).getText().toString()));

        if (IntentUtils.isIntentAvailable(intent)) {
            startActivity(intent);
        }
    };
}