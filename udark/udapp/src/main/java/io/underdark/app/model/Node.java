package io.underdark.app.model;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;

import org.slf4j.impl.StaticLoggerBinder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Random;

import io.underdark.Underdark;
import io.underdark.app.MainActivity;
import io.underdark.app.R;
import io.underdark.transport.Link;
import io.underdark.transport.Transport;
import io.underdark.transport.TransportKind;
import io.underdark.transport.TransportListener;
import io.underdark.util.nslogger.NSLogger;
import io.underdark.util.nslogger.NSLoggerAdapter;

public class Node implements TransportListener
{
	private boolean running;
	private MainActivity activity;
	private long nodeId;
	private Transport transport;

	private ArrayList<Link> links = new ArrayList<>();
	private int framesCount = 0;
	public HashMap<Long, Link> idToLink = new HashMap<Long, Link>();

	public Node(MainActivity activity)
	{
		this.activity = activity;

		do
		{
			nodeId = new Random().nextLong();
		} while (nodeId == 0);

		if(nodeId < 0)
			nodeId = -nodeId;

		configureLogging();

		EnumSet<TransportKind> kinds = EnumSet.of(TransportKind.BLUETOOTH, TransportKind.WIFI);
		//kinds = EnumSet.of(TransportKind.WIFI);
		//kinds = EnumSet.of(TransportKind.BLUETOOTH);

		this.transport = Underdark.configureTransport(
				234235,
				nodeId,
				this,
				null,
				activity.getApplicationContext(),
				kinds
		);
	}

	private void configureLogging()
	{
		NSLoggerAdapter adapter = (NSLoggerAdapter)
				StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger(Node.class.getName());
		adapter.logger = new NSLogger(activity.getApplicationContext());
		adapter.logger.connect("192.168.5.203", 50000);

		Underdark.configureLogging(true);
	}

	public void start()
	{
		if(running)
			return;

		running = true;
		transport.start();
	}

	public void stop()
	{
		if(!running)
			return;

		running = false;
		transport.stop();
	}

	public ArrayList<Link> getLinks()
	{
		return links;
	}

	public int getFramesCount()
	{
		return framesCount;
	}

	public void broadcastFrame(byte[] frameData)
	{
		if(links.isEmpty())
			return;

		++framesCount;
		activity.refreshFrames();

		for(Link link : links)
			link.sendFrame(frameData);
	}

	public void sendFrame(byte[] frameData, Link link){
		if(link ==null)return;
		++framesCount;
		activity.refreshFrames();
		link.sendFrame(frameData);
	}

	public void sendFrame(Link link, int protocolId, String str){

		String newStr = Integer.toString(protocolId) + str;
		byte[] data = newStr.getBytes();
		Log.v("----SENDING STRING P ID", " "+protocolId);
		Log.v("----SENDING STRING---", new String(data));
		sendFrame(data, link);
	}

	//region TransportListener
	@Override
	public void transportNeedsActivity(Transport transport, ActivityCallback callback)
	{
		callback.accept(activity);
	}

	@Override
	public void transportLinkConnected(Transport transport, Link link)
	{
		links.add(link);
		idToLink.put(link.getNodeId(),link);
		activity.refreshPeers();
		activity.refreshButtons();
	}

	@Override
	public void transportLinkDisconnected(Transport transport, Link link)
	{
		links.remove(link);
		idToLink.remove(link.getNodeId());
		activity.refreshPeers();
		activity.refreshButtons();

		if(links.isEmpty())
		{
			framesCount = 0;
			activity.refreshFrames();
		}
	}

	@Override
	public void transportLinkDidReceiveFrame(Transport transport, Link link, byte[] frameData)
	{
		++framesCount;
		Log.v("-----RECEIVED FRAME----",framesCount+"");
		activity.refreshFrames();
		Log.v("----FRAME DATA-------", link.getNodeId() + "  " + new String(frameData));
		activity.showText(link.getNodeId() + ":" + new String(frameData));
	}
	//endregion
} // Node
