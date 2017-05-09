package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.ui.DetailStockActivity;
import com.udacity.stockhawk.ui.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class StockHawkWidgetProvider extends AppWidgetProvider {
    private static final String STOCK_DETAIL_ACTION = "com.udacity.stockhawk.widget.STOCK_DETAIL_ACTION";
    public static final String EXTRA_ITEM = "com.udacity.stockhawk.widget.EXTRA_ITEM";
    private static final String OPEN_APP_ACTION = "com.udacity.stockhawk.widget.OPEN_APP_ACTION";
    public static final String ORIGIN_WIDGET = "com.udacity.stockhawk.widget.ORIGIN_WIDGET";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for(int appWidgetId:appWidgetIds){
            Timber.d("Updating...");

            //Service
            Intent remoteServiceIntent = new Intent(context, StockHawkService.class);
            remoteServiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            remoteServiceIntent.setData(Uri.parse(remoteServiceIntent.toUri(Intent.URI_INTENT_SCHEME)));

            //Filling the views
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_stock_list);
            rv.setRemoteAdapter(R.id.list_view, remoteServiceIntent);
            rv.setEmptyView(R.id.list_view, R.id.empty_view);
            String timestamp = DATE_FORMAT.format(new Date());
            Timber.d("Timestamp -> " + timestamp);
            rv.setTextViewText(R.id.last_update,context.getString(R.string.message_last_update,timestamp));

            // Register an onClickListener to update the data
            PendingIntent updateDataPendingIntent = generateUpdateDataPendingIntent(context,appWidgetIds);
            rv.setOnClickPendingIntent(R.id.iv_refresh, updateDataPendingIntent);

            //Register an onClickListener to open the app
            PendingIntent openAppPendingIntent = generateOpenAppPendingIntent(context,appWidgetIds);
            rv.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent);

            //Setting an intent to launch a new activity when an item is pressed
            PendingIntent detailPendingIntent = generateOpenDetailPendingIntent(context,appWidgetIds);
            remoteServiceIntent.setData(Uri.parse(remoteServiceIntent.toUri(Intent.URI_INTENT_SCHEME)));

            rv.setPendingIntentTemplate(R.id.list_view, detailPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
        super.onUpdate(context,appWidgetManager,appWidgetIds);
    }

    private PendingIntent generateUpdateDataPendingIntent(Context context, int[] appWidgetIds){
        return generatePendingIntent(context,appWidgetIds,AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    }

    private PendingIntent generateOpenAppPendingIntent(Context context, int[] appWidgetIds){
        return generatePendingIntent(context,appWidgetIds,OPEN_APP_ACTION);
    }

    private PendingIntent generateOpenDetailPendingIntent(Context context, int[] appWidgetIds){
        return generatePendingIntent(context,appWidgetIds,StockHawkWidgetProvider.STOCK_DETAIL_ACTION);
    }

    private PendingIntent generatePendingIntent(Context context, int[] appWidgetIds, String action){
        Intent intent = new Intent(context, StockHawkWidgetProvider.class);

        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        return PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.d("Receiving data. Intent -> " + intent.getAction());
        ComponentName thisWidget = new ComponentName(context, StockHawkWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        if (STOCK_DETAIL_ACTION.equals(intent.getAction())) {
            String symbol = intent.getStringExtra(EXTRA_ITEM);
            Intent detailStockIntent = new Intent(context,DetailStockActivity.class);
            detailStockIntent.putExtra(context.getString(R.string.intent_stock_selected),symbol);
            detailStockIntent.putExtra(context.getString(R.string.intent_origin),ORIGIN_WIDGET);
            detailStockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(detailStockIntent);
        }
        else if(AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())){
            retrieveListData(context,appWidgetIds);
        }
        else if (OPEN_APP_ACTION.equals(intent.getAction())) {
            Intent openAppIntent = new Intent(context,MainActivity.class);
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(openAppIntent);
        }
        super.onReceive(context, intent);
    }

    private void retrieveListData(Context context, int[] appWidgetIds){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        Intent remoteServiceIntent = new Intent(context, StockHawkService.class);
        remoteServiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds);
        remoteServiceIntent.setData(Uri.parse(remoteServiceIntent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_stock_list);

        String timestamp = DATE_FORMAT.format(new Date());
        Timber.d("Timestamp -> " + timestamp);

        rv.setTextViewText(R.id.last_update,context.getString(R.string.message_last_update,timestamp));
        rv.setEmptyView(R.id.list_view, R.id.empty_view);
        appWidgetManager.updateAppWidget(appWidgetIds, rv);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view);
    }
}
