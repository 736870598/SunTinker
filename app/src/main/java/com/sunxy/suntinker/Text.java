package com.sunxy.suntinker;

import android.content.Context;
import android.widget.Toast;

/**
 * --
 * <p>
 * Created by sunxy on 2018/8/9 0009.
 */
public class Text {

    public void text(Context context){
        int a = 10;
        int b = 1;
        Toast.makeText(context,
                String.format("a = %s, b = %s, a/b = %s", a, b, a/b)
                , Toast.LENGTH_SHORT).show();
    }


}
