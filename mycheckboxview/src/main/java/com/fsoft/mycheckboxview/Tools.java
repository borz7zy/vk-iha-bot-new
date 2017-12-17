package com.fsoft.mycheckboxview;

import android.content.res.Resources;

/**
 * Created by Dr. Failov on 17.12.2017.
 */

class Tools {
    public static int dp(int dp){
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
    public static int dpi(){
        return Resources.getSystem().getDisplayMetrics().densityDpi;
    }
}
