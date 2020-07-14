package com.qrcode.verify;


import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.result.ParsedResultType;
import com.qrcode.verify.Database.Product;
import com.qrcode.verify.Utils.DatabaseUtils;
import com.qrcode.verify.zxing.AmbientLightManager;
import com.qrcode.verify.zxing.BeepManager;
import com.qrcode.verify.zxing.CaptureActivityHandler;
import com.qrcode.verify.zxing.FinishListener;
import com.qrcode.verify.zxing.InactivityTimer;
import com.qrcode.verify.zxing.Intents;
import com.qrcode.verify.zxing.ViewfinderView;
import com.qrcode.verify.zxing.camera.CameraManager;
import com.qrcode.verify.zxing.result.ResultHandler;
import com.qrcode.verify.zxing.result.ResultHandlerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class ScanFragment extends BaseFragment implements SurfaceHolder.Callback {
    final static private String TAG="ScanFragment";

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private Result savedResultToShow;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType,?> decodeHints;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;

    private boolean isOpenFlahsLight;
    private ImageView flahsView;
    private QRCodeResultBean result;
    private long clickTime;

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public ScanFragment() {
        // Required empty public constructor
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_scan;
    }

    @Override
    protected void initView(View view, Bundle savedInstanceState) {
        hasSurface = false;
        inactivityTimer = new InactivityTimer(getHoldingActivity());
        beepManager = new BeepManager(getHoldingActivity());
        ambientLightManager = new AmbientLightManager(getHoldingActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
//        Log.e(TAG,"onResume");

        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getHoldingActivity().getApplication());

        viewfinderView = (ViewfinderView) getView().findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);
        clickTime=-1;
        viewfinderView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cameraManager!=null){
                    long time=System.currentTimeMillis();
                    Log.e(TAG,"clicked time="+time);
                    if(time-clickTime<1000&&clickTime!=-1){
                        cameraManager.setZoom();
                    }else {
                        clickTime=time;
                    }
                }
            }
        });

        flahsView=getView().findViewById(R.id.iv_flash);
        isOpenFlahsLight=false;
        cameraManager.setTorch(false);
        flahsView.setImageResource(R.drawable.ic_shanguangdeng_guanbi);
        flahsView.setOnClickListener(new OnMultiClickListener() {
            @Override
            public void onMultiClick(View view) {
                if(isOpenFlahsLight){
                    flahsView.setImageResource(R.drawable.ic_shanguangdeng_guanbi);
                    cameraManager.setTorch(false);
                    isOpenFlahsLight=false;
                }else {
                    flahsView.setImageResource(R.drawable.ic_shanguangdeng);
                    cameraManager.setTorch(true);
                    isOpenFlahsLight=true;
                }
            }
        });

        handler = null;

//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getHoldingActivity());

        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        Intent intent = getHoldingActivity().getIntent();

        decodeFormats = null;
        characterSet = null;

        if (intent != null) {

//            String action = intent.getAction();
//            String dataString = intent.getDataString();

//            Log.e(TAG,"action="+action);
//            Log.e(TAG,"dataString="+dataString);
            characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
        }

        SurfaceView surfaceView = (SurfaceView) getView().findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
