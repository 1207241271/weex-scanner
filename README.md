# weex-xc-scanner(Test)

一款二维码扫码weex插件，当前版本支持组件基础及模块集成。    


### 快速开始

使用方法
``` bash
//install
npm install weex-xc-scanner
weexpack plugin add ./node_modules/weex-xc-scanner
//uninstall
weexpack plugin remove weex-xc-scanner
```

编辑你的weex文件

``` we
<template>
    <weex-scanner class="scanner-page" borderColor="#FbF" cornerColor="#FbF" ></weex-scanner>
</template>
<style>
    .scanner-page{
        height: 1200px;
        width: 750px;
    }
</style>
```

### API
#### weex-scanner 属性
| 属性        | 类型         | Demo  | 描述  |
| ------------- |:-------------:| -----:|----------:|
| borderColor   | string | #FFF0 | 边框颜色         |
| cornerColor   | string | #FFF0 | 边角颜色         |
| cornerWidth   | float  | 3.5px | 边角宽度         |
| backgroundAlpha | float|  0.5  | 扫描区周边透明度  |





