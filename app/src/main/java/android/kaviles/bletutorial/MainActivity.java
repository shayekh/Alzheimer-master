package android.kaviles.bletutorial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, SensorEventListener {
    private final static String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;

    private HashMap<String, BTLE_Device> mBTDevicesHashMap;
    private ArrayList<BTLE_Device> mBTDevicesArrayList;
   // private ListAdapter_BTLE_Devices adapter;

    private Button btn_Scan;
    private String placeName;
    private ImageView tapImage;
    private TextView tv_name,tv_rssi,tv_macaddr;

    private BroadcastReceiver_BTState mBTStateUpdateReceiver;
    private Scanner_BTLE mBTLeScanner;
    private MediaPlayer mediaPlayer;
    private FirebaseAuth firebaseAuth;




    //Compass
    ImageView compass_img;
    TextView txt_compass;
    int mAzimuth;
    private SensorManager mSensorManager;
    private Sensor mRotationV, mAccelerometer, mMagnetometer;
    boolean haveSensor = false, haveSensor2 = false;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        placeName=getIntent().getStringExtra("destination");
        firebaseAuth=FirebaseAuth.getInstance();

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Utils.toast(getApplicationContext(), "BLE not supported");
            finish();
        }

        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());
        mBTLeScanner = new Scanner_BTLE(this, 7500, -75);

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();

       // adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);

