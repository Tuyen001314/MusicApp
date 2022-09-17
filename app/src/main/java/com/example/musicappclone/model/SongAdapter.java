package com.example.musicappclone.model;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicappclone.MainActivity;
import com.example.musicappclone.PlayerService;
import com.example.musicappclone.R;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context context;
    List<Song> songs;
    ExoPlayer player;
    ConstraintLayout playerView;

    public SongAdapter(Context context, List<Song> songs, ExoPlayer player, ConstraintLayout playerView) {
        this.context = context;
        this.songs = songs;
        this.player = player;
        this.playerView = playerView;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Song song = songs.get(position);
        SongViewHolder viewHolder = (SongViewHolder) holder;

        viewHolder.title.setText(song.getTitle());
        viewHolder.duration.setText(getDuration(song.getDuration()));
        viewHolder.size.setText(getSize(song.getSize()));

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                context.startService(new Intent(context.getApplicationContext(), PlayerService.class));
                if (!player.isPlaying()) {
                    player.setMediaItems(getMediaItems(), position, 0);
                } else {
                    player.pause();
                    player.seekTo(position, 0);
                    playerView.setVisibility(View.VISIBLE);
                }
                playerView.setVisibility(View.VISIBLE);
                player.prepare();
                player.play();
                Toast.makeText(context, song.getTitle(), Toast.LENGTH_SHORT).show();
                if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ((MainActivity)context).recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);

                }
            }


        });

    }

    private List<MediaItem> getMediaItems() {
        List<MediaItem> mediaItems = new ArrayList<>();

        for (Song song : songs) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetadata(song))
                    .build();

            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    private MediaMetadata getMetadata(Song song) {
        return new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .build();
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {

        TextView title, duration, size;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTittle);
            duration = itemView.findViewById(R.id.tvDuration);
            size = itemView.findViewById(R.id.tvSize);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filerSong(List<Song> filteredList) {
        songs = filteredList;
        notifyDataSetChanged();
    }

    @SuppressLint("DefaultLocale")
    private String getDuration(int totalDuration) {
        String totalDurationText;
        //DecimalFormat dec = new DecimalFormat("0.00");

        int hrs = totalDuration / (1000 * 60 * 60);
        int min = (totalDuration % (1000 * 60 * 60)) / (1000 * 60);
        int secs = (((totalDuration % (1000 * 60 * 60)) % (1000 * 60 * 60)) % (1000 * 600)) / 1000;

        if (hrs < 1) {
            totalDurationText = String.format("%02d:%02d", min, secs);
        } else {
            totalDurationText = String.format("%1d:%02d:%02d", hrs, min, secs);
        }

        return totalDurationText;
    }

    private String getSize(long bytes) {
        String hrSize;
        double b = bytes;
        double k = bytes / 1024.0;
        double m = ((bytes / 1024.0) / 1024.0);
        double g = (((bytes / 1024.0) / 1024.0) / 1024.0);
        double t = ((((bytes / 1024.0) / 1024.0) / 1024.0) / 1024.0);

        DecimalFormat dec = new DecimalFormat("0.00");
        if (t > 1) {
            hrSize = dec.format(t).concat("TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat("GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat("MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat("KB");
        } else {
            hrSize = dec.format(b).concat("Bytes");
        }

        return hrSize;
    }

}