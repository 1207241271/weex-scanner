# weex-xc-scanner(Test)
weex-xc-scanner是一个weex插件，可以通过weexpack快速集成，可以丰富weex功能

支持的WeexSDK版本： >= 0.16.0

# How to use

const scanner = weex.requireModule("weex-xc-scanner");

scanner.scanQR("title",(res) => {
    let result = JSON.parse(res);
    if(res.status == "success"){
        let data = res.result;
    }else{
        let errr = res.msg;
    }
});


