package com.smartflat.smartflatmovie.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.smartflat.smartflatmovie.R;
import com.squareup.picasso.Picasso;

public class MainActivity extends Activity {

    private static final String POSTER_URL = "https://pro100.media/api/ihome/images/vlcsnap-2018-04-02-16h58m50s570.png";

    private ImageView videoImageView;
    private View videoContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_view_activity);

        videoImageView = findViewById(R.id.video_image_view);
        videoContainer = findViewById(R.id.video_container);

        Picasso.with(this)
                .load(POSTER_URL)
                .centerCrop()
                .fit()
                .into(videoImageView);

        videoContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Intent intent = new Intent(getBaseContext(), VideoPlayerActivity.class);
                startActivity(intent);
                return false;
            }
        });
    }

}
