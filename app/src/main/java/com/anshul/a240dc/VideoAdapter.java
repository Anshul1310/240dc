package com.anshul.a240dc;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private Context context; // Need context to start external player
    private List<VideoItem> videoList;

    public VideoAdapter(Context context, List<VideoItem> videoList) {
        this.context = context;
        this.videoList = videoList;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = videoList.get(position);

        holder.tvName.setText(video.getName());
        holder.tvDuration.setText("⏱ " + video.getDuration());

        // Format the technical specs cleanly
        String specs = String.format("%d FPS  •  ISO %d  •  %s",
                video.getFps(), video.getIso(), video.getShutterSpeed());
        holder.tvSpecs.setText(specs);

        if (video.getThumbnailResId() != 0) {
            holder.imgThumbnail.setImageResource(video.getThumbnailResId());
        }

        // --- NEW: Play video externally on click ---
        holder.itemView.setOnClickListener(v -> {
            File videoFile = new File(video.getPath());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(videoFile);
            intent.setDataAndType(uri, "video/mp4");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Starts the device's default video player!
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView tvName, tvDuration, tvSpecs;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            tvName = itemView.findViewById(R.id.tv_video_name);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvSpecs = itemView.findViewById(R.id.tv_specs);
        }
    }
}