//            Log.e(TAG,"hasSurface=true");
            initCamera(surfaceHolder);
        } else {
//            Log.e(TAG,"hasSurface=false");
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    protected int getTitleId() {
        return R.string.qrcode_scan;
    }


    @Override
    public void onPause() {
//        Log.e(TAG,"onPause");
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) getView().findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
//        Log.e(TAG,"onDestroy");
        inactivityTimer.shutdown();
        super.onDestroyView();
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
//        Log.e(TAG,"decodeOrStoreSavedBitmap");
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                Log.e(TAG,"R.id.decode_succeeded="+R.id.decode_succeeded+",savedResultToShow="+savedResultToShow);
                Log.e(TAG,"message:"+message.obj+","+message.arg1+","+message.arg2);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
//        Log.e(TAG,"surfaceCreated");
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
//        Log.e(TAG,"surfaceDestroyed");
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        Log.e(TAG,"surfaceChanged");
        // do nothing
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
//        Log.e(TAG,"handleDecode");
        inactivityTimer.onActivity();
        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            // Then not from history, so beep/vibrate and we have an image to draw on
            beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, scaleFactor, rawResult);
        }

        handleDecodeInternally(rawResult, resultHandler, barcode);
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode   A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
//        Log.e(TAG,"drawResultPoints");
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
//        Log.e(TAG,"drawLine");
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }


    final private static String USUAL_MATCH_CODE="([0-9A-Za-z])";
    final private static String MATCH_CODE="([0-9][0-9][1-9A-C][0-9]{5}[0-9A-Za-z]{6}[0-9A-Za-z\\-]+)";
    // Put up our own UI for how to handle the decoded contents.
    private void handleDecodeInternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {
        //扫描出结果

        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        Log.e(TAG,"handleDecodeInternally");
        Log.e(TAG,""+rawResult.getBarcodeFormat().toString());
        Log.e(TAG,""+formatter.format(rawResult.getTimestamp()));
        Log.e(TAG,""+resultHandler.getType().toString());
        Log.e(TAG,""+resultHandler.getDisplayContents());
        Log.e(TAG,"handleDecodeInternally end");
        //类型为二维码
        result=null;
        if(rawResult.getBarcodeFormat().equals(BarcodeFormat.QR_CODE)){
            //如果是二维码，解析数据
            result=new QRCodeResultBean();
            String rawContent=resultHandler.getDisplayContents().toString();
            result.content=rawContent;
            if(resultHandler.getType().equals(ParsedResultType.TEXT)){
                result.isText=true;
                //正则匹配
                if(!TextUtils.isEmpty(rawContent) && rawContent.matches(MATCH_CODE)){
                    Log.e(TAG,"正则匹配成功");
                    result.isFormatOk=true;
                    result.chars=null;
                    result.date=dateFormat(rawContent.substring(0,3));
                    result.productId=rawContent.substring(3,8);
                    final String supplierId=rawContent.substring(8,14);
                    final String materialId=rawContent.substring(14,rawContent.length());
                    //检测供应商代码和物料代码是否正确
                    DatabaseUtils.searchProductBySupplierId(supplierId,
                            new DatabaseUtils.SearchedSingleCallback() {
                        @Override
                        public void onSearched(Product products) {
                            Log.e(TAG,"searched by supplierId:"+products);
                            if(products!=null){
                                result.isSupplierIdReliable =true;
                                result.supplierId=supplierId;
                            }else {
                                result.isSupplierIdReliable =false;
                                result.supplierId=null;
                            }
                        }
                    });
                    DatabaseUtils.searchProductByMaterialId(materialId,
                            new DatabaseUtils.SearchedSingleCallback() {
                                @Override
                                public void onSearched(Product products) {
                                    Log.e(TAG,"searched by materialId:"+products);
                                    if(products!=null){
                                        result.isMaterialIdReliable =true;
                                        result.materialId=materialId;
                                    }else {
                                        result.isMaterialIdReliable =false;
                                        result.materialId=null;
                                    }
                                }
                            });
                }else {
                    Log.e(TAG,"正则匹配失败");
                    result.isFormatOk=false;
                    //查找非法字符
                    result.chars=new ArrayList<>();
                    for (int i=0;i<rawContent.length();i++){
                        String charI=rawContent.substring(i,i+1);
                        if(!charI.matches(USUAL_MATCH_CODE)){
                            result.chars.add(charI);
                        }
                    }
                }
            }else {
                result.isText=false;
            }
            //保存数据
            getHoldingActivity().setQrCodeResult(result);
            //页面跳转
            addFragment(new ResultFragment());
        }
    }

    private String dateFormat(String str){
        String dateStr="20"+str.substring(0,2)+"年";
        String monthStr=str.substring(2);
        switch (monthStr){
            case "A":
                dateStr+="10月";
                break;
            case "B":
                dateStr+="11月";
                break;
            case "C":
                dateStr+="12月";
                break;
            default:
                dateStr+=monthStr+"月";
                break;
        }
        return dateStr;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        Log.e(TAG,"initCamera");
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.e(TAG, ioe.getLocalizedMessage());
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.e(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        Log.e(TAG,"displayFrameworkBugMessageAndExit");
        AlertDialog.Builder builder = new AlertDialog.Builder(getHoldingActivity());
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    public void drawViewfinder() {
//        Log.e(TAG,"drawViewfinder");
        viewfinderView.drawViewfinder();
    }
}
