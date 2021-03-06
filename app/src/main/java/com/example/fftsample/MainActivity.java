package com.example.fftsample;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.example.fftsample.R;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import com.emotiv.insight.IEdk;
import com.emotiv.insight.IEdkErrorCode;
import com.emotiv.insight.IEdk.IEE_DataChannel_t;
import com.emotiv.insight.IEdk.IEE_Event_t;;

public class MainActivity extends Activity {

	private static final int REQUEST_ENABLE_BT = 1;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean lock = false;
	private boolean isEnablGetData = false;
	private boolean isEnableWriteFile = false;
	int userId;
	private BufferedWriter motion_writer;
	Button Start_button,Stop_button;
	IEE_DataChannel_t[] Channel_list = {IEE_DataChannel_t.IED_AF3, IEE_DataChannel_t.IED_T7,IEE_DataChannel_t.IED_Pz,
			IEE_DataChannel_t.IED_T8,IEE_DataChannel_t.IED_AF4};
	String[] Name_Channel = {"AF3","T7","Pz","T8","AF4"};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
		Start_button = (Button)findViewById(R.id.startbutton);
		Stop_button  = (Button)findViewById(R.id.stopbutton);

		Start_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.e("FFTSample","Start Write File");
				setDataFile();
				isEnableWriteFile = true;
			}
		});
		Stop_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.e("FFTSample","Stop Write File");
				StopWriteFile();
				isEnableWriteFile = false;
			}
		});

		//Connect to emoEngine
		IEdk.IEE_EngineConnect(this,"");
		Thread processingThread=new Thread()
		{
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				while(true)
				{
					try
					{
						handler.sendEmptyMessage(0);
						handler.sendEmptyMessage(1);
						if(isEnablGetData && isEnableWriteFile)handler.sendEmptyMessage(2);
						Thread.sleep(5);
					}

					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
		};
		processingThread.start();
	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

				case 0:
					int state = IEdk.IEE_EngineGetNextEvent();
					if (state == IEdkErrorCode.EDK_OK.ToInt()) {
						int eventType = IEdk.IEE_EmoEngineEventGetType();
						userId = IEdk.IEE_EmoEngineEventGetUserId();
						if(eventType == IEE_Event_t.IEE_UserAdded.ToInt()){
							Log.e("SDK","User added");
							IEdk.IEE_FFTSetWindowingType(userId, IEdk.IEE_WindowsType_t.IEE_BLACKMAN);
							isEnablGetData = true;
						}
						if(eventType == IEE_Event_t.IEE_UserRemoved.ToInt()){
							Log.e("SDK","User removed");
							isEnablGetData = false;
						}
					}

					break;
				case 1:
				/*Connect device with Insight headset*/
					int number = IEdk.IEE_GetInsightDeviceCount();
					if(number != 0) {
						if(!lock){
							lock = true;
							IEdk.IEE_ConnectInsightDevice(0);
						}
					}
					/**************************************/
				/*Connect device with Epoc Plus headset*/
//				int number = IEdk.IEE_GetEpocPlusDeviceCount();
//				if(number != 0) {
//					if(!lock){
//						lock = true;
//						IEdk.IEE_ConnectEpocPlusDevice(0,false);
//					}
//				}
					/**************************************/
					else lock = false;
					break;
				case 2:
					for(int i=0; i < Channel_list.length; i++)
					{
                        DateFormat df = new SimpleDateFormat("HH:mm");
                        String time = df.format(Calendar.getInstance().getTime());
                        double[] data = IEdk.IEE_GetAverageBandPowers(Channel_list[i]);

						if(data.length == 5){
							try {
								motion_writer.write("<eeg channel=\""+Name_Channel[i] +"\" time=\""+time+ "\" dataset=\"");

								for(int j=0; j < data.length;j++)
									addData(data[j]);
                                motion_writer.write("\" />");
								motion_writer.newLine();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}

					break;
			}

		}

	};

	private void setDataFile() {
		try {

			String eeg_header = "<headset>";
			File root = Environment.getExternalStorageDirectory();
			String file_path = root.getAbsolutePath()+ "/FFTSample/";
			File folder=new File(file_path);
			if(!folder.exists())
			{
				folder.mkdirs();
			}
			motion_writer = new BufferedWriter(new FileWriter(file_path+"bandpowerValue.xml"));
			motion_writer.write(eeg_header);
            Log.e("patyh",file_path+"");


			motion_writer.newLine();
		} catch (Exception e) {
			Log.e("","Exception"+ e.getMessage());
		}
	}
	private void StopWriteFile(){
		try {
            String eeg_ender = "</headset>";
            motion_writer.write(eeg_ender);
			motion_writer.flush();
			motion_writer.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	/**
	 * public void addEEGData(Double[][] eegs) Add EEG Data for write int the
	 * EEG File
	 *
	 * @param
	 *            - double array of eeg data
	 */
	public void addData(double data) {

		if (motion_writer == null) {
			return;
		}

		String input = "";
		input += (String.valueOf(data) + ",");
		try {

			motion_writer.write(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}