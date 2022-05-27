package com.izigo.optimized.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.FragmentUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.izigo.optimized.App;
import com.izigo.optimized.R;
import com.izigo.optimized.dialogs.PricingLinksDialogFragment;
import com.izigo.optimized.headers.PricingHeader;
import com.izigo.optimized.models.entities.Country;
import com.izigo.optimized.models.entities.Operation;
import com.izigo.optimized.models.entities.Provider;
import com.izigo.optimized.models.viewmodels.PricingItem;
import com.izigo.optimized.utils.Constants;
import com.izigo.optimized.utils.Utils;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.expandable.ExpandableExtension;
import com.mikepenz.itemanimators.SlideInOutLeftAnimator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;


public class PricingFragment extends Fragment {
    private static final String TAG = PricingFragment.class.getSimpleName();
    private BottomSheetDialog mLinkSheet;
    private ArrayList<Provider> mProviders;

    @BindView(R.id.price_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private FastItemAdapter<IItem<?, ?>> mFastItemAdapter;

    @NonNull
    public static PricingFragment newInstance(@NonNull ArrayList<Provider> providers) {
        final PricingFragment fragment = new PricingFragment();
        final Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.KEY_FRAGMENT_PARCEL_PROVIDERS, providers);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        final Bundle args = requireArguments();
        mProviders = args.getParcelableArrayList(Constants.KEY_FRAGMENT_PARCEL_PROVIDERS);

        if (mProviders == null)
            throw new NullPointerException(TAG.concat(": Provide a non-null ArrayList<Provider>"));

        //create our FastAdapter which will manage everything
        mFastItemAdapter = new FastItemAdapter<>();
        mFastItemAdapter.addExtension(new ExpandableExtension<>());
        mFastItemAdapter.setHasStableIds(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_pricing, container, false);
        ButterKnife.bind(this, root);
        initViews(requireContext());
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Do heavy computation with a task
        computePricingList(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mToolbar.setTitle(R.string.action_prices);

        final String subTitle = String.format(getString(R.string.pricing_toolbar_subtitle_currency), App.APP_CURRENCY);
        mToolbar.postDelayed(() -> mToolbar.setSubtitle(subTitle), 1000);

        final AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(mToolbar);
        Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_pricing, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            FragmentUtils.remove(this);
            FragmentUtils.pop(getParentFragmentManager(), true);
        } else
            PricingLinksDialogFragment.newInstance(mProviders).show(getParentFragmentManager(), "pricing");

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState = mFastItemAdapter.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void updateCountryObject(@NonNull Country country) {
        mProviders = (ArrayList<Provider>) country.getProviders();
        EventBus.getDefault().removeAllStickyEvents();
        Log.d(TAG, "New 'Country' object received: previous object updated.");
    }

    private void initViews(@NonNull Context context) {
        if (mLinkSheet == null)
            mLinkSheet = Utils.getBottomSheetDialog(requireContext());

        //get our recyclerView and do basic setup
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setItemAnimator(new SlideInOutLeftAnimator(mRecyclerView));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        //Assign adapter to recyclerview
        mRecyclerView.setAdapter(mFastItemAdapter);
    }

    private void computePricingList(Bundle savedInstanceState) {
        new ThreadUtils.SimpleTask<ArrayList<IItem<?, ?>>>() {
            @Override
            public ArrayList<IItem<?, ?>> doInBackground() {
                final String[] operations = requireContext().getResources().getStringArray(R.array.list_operation_type);
                final ArrayList<IItem<?, ?>> items = new ArrayList<>(operations.length);

                for (int i = 0; i < operations.length; i++) {
                    int count = 0;
                    final PricingHeader header = new PricingHeader<>(requireContext());
                    final ArrayList<IItem<?, ?>> subItems = new ArrayList<>();

                    for (Provider p : mProviders) {
                        for (Operation op : p.getOperations()) {
                            if (op.getId() == i + 1 && !op.getPricing().isEmpty()) {
                                count++;
                                subItems.add(new PricingItem<>(p.getName(), p.getIconUri(), op.getPricing()));
                                break;
                            }
                        }
                    }
                    header.withTitle(operations[i]).withDescription(count).withSubItems(subItems).withIdentifier(i);
                    items.add(header);
                }
                return items;
            }

            @Override
            public void onSuccess(ArrayList<IItem<?, ?>> result) {
                mFastItemAdapter.add(result);
                if (savedInstanceState != null)
                    mFastItemAdapter.withSavedInstanceState(savedInstanceState);
                else {
                    mFastItemAdapter.getExtension(ExpandableExtension.class).expand(0);
                }
            }
        }.run();
    }
}