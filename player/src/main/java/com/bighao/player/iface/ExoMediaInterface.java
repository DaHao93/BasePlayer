package com.bighao.player.iface;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bighao.player.dict.PlayerStateEnum;
import com.bighao.player.view.PlayerView;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;

/**
 * Created by Hao on 2023/2/4
 */
public class ExoMediaInterface extends MediaInterface implements Player.Listener {

    private SimpleExoPlayer mSimpleExoPlayer;
    private Runnable callback;
    private long previousSeek = 0;

    public ExoMediaInterface(PlayerView playerView) {
        super(playerView);
    }

    @Override
    public void start() {
        mSimpleExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void prepare() {
        Context context = mPlayerView.getContext();
        release();

        mMediaHandlerThread = new HandlerThread("PlayerView");
        mMediaHandlerThread.start();
        mMediaHandler = new Handler(context.getMainLooper());
        mHandler = new Handler();
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                ExoTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
                TrackSelector trackSelector = new DefaultTrackSelector(context, videoTrackSelectionFactory);

                LoadControl loadControl = new DefaultLoadControl.Builder()
                        .setAllocator(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                        .setBufferDurationsMs(360000, 600000, 1000, 5000)
                        .setPrioritizeTimeOverSizeThresholds(false)
                        .setTargetBufferBytes(C.LENGTH_UNSET)
                        .build();

                BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(context).build();

                RenderersFactory renderersFactory = new DefaultRenderersFactory(context);
                mSimpleExoPlayer = new SimpleExoPlayer.Builder(context, renderersFactory)
                        .setTrackSelector(trackSelector)
                        .setLoadControl(loadControl)
                        .setBandwidthMeter(bandwidthMeter)
                        .build();

                DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                        Util.getUserAgent(context, ""));

                String currUrl = mPlayerView.getVideoUrl();
                MediaSource videoSource;

                if (currUrl.contains(".m3u8")) {
                    MediaItem mediaItem=MediaItem.fromUri(Uri.parse(currUrl));
                    videoSource = new HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem);
                } else {
                    MediaItem mediaItem=MediaItem.fromUri(Uri.parse(currUrl));
                    videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem);
                }

                mSimpleExoPlayer.addListener(ExoMediaInterface.this);

//                simpleExoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);//循环播放
                mSimpleExoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);

                mSimpleExoPlayer.prepare(videoSource);
                mSimpleExoPlayer.setPlayWhenReady(true);

                callback = new onBufferingUpdate();

                if (null != mPlayerView.getTextureView()) {
                    SurfaceTexture surfaceTexture = mPlayerView.getTextureView().getSurfaceTexture();
                    if (null != surfaceTexture) {
                        mSimpleExoPlayer.setVideoSurface(new Surface(surfaceTexture));
                    }
                }

            }
        });
    }

    @Override
    public void pause() {
        mSimpleExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public boolean isPlaying() {
        return mSimpleExoPlayer.getPlayWhenReady();
    }

    @Override
    public void seekTo(long time) {
        if (null == mSimpleExoPlayer) {
            return;
        }
        if (time != previousSeek) {
            if (time >= mSimpleExoPlayer.getBufferedPosition()) {
                mPlayerView.onPlayerStateChange(PlayerStateEnum.STATE_LOADING);
            }
            mSimpleExoPlayer.seekTo(time);
            previousSeek = time;
        }
    }

    @Override
    public void release() {
        if (null != mMediaHandler && null != mMediaHandlerThread && null != mSimpleExoPlayer) {
            HandlerThread tmpHandlerThread = mMediaHandlerThread;
            SimpleExoPlayer tmpMediaPlayer = mSimpleExoPlayer;
            MediaInterface.SAVED_SURFACE = null;
            mMediaHandler.post(new Runnable() {
                @Override
                public void run() {
                    tmpMediaPlayer.release();
                    tmpHandlerThread.quit();
                }
            });
            mSimpleExoPlayer = null;
        }
    }

    @Override
    public long getCurrentPosition() {
        if (null != mSimpleExoPlayer) {
            return mSimpleExoPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    @Override
    public long getDuration() {
        if (null != mSimpleExoPlayer) {
            return mSimpleExoPlayer.getDuration();
        } else {
            return 0;
        }
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mSimpleExoPlayer.setVolume(leftVolume);
        mSimpleExoPlayer.setVolume(rightVolume);
    }

    @Override
    public void setSpeed(float speed) {
        PlaybackParameters playbackParameters = new PlaybackParameters(speed, 1.0F);
        mSimpleExoPlayer.setPlaybackParameters(playbackParameters);
    }

    @Override
    public void setSurface(Surface surface) {
        if (null != mSimpleExoPlayer) {
            mSimpleExoPlayer.setVideoSurface(surface);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (null == SAVED_SURFACE) {
            SAVED_SURFACE = surface;
            prepare();
        } else {
            mPlayerView.getTextureView().setSurfaceTexture(SAVED_SURFACE);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPlayerView.onVideoSizeChanged(videoSize.width,videoSize.height);
            }
        });
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (playbackState) {
                    case Player.STATE_IDLE: {
                    }
                    break;
                    case Player.STATE_BUFFERING: {
                        mPlayerView.onPlayerStateChange(PlayerStateEnum.STATE_LOADING);
                        mHandler.post(callback);
                    }
                    break;
                    case Player.STATE_READY: {
                        if (playWhenReady) {
                            mPlayerView.onPlayerStateChange(PlayerStateEnum.STATE_PLAYING);
                        }
                    }
                    break;
                    case Player.STATE_ENDED: {
                        mPlayerView.onPlayerStateChange(PlayerStateEnum.STATE_COMPLETE);
                    }
                    break;
                }
            }
        });
    }

    @Override
    public void onPlayerErrorChanged(@Nullable  PlaybackException error) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPlayerView.onError(1000, 1000);
            }
        });
    }

    private class onBufferingUpdate implements Runnable {
        @Override
        public void run() {
            if (mSimpleExoPlayer != null) {
                final int percent = mSimpleExoPlayer.getBufferedPercentage();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mPlayerView.onBufferingUpdate(percent);
                    }
                });
                if (percent < 100) {
                    mHandler.postDelayed(callback, 300);
                } else {
                    mHandler.removeCallbacks(callback);
                }
            }
        }
    }
}
