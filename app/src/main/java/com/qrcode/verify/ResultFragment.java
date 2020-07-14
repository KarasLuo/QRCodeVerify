package com.qrcode.verify;


import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


public class ResultFragment extends BaseFragment {
    final static private String TAG="ResultFragment";


    public ResultFragment() {
        // Required empty public constructor
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_result;
    }

    @Override
    protected void initView(View view, Bundle savedInstanceState) {
        QRCodeResultBean result=getHoldingActivity().getQrCodeResult();
        if(result!=null){
            //扫描结果
            TextView tvData=view.findViewById(R.id.tv_raw_data);
            if(result.content!=null){
                tvData.setText(result.content);
            }else {
                tvData.setText(getResources().getString(R.string.parse_error));
                tvData.setTextColor(getResources().getColor(R.color.red));
            }
            //生产日期
            TextView tvDate=view.findViewById(R.id.tv_date);
            if(result.date!=null){
                tvDate.setText(result.date);
            }else {
                ((View)tvDate.getParent()).setVisibility(View.GONE);
//                tvDate.setText(getString(R.string.parse_error));
//                tvData.setTextColor(getResources().getColor(R.color.red));
            }
            //产品编号
            TextView tvProductId=view.findViewById(R.id.tv_product_id);
            if(result.productId!=null){
                tvProductId.setText(result.productId);
            }else {
                ((View)tvProductId.getParent()).setVisibility(View.GONE);
//                tvProductId.setText(getString(R.string.parse_error));
//                tvData.setTextColor(getResources().getColor(R.color.red));
            }
            //供应商
            TextView tvSupplier=view.findViewById(R.id.tv_supplier_id);
            if(result.supplierId!=null){
                tvSupplier.setText(result.supplierId);
            }else {
                ((View)tvSupplier.getParent()).setVisibility(View.GONE);
//                tvSupplier.setText(getString(R.string.parse_error));
//                tvData.setTextColor(getResources().getColor(R.color.red));
            }
            //物料ID
            TextView tvMaterialId=view.findViewById(R.id.tv_material_id);
            if(result.materialId!=null&&result.supplierId!=null){
                tvMaterialId.setText(result.materialId);
            }else {
                ((View)tvMaterialId.getParent()).setVisibility(View.GONE);
//                tvMaterialId.setText(getString(R.string.parse_error));
//                tvData.setTextColor(getResources().getColor(R.color.red));
            }
            //分析结果
            TextView tvTip=view.findViewById(R.id.tv_tip);
            TextView tvResult=view.findViewById(R.id.tv_result);
            boolean isOk=false;
            if(result.isText){
                if(result.isFormatOk){
                    if(result.isSupplierIdReliable){
                        if(result.isMaterialIdReliable){
                            tvResult.setText("产品编号正确");
                            isOk=true;
                        }else {
                            //与本地数据库物料编号校验失败
                            tvResult.setTextColor(getResources().getColor(R.color.red));
                            tvResult.setText("物料编号错误\n请核对产品编号");
                            tvTip.setVisibility(View.VISIBLE);
                        }
                    }else {
                        //与本地数据库供应商编号校验失败
                        tvResult.setTextColor(getResources().getColor(R.color.red));
                        tvResult.setText("供应商编号错误\n请核对供应商是否正确");
                        tvTip.setVisibility(View.VISIBLE);
                    }
                }else {
                    //正则校验失败【非法字符或错误格式】
                    tvResult.setTextColor(getResources().getColor(R.color.red));
                    if(result.chars!=null){
                        if(result.chars.size()>0){
                            tvResult.setText("二维码格式错误\n存在非法字符：");
                            for (int i=0;i<result.chars.size();i++){
                                if(result.chars.get(i).equals(" ")){
                                    tvResult.append("\"空格\" ");
                                }else {
                                    tvResult.append("\""+result.chars.get(i)+"\" ");
                                }
                            }
                        }else {
                            tvResult.setText("二维码格式错误\n请核对二维码格式");
                        }
                    }else {
                        tvResult.setText("二维码格式错误\n长度错误或存在非法字符");
                    }
                }
            }else {
                //扫描结果非文本
                tvResult.setTextColor(getResources().getColor(R.color.red));
                tvResult.setText("二维码错误\n扫描结果不是文本");
            }
            //播放提示音
            if(isOk){
                showSound(R.raw.qrcode_success);
            }else {
                showSound(R.raw.qrcode_failed);
            }
        }else {
            //扫描结果为空
            getHoldingActivity().showToast("程序出现异常，请退出重启！");
            getHoldingActivity().finish();
        }
    }

    @Override
    protected int getTitleId() {
        return R.string.qrcode_result;
    }

    /**
     ** 语音提示
     **
     ** @param raw
     **/
    protected void showSound(int raw) {
        final MediaPlayer mediaPlayer = MediaPlayer.create(
                getHoldingActivity().getApplicationContext(), raw);
        mediaPlayer.setVolume(0.99f, 0.99f);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }
        });
    }
}
