package com.sunxy.suntinker;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.sunxy.suntinker.core.SunTinkerManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.cul).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Text text = new Text();
                text.text(v.getContext());
            }
        });

        findViewById(R.id.fix).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dexPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/sunxy_file/dex";
                boolean success = SunTinkerManager.getManager().fix(v.getContext(), dexPath);
                Toast.makeText(MainActivity.this, "fix " + success, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
