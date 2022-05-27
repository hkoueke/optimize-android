package com.izigo.optimized.models.viewmodels;

import android.content.Context;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.izigo.optimized.App;
import com.izigo.optimized.R;
import com.izigo.optimized.models.entities.Line;
import com.izigo.optimized.utils.Utils;
import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.ISubItem;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class PricingItem<T extends IItem & IExpandable> extends AbstractItem<PricingItem<T>, PricingItem.ViewHolder>
        implements ISubItem<PricingItem, T> {

    private final List<Line> pricingList;
    private final String logoUri;
    private final String providerName;
    private T mParent;

    public PricingItem(@NonNull String providerName, @NonNull String logoUri, @NonNull List<Line> pricingList) {
        this.providerName = providerName;
        this.pricingList = pricingList;
        this.logoUri = logoUri;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.pricing_item;
    }

    @Override
    public int getType() {
        return R.id.pricing_layout;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(@NonNull View view) {
        return new ViewHolder(view);
    }

    @Override
    public void bindView(@NonNull ViewHolder holder, @NonNull List<Object> payloads) {
        super.bindView(holder, payloads);
        Picasso.get().load(this.logoUri).into(holder.mAvatar);
        holder.mTitle.setText(providerName);
        buildRowItems(holder.itemView.getContext(), holder.mTable);
    }

    @Override
    public void unbindView(@NonNull ViewHolder holder) {
        super.unbindView(holder);
        holder.mTitle.setText(null);
        holder.mAvatar.setImageDrawable(null);
        holder.mTable.removeViews(1, holder.mTable.getChildCount() - 1);
    }

    private void buildRowItems(@NonNull Context context, @NonNull final TableLayout table) {
        for (Line line : this.pricingList) {
            final TableRow row = Utils.buildTableRow(context);
            final AppCompatTextView txtTrxFrom = Utils.buildTextView(context, R.id.table_row_trx_count);
            final AppCompatTextView txtTrxTo = Utils.buildTextView(context, R.id.table_row_trx);
            final AppCompatTextView txtTrxFee = Utils.buildTextView(context, R.id.table_row_fee);

            final String trxFrom = String.format(Locale.getDefault(), App.NUMBER_FORMAT, line.getLower());
            final String trxTo = String.format(Locale.getDefault(), App.NUMBER_FORMAT, line.getUpper());

            final String trxFee = line.getFee() < 1
                    ? String.valueOf(line.getFee() * 100).concat(line.getFee() <= 0 ? "" : " %")
                    : String.format(Locale.getDefault(), App.NUMBER_FORMAT, line.getFee());

            txtTrxFrom.setText(trxFrom);
            txtTrxTo.setText(trxTo);
            txtTrxFee.setText(trxFee);

            row.addView(txtTrxFrom);
            row.addView(txtTrxTo);
            row.addView(txtTrxFee);
            table.addView(row);
        }
    }

    @Override
    public T getParent() {
        return mParent;
    }

    @Override
    public PricingItem withParent(@NonNull T parent) {
        mParent = parent;
        return this;
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.avatar_image)
        CircleImageView mAvatar;
        @BindView(R.id.expanded_menu)
        AppCompatImageView mExpand;
        @BindView(R.id.layout_more)
        View mLayoutMore;
        @BindView(R.id.row_container)
        TableLayout mTable;
        @BindView(R.id.title_text)
        AppCompatTextView mTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            mExpand.setOnClickListener(view -> {
                final int angle = mLayoutMore.getVisibility() == View.VISIBLE ? 0 : 180;
                final boolean isExpanded = mLayoutMore.getVisibility() == View.VISIBLE;
                mLayoutMore.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                ViewCompat.animate(view).rotation(angle).setDuration(200).start();
            });

            mLayoutMore.setVisibility(View.GONE);
        }
    }
}