package com.qrcode.verify;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    final static private String TAG="MainActivity";

    public ToolbarManager toolbarManager;

    private QRCodeResultBean qrCodeResult;

    public void setQrCodeResult(QRCodeResultBean qrCodeResult) {
        this.qrCodeResult = qrCodeResult;
    }

    public QRCodeResultBean getQrCodeResult() {
        return qrCodeResult;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持屏幕常亮
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//禁止横屏
        setContentView(R.layout.activity_main);
        toolbarManager=new ToolbarManager();
        toolbarManager.initToolbar();
        toolbarManager.setBackBtnEnable(false);
        addFragment(new QRCodeFragment());
        //动态权限申请（读写、手机状态读取）
        requestPermission();
    }

    public void showToast(int stringId){
        showToast(getString(stringId));
    }

    public void showToast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    /**
     * 管理toolbar和title的类
     */
    public class ToolbarManager{
        private Toolbar toolbar;
        private TextView tvTitle;
        private List<String> titleStack=new ArrayList<>();

        /**
         * toolbar的初始化
         */
        void initToolbar(){
            //toolbar
            toolbar=(Toolbar)findViewById(R.id.toolbar_activity);
            setSupportActionBar(toolbar);
            setBackBtnEnable(true);
            tvTitle=(TextView) findViewById(R.id.tv_activity_title);
        }

        void setBackBtnEnable(boolean enable){
            ActionBar actionBar=getSupportActionBar();
            if(actionBar!=null){
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setDisplayHomeAsUpEnabled(enable);
            }
    }

        /**
         * 设置标题
         * @param titleResId 字符串id
         */
        void addTitle(int titleResId){
            if(tvTitle!=null&&titleStack!=null){
                String title="";
                if(titleResId!=0){
                    title=getString(titleResId);
                }
                titleStack.add(title);
                setTitle(title);
            }
        }

        /**
         * 移除标题
         */
        void removeTitle(){
            if(titleStack!=null){
                int size=titleStack.size();
                if(size>=2){
                    setTitle(titleStack.get(size-2));
                    titleStack.remove(size-1);
                }
            }
        }

        private void setTitle(String title){
            if(tvTitle!=null&&toolbar!=null){
                tvTitle.setText(title);
                //判断是否需要隐藏toolbar
                if(title.equals("")){
                    ((AppBarLayout)toolbar.getParent()).setVisibility(View.INVISIBLE);
                }else {
                    ((AppBarLayout)toolbar.getParent()).setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public int getFragmentContainerId() {
        return R.id.fragment_container;
    }

    /**
     * 添加fragment，不带动画
     * 用于添加activity的第一个fragment
     * @param fragment 目标fragment
     */
    public void addFragment(BaseFragment fragment){
        if(fragment!=null){
            getSupportFragmentManager().beginTransaction()
                    .add(getFragmentContainerId(),fragment)
                    .addToBackStack(((Object)fragment).getClass().getSimpleName())
                    .commit();
        }
    }

    /**
     * 添加fragment，带动画
     * @param fragment 目标fragment
     */
    public void addFragmentWithAnimations(BaseFragment fragment){
        if(fragment!=null){
            int size=getSupportFragmentManager().getFragments().size();
            BaseFragment lastTopFragment=(BaseFragment) getSupportFragmentManager()
                    .getFragments().get(size-1);
            if(lastTopFragment!=null){
                Log.e(TAG,"BackStack size="+size);
                Log.e(TAG,"lastTopFragment="+lastTopFragment);
                lastTopFragment.onPause();
            }
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.push_left_in,R.anim.push_left_in,
                            R.anim.push_right_out,R.anim.push_right_out)
                    .add(getFragmentContainerId(),fragment)
                    .addToBackStack(((Object)fragment).getClass().getSimpleName())
                    .commit();
            toolbarManager.setBackBtnEnable(true);
        }
    }

    /**
     * 顶部fragment出栈
     * @return boolean 是否只剩一个fragment
     */
    public boolean removeFragmentWithAnimations(){
        if(getSupportFragmentManager().getBackStackEntryCount()>1){
            int size=getSupportFragmentManager().getFragments().size();
            if(size<=3){
                toolbarManager.setBackBtnEnable(false);
            }
            BaseFragment nowTopFragment=(BaseFragment) getSupportFragmentManager()
                    .getFragments().get(size-2);
            if(nowTopFragment!=null){
                Log.e(TAG,"BackStack size="+size);
                Log.e(TAG,"nowTopFragment="+nowTopFragment);
                nowTopFragment.onResume();
            }
            getSupportFragmentManager().popBackStack();
            return false;
        }else {
            Log.e(TAG,"there is only 1 fragment");
            return true;
        }
    }

    public int getFragmentCount(){
        return getSupportFragmentManager().getBackStackEntryCount();
    }

    /**
     * 获取动态权限
     */
    public void requestPermission(){
        RxPermissions rxPermissions=new RxPermissions(this);
        Disposable disposable = rxPermissions.request(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.VIBRATE,
                Manifest.permission.CAMERA,
//                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
//                Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//                Manifest.permission.READ_LOGS
        )
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            //已开启所有权限
                            Log.e(TAG, "已开启所有权限");
                        } else {
                            //权限被拒绝
                            Log.e(TAG, "权限被拒绝");
                            showToast("为确保程序正常运行，请前往系统设置开启相应权限");
                        }
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //记录用户首次点击返回键的时间
    private long firstTime = 0;
    @Override
    public void onBackPressed() {
        if(removeFragmentWithAnimations()){
            long secondTime = System.currentTimeMillis();
            if (secondTime - firstTime > 2000) {
                showToast("双击退出程序");
                firstTime = secondTime;
            } else{
                finish();
            }
        }
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
