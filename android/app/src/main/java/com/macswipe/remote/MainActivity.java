package com.macswipe.remote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private final int bg = Color.rgb(14, 15, 24);
    private final int panel = Color.rgb(24, 26, 41);
    private final int border = Color.rgb(59, 57, 89);
    private final int bright = Color.rgb(170, 150, 255);
    private final int subtle = Color.rgb(157, 159, 181);
    private RemoteSocket remote;
    private EditText ipField, tokenField;
    private Button connectButton;
    private TextView status;
    private boolean connected;

    private int dp(int value) { return (int) (getResources().getDisplayMetrics().density * value); }

    private GradientDrawable shape(int color, float radius, int stroke) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(color);
        result.setCornerRadius(dp((int) radius));
        result.setStroke(dp(1), stroke);
        return result;
    }
    private GradientDrawable shape(int color, float radius) { return shape(color, radius, border); }

    private TextView label(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }

    private EditText field(String hint, String value, boolean secret) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setText(value);
        field.setHint(hint);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.rgb(114, 113, 137));
        field.setTextSize(14);
        field.setInputType(secret ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
            : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
        field.setSelectAllOnFocus(true);
        field.setBackground(shape(panel, 12));
        field.setPadding(dp(12), 0, dp(12), 0);
        return field;
    }

    private Button action(String text, String command) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(12); b.setAllCaps(false);
        b.setTextColor(Color.rgb(230, 227, 255));
        b.setBackgroundTintList(null);
        b.setBackground(shape(panel, 13));
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setOnClickListener(v -> remote.send(command));
        return b;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        SharedPreferences prefs = getSharedPreferences("connection", MODE_PRIVATE);
        remote = new RemoteSocket((ready, info) -> {
            connected = ready;
            status.setText((ready ? "●  " : "○  ") + info);
            status.setTextColor(ready ? Color.rgb(126, 231, 189) : subtle);
            connectButton.setText(ready ? "Disconnect" : "Connect");
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(dp(18), dp(16), dp(18), dp(14));
        setContentView(root);

        TextView wordmark = label("MACSWIPE", 12, bright, true);
        wordmark.setLetterSpacing(.22f);
        root.addView(wordmark, new LinearLayout.LayoutParams(-1, dp(23)));
        root.addView(label("Your Mac, at your fingertips.", 22, Color.WHITE, true), new LinearLayout.LayoutParams(-1, dp(40)));
        root.addView(label("Connect over the same Wi-Fi network", 12, subtle, false), new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout address = new LinearLayout(this);
        address.setOrientation(LinearLayout.HORIZONTAL);
        ipField = field("Mac IP · 192.168.x.x", prefs.getString("ip", ""), false);
        LinearLayout.LayoutParams ipLayout = new LinearLayout.LayoutParams(0, dp(48), 1);
        ipLayout.rightMargin = dp(8);
        address.addView(ipField, ipLayout);
        connectButton = new Button(this);
        connectButton.setText("Connect");
        connectButton.setAllCaps(false);
        connectButton.setTextColor(Color.WHITE);
        connectButton.setTextSize(13);
        connectButton.setBackgroundTintList(null);
        connectButton.setBackground(shape(Color.rgb(103,79,214), 12, Color.rgb(103,79,214)));
        address.addView(connectButton, new LinearLayout.LayoutParams(dp(108), dp(48)));
        root.addView(address, new LinearLayout.LayoutParams(-1, dp(48)));

        tokenField = field("Token shown on the Mac", prefs.getString("token", ""), true);
        LinearLayout.LayoutParams tokenParams = new LinearLayout.LayoutParams(-1, dp(48));
        tokenParams.topMargin = dp(8);
        root.addView(tokenField, tokenParams);
        status = label("○  Not connected", 12, subtle, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, dp(35));
        statusParams.topMargin = dp(10);
        root.addView(status, statusParams);

        connectButton.setOnClickListener(v -> {
            if (connected) { remote.disconnect(); return; }
            String host = ipField.getText().toString().trim();
            String token = tokenField.getText().toString().trim();
            if (host.isEmpty() || token.isEmpty()) {
                status.setText("Enter Mac IP and pairing token");
                return;
            }
            prefs.edit().putString("ip", host).putString("token", token).apply();
            InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(ipField.getWindowToken(), 0);
            ipField.clearFocus(); tokenField.clearFocus();
            status.setText("○  Connecting…");
            remote.connect(host, token);
        });

        TextView quick = label("QUICK ACTIONS", 11, subtle, true);
        quick.setLetterSpacing(.12f);
        root.addView(quick, new LinearLayout.LayoutParams(-1, dp(28)));
        addRow(root, new String[]{"← Desktop", "Mission Ctrl", "Desktop →"},
            new String[]{"space_left", "mission_control", "space_right"}, 48, 0);
        addRow(root, new String[]{"App Exposé", "Copy", "Paste", "Esc"},
            new String[]{"app_expose", "copy", "paste", "escape"}, 43, 6);

        TextView padTitle = label("TRACKPAD", 11, subtle, true);
        padTitle.setLetterSpacing(.12f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, dp(32));
        titleParams.topMargin = dp(15);
        root.addView(padTitle, titleParams);
        TrackpadView pad = new TrackpadView(this, (command, dx, dy) -> remote.send(command, dx, dy));
        pad.setBackground(shape(panel, 20, Color.rgb(67, 61, 107)));
        pad.setContentDescription("Gesture trackpad for controlling Mac");
        root.addView(pad, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView hints = label("TAP  click      TWO-FINGER TAP  right-click      HOLD  drag\nTHREE-FINGER SWIPE  switch desktops / window overview", 11, subtle, false);
        hints.setGravity(Gravity.CENTER);
        hints.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, dp(60));
        hintParams.topMargin = dp(3);
        root.addView(hints, hintParams);
    }

    private void addRow(LinearLayout root, String[] titles, String[] commands, int height, int top) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < titles.length; i++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(height), 1);
            if (i > 0) params.leftMargin = dp(6);
            row.addView(action(titles[i], commands[i]), params);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(height));
        params.topMargin = dp(top);
        root.addView(row, params);
    }

    @Override protected void onDestroy() { remote.shutdown(); super.onDestroy(); }
}
