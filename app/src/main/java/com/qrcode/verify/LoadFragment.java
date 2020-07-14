package com.qrcode.verify;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.qrcode.verify.Database.Product;
import com.qrcode.verify.Utils.CsvUtils;
import com.qrcode.verify.Utils.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

import cn.qqtheme.framework.picker.FilePicker;
import cn.qqtheme.framework.util.StorageUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 */
public class LoadFragment extends BaseFragment {
    final static private String TAG="LoadFragment";

    private ImageView ivLoadFile;
    private TextView tvUpdateTime;
    private TextView tvDataNumbers;
    public LoadFragment() {
        // Required empty public constructor
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_load;
    }

    @Override
    protected void initView(View view, Bundle savedInstanceState) {
        ivLoadFile=view.findViewById(R.id.iv_load_file);
        ivLoadFile.setOnClickListener(new OnMultiClickListener() {
            @Override
            public void onMultiClick(View view) {
                showFilePicker();
            }
        });
        tvUpdateTime=view.findViewById(R.id.tv_database_update_time);
        tvDataNumbers=view.findViewById(R.id.tv_data_numbers);
        updateTip();
    }

    @Override
    protected int getTitleId() {
        return R.string.data_input;
    }


    private void showFilePicker(){
        FilePicker filePicker=new FilePicker(getHoldingActivity(),FilePicker.FILE);
        filePicker.setShowHideDir(false);
        filePicker.setShowUpDir(true);
        filePicker.setUpIcon(getResources().getDrawable(R.drawable.ic_upback));
        filePicker.setTopLineColor(getResources().getColor(R.color.gray_light));
        filePicker.setTitleText("选择文件");
        filePicker.setPressedTextColor(getResources().getColor(R.color.colorPrimary));
        filePicker.setTitleTextColor(getResources().getColor(R.color.colorPrimary));
        filePicker.setSubmitTextColor(getResources().getColor(R.color.colorPrimary));
        filePicker.setFileIcon(getResources().getDrawable(R.drawable.ic_wenjian_green));
        filePicker.setFolderIcon(getResources().getDrawable(R.drawable.ic_wenjianjia_green));
        filePicker.setAllowExtensions(new String[]{".csv"});
        filePicker.setRootPath(StorageUtils.getExternalRootPath());
        filePicker.setOnFilePickListener(new FilePicker.OnFilePickListener() {
            @Override
            public void onFilePicked(final String currentPath) {
                //通过文件读取数据生成list
                getHoldingActivity().showToast("正在导入数据");
                Log.e(TAG,"onFilePicked:"+currentPath);
                Disposable subscribe = Observable.create(new ObservableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                        ArrayList<Product> products = CsvUtils.read(currentPath);
                        Log.e(TAG,"read products size="+products.size());
                        DatabaseUtils.clearTable();
                        emitter.onNext(DatabaseUtils.init(products));
                        emitter.onComplete();
                    }
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean aBoolean) throws Exception {
                                if(aBoolean){
                                    Log.e(TAG,"数据导入成功");
                                    getHoldingActivity().showToast("数据导入成功");
                                    updateTip();
                                }else {
                                    Log.e(TAG,"数据导入失败");
                                    getHoldingActivity().showToast("数据导入失败");
                                }
                            }
                        });
            }
        });
        filePicker.show();
    }

    private void updateTip(){
        DatabaseUtils.searchProducts(new DatabaseUtils.SearchedListCallback() {
            @Override
            public void onSearched(List<Product> products) {
                Log.e(TAG,"searchProducts size="+products.size());
                if(products.size()>0){
                    tvDataNumbers.setText("数据个数："+products.size());
                    tvUpdateTime.setText("本地数据更新时间："+products.get(0).time);
                }else {
                    tvDataNumbers.setText("数据个数：0");
                    tvUpdateTime.setText("本地数据更新时间：无");
                }
//                for(int i=0;i<products.size();i++){
//                    Log.e(TAG,"["+products.get(i).supplierId+
//                            ","+products.get(i).materialId+
//                            "],"+products.get(i).id);
//                }
            }
        });
    }
}
