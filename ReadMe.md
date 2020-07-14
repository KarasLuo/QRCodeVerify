# **APP安装说明**

本app适用于`android5.0`及以上版本的安卓手机，使用app时，请确保授予所有需要
的手机权限(如`相机权限`)，否则相关功能无法使用。

# **操作方法**

**扫描二维码**

点击主页扫描二维码的图标，进入二维码扫描页面，将二维码放入方形框内进行扫描，
可点击方框下方按钮开关闪光灯,双击屏幕可放大缩小。 然后展示扫描结果。返回扫描界面可继续扫描。

> 解析的优先级为：文本>格式(包括非法字符)>供应商编号>物料编号。如果解析出错，按照
> 解析的优先级报错。

**导入数据**

点击主页的导入数据图表，进入数据导入页面，展示有本地数据的更新时间和数据个数。
点击图标，在弹出的对话框中选择需要导入的文件。`首次使用时必须先导入文件，否则校验总是失败。`

>导入的文件格式为.csv(逗号分隔值文件)，可用excel编辑再`另存为`csv格式。`表格分为两列。第一列为供应商编号，第二列为物料编号。`逐行编辑。


# **常见使用问题**

- **相机启动失败，提示重启**：
  可能是没有授予相应权限。重启app按提示授权或进入手机设置的应用管理开启权限。

- **找不到需要导入的文件**： 请确保生成的文件格式为.csv,并且是小写。

- **导入的数据量明显不足**：
  使用excel进行格式转换，而不能直接更改文件后缀。因为.xlsx文件有非文本的格式，无法直接转换，导致解析异常。
  
 

