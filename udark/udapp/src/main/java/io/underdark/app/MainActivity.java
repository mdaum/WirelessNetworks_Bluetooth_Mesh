package io.underdark.app;

import android.app.ActionBar;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import io.underdark.app.log.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.underdark.app.model.Node;
import io.underdark.transport.Link;


public class MainActivity extends AppCompatActivity
{
	private TextView peersTextView;
	private TextView framesTextView;
	private int frameSize = 1028;
	List<Button> buttons;
	Node node;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Logger.init();
		peersTextView = (TextView) findViewById(R.id.peersTextView);
		framesTextView = (TextView) findViewById(R.id.framesTextView);
		node = new Node(this);
		buttons = new ArrayList<Button>();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		node.start();
		Logger.info("STARTING\n");
	}

	protected View.OnClickListener MemberClicked = new View.OnClickListener(){
		public void onClick(View v){ //send a frame to node represented by button presses
			Button b = (Button) v;
			long id = Long.parseLong((String)b.getText());

			/*Link l = node.idToLink.get(id);

			TextView message = (TextView)findViewById(R.id.message);
			//if it's empty or it just has the default text dont send anything
			if(message.getText().toString().equals("Message") || message.getText().toString().equals("")){
				showToast("Please enter a message to send");
			}else {
				String strData = message.getText().toString();
				node.sendFrame(l, 1, strData);
			}*/

			/*Link l = node.idToLink.get(node.routingTable.get(id).getRouterDest());//get routing dest of desired id and grab that link
			byte[] frameData = new byte[100000];
			new Random().nextBytes(frameData);
			byte[]toSend = ("2"+new String(frameData)).getBytes();
			node.sendFrame(toSend,l);*/


			long myId = node.getId();

			Link l = node.idToLink.get(node.routingTable.get(id).getRouterDest());//get routing dest of desired id and grab that link

			TextView message = (TextView)findViewById(R.id.messageOrBytes);
			Switch randomBytes = (Switch) findViewById(R.id.randomBytes);
			//if the randombytes switch is activated send a sequence of random bytes
			if(randomBytes.isChecked()){
				if(message.getText().toString().equals("Message") || message.getText().toString().equals("")){
					showToast("Please enter an amount of bytes to send");
				}else {
					int length = Integer.parseInt(message.getText().toString());
					byte[] frameData = new byte[length];
					new Random().nextBytes(frameData);
					node.sendFrame(l, 2, new String(frameData));
				}
				//otherwise send a message
			}else{
				if(message.getText().toString().equals("Message") || message.getText().toString().equals("")){
					showToast("Please enter a message to send");
				}else {
					String strData = message.getText().toString();
					node.sendFrame(l, 2, strData);
				}
			}

			//2 = random message, 1 = actual message
		}
	};

	@Override
	protected void onStop()
	{
		super.onStop();

		if(node != null)
			node.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings)
		{
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private static boolean started = false;

	public void refreshPeers()
	{
		peersTextView.setText(node.routingTable.size() + " connected");
	}

	public void refreshFrames()
	{
		framesTextView.setText(node.getFramesCount() + " frames");
	}

	public void showToast(String message){
		Toast.makeText(this, message,
				Toast.LENGTH_SHORT).show();
	}

	public void showText(String text){
		LinearLayout layout = (LinearLayout)findViewById(R.id.ll);
		TextView textView = (TextView)findViewById(R.id.textMessage);
		textView.setText("");
		textView.setText(text);
	}


	public void refreshButtons(){
		LinearLayout layout = (LinearLayout)findViewById(R.id.ll);
		for (Button b: buttons) {
			layout.removeView(b);
		}
		buttons.clear();
		for (long id : node.routingTable.keySet()) {
			Button toAdd = new Button(this);
			toAdd.setText(""+id);
			if(id != node.routingTable.get(id).getRouterDest()){
				toAdd.setBackgroundColor(Color.BLUE);
			}
			toAdd.setOnClickListener(MemberClicked);
			buttons.add(toAdd);
			layout.addView(toAdd,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));
		}
	}

} // MainActivity