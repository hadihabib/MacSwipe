package com.macswipe.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** One finger = pointer/tap/hold-and-drag; two = scroll/tap right-click; three = desktop/overview. */
final class TrackpadView extends View {
    interface GestureListener { void send(String command, double dx, double dy); }

    private final GestureListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final float density;
    private final float slop;
    private final Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accent = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long gestureStart;
    private long lastSent;
    private int fingers;
    private boolean handled, moved, dragging;
    private float startX, startY, lastX, lastY, touchX = -1, touchY = -1;
    private Runnable pendingLongPress;

    TrackpadView(Context context, GestureListener listener) {
        super(context);
        this.listener = listener;
        this.density = getResources().getDisplayMetrics().density;
        this.slop = ViewConfiguration.get(context).getScaledTouchSlop();
        dots.setColor(Color.rgb(39, 43, 64));
        ring.setColor(Color.rgb(150, 128, 250));
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2.3f * density);
        text.setColor(Color.rgb(105, 107, 137));
        text.setTextAlign(Paint.Align.CENTER);
        accent.setColor(Color.rgb(139, 110, 252));
        setClickable(true);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float gap = 27 * density;
        for (float x = gap; x < getWidth(); x += gap) {
            for (float y = gap; y < getHeight(); y += gap) canvas.drawCircle(x, y, density, dots);
        }
        text.setTextSize(15 * density);
        canvas.drawText("TOUCH SURFACE", getWidth() / 2f, getHeight() / 2f - 10 * density, text);
        text.setTextSize(12 * density);
        canvas.drawText("1 finger move   •   2 scroll   •   3 swipe", getWidth() / 2f, getHeight() / 2f + 17 * density, text);
        if (touchX >= 0) {
            canvas.drawCircle(touchX, touchY, 20 * density, ring);
            canvas.drawCircle(touchX, touchY, 3 * density, accent);
        }
    }

    private float[] center(MotionEvent event) {
        float x = 0, y = 0;
        for (int i = 0; i < event.getPointerCount(); i++) {
            x += event.getX(i); y += event.getY(i);
        }
        return new float[]{x / event.getPointerCount(), y / event.getPointerCount()};
    }

    private void cancelLongPress() {
        if (pendingLongPress != null) handler.removeCallbacks(pendingLongPress);
        pendingLongPress = null;
    }

    private void scheduleLongPress() {
        cancelLongPress();
        pendingLongPress = () -> {
            if (fingers == 1 && !moved && !dragging) {
                dragging = true;
                listener.send("drag_start", 0, 0);
            }
        };
        handler.postDelayed(pendingLongPress, 430);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float[] c = center(event);
        float x = c[0], y = c[1];
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gestureStart = System.currentTimeMillis();
                fingers = 1; handled = false; moved = false; dragging = false;
                startX = lastX = touchX = x;
                startY = lastY = touchY = y;
                scheduleLongPress();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                cancelLongPress();
                fingers = Math.max(fingers, event.getPointerCount());
                startX = lastX = touchX = x;
                startY = lastY = touchY = y;
                moved = false;
                break;
            case MotionEvent.ACTION_MOVE:
                touchX = x; touchY = y;
                if (event.getPointerCount() == fingers && !handled) {
                    float dx = x - lastX, dy = y - lastY;
                    float totalX = x - startX, totalY = y - startY;
                    switch (fingers) {
                        case 1:
                            if (Math.hypot(totalX, totalY) > slop) { moved = true; cancelLongPress(); }
                            long now = System.currentTimeMillis();
                            if (now - lastSent >= 14 && (Math.abs(dx) > .25f || Math.abs(dy) > .25f)) {
                                listener.send("move", dx * 1.45, dy * 1.45);
                                lastSent = now; lastX = x; lastY = y;
                            }
                            break;
                        case 2:
                            if (Math.hypot(totalX, totalY) > slop) moved = true;
                            if (Math.abs(dx) > .5f || Math.abs(dy) > .5f) listener.send("scroll", dx * .85, dy * .85);
                            lastX = x; lastY = y;
                            break;
                        case 3:
                            if (Math.hypot(totalX, totalY) > 66 * density) {
                                handled = true;
                                String action = Math.abs(totalX) > Math.abs(totalY)
                                    ? (totalX < 0 ? "space_right" : "space_left")
                                    : (totalY < 0 ? "mission_control" : "app_expose");
                                listener.send(action, 0, 0);
                            }
                            break;
                        default: break;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                cancelLongPress();
                break;
            case MotionEvent.ACTION_UP:
                cancelLongPress();
                if (dragging) listener.send("drag_end", 0, 0);
                else if (System.currentTimeMillis() - gestureStart < 340 && !handled) {
                    if (fingers == 1 && !moved && Math.hypot(x - startX, y - startY) < slop * 2) {
                        listener.send("click", 0, 0);
                        performClick();
                    } else if (fingers == 2 && !moved) listener.send("right_click", 0, 0);
                }
                touchX = touchY = -1; fingers = 0;
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelLongPress();
                if (dragging) listener.send("drag_end", 0, 0);
                touchX = touchY = -1; fingers = 0;
                break;
            default: break;
        }
        invalidate();
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}
