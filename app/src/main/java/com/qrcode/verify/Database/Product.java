package com.qrcode.verify.Database;


import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.UUID;

/**
 * Created by Hongliang Luo on 2019/3/15.
 **/
@Table(database = AppDatabase.class)
public class Product extends BaseModel {
    @PrimaryKey
    public UUID id;

    @Column
    public String supplierId;

    @Column
    public String materialId;

    @Column
    public String time;
}
