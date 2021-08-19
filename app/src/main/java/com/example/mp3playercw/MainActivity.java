package com.example.mp3playercw;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
{

    static SeekBar musicProgBar;
    Handler progBarHandler = new Handler();
    final int progBarDelay = 1000;
    Runnable r = new Runnable()
    {
        @Override
        public void run()
        {
            if (musicProgBar.getProgress() == myService.fetchProgress() && myService.fetchProgress() != 0)
                Log.d("audio", "over");
            else if (myService.fetchPlayerState() == MP3Player.MP3PlayerState.PLAYING)
            {
                int tempInt = myService.fetchProgress();
                long minutes = TimeUnit.MILLISECONDS.toMinutes(tempInt);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(tempInt);
                if (seconds >= 60)
                    seconds = seconds % 60;
                String tempString = minutes + ":" + seconds;
                progressText.setText(tempString);
                musicProgBar.setProgress(tempInt); // sets the current progress of the seekbar to be the progress of the song
                progBarHandler.postDelayed(this, progBarDelay);
            }
        }
    };

    private MP3Service.MyBinder myService = null;
    static TextView progressText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        musicProgBar = findViewById(R.id.musicProgressBar);
        progressText = findViewById(R.id.progressText);
        final ListView lv = (ListView) findViewById(R.id.musicList);
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                null,
                null);

        lv.setAdapter(new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                cursor,
                new String[] { MediaStore.Audio.Media.DATA},
                new int[] { android.R.id.text1 }));

        final Intent tempIntent = new Intent(this, MP3Service.class);
        startService(tempIntent);
        bindService(tempIntent, serviceConnection, 0);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> myAdapter,
                                    View myView,
                                    int myItemInt,
                                    long myLong) {
                Cursor c = (Cursor) lv.getItemAtPosition(myItemInt);
                String uri = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA));
                Log.d("g53mdp", uri);

                if (myService.fetchPlayerState() != MP3Player.MP3PlayerState.STOPPED)
                    myService.stopMusic();

                myService.loadMusic(uri);
                myService.playMusic();
                musicProgBar.setMax(myService.fetchDuration()); // set the max value of the seekbar to be the duration of the song

                progBarHandler.post(r);
            }});
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            myService = (MP3Service.MyBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            myService = null;
        }
    };

    public void playButClicked(View view)
    {
        myService.playMusic();
        progBarHandler.post(r);
    }

    public void pauseButClicked(View view)
    {
        myService.pauseMusic();
        progBarHandler.removeCallbacks(r);
    }

    public void stopButClicked(View view)
    {
        myService.stopMusic();
        progBarHandler.removeCallbacks(r);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        int tempInt = savedInstanceState.getInt("progbar");
        musicProgBar.setProgress(tempInt);
        musicProgBar.setMax(savedInstanceState.getInt("maxval"));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tempInt);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(tempInt);
        if (seconds >= 60)
            seconds = seconds % 60;
        String tempString = minutes + ":" + seconds;
        progressText.setText(tempString);
        progBarHandler.post(r);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("progbar", myService.fetchProgress());
        savedInstanceState.putInt("maxval", musicProgBar.getMax());
    }

    @Override
    protected void onDestroy()
    {
        unbindService(serviceConnection);
        progBarHandler.removeCallbacks(r);
        super.onDestroy();
    }
}