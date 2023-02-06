package com.bighao.player.request.permission;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bighao.player.IPlayer;

public class MainActivity extends AppCompatActivity {

    private IPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayer = findViewById(R.id.player_view);
        mPlayer.setVideoUrl("https://image.talk2best.cn/upload/2019-09-05/a59188491009596d95d978d8b9264531.mp3");

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.startVideo();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mPlayer.onContinue();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
    }
}