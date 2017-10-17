package io.underdark.app.model;

import android.content.Context;

import org.slf4j.impl.StaticLoggerBinder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Random;

import io.underdark.Underdark;
import io.underdark.app.MainActivity;
import io.underdark.app.log.Logger;
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
	public HashMap<Long,RoutingInfo> routingTable = new HashMap<Long,RoutingInfo>();

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

	//protocolId: 0 = send routingtable, 1 = del destination, 2 = random message
	public void sendFrame(Link link, int protocolId, String str){
		String newStr = Integer.toString(protocolId) + str;
		byte[] data = newStr.getBytes();
		Logger.info("SENDING TO " + link.getNodeId()+", "+"PID: " + " "+protocolId);
		Logger.info("----DATA: ---: "+ new String(data) + "\n");
		sendFrame(data, link);
	}

	public String encodeRoutingTable(){
		String toRet = "";
		for (Long id:routingTable.keySet()) {
			toRet += id +"|";
			toRet += routingTable.get(id).getRouterDest() + "|";
			toRet += routingTable.get(id).getStep()+";";
		}
		return toRet.substring(0,toRet.length()-1); //chop off last ';'
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
		routingTable.put(link.getNodeId(),new RoutingInfo(link.getNodeId(),1));
		activity.refreshPeers();
		activity.refreshButtons();
		String rt = encodeRoutingTable();
		//now send encoded routing table to each adjacent node
		for (Link l:
			 links) {
			sendFrame(l,0,rt);
		}
	}

	@Override
	public void transportLinkDisconnected(Transport transport, Link link)
	{
		links.remove(link);
		routingTable.remove(link.getNodeId());
		idToLink.remove(link.getNodeId());
		activity.refreshPeers();
		activity.refreshButtons();

		if(links.isEmpty())
		{
			framesCount = 0;
			activity.refreshFrames();
		}
		//now send the deletion event to all adjacent nodes
		for (Link l:
			 links) {
			sendFrame(l,1,""+link.getNodeId());
		}
	}
	@Override
	public void transportLinkDidReceiveFrame(Transport transport, Link sender, byte[] frameData)
	{
		++framesCount;
		activity.refreshFrames();
		//parse message type
		String message = new String(frameData);
        int mode = Integer.parseInt(message.substring(0, 1));
        Logger.info("RECIEVING FROM " + sender.getNodeId()+", "+"PID: " + " "+mode);
		Logger.info("----DATA----:" + message + "\n");
		switch(mode){
			case 0: //recieved a routing table
				String[] entries = message.substring(1).split(";");
				if(entries.length==0) entries[0] = message.substring(1);//if there is only one routing table entry
				boolean routingChange = false;
				for (String entry:
					 entries) {
					Logger.info("entry is: "+entry);
					String[] drh = entry.split("\\|");//destination route hops
					Logger.info("entry[0]: "+drh[0]);
					//don't process if destination is already in your routing table, or is you!
					if(routingTable.containsKey(Long.parseLong(drh[0])))continue;
					if(Long.parseLong(drh[0]) == nodeId)continue;
					//otherwise add to your routing table the destination and mark sender as route to that destination
					routingTable.put(Long.parseLong(drh[0]),new RoutingInfo(sender.getNodeId(),Integer.parseInt(drh[2])+1));
					Logger.info("added dest "+Long.parseLong(drh[0])+" to routing table");
					routingChange = true;
				}
				//if there was a routing change made, you must send your routing change to all links except for sender
				if(routingChange) {
					String toSend = encodeRoutingTable();
					for (Link l :
							links) {
						if (l.getNodeId() == sender.getNodeId()) continue; // do not to ping sender back...superflous
						sendFrame(l, 0,toSend);
					}
				}
				break;
			case 1: //node left the mesh notification (outside of network)
				Long toKill = Long.parseLong(message.substring(1));
				if(!routingTable.containsKey(toKill))break;
				Logger.info("Will delete node " + toKill + " from routing table");
				routingTable.remove(toKill);
				for (Link l:
					 links) {
					if(l.getNodeId() == sender.getNodeId()) continue;
					sendFrame(l,1,""+toKill);
				}
				break;
			case 2:
				break; //nothing for now....this is just random data
			default: activity.showToast("invalid frame recieved");
		}

	}

	//endregion
} // Node
