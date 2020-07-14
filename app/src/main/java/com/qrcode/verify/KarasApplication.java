package com.qrcode.verify;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.raizlabs.android.dbflow.config.FlowManager;


/**
 * Created by Hongliang Luo on 2019/3/15.
 **/
public class KarasApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FlowManager.init(this);
    }


    /**
     * 设置界面字体大小不随系统变化
     * @return 资源
     */
    @Override
    public Resources getResources() {
        Resources res=super.getResources();
        Configuration configuration=res.getConfiguration();
        if(configuration.fontScale!=1.0f){
            configuration.fontScale=1.0f;//app的字体缩放还原为1.0，即不缩放
            res.updateConfiguration(configuration,res.getDisplayMetrics());
        }
        return res;
    }
}
