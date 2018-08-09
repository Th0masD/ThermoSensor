package com.thermosensor;

        import android.content.Context;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.media.AudioFormat;
        import android.media.AudioManager;
        import android.media.AudioRecord;
        import android.media.MediaPlayer;
        import android.media.MediaRecorder;
        import android.net.Uri;
        import android.preference.PreferenceManager;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Message;
        import android.util.Log;
        import android.view.Menu;
        import android.view.MenuInflater;
        import android.view.MenuItem;
        import android.view.View;
        import android.widget.TextView;
        import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


    TextView myTextFrequency;
    TextView myTextTemperature;
    TextView myTextUnits;


    public float fTemperatureCelsius;
    public float frequency;
    public int number_of_periods;
    public int number_of_samples;
    public int number_of_samples_temp;
    public int start_samples;
    public int stop_samples;

    RecorderThread recorderThread;
    public boolean recording;

    public MediaPlayer mp;


    class RecorderThread extends Thread {


        public RecorderThread () {
        }

        @Override
        public void run() {
            AudioRecord recorder;
            short audioData[];
            int bufferSize,p;

            bufferSize=50000;

            recorder = new AudioRecord (MediaRecorder.AudioSource.MIC,44100,AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,bufferSize);
            audioData = new short [bufferSize];


            while (recording) {  //loop while recording is needed
                if (recorder.getState()==android.media.AudioRecord.STATE_INITIALIZED)
                    if (recorder.getRecordingState()==android.media.AudioRecord.RECORDSTATE_STOPPED)
                        recorder.startRecording();

                    else {

                        recorder.read(audioData,0,bufferSize); //read the PCM audio data into the audioData array

                        //Now we need to decode the PCM data using the Zero Crossings Method
                        p=0;
                        number_of_periods=0;
                        start_samples=0;
                        stop_samples=0;


                        while ((audioData[p]<0 && audioData[p+1]>=0)==false) {
                            p++;
                        }

                        for (number_of_samples=0;number_of_samples<=10000;number_of_samples++)
                        {

                            if(audioData[p]<0 && audioData[p+1]>=0) {
                                number_of_periods++;
                                number_of_samples_temp=0;
                            }
                            number_of_samples_temp++;
                            p++;
                        }

                        number_of_periods--;
                    }


                frequency =(float) ((float) 1/(((10000-number_of_samples_temp)/(float)number_of_periods)*0.0000226757));
                fTemperatureCelsius=(frequency/4)-273;

                handler1.sendMessage(handler1.obtainMessage());
            } //while recording

            if (recorder.getState()==android.media.AudioRecord.RECORDSTATE_RECORDING) recorder.stop();
            recorder.release();

        }

    }


    Handler handler1 = new Handler(){

        @Override
        public void handleMessage(Message msg) {

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String strTemperatureOffset = SP.getString("TemperatureOffset","0");
            boolean bFrequencySwitch = SP.getBoolean("ShowFrequency",true);
            String strTemperatureUnits = SP.getString("TemperatureUnits","NA");

            float fTemperatureOffset = Float.parseFloat(strTemperatureOffset);
            float fTemperatureFahrenheit;


            if (strTemperatureUnits.equals("1")) {
                myTextUnits.setText("°C");
                myTextTemperature.setText(String.format( "%.1f", fTemperatureCelsius+fTemperatureOffset) );
            } else if (strTemperatureUnits.equals("2")) {
                myTextUnits.setText("°F");
                fTemperatureFahrenheit= (float) (fTemperatureCelsius*1.8+32); // convert Celsius to Fahrenheit
                myTextTemperature.setText(String.format( "%.1f", fTemperatureFahrenheit+fTemperatureOffset) );
            }


            if (bFrequencySwitch==true) {
                myTextFrequency.setText(String.format( "%.2f"+"Hz", frequency ) );
            }   else {
                myTextFrequency.setText(String.format( "", frequency ) );
            }

        }

    };



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myTextFrequency = (TextView)findViewById(R.id.textFrequency);
        myTextTemperature = (TextView)findViewById(R.id.textTemperature);
        myTextUnits = (TextView)findViewById(R.id.textUnits);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                MyPreferencesActivity();
                return true;
            case R.id.action_about:
                About();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    private void MyPreferencesActivity() {
            Intent i = new Intent(MainActivity.this, MyPreferencesActivity.class);
            startActivity(i);
        }

    private void About() {
        Intent i = new Intent(MainActivity.this, About.class);
        startActivity(i);
    }


    @Override
    protected void onStart() {
        super.onStart();

        mp = new MediaPlayer();
        mp = MediaPlayer.create(this, R.raw.invert120s15khz);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setLooping(true);
        mp.start();

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);


        recorderThread = new RecorderThread();
        recorderThread.start();
        recording = true;

    }

    protected void onPause() {
        super.onPause();
    }


    protected void onResume() {
        super.onResume();
    }



    @Override
    protected void onStop() {
        super.onStop();
        recording = false;
        recorderThread.interrupt();
        mp.reset();
        mp.release();



    }
}