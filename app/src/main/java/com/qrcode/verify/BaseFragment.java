package com.qrcode.verify;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.yokeyword.swipebackfragment.SwipeBackFragment;
import me.yokeyword.swipebackfragment.SwipeBackLayout;

/**
 * Created by Hongliang Luo on 2019/3/12.
 **/
public abstract class BaseFragment extends SwipeBackFragment {
    final static private String TAG="BaseFragment";

    protected abstract int getLayoutId();
    protected abstract void initView(View view, Bundle savedInstanceState);
    protected abstract int getTitleId();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(getLayoutId(),container,false);
        initView(view,savedInstanceState);
        setFragmentTitle();
        setSwipeBackListener();
        return attachToSwipeBack(view);
    }

    private void setFragmentTitle(){
        getHoldingActivity().toolbarManager.addTitle(getTitleId());
    }

    /**
     * 设置滑动监听
     */
    private void setSwipeBackListener(){
        getSwipeBackLayout().addSwipeListener(new SwipeBackLayout.OnSwipeListener() {
            @Override
            public void onDragStateChange(int state) {

            }

            @Override
            public void onEdgeTouch(int oritentationEdgeFlag) {
                //判断activity里是否不止一个fragment，否则不让滑动
                if(getHoldingActivity().getFragmentCount()>1){
                    Log.e(TAG,"fragment onEdgeTouch:swipeBackEnable=true");
                    setSwipeBackEnable(true);
                }else {
                    Log.e(TAG,"fragment onEdgeTouch:swipeBackEnable=false");
                    setSwipeBackEnable(false);
                }
            }

            @Override
            public void onDragScrolled(float scrollPercent) {

            }
        });
    }

    /**
     * 获取activity容器的实例，方便使用activity的方法
     * @return MainActivity
     */
    public MainActivity getHoldingActivity(){
        if(getActivity() instanceof MainActivity){
            return (MainActivity)getActivity();
        }else {
            throw new ClassCastException("activity must extends MainActivity:"+
                    " get holding activity failed!");
        }
    }

    /**
     * 当前fragment添加新的fragment
     * 从fragment中添加的fragment都不是栈底，要用跳转动画
     * @param fragment fragment
     */
    public void addFragment(BaseFragment fragment){
        getHoldingActivity().addFragmentWithAnimations(fragment);
    }

    /**
     * 移除fragment
     */
    public void removeFragment(){
        getHoldingActivity().removeFragmentWithAnimations();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getHoldingActivity().toolbarManager.removeTitle();
    }
}
