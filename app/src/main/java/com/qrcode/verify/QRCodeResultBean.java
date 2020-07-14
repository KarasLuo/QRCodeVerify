package com.qrcode.verify;

import java.util.List;

/**
 * Created by Hongliang Luo on 2019/3/14.
 **/
public class QRCodeResultBean {

    boolean isText;
    String content;
    String date;
    String productId;
    String supplierId;
    String materialId;
    boolean isSupplierIdReliable;
    boolean isMaterialIdReliable;
    boolean isFormatOk;
    List<String>chars;//非法字符
}
