package com.example.musicappclone;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;

import com.example.musicappclone.R;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Binder;
import android.os.IBinder;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.util.Objects;

public class PlayerService extends Service {

    ExoPlayer exoPlayer;
    private final IBinder serviceBinder = new ServiceBinder();
    PlayerNotificationManager notificationManager;

    public class ServiceBinder extends Binder {
        public PlayerService getPlayerService() {
            return PlayerService.this;
        }

    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return serviceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build();

        exoPlayer.setAudioAttributes(audioAttributes, true);

        final String channelId =  getResources().getString(R.string.app_name) + " Music Channel";
        final int notificationId = 1111111;
        notificationManager = new PlayerNotificationManager.Builder(this, notificationId, channelId)
                .setChannelImportance(IMPORTANCE_HIGH)
                .setNotificationListener(notificationListener)
                .setMediaDescriptionAdapter(descriptionAdapter)
                .setSmallIconResourceId(R.drawable.ic_launcher_foreground)
                .setChannelDescriptionResourceId(R.string.app_name)
                .setNextActionIconResourceId(R.drawable.ic_baseline_skip_next_24)
                .setPreviousActionIconResourceId(R.drawable.ic_baseline_skip_previous_24)
                .setPauseActionIconResourceId(R.drawable.ic_baseline_pause_24)
                .setPlayActionIconResourceId(R.drawable.ic_baseline_play_arrow_24)
                .setChannelNameResourceId(R.string.app_name)

                .build();

        notificationManager.setPlayer(exoPlayer);
        notificationManager.setPriority(NotificationCompat.PRIORITY_MAX);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseFastForwardAction(false);

    }

    //notification listener

    PlayerNotificationManager.NotificationListener notificationListener = new PlayerNotificationManager.NotificationListener() {
        @Override
        public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
            PlayerNotificationManager.NotificationListener.super.onNotificationCancelled(notificationId, dismissedByUser);
            stopForeground(true);
            if(exoPlayer.isPlaying()) {
                exoPlayer.pause();
            }
        }

        @Override
        public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
            PlayerNotificationManager.NotificationListener.super.onNotificationPosted(notificationId, notification, ongoing);
            startForeground(notificationId, notification);
        }
    };


    PlayerNotificationManager.MediaDescriptionAdapter descriptionAdapter = new PlayerNotificationManager.MediaDescriptionAdapter() {
        @Override
        public CharSequence getCurrentContentTitle(Player player) {
            return Objects.requireNonNull(exoPlayer.getCurrentMediaItem()).mediaMetadata.title;
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {

            // Intent to open the app when clicker
            Intent openAppIntent = new Intent(getApplicationContext(), MainActivity.class);
            return PendingIntent.getActivity(getApplicationContext(), 0 , openAppIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }

        @Nullable
        @Override
        public CharSequence getCurrentContentText(Player player) {
            return null;
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            ImageView view = new ImageView(getApplicationContext());
            view.setImageResource(R.drawable.image1);

            BitmapDrawable bitmapDrawable = (BitmapDrawable) view.getDrawable();
//            if(bitmapDrawable == null) {
//                bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_launcher_background);
//
//            }
//
            assert  bitmapDrawable != null;
            return bitmapDrawable.getBitmap();
            ///return  null;

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(exoPlayer.isPlaying()) exoPlayer.stop();
        notificationManager.setPlayer(null);
        exoPlayer.release();
        exoPlayer = null;
        stopForeground(true);
        stopSelf();

    }
}