package com.izigo.optimized.headers;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.annotations.NotNull;
import com.izigo.optimized.R;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.ISubItem;
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem;
import com.mikepenz.fastadapter.listeners.OnClickListener;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PricingHeader<Parent extends IItem & IExpandable, SubItem extends IItem & ISubItem> extends AbstractExpandableItem<PricingHeader<Parent, SubItem>, PricingHeader.ViewHolder, SubItem> {
    private String title;
    private String plurals;
    private final Context context;
    private OnClickListener<PricingHeader> mOnClickListener;

    public PricingHeader(@NonNull Context context) {
        this.context = context;
    }

    public PricingHeader<Parent, SubItem> withTitle(@NonNull String title) {
        this.title = title;
        return this;
    }

    public PricingHeader<Parent, SubItem> withDescription(int itemCount) {
        this.plurals = context.getResources().
                getQuantityString(R.plurals.pricing_header_providers_count_plurals, itemCount, itemCount);
        return this;
    }

    /*public PricingHeader<Parent, SubItem> withOnClickListener(OnClickListener<PricingHeader> mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
        return this;
    }*/

    @Override
    public int getLayoutRes() {
        return R.layout.pricing_list_header_item;
    }

    @Override
    public int getType() {
        return R.id.pricing_header;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(@NonNull View v) {
        return new ViewHolder(v);
    }

    @Override
    public void bindView(ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);

        holder.expand.clearAnimation();
        holder.headerTitle.setText(title);
        holder.headerSubtitle.setText(plurals);
        final float angle = isExpanded() ? 180f : 0f;
        ViewCompat.animate(holder.expand).rotation(angle).setDuration(200).start();
    }

    @Override
    public void unbindView(@NotNull ViewHolder holder) {
        super.unbindView(holder);

        holder.headerTitle.setText(null);
        holder.headerSubtitle.setText(null);
        holder.expand.clearAnimation();
    }

    @Override
    public boolean isSelectable() {
        return getSubItems() == null;
    }

    //we define a clickListener in here so we can directly animate
    final private OnClickListener<PricingHeader<Parent, SubItem>> onClickListener =
            new OnClickListener<PricingHeader<Parent, SubItem>>() {
                @Override
                public boolean onClick(View v, @NonNull IAdapter adapter, @NonNull PricingHeader item, int position) {
                    if (item.getSubItems() != null) {
                        final float angle = !item.isExpanded() ? 0f : 180f;
                        ViewCompat.animate(v.findViewById(R.id.header_expand)).rotation(angle).start();
                        return mOnClickListener == null || mOnClickListener.onClick(v, adapter, item, position);
                    }
                    return mOnClickListener != null && mOnClickListener.onClick(v, adapter, item, position);
                }
            };

    @Override
    public OnClickListener<PricingHeader<Parent, SubItem>> getOnItemClickListener() {
        return onClickListener;
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.header_title)
        AppCompatTextView headerTitle;

        @BindView(R.id.header_subtitle)
        AppCompatTextView headerSubtitle;

        @BindView(R.id.header_expand)
        AppCompatImageView expand;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
