package com.petergeras.maps;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;


public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter, GoogleMap.OnInfoWindowClickListener {

    private final View mWindow;
    private Context mContext;


    public CustomInfoWindowAdapter(Context mContext) {
        this.mContext = mContext;
        mWindow = LayoutInflater.from(mContext).inflate(R.layout.custom_info_layout, null);
    }


    private void renderWindowText(Marker marker, View view){

        String title = marker.getTitle();
        TextView tvTitle = view.findViewById(R.id.title);

        if (!title.equals("")) {
            tvTitle.setText(title);
        }

        String snippet = marker.getSnippet();
        TextView tvSnippet = view.findViewById(R.id.snippet);

        if (!snippet.equals("")) {
            tvSnippet.setText(snippet);
        }


        if (snippet.equals("")) {
            tvSnippet.setText(title);
        }
    }

    @Override
    public View getInfoWindow(Marker marker) {

        renderWindowText(marker, mWindow);
        return mWindow;

    }

    @Override
    public View getInfoContents(Marker marker) {
        return mWindow;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        // When the user clicks on the InfoWindow, the user is then directed to the WebActivity 
        // that brings the user to internet. 
        Intent intent = new Intent(mContext, WebActivity.class);
        mContext.startActivity(intent);
    }
}
