package com.lb.easy_account;

import java.util.ArrayList;

import com.lb.custom_view.CustomToast;
import com.lb.custom_view.FloatLabeledEditText;
import com.lb.utils.Accounts;
import com.lb.utils.LocalUtils;
import com.lb.utils.Members;
import com.lb.utils.SQLiteHelper;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class KeepAccountFragment extends Fragment {
	private final String TAG = "KeepAccountFragment  ";
	
	private Activity context;
	private SQLiteHelper sqLiteHelper;

	private FloatLabeledEditText fletPay;
	private FloatLabeledEditText fletPayer;
	private FloatLabeledEditText fletConsumer;
	private TextView tvAverage;
	private Button btnKeep;

	private String rmb = "￥";

	private String[] consumers;
	private boolean[] selectorOfCustomers;
	ArrayList<Members> members;

	private View fragmentPageView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		System.out.println(TAG + "onCreateView");
		
		fragmentPageView = inflater.inflate(R.layout.fg_keep_accout, null);
		return fragmentPageView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		System.out.println(TAG + "onActivityCreated");
		
		fletPay = (FloatLabeledEditText) findViewById(R.id.payMoney);
		fletPayer = (FloatLabeledEditText) findViewById(R.id.payer);
		fletConsumer = (FloatLabeledEditText) findViewById(R.id.consumers);
		tvAverage = (TextView) findViewById(R.id.average);
		btnKeep = (Button) findViewById(R.id.keep);
		
		context = getActivity();
		sqLiteHelper = SQLiteHelper.getInstance();
		
		initDate();
	}

	private void initDate() {

		SQLiteHelper SQLiteUtils = SQLiteHelper.getInstance();
		 members = SQLiteUtils.getMembers(context);
		if (members.size() < 1) {

		}

		int loop = members.size();
		consumers = new String[loop];
		selectorOfCustomers = new boolean[loop];

		for (int i = 0; i < loop; i++) {
			consumers[i] = members.get(i).memberName;
			selectorOfCustomers[i] = false;
		}
		
		initClick();
	}

	private void initClick() {

		fletPayer.setOnClick(context, new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				fletPayer.getFocus();
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				// builder.setMultiChoiceItems(_items, _selection, this);
				builder.setSingleChoiceItems(consumers, -1,
						new AlertDialog.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub
								fletPayer.setText(consumers[which]);
								dialog.dismiss();
								changeSelectorArr(consumers[which]);
								showAverage(fletConsumer.getTextString(), fletPay.getTextString());
							}
						});
				builder.show();
			}
		});

		fletConsumer.setOnClick(context, new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				fletConsumer.getFocus();
				AlertDialog.Builder builder = new AlertDialog.Builder(context);

				builder.setMultiChoiceItems(consumers, selectorOfCustomers,
						new AlertDialog.OnMultiChoiceClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which, boolean isChecked) {
								// TODO Auto-generated method stub
								selectorOfCustomers[which] = isChecked;
								fletConsumer.setText(buildSelectedItemString());
								dialog.dismiss();
	
								showAverage(fletConsumer.getTextString(), fletPay.getTextString());
							}
						});

				builder.show();
			}
		});
		
		btnKeep.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String payMoney, payPerson, consumerString;
				float money;
				
				payMoney = fletPay.getTextString();
				if(null == payMoney || payMoney.equals("")){				
					CustomToast.makeText(context, "我们到底花了多少?", 
							new CustomToast.Style(CustomToast.LENGTH_SHORT, R.color.alert)).show();
					return;
				}
				
				money = Float.valueOf(payMoney);
				if(money <= 0f){
					CustomToast.makeText(context, "我们到底花了多少?", 
							new CustomToast.Style(CustomToast.LENGTH_SHORT, R.color.alert)).show();
					return;
				}
				
				payPerson = fletPayer.getTextString();
				if(null == payPerson || payPerson.equals("")){				
					CustomToast.makeText(context, "是谁付的钱?", 
							new CustomToast.Style(CustomToast.LENGTH_SHORT, R.color.alert)).show();
					return;
				}
				
				consumerString = fletConsumer.getTextString();
				if(null == consumers || consumers.equals("")){				
					CustomToast.makeText(context, "有哪些人消费了的?", 
							new CustomToast.Style(CustomToast.LENGTH_SHORT, R.color.alert)).show();
					return;
				}
				
				String []isConsumers = reductionCustomers(consumerString);
				
				if(insertAccountToDB(money, payPerson, isConsumers)){
					CustomToast.makeText(context, "账单存储完毕!", 
							new CustomToast.Style(CustomToast.LENGTH_SHORT, R.color.info)).show();
				}else{
					CustomToast.makeText(context, "是不是哪儿出错了,检查一下看看?", 
							new CustomToast.Style(CustomToast.LENGTH_SHORT, R.color.alert)).show();
				}
			}
		});
		
		tvAverage.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					LocalUtils.hideSoftware(context);
				}
				return false;
			}
		});	
	}

	private void showAverage(String consumerString, String pay){
		if(null == consumerString || consumerString.equals("") || null == pay || pay.equals("")){
			return;
		}
		
		String []consumerArr = reductionCustomers(consumerString);
		int num = consumerArr.length;
		float money = Float.valueOf(pay);
		
		if(num < 1 || money <= 0f){
			return;
		}
		
		float temp = money/((float)num);
		float average = LocalUtils.formatFloatAccuracy(temp, 2);

		tvAverage.setText("人均消费:" + average + rmb);
	}
	
	private String buildSelectedItemString() {
		StringBuilder sb = new StringBuilder();
		boolean foundOne = false;

		for (int i = 0; i < consumers.length; ++i) {
			if (selectorOfCustomers[i]) {
				if (foundOne) {
					sb.append(", ");
				}
				foundOne = true;

				sb.append(consumers[i]);
			}
		}

		return sb.toString();
	}

	private String[] reductionCustomers(String consumersString){
		String str = consumersString.replaceAll(" ", "");	
		return str.split(",");
	}
	
	private void changeSelectorArr(String consumerName){
		int id = getConsumerArrayId(consumerName);
		for(int i = 0; i < selectorOfCustomers.length; i++){
			selectorOfCustomers[i] = false;
		}
		selectorOfCustomers[id] = true;
	}
	
	private int getConsumerArrayId(String consumerName){
		int i;
		
		for(i = 0; i < consumers.length;i++){
			if(consumers[i] == consumerName){
				break;
			}
		}
		
		return i;
	}
	
	private boolean insertAccountToDB(float pay, String payer, String []consumers){
		
		float temp = pay/((float)consumers.length);
		float average = LocalUtils.formatFloatAccuracy(temp, 2);
		
		int money = (int) (average*100);
		
		ArrayList<Members> membersToInsert = new ArrayList<Members>();
		
		for(int i = 0; i < consumers.length; i++){
			for(int j = 0; j < members.size(); j++){
				if(members.get(j).memberName.equals(consumers[i])){
					membersToInsert.add(members.get(j));
					break;
				}
			}
		}
		
		if(membersToInsert.size() < 1){
			return false;
		}

		Accounts[] accounts = Accounts.warpper(money, payer, membersToInsert);
		{
			if(null == accounts){
				return false;
			}
		}
		
		sqLiteHelper.insertAccount(context, accounts);

		return true;
	}
	
	private View findViewById(int id) {
		return fragmentPageView.findViewById(id);
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		System.out.println(TAG + "onPause");
		
		fletPay.clearFocus();
		fletPay.setText("");
		fletPayer.clearFocus();
		fletPayer.setText("");
		fletConsumer.clearFocus();
		fletConsumer.setText("");

		super.onPause();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);System.out.println(TAG + "onCreate");
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();System.out.println(TAG + "onDestroy");
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();System.out.println(TAG + "onDestroyView");
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();System.out.println(TAG + "onResume");
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();System.out.println(TAG + "onStart");
	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();System.out.println(TAG + "onStop");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		System.out.println(TAG + "onSaveInstanceState");
	}

	@Override
	public void setInitialSavedState(SavedState state) {
		// TODO Auto-generated method stub
		System.out.println(TAG + "setInitialSavedState");
	}
}
