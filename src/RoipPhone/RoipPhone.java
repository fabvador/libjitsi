package RoipPhone;



import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import javax.media.PlugInManager;
import javax.media.control.PacketSizeControl;

import org.jitsi.impl.neomedia.AudioMediaStreamImpl;
import org.jitsi.impl.neomedia.MediaServiceImpl;
import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.AudioSystem.DataFlow;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.neomedia.DefaultStreamConnector;
import org.jitsi.service.neomedia.MediaDirection;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.neomedia.MediaStream;
import org.jitsi.service.neomedia.MediaStreamTarget;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.service.neomedia.MediaUseCase;
import org.jitsi.service.neomedia.codec.Constants;
import org.jitsi.service.neomedia.format.MediaFormat;

import RoipPhone.impl.neomedia.codec.audio.ulaw.Packetizer;
import RoipPhone.impl.neomedia.transform.ABCD.AbcdTransformEngine;
import RoipPhone.service.neomedia.ABCDRtpSignal;
import RoipPhone.service.neomedia.event.ABCDListener;
import RoipPhone.service.neomedia.event.ABCDSignalEvent;
import RoipPhone.service.protocol.ABCDSignal;









public class RoipPhone
{
	private MediaService mediaService				= null;
	private MediaFormat format						= null;
	private MediaStream mediaStream					= null;

	private char keyPressed							= 0;
	private boolean transmit						= false;
	private boolean stopApp							= false;

