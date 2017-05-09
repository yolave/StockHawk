package com.udacity.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import timber.log.Timber;

public class StockHawkService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    private class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private Cursor items;
        private Context mContext;
        private int mAppWidgetId;
        private final DecimalFormat dollarFormatWithPlus;
        private final DecimalFormat dollarFormat;
        private final DecimalFormat percentageFormat;

        StackRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus.setPositivePrefix("+$");
            percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
            percentageFormat.setMaximumFractionDigits(2);
            percentageFormat.setMinimumFractionDigits(2);
            percentageFormat.setPositivePrefix("+");
        }

        @Override
        public void onCreate() {
            initCursor();
            if(items != null){
                items.moveToFirst();
            }
        }

        private void initCursor(){
            if(items != null) {
                items.close();
            }
            long identityToken = Binder.clearCallingIdentity();

            QuoteSyncJob.syncImmediately(mContext);
            QuoteSyncJob.initialize(mContext);

            items = mContext.getContentResolver().query(Contract.Quote.URI,
                    Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                    null, null, Contract.Quote.COLUMN_SYMBOL);
            Binder.restoreCallingIdentity(identityToken);
        }

        @Override
        public void onDataSetChanged() {
            initCursor();
        }

        @Override
        public void onDestroy() {
            items.close();
        }

        @Override
        public int getCount() {
            return items.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            Timber.d("Retrieving data at position " + position + "...");
            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
            items.moveToPosition(position);
            //Symbol
            String symbol = items.getString(items.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
            remoteViews.setTextViewText(R.id.symbol, symbol);
            //Price
            remoteViews.setTextViewText(R.id.price,dollarFormat.format(items.getFloat(Contract.Quote.POSITION_PRICE)));
            //Change
            float rawAbsoluteChange = items.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = items.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            if (rawAbsoluteChange > 0) {
                remoteViews.setInt(R.id.change
                        ,"setBackgroundResource"
                        ,R.drawable.percent_change_pill_green);
            }
            else {
                remoteViews.setInt(R.id.change
                        ,"setBackgroundResource"
                        ,R.drawable.percent_change_pill_red);
            }

            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);

            if (PrefUtils.getDisplayMode(mContext)
                    .equals(mContext.getString(R.string.pref_display_mode_absolute_key))) {
                remoteViews.setTextViewText(R.id.change,change);
            }
            else {
                remoteViews.setTextViewText(R.id.change,percentage);
            }

            Bundle extras = new Bundle();
            extras.putString(StockHawkWidgetProvider.EXTRA_ITEM, symbol);
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);

            remoteViews.setOnClickFillInIntent(R.id.list_item_stock, fillInIntent);

            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
