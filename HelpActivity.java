package com.exercise.currencyconverter;

import android.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class HelpActivity extends AppCompatActivity {
    TextView tvDesc, tvQ1ans, tvQ2ans1, tvQ2ans2, tvQ2ans3, tvQ2ans4, tvQ2ans5, tvQ2ans6, tvQ2ans7, tvQ3ans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setTitle("Help");

        tvDesc = (TextView)findViewById(R.id.tvDesc);
        tvQ1ans = (TextView)findViewById(R.id.tvQ1ans);
        tvQ2ans1 = (TextView)findViewById(R.id.tvQ2ans1);
        tvQ2ans2 = (TextView)findViewById(R.id.tvQ2ans2);
        tvQ2ans3 = (TextView)findViewById(R.id.tvQ2ans3);
        tvQ2ans4 = (TextView)findViewById(R.id.tvQ2ans4);
        tvQ2ans5 = (TextView)findViewById(R.id.tvQ2ans5);
        tvQ2ans6 = (TextView)findViewById(R.id.tvQ2ans6);
        tvQ2ans7 = (TextView)findViewById(R.id.tvQ2ans7);
        tvQ3ans = (TextView)findViewById(R.id.tvQ3ans);

        //set all texts
        tvDesc.setText(getString(R.string.help_desc));
        tvQ1ans.setText(getString(R.string.help_q1));
        tvQ2ans1.setText(getString(R.string.help_q2_1));
        tvQ2ans2.setText(getString(R.string.help_q2_2));
        tvQ2ans3.setText(getString(R.string.help_q2_3));
        tvQ2ans4.setText(getString(R.string.help_q2_4));
        tvQ2ans5.setText(getString(R.string.help_q2_5));
        tvQ2ans6.setText(getString(R.string.help_q2_6));
        tvQ2ans7.setText(getString(R.string.help_q2_7));
        tvQ3ans.setText(getString(R.string.help_q3));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        this.finish();
        return super.onOptionsItemSelected(item);
    }
}
