package com.ckr.upgrade;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView versionView;
    private TextView descriptionView;
    private TextView thinkerIdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        descriptionView = findViewById(R.id.description);
        versionView = findViewById(R.id.version);
        thinkerIdView = findViewById(R.id.thinkerId);
    }
}
