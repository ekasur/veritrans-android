package com.midtrans.sdk.ui.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;

import com.midtrans.sdk.ui.MidtransUiKit;
import com.midtrans.sdk.ui.themes.BaseColorTheme;
import com.midtrans.sdk.ui.utils.Logger;

/**
 * Created by ziahaqi on 3/5/17.
 */

public class ColoredTextInputEditText extends TextInputEditText {
    public ColoredTextInputEditText(Context context) {
        super(context);
        init();
    }

    public ColoredTextInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColoredTextInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        MidtransUiKit uiSdk = MidtransUiKit.getInstance();
        if (uiSdk != null && uiSdk.getColorTheme() != null) {
            BaseColorTheme colorTheme = uiSdk.getColorTheme();
            try {
                setSupportBackgroundTintList(new ColorStateList(new int[][]{{0}}, new int[]{colorTheme.getSecondaryColor()}));
            } catch (RuntimeException exception) {
                Logger.e(this.getClass().getSimpleName(), exception.getMessage());
            }
        }
    }
}
