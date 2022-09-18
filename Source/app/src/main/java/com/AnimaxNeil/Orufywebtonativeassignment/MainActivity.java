// Copyright (c) AnimaxNeil

package com.AnimaxNeil.Orufywebtonativeassignment;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private WebView mainWebView;
    private ProgressBar mainLoadIndicator;
    private TabLayout mainFooter;
    private long last_back_pressed_time = 0;
    private boolean dontLoadTabLink; // to fix multi tab select glitch

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainWebView = findViewById(R.id.mainWebView);
        mainLoadIndicator= findViewById(R.id.mainLoadIndicator);
        mainFooter = findViewById(R.id.mainFooter);
        setUpActivity(mainWebView);
        setUpFooter(mainFooter, Objects.requireNonNull(getFooterDataFromJSONFile()));
    }

    @Override
    public void onBackPressed() {
        if (mainWebView.canGoBack()) mainWebView.goBack();
        else {
            if (System.currentTimeMillis() - last_back_pressed_time > 2000) {
                Toast.makeText(
                        getBaseContext(), "Press Again to Exit", Toast.LENGTH_SHORT
                ).show();
            }
            else super.onBackPressed();
            last_back_pressed_time = System.currentTimeMillis();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setUpActivity(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mainLoadIndicator.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                mainLoadIndicator.setVisibility(View.GONE);
                // to fix the bug where the home url ads an extra '/' at the end
                // example: https://www.webtonative.com/ [x]
                if (url != null && url.charAt(url.length() - 1) == '/')
                    url = url.substring(0, url.length() - 1);
                for (int i = 0; i < mainFooter.getTabCount(); i++) {
                    TabLayout.Tab tab = mainFooter.getTabAt(i);
                    if (tab != null && Objects.requireNonNull(
                            tab.getContentDescription()).toString().equals(url)
                            && !tab.isSelected()) {
                        dontLoadTabLink = true;
                        tab.select();
                        break;
                    }
                }
                super.onPageFinished(view, url);
            }
        });
    }

    private void setUpFooter(TabLayout tabLayout, JSONObject jsonObject) {
        String
                textColorUnSelected = null,
                textColorSelected = null,
                bgColor = null,
                selectedIndicatorColor = null,
                height = null;

        JSONObject[] tabs = null;

        try {
            textColorUnSelected = jsonObject.getString("textColorUnSelected");
            textColorSelected = jsonObject.getString("textColorSelected");
            bgColor = jsonObject.getString("bgColor");
            selectedIndicatorColor = jsonObject.getString("selectedIndicatorColor");
            height = jsonObject.getString("height");
            JSONArray tabsArr = jsonObject.getJSONArray("tabs");
            tabs = new JSONObject[tabsArr.length()];
            for (int i = 0; i < tabs.length; i++) {
                tabs[i] = tabsArr.getJSONObject(i);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
            Log.e("JSON", "missing JSON data");
        }

        if (tabs != null && tabs.length > 0) {
            try {
                String label, link;
                int iconId;
                for (JSONObject tab : tabs) {
                    label = tab.getString("label");
                    link = tab.getString("link");
                    iconId = get_R_id_ofTabIcon(tab.getString("icon"));
                    if (label != null && link != null && iconId != 0) {
                        tabLayout.addTab(tabLayout.newTab()
                                .setText(label)
                                .setContentDescription(link)
                                .setIcon(iconId)
                        );
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        tabLayout.setBackgroundColor(
                bgColor != null ? Color.parseColor(bgColor) : Color.BLUE
        );
        tabLayout.setTabTextColors(
                textColorUnSelected != null ? Color.parseColor(textColorUnSelected) : Color.GRAY,
                textColorSelected != null ? Color.parseColor(textColorSelected) : Color.RED
        );
        tabLayout.setMinimumHeight(
                height != null ? Integer.parseInt(height) : 0
        );
        tabLayout.setSelectedTabIndicatorColor(
                 selectedIndicatorColor != null ? Color.parseColor(selectedIndicatorColor) : Color.RED
        );
        int[][] states = new int[][] {
                new int[]{android.R.attr.state_selected},
                new int[]{}
        };
        int[] colors = new int[] {
                textColorSelected != null ? Color.parseColor(textColorSelected) : Color.RED,
                textColorUnSelected != null ? Color.parseColor(textColorUnSelected) : Color.GRAY
        };
        tabLayout.setTabIconTint(new ColorStateList(states, colors));

        mainLoadIndicator.setBackgroundColor(
                bgColor != null ? Color.parseColor(bgColor) : Color.BLUE
        );

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                handleTabSelection(tab);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                handleTabSelection(tab);
            }
        });
        dontLoadTabLink = false;
        Objects.requireNonNull(tabLayout.getTabAt(0)).select();
    }

    private void handleTabSelection(TabLayout.Tab tab) {
        String tabURL = Objects.requireNonNull(tab.getContentDescription()).toString();
        if (tabURL != null && !dontLoadTabLink) {
            mainWebView.stopLoading();
            if (!tabURL.equals(mainWebView.getOriginalUrl())) {
                mainWebView.loadUrl(tabURL);
            }
        }
        dontLoadTabLink = false;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String readJsonFile(final String file) {
        if (file == null) return null;
        try {
            InputStream inputStream = getBaseContext().getAssets().open(file);
            byte[] buffer = new byte[2048];
            inputStream.read(buffer);
            inputStream.close();
            return new String(buffer, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject getFooterDataFromJSONFile() {
        try {
            return new JSONObject(readJsonFile("footer.json"))
                    .getJSONObject("stickyFooter")
                    .getJSONArray("data")
                    .getJSONObject(0);
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int get_R_id_ofTabIcon(String iconName) {
        if (iconName == null) return 0;
        switch (iconName.toLowerCase()) {
            case "home": return R.drawable.ic_tab_home;
            case "showcase": return R.drawable.ic_tab_showcase;
            case "features": return R.drawable.ic_tab_features;
            case "faq": return R.drawable.ic_tab_faq;
            case "pricing": return R.drawable.ic_tab_pricing;
            default: return 0;
        }
    }

}