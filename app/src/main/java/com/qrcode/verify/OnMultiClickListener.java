package com.qrcode.verify;

import android.view.View;

/**
 * Created by Hongliang Luo on 2018/8/6.
 **/
public abstract class OnMultiClickListener implements View.OnClickListener{
    //连续点击按钮间隔
    private static final int CLICK_DURATION=350;
    private static long firstClickTime;
    public abstract void onMultiClick(View view);

    @Override
    public void onClick(View view) {
        long secondeClickTime=System.currentTimeMillis();
        if((secondeClickTime-firstClickTime)>=CLICK_DURATION){
            firstClickTime=secondeClickTime;
            onMultiClick(view);
        }
    }

}
