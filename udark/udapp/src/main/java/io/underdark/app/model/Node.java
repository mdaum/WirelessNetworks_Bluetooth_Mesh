package io.underdark.app.model;

import android.util.Log;

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

import static impl.underdark.logging.Logger.log;

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

	public Node(MainActivity activity, long id)
	{
		this.activity = activity;

		do
		{
			nodeId = id;
			if(id==0) id = new Random().nextLong();
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

	public long getId(){
		return this.nodeId;
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

	//protocolId: 0 = send routingtable, 1 = del destination
	public void sendFrame(Link link, int protocolId, String str){ // this one is only used for protocols 0 and 1
		String newStr = Integer.toString(protocolId) + str;
        byte[] data = newStr.getBytes();
		Logger.info("SENDING TO " + link.getNodeId()+", "+"protocol: " + " "+protocolId);
		Logger.info("----DATA: ---: "+ new String(data) + "\n");
		sendFrame(data, link);
	}
    //protocolId: 2 = random message|writtenMessage
    public void sendFrame(Link link, long intendedSender, long intendedReciever, int protocolId, String str){ // only used for protocols > 1
        //protocol id | sender id | reciever id | payload       when in protocol > 1
        String newStr = Integer.toString(protocolId)
					+ "|" + intendedSender
					+ "|" + intendedReciever
					+ "|" + System.currentTimeMillis()
					+ "|" + str;
        byte[] data = newStr.getBytes();
        Logger.info("SENDING TO " + link.getNodeId()+", "+"protocol: " + " "+protocolId);
        String toLog;
        if(str.getBytes().length > 1000000) toLog = "A random byte-string of size "+ data.length + "\n";
        else toLog = new String(data)  + "\n";
        Logger.info("----DATA: ---: "+ toLog);
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
		//rt for 1
		//routingTable.put((long)2,new RoutingInfo(2,1));
		///routingTable.put((long)3,new RoutingInfo(2,2));
		//routingTable.put((long)4,new RoutingInfo(2,3));
		//routingTable.put((long)5,new RoutingInfo(2,4));

		//rt for 2
//		routingTable.put((long)1,new RoutingInfo(1,1));
//		routingTable.put((long)3,new RoutingInfo(3,1));
		//routingTable.put((long)4,new RoutingInfo(3,2));
		//routingTable.put((long)5,new RoutingInfo(3,3));

		//tr for 3
		routingTable.put((long)1,new RoutingInfo(2,2));
		routingTable.put((long)2,new RoutingInfo(2,1));
		routingTable.put((long)4,new RoutingInfo(4,1));
		routingTable.put((long)5,new RoutingInfo(4,2));

		//tr for 4
		/*routingTable.put((long)1,new RoutingInfo(3,3));
		routingTable.put((long)2,new RoutingInfo(3,2));
		routingTable.put((long)3,new RoutingInfo(3,1));
		routingTable.put((long)5,new RoutingInfo(5,1));*/

		//tr for 5
		/*routingTable.put((long)1,new RoutingInfo(4,4));
		routingTable.put((long)2,new RoutingInfo(4,3));
		routingTable.put((long)3,new RoutingInfo(4,2));
		routingTable.put((long)4,new RoutingInfo(4,1));*/

		activity.refreshPeers();
		activity.refreshButtons();
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
		Logger.info("-----RECEIVED FRAME----"+ " "+framesCount+"");
		activity.refreshFrames();


		//parse message type
		String message = new String(frameData);
		int mode = Integer.parseInt(message.substring(0, 1));
		Logger.info("RECIEVING FROM " + sender.getNodeId());
		Logger.info("CURENT MODE: "+ mode+"");
		String toLog;
		if(frameData.length > 1000000) toLog = "A random byte-string of size "+ frameData.length;
		else toLog = message;
		Logger.info("----DATA: ---: "+ toLog);
		switch(mode){

			case 0: //recieved a routing table
				String[] entries = message.substring(1).split(";");
				if(entries.length==0) entries[0] = message.substring(1);//if there is only one routing table entry
				boolean routingChange = false;
				for (String entry:
						entries) {
					Logger.info("entry is: "+entry);
					String[] drh = entry.split("\\|");//destination route hops
					Logger.info("DRH[0]: "+drh[0]);
					Logger.info("DRH[1]: "+drh[1]);
					Logger.info("DRH[2]: "+drh[2]);
					//don't process if destination is already in your routing table, or is you!
					long dest = Long.parseLong(drh[0]);
					if(routingTable.containsKey(dest))continue;
					if(dest == nodeId)continue;
					//otherwise add to your routing table the destination and mark sender as route to that destination
					routingTable.put(dest,new RoutingInfo(sender.getNodeId(),Integer.parseInt(drh[2])+1));
					Logger.info("added dest "+dest+" to routing table");
					Logger.info("it's mapped routingInfo is "+routingTable.get(dest));
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
			//actual message, could be random or typed
			case 2:
				String [] messagePieces = message.split("\\|");
				Long intendedSender = Long.parseLong(messagePieces[1]);
                Long intendedReciever = Long.parseLong(messagePieces[2]);
				Long ogTimestamp = Long.parseLong(messagePieces[3]);
                Long latency = System.currentTimeMillis() - ogTimestamp;

                if(this.nodeId == intendedReciever){
					String messageText = messagePieces[messagePieces.length-1];
					Logger.info("Message size is "+frameData.length);
                    if(frameData.length > 1000000) this.activity.showText(
                    				"latency = " + latency +
									",\nsender = " + intendedSender +
									",\nmessage = " + "A random string with byte size " +
									messageText.length() + " was recieved from "+intendedSender);

					else this.activity.showText("latency = " + latency +
                                                ",\nsender = " + intendedSender +
                                                ",\nmessage = " + messageText);

				}
				else{ //need to forward this message on instead of showing it
					Logger.info("we are not the intended reciever for this message.");
                    Link route = idToLink.get(routingTable.get(intendedReciever).getRouterDest());
					Logger.info("going to forward message on to "+route.getNodeId());
					this.activity.showText("going to forward message from "+ intendedSender + " intended for "+intendedReciever + "\nforwarding to "+route.getNodeId());
                    route.sendFrame(frameData); //forward the message on....don't need to reprocess info
                }
				break;
			case 3:
				messagePieces = message.split("\\|");
				intendedSender = Long.parseLong(messagePieces[1]);
				intendedReciever = Long.parseLong(messagePieces[2]);
				ogTimestamp = Long.parseLong(messagePieces[3]);
				latency = System.currentTimeMillis() - ogTimestamp;
				if(this.nodeId == intendedReciever){
					String messageText = messagePieces[messagePieces.length-1];
					Logger.info("Message size is "+frameData.length);
					this.activity.showText(
							"latency = " + latency +
							",\nsender = " + intendedSender +
							",\nmessage = " + "A picture was sent to you, length " +
							messageText.length());
					this.activity.setEncodedImageToImageView(messageText);

				}
				else{ //need to forward this message on instead of showing it
					Logger.info("we are not the intended reciever for this message.");
					Link route = idToLink.get(routingTable.get(intendedReciever).getRouterDest());
					Logger.info("going to forward message on to "+route.getNodeId());
					this.activity.showText("going to forward message from "+ intendedSender + " intended for "+intendedReciever + "\nforwarding to "+route.getNodeId());
					route.sendFrame(frameData); //forward the message on....don't need to reprocess info
				}
				break;

			default: activity.showToast("invalid frame recieved");
		}
		//finally refresh your buttons and peers
		activity.refreshPeers();
		activity.refreshButtons();
	}

	//endregion
} // Node
