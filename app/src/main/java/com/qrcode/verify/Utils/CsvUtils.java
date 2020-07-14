package com.qrcode.verify.Utils;

import android.util.Log;

import com.qrcode.verify.Database.Product;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by Hongliang Luo on 2019/3/15.
 **/
public class CsvUtils {
    private final static String TAG="CsvUtils";

    /**
     * 读取整个csv文件
     * [supplierId,materialId]
     * @return
     */
    public static ArrayList<Product> read(String path){
        File csv = new File(path);
        ArrayList<Product> products=new ArrayList<>();
        //读取
        try {
            FileInputStream fis=new FileInputStream(csv);
            InputStreamReader isw=new InputStreamReader(fis,"GBK");
            BufferedReader reader = new BufferedReader(isw);
            String line;
            while((line=reader.readLine())!=null){
                String[] ids=line.split(",");
                Log.e(TAG,"load line="+Arrays.toString(ids));
                if(ids.length==2){
                    Product product=new Product();
                    product.id=UUID.randomUUID();
                    product.supplierId=ids[0];
                    product.materialId=ids[1];
                    Calendar calendar=Calendar.getInstance();
                    String time=calendar.get(Calendar.YEAR)+"/"
                            +(calendar.get(Calendar.MONTH)+1)+"/"
                            +calendar.get(Calendar.DAY_OF_MONTH);
                    Log.e(TAG,"time="+time);
                    product.time=time;
                    products.add(product);
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }
        return products;
    }
}
