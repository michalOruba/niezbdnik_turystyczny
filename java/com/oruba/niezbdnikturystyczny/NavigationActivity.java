package com.oruba.niezbdnikturystyczny;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class NavigationActivity extends Activity{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_layout);
    }
}