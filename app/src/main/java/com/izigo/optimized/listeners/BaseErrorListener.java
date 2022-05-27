package com.izigo.optimized.listeners;

import com.blankj.utilcode.util.LogUtils;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequestErrorListener;

public class BaseErrorListener implements PermissionRequestErrorListener {
    @Override
    public void onError(DexterError dexterError) {
        LogUtils.eTag(BaseErrorListener.class.getSimpleName(), this, dexterError);
    }
}
