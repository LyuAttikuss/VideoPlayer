package com.smartflat.smartflatmovie.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.smartflat.smartflatmovie.R;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

public class VideoController extends FrameLayout {

    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;

    private MediaPlayerControl mediaPlayerControl;
    private Context context;
    private ViewGroup anchorView;
    private View root;
    private ProgressBar progressBar;
    private TextView tvEndTime;
    private TextView tvCurrentTime;
    private boolean isShowing;
    private boolean isDragging;
    StringBuilder formatBuilder;
    Formatter formatter;
    private ImageButton btnPause;
    private ImageButton btnForward;
    private ImageButton btnBackward;
    private ImageButton btnFullScreen;
    private Handler handler = new MessageHandler(this);

    public VideoController(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        if (root != null) {
            initControllerView(root);
        }
    }

    public void setMediaPlayer(MediaPlayerControl player) {
        mediaPlayerControl = player;
        updatePausePlay();
        updateFullScreen();
    }

    /**
     * Устанавливаем якорь на ту вьюшку, на которой будут контролы
     */
    public void setAnchorView(ViewGroup view) {
        anchorView = view;

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);
    }

    /**
     * Создание контролов поверх VideoView
     */
    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        root = inflate.inflate(R.layout.video_player_layout, null);
        initControllerView(root);
        return root;
    }

    private void initControllerView(View v) {
        btnPause = v.findViewById(R.id.pause);
        if (btnPause != null) {
            btnPause.requestFocus();
            btnPause.setOnClickListener(pauseListener);
        }

        btnFullScreen = v.findViewById(R.id.full_screen);
        if (btnFullScreen != null) {
            btnFullScreen.requestFocus();
            btnFullScreen.setOnClickListener(fullScreenListener);
        }

        btnForward = v.findViewById(R.id.forward);
        if (btnForward != null) {
            btnForward.setOnClickListener(forwardListener);
        }

        btnBackward = v.findViewById(R.id.backward);
        if (btnBackward != null) {
            btnBackward.setOnClickListener(backwardListener);
        }

        progressBar = v.findViewById(R.id.media_controller_progress);
        if (progressBar != null) {
            if (progressBar instanceof SeekBar) {
                SeekBar seeker = (SeekBar) progressBar;
                seeker.setOnSeekBarChangeListener(seekListener);
            }
            progressBar.setMax(1000);
        }

        tvEndTime = v.findViewById(R.id.rest_time);
        tvCurrentTime = v.findViewById(R.id.current_time);
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
    }

    public void show() {
        show(DEFAULT_TIMEOUT);
    }

    /**
     * Отображение контролов на экране
     * Исчезнут автоматически после таймаута
     */
    public void show(int timeout) {
        if (!isShowing && anchorView != null) {
            setProgress();

            if (btnPause != null) {
                btnPause.requestFocus();
            }

            FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );

            anchorView.addView(this, tlp);
            isShowing = true;
        }

        updatePausePlay();
        updateFullScreen();

        handler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = handler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            handler.removeMessages(FADE_OUT);
            handler.sendMessageDelayed(msg, timeout);
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    /**
     * Удаление контролов с экрана
     */
    public void hide() {
        if (anchorView == null) {
            return;
        }

        try {
            anchorView.removeView(this);
            handler.removeMessages(SHOW_PROGRESS);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        isShowing = false;
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        formatBuilder.setLength(0);

        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mediaPlayerControl == null || isDragging) {
            return 0;
        }

        int position = mediaPlayerControl.getCurrentPosition();
        int duration = mediaPlayerControl.getDuration();

        if (progressBar != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                progressBar.setProgress((int) pos);
            }
            int percent = mediaPlayerControl.getBufferPercentage();
            progressBar.setSecondaryProgress(percent * 10);
        }

        tvEndTime.setText(String.format("- %s", stringForTime(duration - position)));
        tvCurrentTime.setText(stringForTime(position));

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show(DEFAULT_TIMEOUT);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(DEFAULT_TIMEOUT);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mediaPlayerControl == null) {
            return true;
        }

        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(DEFAULT_TIMEOUT);
                if (btnPause != null) {
                    btnPause.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mediaPlayerControl.isPlaying()) {
                mediaPlayerControl.start();
                updatePausePlay();
                show(DEFAULT_TIMEOUT);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mediaPlayerControl.isPlaying()) {
                mediaPlayerControl.pause();
                updatePausePlay();
                show(DEFAULT_TIMEOUT);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(DEFAULT_TIMEOUT);
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener pauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(DEFAULT_TIMEOUT);
        }
    };

    private View.OnClickListener fullScreenListener = new View.OnClickListener() {
        public void onClick(View v) {
            doToggleFullscreen();
            show(DEFAULT_TIMEOUT);
        }
    };

    public void updatePausePlay() {
        if (root == null || btnPause == null || mediaPlayerControl == null) {
            return;
        }

        if (mediaPlayerControl.isPlaying()) {
            btnPause.setImageResource(R.drawable.ic_media_pause);
        } else {
            btnPause.setImageResource(R.drawable.ic_media_play);
        }
    }

    public void updateFullScreen() {
        if (root == null || btnFullScreen == null || mediaPlayerControl == null) {
            return;
        }

        if (mediaPlayerControl.isFullScreen()) {
            btnFullScreen.setImageResource(R.drawable.ic_media_fullscreen_shrink);
        } else {
            btnFullScreen.setImageResource(R.drawable.ic_media_fullscreen_stretch);
        }
    }

    private void doPauseResume() {
        if (mediaPlayerControl == null) {
            return;
        }

        if (mediaPlayerControl.isPlaying()) {
            mediaPlayerControl.pause();
        } else {
            mediaPlayerControl.start();
        }
        updatePausePlay();
    }

    private void doToggleFullscreen() {
        if (mediaPlayerControl == null) {
            return;
        }

        mediaPlayerControl.toggleFullScreen();
    }

    private OnSeekBarChangeListener seekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);

            isDragging = true;
            handler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mediaPlayerControl == null) {
                return;
            }

            if (!fromuser) {
                return;
            }

            long duration = mediaPlayerControl.getDuration();
            long newposition = (duration * progress) / 1000L;
            mediaPlayerControl.seekTo((int) newposition);
            if (tvCurrentTime != null) {
                tvCurrentTime.setText(stringForTime((int) newposition));
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
            isDragging = false;
            setProgress();
            updatePausePlay();
            show(DEFAULT_TIMEOUT);

            handler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    private View.OnClickListener backwardListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mediaPlayerControl == null) {
                return;
            }

            int pos = mediaPlayerControl.getCurrentPosition();
            pos -= 15000;
            mediaPlayerControl.seekTo(pos);
            setProgress();

            show(DEFAULT_TIMEOUT);
        }
    };

    private View.OnClickListener forwardListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mediaPlayerControl == null) {
                return;
            }

            int pos = mediaPlayerControl.getCurrentPosition();
            pos += 15000;
            mediaPlayerControl.seekTo(pos);
            setProgress();

            show(DEFAULT_TIMEOUT);
        }
    };

    public interface MediaPlayerControl {
        void start();

        void pause();

        int getDuration();

        int getCurrentPosition();

        void seekTo(int pos);

        boolean isPlaying();

        int getBufferPercentage();

        boolean isFullScreen();

        void toggleFullScreen();
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<VideoController> mView;

        MessageHandler(VideoController view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoController view = mView.get();
            if (view == null || view.mediaPlayerControl == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    view.hide();
                    break;
                case SHOW_PROGRESS:
                    pos = view.setProgress();
                    if (!view.isDragging && view.isShowing && view.mediaPlayerControl.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }
}