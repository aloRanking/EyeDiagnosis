package com.aloranking.eyediagnosis;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class Settings extends AppCompatActivity {

    private static RadioGroup descTypes;
    private static RadioButton brief, brisk, freak, orb;
    private static Button apply;
    private static EditText DIST_LIMIT, MIN_MATCHES;
    private static int descriptor, min_dist, min_matches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        descTypes =  findViewById(R.id.radioGroup1);
        brief =  findViewById(R.id.radio0);
        brisk =  findViewById(R.id.radio1);
        freak =  findViewById(R.id.radio2);
        orb =  findViewById(R.id.radio3);
        apply =  findViewById(R.id.button1);
        DIST_LIMIT =  findViewById(R.id.editText1);
        MIN_MATCHES =  findViewById(R.id.editText2);
        MIN_MATCHES.setText("100");
        DIST_LIMIT.setText("80");
    }
}
