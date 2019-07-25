package test.practical.com.nfsdemo1;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.aykuttasil.callrecord.CallRecord;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 0;
    CallRecord callRecord;
    AudioRecorder audioRecorder;
    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        callRecord = new CallRecord.Builder(this)
                .setRecordFileName("RecordFileName")
                .setRecordDirName("RecordDirName")
                .setRecordDirPath(Environment.getExternalStorageDirectory().getPath()) // optional & default value
                .setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // optional & default value
                .setOutputFormat(MediaRecorder.OutputFormat.AMR_NB) // optional & default value
                .setAudioSource(MediaRecorder.AudioSource.DEFAULT) // optional & default value
                .setShowSeed(true)// optional & default value ->Ex: RecordFileName_incoming.amr || RecordFileName_outgoing.amr
                .build();
         audioRecorder = new AudioRecorder("MyRecord/parshva_recording");
        callRecord.startCallReceiver();

    }

    public void start(View view) {
        callRecord.startCallReceiver();
        /*try {
            audioRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void stop(View view) {
        callRecord.stopCallReceiver();
        /*try {
            audioRecorder.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }
}
