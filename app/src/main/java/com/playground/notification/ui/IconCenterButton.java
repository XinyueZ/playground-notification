package com.playground.notification.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import com.chopping.utils.DeviceUtils;

public final class IconCenterButton extends AppCompatButton {
	public IconCenterButton(Context context) {
		super(context);
		init(context);
	}

	public IconCenterButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public IconCenterButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		DeviceUtils.ScreenSize screenSize = DeviceUtils.getScreenSize(context);
		setPadding((int) (screenSize.Width / 3f), 0, 0, 0);
	}
}
