package com.example.musicappclone;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chibde.visualizer.BarVisualizer;
import com.example.musicappclone.model.Song;
import com.example.musicappclone.model.SongAdapter;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.jgabrielfreitas.core.BlurImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {


    ArrayList<String> arrayList;
    String[] items;
    RecyclerView recyclerView;
    SongAdapter myAdapter;
    List<Song> allSongs = new ArrayList<>();
    List<Song> songs;

    ActivityResultLauncher<String> storagePermissionLauncher;
    final String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
    ExoPlayer exoPlayer;
    final String recordAudioPermission = Manifest.permission.RECORD_AUDIO;
    ConstraintLayout playerView;

    public ActivityResultLauncher<String> recordAudioPermissionLauncher;
    ImageView playerCloseBtn;
    TextView songNameView, homeSongNameView;
    //controls
    ImageView skipPreviousBtn, skipNextBtn, playPauseBtn, repeatModeBtn, playlistBtn;
    ImageView homeSkipPreviousBtn, homePlayPauseBtn, homeSkipNextBtn;
    //wrappers
    ConstraintLayout homeControlWrapper, headWrapper, artworkWrapper, seekbarWrapper, controlWrapper, audioVisualizerWrapper;
    //artwork
    CircleImageView artworkView;
    //seekbar
    SeekBar seekBar;
    TextView progressView, durationView;
    //audio visualier
    BarVisualizer audioVisualier;
    //blur image view
    BlurImageView blurImageView;
    //status bar and n
    // avigation color
    int defaultStatusColor;
    //repeat model
    int repeatMode = 1; // repeat all = 1, repeat one = 2, shuffle all = 3;


    boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyceler);
        //runtimePermission();
        defaultStatusColor = getWindow().getStatusBarColor();
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
           if(result) {
               fetchSongs();
           }
           else {
               userResponse();
           }
        });

        storagePermissionLauncher.launch(permission);

        recordAudioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if(result && exoPlayer.isPlaying()) {
                activateAudioVisualizer();
            } else {
                userResponseOnRecordAudioPerm();
            }
        });

        //exoPlayer = new ExoPlayer.Builder(this).build();

        playerView = findViewById(R.id.playerView);
        playerCloseBtn = findViewById(R.id.btnBack);
        songNameView = findViewById(R.id.nameSong);
        skipPreviousBtn = findViewById(R.id.skipPreviousBtn);
        skipNextBtn = findViewById(R.id.skipNextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        repeatModeBtn = findViewById(R.id.repeatModeBtn);
        playlistBtn = findViewById(R.id.listItem);

        homeSongNameView = findViewById(R.id.homeNameSong);
        homeSkipNextBtn = findViewById(R.id.homeSkipNextBtn);
        homeSkipPreviousBtn = findViewById(R.id.homeSkipPreviousBtn);
        homePlayPauseBtn = findViewById(R.id.homePlayBtn);

        homeControlWrapper = findViewById(R.id.homeControlWrapper);
        headWrapper = findViewById(R.id.headWrapper);
        artworkWrapper = findViewById(R.id.artWrapper);
        seekbarWrapper = findViewById(R.id.seekbarWrapper);
        controlWrapper= findViewById(R.id.controlWrapper);
        //audioVisualizerWrapper = findViewById(R.id.audioVisualizerWrapper);

        artworkView = findViewById(R.id.artworkView);
        seekBar = findViewById(R.id.seekbar);
        progressView = findViewById(R.id.progressView);
        durationView = findViewById(R.id.durationView);
        //audioVisualier = findViewById(R.id.visualizer);

        blurImageView = findViewById(R.id.blurImageView);
        ///playerControl();


        doBindService();

    }

    private void doBindService() {
        Intent playerServiceIntent = new Intent(this, PlayerService.class);
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.ServiceBinder binder = (PlayerService.ServiceBinder) service;
            exoPlayer = binder.getPlayerService().exoPlayer;
            isBound = true;
            //ready to show songs
            storagePermissionLauncher.launch(permission);
            //call player control method
            playerControl();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    private void playerControl() {
        songNameView.setSelected(true);
        homeSongNameView.setSelected(true);

        playerCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitPlayerView();
            }
        });

        playlistBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitPlayerView();
            }
        });

        homeControlWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlayerView();
            }
        });

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                assert mediaItem != null;
                songNameView.setText(mediaItem.mediaMetadata.title);
                homeSongNameView.setText(mediaItem.mediaMetadata.title);
                
                progressView.setText(getReadableTime((int) exoPlayer.getCurrentPosition()));
                seekBar.setProgress((int) exoPlayer.getCurrentPosition());
                seekBar.setMax((int) exoPlayer.getDuration());
                durationView.setText(getReadableTime((int) exoPlayer.getDuration()));
                playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
                homePlayPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24);

                //show the current art work
                showCurrentArtwork();
                //update the progress position of a current playing song
                updatePlayerPositionProgress();
                //load artwork animation
                artworkView.setAnimation(loadRotation());
                //set audio visualier
                activateAudioVisualizer();
                updatePlayerColors();
                if(!exoPlayer.isPlaying()) {
                    exoPlayer.play();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if(playbackState == ExoPlayer.STATE_READY) {
                    songNameView.setText(Objects.requireNonNull(exoPlayer.getCurrentMediaItem()).mediaMetadata.title);
                    homeSongNameView.setText(exoPlayer.getCurrentMediaItem().mediaMetadata.title);
                    progressView.setText(getReadableTime((int) exoPlayer.getCurrentPosition()));
                    durationView.setText(getReadableTime((int) exoPlayer.getDuration()));
                    seekBar.setMax((int) exoPlayer.getDuration());
                    seekBar.setProgress((int) exoPlayer.getCurrentPosition());
                    playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
                    homePlayPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24);

                    //show the current art work
                    showCurrentArtwork();

                    //update the progress position of a current playing song
                    updatePlayerPositionProgress();
                    //load artwork animation
                    artworkView.setAnimation(loadRotation());

                    //set audio visualier
                    //activateAudioVisualizer();

                    updatePlayerColors();
                }
                else {
                    playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24);
                }

            }
        });

        skipNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToNextSong();
            }
        });

        homeSkipNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToNextSong();
            }
        });

        skipPreviousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToPreviousSong();
            }
        });

        homeSkipPreviousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToPreviousSong();
            }
        });
        
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playOrPausePlayer();
            }
        });

        homePlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playOrPausePlayer();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressValue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(exoPlayer.getPlaybackState() == ExoPlayer.STATE_READY) {
                    seekBar.setProgress(progressValue);
                    progressView.setText(getReadableTime(progressValue));
                    exoPlayer.seekTo(progressValue);
                }
                //seekBar.setProgress(progressValue);

            }
        });

        repeatModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(repeatMode == 1) {
                    exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                    repeatModeBtn.setImageResource(R.drawable.ic_baseline_repeat_one_24);
                    repeatMode = 2;
                }
                else if(repeatMode == 2) {
                    exoPlayer.setShuffleModeEnabled(true);
                    exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
                    repeatMode = 3;
                    repeatModeBtn.setImageResource(R.drawable.ic_baseline_shuffle_24);
                }
                else {
                    exoPlayer.setShuffleModeEnabled(false);
                    exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
                    repeatMode = 1;
                    repeatModeBtn.setImageResource(R.drawable.ic_baseline_repeat_24);

                }

                updatePlayerColors();
            }
        });

    }

    private void playOrPausePlayer() {
        if(exoPlayer.isPlaying()) {
            exoPlayer.pause();
            playPauseBtn.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
            homePlayPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            artworkView.clearAnimation();
        }
        else {
            exoPlayer.play();
            playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
            homePlayPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24);
            artworkView.startAnimation(loadRotation());
        }
        updatePlayerColors();
    }

    private void skipToPreviousSong() {
        if(exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPrevious();
        }
    }

    private void skipToNextSong() {
        if(exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNext();
        }
    }

    private Animation loadRotation() {
        RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(exoPlayer.isPlaying()) {
                    progressView.setText(getReadableTime((int) exoPlayer.getCurrentPosition()));
                    seekBar.setProgress((int) exoPlayer.getCurrentPosition());
                }
                // repeat calling method
                updatePlayerPositionProgress();
            }
        }, 1000);
    }

    private void showCurrentArtwork() {
        artworkView.setImageURI(Objects.requireNonNull(exoPlayer.getCurrentMediaItem()).mediaMetadata.artworkUri);
        if(artworkView.getDrawable() == null) {
            artworkView.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
            homePlayPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }
    }

    String getReadableTime(int totalDuration) {
        String time;
        //DecimalFormat dec = new DecimalFormat("0.00");

        int hrs = totalDuration / (1000 * 60 * 60);
        int min = (totalDuration % (1000 * 60 * 60)) / (1000 * 60);
        int secs = (((totalDuration % (1000 * 60 * 60)) % (1000 * 60 * 60)) % (1000 * 600)) / 1000;

        if (hrs < 1) {
            time = String.format("%02d:%02d", min, secs);
        } else {
            time = String.format("%1d:%02d:%02d", hrs, min, secs);
        }

        return time;
    }

    private void showPlayerView() {
        playerView.setVisibility(View.VISIBLE);
        updatePlayerColors();
    }

    private void updatePlayerColors() {

        if(playerView.getVisibility() == View.GONE) {
            return;
        }
        BitmapDrawable bitmapDrawable = (BitmapDrawable) artworkView.getDrawable();
        if(bitmapDrawable == null) {
            bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.image1);
         }


        assert  bitmapDrawable != null;
        Bitmap bmp = bitmapDrawable.getBitmap();

        blurImageView.setImageBitmap(bmp);
        blurImageView.setBlur(4);

        // player control colors

        Palette.from(bmp).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                if(palette != null) {
                    Palette.Swatch swatch = palette.getDarkVibrantSwatch();
                    if(swatch == null) {
                        swatch = palette.getMutedSwatch();
                        if(swatch == null) {
                            swatch = palette.getDominantSwatch();
                        }
                    }

                    assert  swatch != null;
                    int titleTextColor = swatch.getTitleTextColor();
                    int bodyTextColor = swatch.getBodyTextColor();
                    int rgbColor = swatch.getRgb();

                    //sets colors to player views;
                    // status & navigation bar colors
                    getWindow().setStatusBarColor(rgbColor);
                    getWindow().setNavigationBarColor(rgbColor);

                    songNameView.setTextColor(titleTextColor);
                    playerCloseBtn.setColorFilter(titleTextColor);
                    progressView.setTextColor(bodyTextColor);
                    durationView.setTextColor(bodyTextColor);

                    repeatModeBtn.setColorFilter(bodyTextColor);
                    skipPreviousBtn.setColorFilter(bodyTextColor);
                    skipNextBtn.setColorFilter(bodyTextColor);

                    playlistBtn.setColorFilter(bodyTextColor);
                    playPauseBtn.setColorFilter(bodyTextColor);
                    if(playPauseBtn.getResources().equals(R.drawable.ic_baseline_pause_circle_outline_24)) {
                        playPauseBtn.setColorFilter(titleTextColor);
                    }
                }
            }
        });


    }

    private void exitPlayerView() {
        playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

    }

    private void userResponseOnRecordAudioPerm() {
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.M) {
            if(shouldShowRequestPermissionRationale(recordAudioPermission)) {
                new AlertDialog.Builder(this)
                        .setTitle("Requesting to show Audio Visualizer")
                        .setMessage("Alow this app to display audio visualier when music is playing")
                        .setPositiveButton("allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                recordAudioPermissionLauncher.launch(recordAudioPermission);
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getApplicationContext(), "you denied to show the audio visualier", Toast.LENGTH_SHORT).show();
                            }
                        }).show();
            } else {
                Toast.makeText(getApplicationContext(), "you denied to show the audio visualier", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void activateAudioVisualizer() {
        // check if we have record audio permission to show an audio visualier
//        if(ContextCompat.checkSelfPermission(this, recordAudioPermission) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        // set color to audio visualier
//        audioVisualier.setColor(ContextCompat.getColor(this, R.color.green));
//        //set number of visualier btn 1 and 256
//        audioVisualier.setDensity(70);
//        //set the audio session id from the player
//        audioVisualier.setPlayer(exoPlayer.getAudioSessionId());
    }

    private void userResponse() {

        if(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            fetchSongs();
        }
        else if(shouldShowRequestPermissionRationale(permission)) {
            new AlertDialog.Builder(this)
                    .setTitle("Requesting Permission")
                    .setMessage("Allow us to fetch and show songs on your device")
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            storagePermissionLauncher.launch(permission);
                        }
                    }).setNegativeButton("Don't Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "Tou denied to fetch songs", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    }).show();
        } else {
            Toast.makeText(this, "You denied to fetch songs", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchSongs() {
        Toast.makeText(this, "We are ready to fetch songs. Thanks", Toast.LENGTH_SHORT).show();

        songs = new ArrayList<>();
        Uri songLibraryUri;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            songLibraryUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            songLibraryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                //MediaStore.Audio.Media.ALBUM_ID,
        };
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        try(Cursor cursor = getContentResolver().query(songLibraryUri, projection, null, null, sortOrder)) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColum = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
//            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColum);
                int size = cursor.getInt(sizeColumn);
//                long albumId = cursor.getLong(albumIdColumn);

                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                //Uri albumArtworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                name = name.substring(0, name.lastIndexOf("."));

                Song song = new Song(name, uri, size, duration);
                
                songs.add(song);

            }
            showSongs(songs);
        }
    }

    @Override
    public void onBackPressed() {
        // we say if the player view is visible , close it

        if(playerView.getVisibility() == View.VISIBLE) {
            exitPlayerView();
        }
        super.onBackPressed();

    }

    private void showSongs(List<Song> songs) {
        if(songs.size() == 0) {
            Toast.makeText(this, "No Songs", Toast.LENGTH_SHORT).show();
            Log.e("TAG", songs.size() +  "");
            return;


        }

        allSongs.clear();
        allSongs.addAll(songs);
        //String title = getResources().getString(R.string.app_name);
        myAdapter = new SongAdapter(this, songs, exoPlayer, playerView);

        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(myAdapter);
        scaleInAnimationAdapter.setDuration(1000);
        scaleInAnimationAdapter.setInterpolator((new OvershootInterpolator()));
        scaleInAnimationAdapter.setFirstOnly(false);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(linearLayoutManager);
        //ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, allSongs);
        recyclerView.setAdapter(scaleInAnimationAdapter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnBindService();
    }

    private void doUnBindService() {
        if(isBound) {
            unbindService(playerServiceConnection);
            isBound = false;
        }
    }
}