	private AbcdTransformEngine abcdTransformEngine	= null;

	
	
	
	private void initialize()
	{
		mediaService = LibJitsi.getMediaService();
		format = mediaService.getFormatFactory().createMediaFormat("PCMU", 8000);

		try
		{
			String newCodecName = Packetizer.class.getCanonicalName();
			String oldCodecName = org.jitsi.impl.neomedia.codec.audio.ulaw.Packetizer.class.getCanonicalName();
			Packetizer paketizer = new Packetizer(); 
			//PlugInManager.removePlugIn(oldCodecName, PlugInManager.CODEC);
			PlugInManager.addPlugIn(newCodecName, paketizer.getSupportedInputFormats(), paketizer.getSupportedOutputFormats(null), PlugInManager.CODEC);
	        
			@SuppressWarnings("unchecked")
			Vector<String> codecs = PlugInManager.getPlugInList(null, null, PlugInManager.CODEC);
			if (codecs != null)
			{
				boolean setPlugInList = false;
				int newClassNameIndex = codecs.indexOf(newCodecName);
				int oldClassNameIndex = codecs.indexOf(oldCodecName);
				if ((newClassNameIndex != -1) && (oldClassNameIndex != -1))
				{
					codecs.set(oldClassNameIndex, newCodecName);
					codecs.remove(newClassNameIndex);
					setPlugInList = true;
				}
				if (setPlugInList)
				{
					PlugInManager.setPlugInList(codecs, PlugInManager.CODEC);
				}
			}
			PlugInManager.commit();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	
	
	
	
	private void configure() throws Exception
	{	
		mediaStream = prepareRoipStream(6666, "10.0.5.20", 5004);
	}
	private MediaStream prepareRoipStream(int localPort, String remoteIP, int remotePort) throws Exception
	{
		//=====================
		// set the audioSystem
		//AudioSystem[] list = ((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().getAvailableAudioSystems();
		//((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().setAudioSystem(list[0], true);
		
		//=====================
		// creates the stream
		MediaStream mediaStream = mediaService.createMediaStream(MediaType.AUDIO);

		//=====================
		// connector
		DefaultStreamConnector connector = new DefaultStreamConnector(
				new DatagramSocket(localPort),
				new DatagramSocket(localPort + 1));
		mediaStream.setConnector(connector);
		
		//=====================
		// set the device
		mediaStream.setDevice(mediaService.getDefaultDevice(MediaType.AUDIO, MediaUseCase.CALL));
		
		//=====================
		// direction
		mediaStream.setDirection(MediaDirection.SENDRECV);
		
		//=====================
		// format
		mediaStream.setFormat(format);

		//=====================
		// custom RTP payload
		mediaStream.addDynamicRTPPayloadType((byte) 97, mediaService.getFormatFactory().createMediaFormat(Constants.TELEPHONE_EVENT, 8000));
		if (mediaStream instanceof AudioMediaStreamImpl)
		{
			abcdTransformEngine = new AbcdTransformEngine((AudioMediaStreamImpl) mediaStream);
			abcdTransformEngine.addAbcdListener(new ABCDListener()
			{
				@Override
				public void abcdSignalReception(ABCDSignalEvent event) 
				{
					System.out.print("abcdSignalReception");
					sendPttCommand(transmit);
				}
			});
			mediaStream.setExternalTransformer(abcdTransformEngine);
		}
		
		//=====================
		// target
		MediaStreamTarget target = new MediaStreamTarget(
				new InetSocketAddress(remoteIP, remotePort),
				new InetSocketAddress(remoteIP, remotePort + 1)); 
		mediaStream.setTarget(target);		

		
		return mediaStream;
	}
	private void start()
	{
		if (mediaStream != null)
		{
			mediaStream.start();

			//=====================
			// set the packet size
			Set<PacketSizeControl> packetControlList = ((AudioMediaStreamImpl) mediaStream).getDeviceSession().getEncoderControls(PacketSizeControl.class);
			Iterator<PacketSizeControl> iterator = packetControlList.iterator();
			while (iterator.hasNext())
			{
				PacketSizeControl control = iterator.next();
				control.setPacketSize(320);
			}


			new Thread(new Runnable()
			{
				@Override
				public void run() 
				{
					Scanner scanner = new Scanner(System.in);
					while ((keyPressed != 'x') && (keyPressed != 'q'))
					{
						String line = scanner.nextLine().trim(); 
						keyPressed = line.charAt(0);
						
						if (keyPressed == 't')
						{
							sendPttCommand(true);
							keyPressed = 0;
						}
						else if (keyPressed == 's')
						{
							sendPttCommand(false);
							keyPressed = 0;
						}
						else if (keyPressed == 'p')
						{
							boolean processed = false;
							if (line.length() > 1)
							{
								try
								{
									int i = Integer.parseInt(line.substring(1));
									AudioSystem audioSystem = ((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().getAudioSystem();
									CaptureDeviceInfo2 device = audioSystem.getDevices(DataFlow.PLAYBACK).get(i);
									audioSystem.setDevice(DataFlow.PLAYBACK, device, true);
									processed = true;
								}
								catch (Exception e)
								{
									e.printStackTrace();
								}
							}
							if (!processed)
							{
								AudioSystem audioSystem = ((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().getAudioSystem();
								CaptureDeviceInfo2 selected = audioSystem.getSelectedDevice(DataFlow.PLAYBACK);
								List<CaptureDeviceInfo2> list = audioSystem.getDevices(DataFlow.PLAYBACK);
								for (int i = 0; i < list.size(); i++)
								{
									System.err.println(i + (selected == list.get(i) ? "*\t" : "\t") + list.get(i).getName() + "," + list.get(i).getLocator());
								}		
							}
						}
					}
					stopApp = true;
				}	
			}).start();
		}
	}
	private void stop()
	{
		if (mediaStream != null)
		{
			mediaStream.stop();
			mediaStream.close();
		}
	}
	private void sendPttCommand(boolean activate)
	{
		transmit = activate;
		mediaStream.setMute(!transmit);
		if (abcdTransformEngine != null)
		{
			abcdTransformEngine.sendSignal(ABCDRtpSignal.mapSignal(transmit ? ABCDSignal.ABCD_8 : ABCDSignal.ABCD_0));
		}
	}






	public static void main(String[] args) throws Exception
	{
		LibJitsi.start();
		try
		{
			RoipPhone phone = new RoipPhone();
			phone.initialize();

			phone.configure();
			phone.start();
			while (!phone.stopApp)
			{
				Sleep(1000);
			}
			phone.stop();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		LibJitsi.stop();

		System.exit(0);
	}
	private static void Sleep(int delay) 
	{
		try
		{
			System.err.print(".");
			Thread.sleep(delay);
		}
		catch (InterruptedException ie)
		{
		}
	}
}
