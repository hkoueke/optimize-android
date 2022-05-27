package com.izigo.optimized.models.viewmodels;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.FloatRange;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.izigo.optimized.R;
import com.izigo.optimized.models.Transaction;
import com.izigo.optimized.models.entities.Provider;
import com.izigo.optimized.utils.Utils;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import java8.lang.Doubles;
import java8.util.Objects;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

@Keep
public class ResultItem extends AbstractItem<ResultItem, ResultItem.ViewHolder> implements Comparable<ResultItem>, Parcelable {
    private final Provider provider;
    private double initialAmount;
    private double initialFee;
    private List<Transaction> transactions;
    private double optimizedFee;
    private double savings;
    private final String format;
    private final String mCurrency;

    public ResultItem(@NonNull Provider provider, @NonNull final String format, @NonNull final String currency) {
        this.provider = provider;
        this.format = format;
        this.mCurrency = currency;
    }

    public ResultItem withInitialAmount(@FloatRange(from = 0, fromInclusive = false) final double initialAmount) {
        this.initialAmount = initialAmount;
        return this;
    }

    public ResultItem withInitialFee(@FloatRange(from = 0, fromInclusive = false) final double initialFee) {
        this.initialFee = initialFee;
        return this;
    }

    public ResultItem withTransactions(@NonNull final List<Transaction> transactions) {
        if (!transactions.isEmpty()) {
            this.transactions = StreamSupport.stream(transactions).filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());
            this.optimizedFee = StreamSupport.stream(transactions).map(Transaction::getTrxFee).reduce(0d, Doubles::sum);
        } else {
            final List<Transaction> trx = new ArrayList<>(1);
            trx.add(new Transaction(this.initialAmount, this.initialFee));
            this.transactions = trx;
            this.optimizedFee = initialFee;
        }
        this.savings = this.initialFee - this.optimizedFee;
        return this;
    }

    @NonNull
    public Provider getProvider() {
        return this.provider;
    }

    public double getOptimizedFee() {
        return this.optimizedFee;
    }

    @NonNull
    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    public double getSavings() {
        return this.savings;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(@NonNull View v) {
        return new ViewHolder(v);
    }

    @Override
    public int getType() {
        return R.id.card_view;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.optimize_result_item;
    }

    @Override
    public void bindView(@NonNull ViewHolder holder, @NonNull List<Object> payloads) {
        super.bindView(holder, payloads);
        final Context context = holder.itemView.getContext();

        holder.mLayoutMore.setVisibility(View.VISIBLE);
        Picasso.get().load(this.provider.getIconUri()).into(holder.mAvatar);
        holder.mTitle.setText(this.provider.getName());

        final String subTitleText = String.format(context.getString(R.string.result_subhead_desc),
                String.format(Locale.getDefault(), this.format, this.optimizedFee), this.mCurrency,
                String.format(Locale.getDefault(), this.format, this.savings));

        holder.mSubtitle.setText(subTitleText);
        holder.mSubtitle.setSelected(true);
        buildRowItems(context, holder.mTable);

        holder.mExpand.setOnClickListener(view -> {
            final float angle = holder.mLayoutMore.getVisibility() == View.VISIBLE ? 0f : 180f;
            final boolean isExpanded = holder.mLayoutMore.getVisibility() == View.VISIBLE;
            holder.mLayoutMore.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            ViewCompat.animate(view).rotation(angle).setDuration(200).start();
        });
    }

    @Override
    public void unbindView(@NonNull ViewHolder holder) {
        super.unbindView(holder);
        holder.mAvatar.setImageDrawable(null);
        holder.mTitle.setText(null);
        holder.mSubtitle.setText(null);
        holder.mTable.removeViews(1, holder.mTable.getChildCount() - 1);
        holder.mLayoutMore.setVisibility(View.GONE);
    }

    private void buildRowItems(@NonNull final Context context, @NonNull final TableLayout table) {
        final String trxStr = context.getString(R.string.result_line_header_transaction_amount_text);
        final String trxFeeStr = context.getString(R.string.result_line_header_transaction_fee_text);
        int count = 1;
        for (Transaction tx : this.transactions) {
            final TableRow row = Utils.buildTableRow(context);
            final AppCompatTextView txtTrxCount = Utils.buildTextView(context, R.id.table_row_trx_count);
            final AppCompatTextView txtTrxDesc = Utils.buildTextView(context, R.id.table_row_trx);
            final AppCompatTextView txtTrxFee = Utils.buildTextView(context, R.id.table_row_fee);
            final String trxAmt = String.format(Locale.getDefault(), this.format, tx.getTrxAmount());
            final String trxFee = String.format(Locale.getDefault(), this.format, tx.getTrxFee());

            txtTrxCount.setText(String.valueOf(count));
            txtTrxDesc.setText(String.format(trxStr, trxAmt, mCurrency));
            txtTrxFee.setText(String.format(trxFeeStr, trxFee, mCurrency));

            row.addView(txtTrxCount);
            row.addView(txtTrxDesc);
            row.addView(txtTrxFee);
            table.addView(row);
            count++;
        }
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.avatar_image)
        CircleImageView mAvatar;

        @BindView(R.id.title_text)
        AppCompatTextView mTitle;

        @BindView(R.id.subtitle_text)
        AppCompatTextView mSubtitle;

        @BindView(R.id.expanded_menu)
        AppCompatImageView mExpand;

        @BindView(R.id.layout_more)
        View mLayoutMore;

        @BindView(R.id.row_container)
        TableLayout mTable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            ViewCompat.animate(mExpand).rotation(180f).setDuration(200).start();
            mSubtitle.setSelected(true);
        }
    }

    @Override
    public int compareTo(@NonNull final ResultItem result) {
        return Integer.compare(this.transactions.size(), result.transactions.size());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(this.provider, flags);
        dest.writeDouble(this.initialAmount);
        dest.writeDouble(this.initialFee);
        dest.writeList(this.transactions);
        dest.writeDouble(this.optimizedFee);
        dest.writeDouble(this.savings);
        dest.writeString(this.format);
        dest.writeString(this.mCurrency);
    }

    protected ResultItem(@NonNull Parcel in) {
        this.provider = in.readParcelable(Provider.class.getClassLoader());
        this.initialAmount = in.readDouble();
        this.initialFee = in.readDouble();
        this.transactions = new ArrayList<>();
        in.readList(this.transactions, Transaction.class.getClassLoader());
        this.optimizedFee = in.readDouble();
        this.savings = in.readDouble();
        this.format = in.readString();
        this.mCurrency = in.readString();
    }

    public static final Creator<ResultItem> CREATOR = new Creator<ResultItem>() {
        @Override
        public ResultItem createFromParcel(@NonNull Parcel source) {
            return new ResultItem(source);
        }

        @Override
        public ResultItem[] newArray(int size) {
            return new ResultItem[size];
        }
    };
}