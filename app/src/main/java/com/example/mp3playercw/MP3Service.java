package com.example.mp3playercw;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MP3Service extends Service
{
    public static MP3Service instance = null; // implementing singleton by instantiating a reference to the instance
    private final String SERVICE_ID = "100"; // id for the service
    NotificationManager notificationManager = null;
    int notificationID = 001;
    private final IBinder binder = new MyBinder();
    MP3Player mainPlayer;
    int prevProgressTick = 1;
    Handler endServiceCheck = new Handler();
    Runnable r = new Runnable()
    {
        @Override
        public void run()
        {
            if (prevProgressTick == mainPlayer.getProgress())
            {
                mainPlayer.stop();
                instance.onDestroy();
            }
            else {
                prevProgressTick = mainPlayer.getProgress();
                endServiceCheck.postDelayed(this, 1000);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public void onCreate()
    {
        prevProgressTick = 1;
        instance = this;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mainPlayer = new MP3Player();
        super.onCreate();
    }

    @Override
    public void onDestroy()
    {
        instance = null;
        stopForeground(false);
        notificationManager.cancel(notificationID);
        super.onDestroy();
    }

    public void createNotification() // not gonna lie, this code was greatly inspired by lab03
    {
        CharSequence name = "MP3 Notification";
        String description = "Displays a notification when a song is playing";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(SERVICE_ID, name, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // when the user clicks the notification, this will bring them to the activity
        PendingIntent navToMainActivity = PendingIntent.getActivity(this, 0, intent, 0);

        // sets up the info and details for the notification
        final NotificationCompat.Builder mNotification = new NotificationCompat.Builder(this, SERVICE_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("MP3 Player")
                .setContentText("Playing music")
                .setContentIntent(navToMainActivity)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        startForeground(notificationID, mNotification.build());
    }

    public void removeNotification()
    {
        stopForeground(false);
        notificationManager.cancel(notificationID);
    }

    public class MyBinder extends Binder
    {

        public void loadMusic(String filePath)
        {
            if (instance == null)
                onCreate();
            mainPlayer.load(filePath);
        }

        public void playMusic()
        {
            mainPlayer.play();
            endServiceCheck.post(r);
            createNotification();
        }

        public void pauseMusic()
        {
            removeNotification();
            endServiceCheck.removeCallbacks(r);
            mainPlayer.pause();
        }

        public void stopMusic()
        {
            removeNotification();
            endServiceCheck.removeCallbacks(r);
            mainPlayer.stop();
        }

        public MP3Player.MP3PlayerState fetchPlayerState()
        {
            return mainPlayer.getState();
        }

        public int fetchDuration()
        {
            return mainPlayer.getDuration();
        }

        public int fetchProgress()
        {
            return mainPlayer.getProgress();
        }
    }
}
