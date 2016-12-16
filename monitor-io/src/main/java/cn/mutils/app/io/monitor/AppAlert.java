package cn.mutils.app.io.monitor;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by wenhua.ywh on 2016/12/4.
 */
public class AppAlert implements View.OnClickListener {

    private Toast mToast;
    private Context mContext;
    private volatile boolean mShow;
    private boolean mShowing;

    private Object mTN;
    private Method mTNShow;
    private Method mTNHide;
    private Field mTNNextView;
    private View mView;
    private TextView mTextView;
    private CharSequence mClipboardText;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mShowingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mShow) {
                return;
            }
            if (isAppInBackground()) {
                if (mShowing) {
                    hideTN();
                }
            } else {
                if (!mShowing) {
                    showTN();
                }
            }
            mHandler.postDelayed(mShowingRunnable, 40L);
        }
    };

    private volatile long mManualCloseTime;

    public AppAlert(Context context) {
        this.mContext = context;
        initView();
        mToast = new Toast(mContext);
        mToast.setView(mView);
        mToast.setGravity(Gravity.LEFT | Gravity.BOTTOM, 0, 0);
        initTN();
    }

    public long getManualCloseTime() {
        return mManualCloseTime;
    }

    private void initView() {
        int padding = dpToPx(8);
        RelativeLayout rl = new RelativeLayout(mContext);
        rl.setBackgroundColor(0x00000000);
        rl.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        rl.setPadding(padding, 0, padding, padding);
        rl.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mTextView = new TextView(mContext);
        mTextView.setVerticalScrollBarEnabled(true);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        mTextView.setPadding(padding, padding, padding, padding);
        mTextView.setTextSize(8);
        mTextView.setTextColor(0xFFFFFFFF);
        mTextView.setBackgroundColor(0xFF000000);
        mTextView.setMaxHeight(dpToPx(260));
        mTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        rl.addView(mTextView);
        mView = rl;
        mTextView.setOnClickListener(this);
        rl.setOnClickListener(this);
    }

    private void initTN() {
        try {
            Class<?> toastClass = mToast.getClass();
            Field tnField = toastClass.getDeclaredField("mTN");
            tnField.setAccessible(true);
            mTN = tnField.get(mToast);
            Class<?> tnClass = mTN.getClass();
            mTNShow = tnClass.getMethod("show");
            mTNHide = tnClass.getMethod("hide");
            mTNNextView = tnClass.getDeclaredField("mNextView");
            mTNNextView.setAccessible(true);
            Field tnParamsField = tnClass.getDeclaredField("mParams");
            tnParamsField.setAccessible(true);
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) tnParamsField.get(mTN);
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setText(int resId) {
        setText(mContext.getText(resId));
    }

    public void setText(CharSequence s) {
        mTextView.setText(s);
    }

    public void setClipboardText(CharSequence s) {
        mClipboardText = s;
    }

    public boolean isShow() {
        return mShow;
    }

    public void show() {
        if (mShow) {
            return;
        }
        mShow = true;
        showTN();
        mHandler.post(mShowingRunnable);
    }

    private void showTN() {
        try {
            if (mTNNextView != null) {
                mTNNextView.set(mTN, mView);
            }
            if (mTNShow != null) {
                mTNShow.invoke(mTN);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mShowing = true;
    }

    public void hide() {
        if (!mShow) {
            return;
        }
        mShow = false;
        hideTN();
    }

    private void hideTN() {
        try {
            if (mTNHide != null) {
                mTNHide.invoke(mTN);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mShowing = false;
        mHandler.removeCallbacks(mShowingRunnable);
    }

    @Override
    public void onClick(View v) {
        hide();
        copyToClipboard();
        mManualCloseTime = System.currentTimeMillis();
    }

    public boolean isAppInBackground() {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(mContext.getPackageName())) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean post(Runnable action) {
        return mHandler.post(action);
    }

    public boolean postDelayed(Runnable action, long delayMillis) {
        return mHandler.postDelayed(action, delayMillis);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mContext.getResources().getDisplayMetrics());
    }

    private void copyToClipboard() {
        ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("text", mClipboardText));
    }
}
