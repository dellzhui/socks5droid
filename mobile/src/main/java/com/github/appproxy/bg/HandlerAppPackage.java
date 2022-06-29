package com.github.appproxy.bg;

import com.inspur.reflect.ProxyProfileInfo;

public class HandlerAppPackage {
    private ProxyProfileInfo proxyProfileInfo;
    private String appList[] = null;
    public HandlerAppPackage(ProxyProfileInfo proxyProfileInfo){
        this.proxyProfileInfo = proxyProfileInfo;
    }
    public String[]  getAppList(){
        String appList = proxyProfileInfo.getAppList();
        return appList.split(";");

    }






}
