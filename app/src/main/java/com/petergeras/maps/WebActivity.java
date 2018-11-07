package com.petergeras.maps;

import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends Activity {

    private WebView mWebView;
    private PlaceInfo mPlaceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        mPlaceInfo = new PlaceInfo();

        // Back button
        if(getActionBar() != null){
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }


        mWebView = findViewById(R.id.webView);

        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl(MapsActivity.mWebsiteUri);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.supportZoom();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // The user can use the back button to return to the previous web page if needed
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
