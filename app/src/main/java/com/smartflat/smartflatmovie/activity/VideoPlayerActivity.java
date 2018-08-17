package com.smartflat.smartflatmovie.activity;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.smartflat.smartflatmovie.R;
import com.smartflat.smartflatmovie.view.VideoController;

/**
 * Просмотр видео
 */
public class VideoPlayerActivity extends Activity implements VideoController.MediaPlayerControl {

    private final static String VIDEO_URL = "https://pro100.media/api/ihome/images/kib.mp4";
    private final static String CURRENT_POSITION = "currentPosition";

    private VideoView videoView;
    private int position = 0;
    private VideoController videoController;
    private boolean isFullScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);

        videoView = findViewById(R.id.video_view);
        videoController = new VideoController(this);

        try {
            videoView.setVideoURI(Uri.parse(VIDEO_URL));
        } catch (Exception e) {
            e.printStackTrace();
        }

        videoView.requestFocus();
        videoController.setMediaPlayer(this);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoView.seekTo(position);
                videoView.start();

                // Размер экрана изменился
                mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        videoController.setAnchorView((FrameLayout) findViewById(R.id.video_frame_layout));
                    }
                });
            }
        });

        isFullScreen = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        videoController.show();
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(CURRENT_POSITION, videoView.getCurrentPosition());
        videoView.pause();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        position = savedInstanceState.getInt(CURRENT_POSITION);
        videoView.seekTo(position);
    }

    @Override
    public void start() {
        videoView.start();
    }

    @Override
    public void pause() {
        videoView.pause();
    }

    @Override
    public int getDuration() {
        return videoView.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return videoView.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        videoView.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return videoView.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean isFullScreen() {
        return isFullScreen;
    }

    @Override
    public void toggleFullScreen() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags ^= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);

        if (isFullScreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isFullScreen = false;
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            isFullScreen = true;
        }
    }
}
