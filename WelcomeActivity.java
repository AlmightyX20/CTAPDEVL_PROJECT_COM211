package com.example.faceshape;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        TextView textView = findViewById(R.id.Welcome);
        Button getStartedButton = findViewById(R.id.getStartedButton);
        TextView textView1 = findViewById(R.id.Jovexian);

        // Create a SpannableString for "Welcome to FaceFit!"
        String text = "Welcome to FaceFit!";
        SpannableString spannableString = new SpannableString(text);

        // Create a SpannableString for "Welcome to FaceFit!"
        String text1 = "Powered by Jovexian";
        SpannableString spannableString1 = new SpannableString(text1);

        // Color "Welcome to" black
        spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Color "Powered by" black
        spannableString1.setSpan(new ForegroundColorSpan(Color.BLACK), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Color "FaceFit!" in RGB: 120 121 248 (color code: #7879F8)
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.facefit_blue)), 11, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the styled text to the TextView
        textView.setText(spannableString);

        // Color "Jovexian!" in RGB: 120 121 248 (color code: #7879F8)
        spannableString1.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.facefit_blue)), 11, text1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the styled text to the TextView
        textView1.setText(spannableString);

        // Handle button click to start MainActivity
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