//        ListView listView = new ListView(this);
//        listView.setAdapter(adapter);
//        listView.setOnItemClickListener(this);

        btn_Scan = (Button) findViewById(R.id.btn_scan);
        tapImage= (ImageView) findViewById(R.id.img_tap);
        //((ScrollView) findViewById(R.id.scrollView)).addView(listView);
        findViewById(R.id.btn_scan).setOnClickListener(this);

        tv_name = (TextView) findViewById(R.id.tv_name);

        tv_rssi = (TextView) findViewById(R.id.tv_rssi);


        tv_macaddr = (TextView) findViewById(R.id.tv_macaddr);

        tapImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBTDevicesArrayList.size()>0)
                {
                    BTLE_Device words= mBTDevicesArrayList.get(0);

                    if(placeName.equals("A Place (EX : Cardiac Center)") && words.getName().equals("Smart Watch 5"))
                    {
                        Toast.makeText(MainActivity.this,"Go South", Toast.LENGTH_LONG).show();
                        mediaPlayer= MediaPlayer.create(getApplicationContext(),R.raw.gotostraight);
                        if(mediaPlayer!=null) {
                            mediaPlayer.start();

                        }
                    }
                    else if(placeName.equals("B Place (EX : Report Delivery Center)") && words.getName().equals("Smart Watch 5"))
                    {
                        Toast.makeText(MainActivity.this,"Go North", Toast.LENGTH_LONG).show();
                        mediaPlayer= MediaPlayer.create(getApplicationContext(),R.raw.goright);
                        if(mediaPlayer!=null) {
                            mediaPlayer.start();
                        }
                    }
                    else if(placeName.equals("C Place (EX : Pharmacy)") && words.getName().equals("Smart Watch 5"))
                    {
                        Toast.makeText(MainActivity.this,"Go East", Toast.LENGTH_LONG).show();
                        mediaPlayer= MediaPlayer.create(getApplicationContext(),R.raw.goleft);
                        if(mediaPlayer!=null) {
                            mediaPlayer.start();
                        }
                    }
                    else if(placeName.equals("D Place (EX : Exit)") && words.getName().equals("Smart Watch 5"))
                    {
                        Toast.makeText(MainActivity.this,"Go West", Toast.LENGTH_LONG).show();
                        mediaPlayer= MediaPlayer.create(getApplicationContext(),R.raw.turn);
                        if(mediaPlayer!=null) {
                            mediaPlayer.start();
                        }
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, "Outer Signal"+words.getName(), Toast.LENGTH_LONG).show();
                    }

                }

                else {
                    Toast.makeText(MainActivity.this, "Scan First", Toast.LENGTH_LONG).show();
                }



            }
        });



        //compass
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        compass_img = (ImageView) findViewById(R.id.img_compass);
        txt_compass = (TextView) findViewById(R.id.txt_azimuth);

        start();


    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(mBTStateUpdateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();

    }

    @Override
    protected void onPause() {
        super.onPause();

        stopScan();
        stop();
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mBTStateUpdateReceiver);
        stopScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Utils.toast(getApplicationContext(), "Thank you for turning on Bluetooth");
            }
            else if (resultCode == RESULT_CANCELED) {
                Utils.toast(getApplicationContext(), "Please turn on Bluetooth");
            }
        }
    }




    /**
     * Called when an item in the ListView is clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Used in future BLE tutorials
        BTLE_Device words= mBTDevicesArrayList.get(position);
        if(placeName.equals("A Place (EX : Cardiac Center)") && words.getName().equals("Smart Watch 5"))
        {
            Toast.makeText(this,"Go South", Toast.LENGTH_LONG).show();
            mediaPlayer= MediaPlayer.create(getApplicationContext(),R.raw.gotostraight);
            if(mediaPlayer!=null) {
                mediaPlayer.start();

            }
        }
        else if(placeName.equals("B Place (EX : Report Delivery Center)") && words.getName().equals("Smart Watch 5"))
        {
            Toast.makeText(this,"Go North", Toast.LENGTH_LONG).show();
            mediaPlayer= MediaPlayer.create(getApplicationContext(),R.raw.goright);
            if(mediaPlayer!=null) {
                mediaPlayer.start();
            }
        }
       else if(placeName.equals("C Place (EX : Pharmacy)") && words.getName().equals("Smart Watch 5"))
        {
            Toast.makeText(this,"Go East", Toast.LENGTH_LONG).show();
            mediaPlayer= MediaPlayer.create(getApplicationContext(),R.raw.goleft);
            if(mediaPlayer!=null) {
                mediaPlayer.start();
            }
        }
        else if(placeName.equals("D Place (EX : Exit)") && words.getName().equals("Smart Watch 5"))
        {
            Toast.makeText(this,"Go West", Toast.LENGTH_LONG).show();
            mediaPlayer= MediaPlayer.create(getApplicationContext(),R.raw.turn);
            if(mediaPlayer!=null) {
                mediaPlayer.start();
            }
        }
        else
        {
            Toast.makeText(this, "Outer Signal"+words.getName(), Toast.LENGTH_LONG).show();
        }
       // Toast.makeText(this, words.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Called when the scan button is clicked.
     * @param v The view that was clicked
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btn_scan:
                Utils.toast(getApplicationContext(), "Scan Button Pressed");

                if (!mBTLeScanner.isScanning()) {
                    startScan();
                }
                else {
                    stopScan();
                }

                break;
            default:
                break;
        }
    }

    /**
     * Adds a device to the ArrayList and Hashmap that the ListAdapter is keeping track of.
     * @param device the BluetoothDevice to be added
     * @param rssi the rssi of the BluetoothDevice
     */
    public void addDevice(BluetoothDevice device, int rssi) {

        String address = device.getAddress();
        if (!mBTDevicesHashMap.containsKey(address)) {
            BTLE_Device btleDevice = new BTLE_Device(device);
            btleDevice.setRSSI(rssi);

            mBTDevicesHashMap.put(address, btleDevice);
            mBTDevicesArrayList.add(btleDevice);
        }
        else {
            mBTDevicesHashMap.get(address).setRSSI(rssi);
        }
        Collections.sort(mBTDevicesArrayList);
        Collections.reverse(mBTDevicesArrayList);

        if(mBTDevicesArrayList.size()!=0) {
            if (mBTDevicesArrayList.get(0).getName() != null && mBTDevicesArrayList.get(0).getName().length() > 0) {
                tv_name.setText(mBTDevicesArrayList.get(0).getName());
            } else {
                tv_name.setText("No Name");
            }

            tv_rssi.setText("RSSI: " + Integer.toString(mBTDevicesArrayList.get(0).getRSSI()));
            if (mBTDevicesArrayList.get(0).getAddress() != null && mBTDevicesArrayList.get(0).getAddress().length() > 0) {
                tv_macaddr.setText(mBTDevicesArrayList.get(0).getAddress());
            } else {
                tv_macaddr.setText("No Address");
            }
        }

        //adapter.notifyDataSetChanged();
    }

    /**
     * Clears the ArrayList and Hashmap the ListAdapter is keeping track of.
     * Starts Scanner_BTLE.
     * Changes the scan button text.
     */
    public void startScan(){
        btn_Scan.setText("Scanning...");

        mBTDevicesArrayList.clear();
        mBTDevicesHashMap.clear();

        //adapter.notifyDataSetChanged();

        mBTLeScanner.start();
    }

    /**
     * Stops Scanner_BTLE
     * Changes the scan button text.
     */
    public void stopScan() {
        btn_Scan.setText("Scan Again");

        mBTLeScanner.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId=item.getItemId();
        if (itemId==R.id.menu_logOut){
            firebaseAuth.signOut();
            Intent intent=new Intent(MainActivity.this,LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }




    //compass
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        mAzimuth = Math.round(mAzimuth);
        compass_img.setRotation(-mAzimuth);

        String where = "NW";

        if (mAzimuth >= 350 || mAzimuth <= 10)
            where = "N";
        if (mAzimuth < 350 && mAzimuth > 280)
            where = "NW";
        if (mAzimuth <= 280 && mAzimuth > 260)
            where = "W";
        if (mAzimuth <= 260 && mAzimuth > 190)
            where = "SW";
        if (mAzimuth <= 190 && mAzimuth > 170)
            where = "S";
        if (mAzimuth <= 170 && mAzimuth > 100)
            where = "SE";
        if (mAzimuth <= 100 && mAzimuth > 80)
            where = "E";
        if (mAzimuth <= 80 && mAzimuth > 10)
            where = "NE";


        txt_compass.setText(mAzimuth + "° " + where);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void start() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if ((mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) || (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)) {
                noSensorsAlert();
            }
            else {
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                haveSensor = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
                haveSensor2 = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
        else{
            mRotationV = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = mSensorManager.registerListener(this, mRotationV, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void noSensorsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Your device doesn't support the Compass.")
                .setCancelable(false)
                .setNegativeButton("Close",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        alertDialog.show();
    }

    public void stop() {
        if (haveSensor) {
            mSensorManager.unregisterListener(this, mRotationV);
        }
        else {
            mSensorManager.unregisterListener(this, mAccelerometer);
            mSensorManager.unregisterListener(this, mMagnetometer);
        }
    }

}
