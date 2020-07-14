package com.qrcode.verify.Utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.qrcode.verify.Database.Product;
import com.qrcode.verify.Database.Product_Table;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hongliang Luo on 2019/3/15.
 **/
public class DatabaseUtils {
    final static private String TAG="DatabaseUtils";

    /**
     * 向數據庫插入一条数据
     */
    public static boolean init(ArrayList<Product>products){
        boolean isAllSaved=true;
        for (int i=0;i<products.size();i++){
            Log.e(TAG,"init:"+(i+1)+"/"+products.size());
            if(!products.get(i).save()){
                isAllSaved=false;
                break;
            }
        }
        return isAllSaved;
    }

    public static void searchProductBySupplierId(String id,
                                                 final SearchedSingleCallback callback){
        SQLite.select().from(Product.class)
                .where(Product_Table.supplierId.is(id))
                .async().querySingleResultCallback(
                        new QueryTransaction.QueryResultSingleCallback<Product>() {
            @Override
            public void onSingleQueryResult(QueryTransaction transaction,
                                            @Nullable Product product) {
//                Log.e(TAG,"searched product="+product);
                callback.onSearched(product);
            }
        }).execute();
    }

    public static void searchProductByMaterialId( String id,
                                                  final SearchedSingleCallback callback){
        SQLite.select().from(Product.class)
                .where(Product_Table.materialId.is(id))
                .async().querySingleResultCallback(
                new QueryTransaction.QueryResultSingleCallback<Product>() {
                    @Override
                    public void onSingleQueryResult(QueryTransaction transaction,
                                                    @Nullable Product product) {
//                        Log.e(TAG,"searched product="+product);
                        callback.onSearched(product);
                    }
                }).execute();
    }

    /**
     * 异步查询并返回查询结果
     * @param callback
     */
    public static void searchProducts(final SearchedListCallback callback){
        SQLite.select().from(Product.class)
                .async().queryListResultCallback(
                        new QueryTransaction.QueryResultListCallback<Product>() {
            @Override
            public void onListQueryResult(QueryTransaction transaction,
                                          @NonNull List<Product> tResult) {
//                Log.e(TAG,"searched products="+ Arrays.toString(tResult.toArray()));
                callback.onSearched(tResult);
            }
        }).execute();
    }

    public interface SearchedListCallback {
        void onSearched(List<Product> products);
    }

    public interface SearchedSingleCallback {
        void onSearched(Product products);
    }

    public static void clearTable(){
        Delete.table(Product.class);
    }
}
