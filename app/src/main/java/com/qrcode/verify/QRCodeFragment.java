package com.qrcode.verify;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class QRCodeFragment extends BaseFragment {
    final static private String TAG="QRCodeFragment";

    private ImageView ivScan;
    private ImageView ivInputExcel;

    public QRCodeFragment() {
        // Required empty public constructor
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_qrcode;
    }

    @Override
    protected void initView(View view, Bundle savedInstanceState) {
        ivScan=view.findViewById(R.id.iv_qrcode_scan);
        ivScan.setOnClickListener(new OnMultiClickListener() {
            @Override
            public void onMultiClick(View view) {
                Log.e(TAG,"ivScan");
                addFragment(new ScanFragment());
            }
        });
        ivInputExcel=view.findViewById(R.id.iv_data_input);
        ivInputExcel.setOnClickListener(new OnMultiClickListener() {
            @Override
            public void onMultiClick(View view) {
                Log.e(TAG,"ivInputExcel");
                addFragment(new LoadFragment());
            }
        });

    }

    @Override
    protected int getTitleId() {
        return R.string.qrcode_verify;
    }

}
