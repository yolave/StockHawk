package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.widget.StockHawkWidgetProvider;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import au.com.bytecode.opencsv.CSVReader;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class DetailStockActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    @BindView(R.id.stock_chart)
    LineChart stockChart;
    @BindView(R.id.error_message)
    TextView errorMessage;

    private String mSymbol;
    private String mOrigin;
    private static final int STOCK_LOADER = 0;
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_stock);

        ButterKnife.bind(this);

        mSymbol = getIntent().getStringExtra(getString(R.string.intent_stock_selected));
        mOrigin = getIntent().getStringExtra(getString(R.string.intent_origin));
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.makeUriForStock(mSymbol),
                null,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data.getCount() != 0){
            try {
                data.moveToFirst();
                String historicalPrices = data.getString(data.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
                if(TextUtils.isEmpty(historicalPrices)){
                    showError();
                    return;
                }

                CSVReader reader = new CSVReader(new StringReader(historicalPrices));
                List<Entry> entries = new ArrayList<>();

                final List<String> formattedStockTs = new ArrayList<>();

                List<String[]> lines = reader.readAll();
                Collections.sort(lines, new Comparator<String[]>() {
                    @Override
                    public int compare(String[] o1, String[] o2) {
                        Long timestamp1 = Long.parseLong(o1[0]);
                        Long timestamp2 = Long.parseLong(o2[0]);
                        return timestamp1.compareTo(timestamp2);
                    }
                });

                for (int i = 0; i < lines.size();i++) {
                    String[] line = lines.get(i);
                    long timestamp = Long.parseLong(line[0]);
                    float stock = Float.parseFloat(line[1]);

                    entries.add(new Entry(i, stock));

                    formattedStockTs.add(SIMPLE_DATE_FORMAT.format(new Date(timestamp)));
                    Timber.d("[count, timestamp, stock] -> [%s,%s,%s]",i,timestamp,stock);
                }

                LineDataSet dataSet = new LineDataSet(entries,mSymbol);
                dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
                dataSet.setColor(ContextCompat.getColor(this,R.color.colorAccent));
                dataSet.setValueTextColor(ContextCompat.getColor(this,android.R.color.white));

                //Formatting X axis values
                IAxisValueFormatter xAxisFormatter = new IAxisValueFormatter() {
                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        return formattedStockTs.get((int)value);
                    }
                };

                LineData lineData = new LineData(dataSet);
                stockChart.setData(lineData);
                stockChart.getDescription().setEnabled(false);

                XAxis xAxis = stockChart.getXAxis();
                xAxis.setGranularity(1f);
                xAxis.setValueFormatter(xAxisFormatter);
                xAxis.setTextColor(ContextCompat.getColor(this,android.R.color.white));

                stockChart.getAxisLeft().setTextColor(ContextCompat.getColor(this,android.R.color.white));
                stockChart.getAxisRight().setTextColor(ContextCompat.getColor(this,android.R.color.white));
                stockChart.getLegend().setTextColor(ContextCompat.getColor(this,android.R.color.white));


                stockChart.invalidate();
                showChart();
            }
            catch (IOException ex){
                Timber.e("Error retrieving historical stock price",ex);
                showError();
            }
        }
        else{
            showError();
        }
    }

    @Override
    public void onBackPressed() {
        if(mOrigin.equals(StockHawkWidgetProvider.ORIGIN_WIDGET)){
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
            finish();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(android.R.id.home == item.getItemId() && mOrigin.equals(StockHawkWidgetProvider.ORIGIN_WIDGET)){
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        stockChart.invalidate();
    }

    private void showError(){
        stockChart.setVisibility(View.INVISIBLE);
        errorMessage.setVisibility(View.VISIBLE);
    }

    private void showChart(){
        stockChart.setVisibility(View.VISIBLE);
        errorMessage.setVisibility(View.INVISIBLE);
    }
}
