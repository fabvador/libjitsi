package RoipPhone;



import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import javax.media.PlugInManager;

import org.jitsi.impl.neomedia.AudioMediaStreamImpl;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.neomedia.DefaultStreamConnector;
import org.jitsi.service.neomedia.MediaDirection;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.neomedia.MediaStream;
import org.jitsi.service.neomedia.MediaStreamTarget;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.service.neomedia.MediaUseCase;
import org.jitsi.service.neomedia.RTPExtension;
import org.jitsi.service.neomedia.codec.Constants;
import org.jitsi.service.neomedia.device.MediaDevice;
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

		//=====================
		// list All
		List<MediaDevice> list = mediaService.getDevices(MediaType.AUDIO, MediaUseCase.CALL);
		for (int i = 0; i < list.size(); i++)
		{
			System.out.println("=================\nINDEX:" + i + " ===> " + list.get(i).toString());
			System.out.println("\t" + list.get(i).getDirection());
			System.out.println("\t--");
			for (MediaFormat format : list.get(i).getSupportedFormats())
			{
				System.out.println("\t" + format.toString());
			}
			System.out.println("\t--");
			for (RTPExtension format : list.get(i).getSupportedExtensions())
			{
				System.out.println("\t" + format.toString());
			}
			System.out.println("==");
		}
		
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
	private void configure() throws Exception
	{	
		//=====================
		// device
		MediaDevice device = mediaService.getDevices(MediaType.AUDIO, MediaUseCase.CALL).get(1);
		System.out.println("=================\nSelected Device: " + device.toString());
		
		mediaStream = prepareRoipStream(6666, "10.0.5.20", 5004, device);
	}
	private MediaStream prepareRoipStream(int localPort, String remoteIP, int remotePort, MediaDevice device) throws Exception
	{
		//=====================
		// connector
		DefaultStreamConnector connector = new DefaultStreamConnector(
				new DatagramSocket(localPort),
				new DatagramSocket(localPort + 1));
		MediaStream mediaStream = mediaService.createMediaStream(connector, device);

		//=====================
		// direction
		mediaStream.setDirection(MediaDirection.SENDRECV);

		//=====================
		// format
		mediaStream.setFormat(format);

		//=====================
		// target
		mediaStream.addDynamicRTPPayloadType((byte) 97, mediaService.getFormatFactory().createMediaFormat(Constants.TELEPHONE_EVENT, 8000));
		if (mediaStream instanceof AudioMediaStreamImpl)
		{
			abcdTransformEngine = new AbcdTransformEngine((AudioMediaStreamImpl) mediaStream);
			abcdTransformEngine.addAbcdListener(new ABCDListener()
			{
				@Override
				public void abcdSignalReception(ABCDSignalEvent event) 
				{
					System.out.println("abcdSignalReception");
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
			
			new Thread(new Runnable()
			{
				@Override
				public void run() 
				{
					Scanner scanner = new Scanner(System.in);
					while ((keyPressed != 'x') && (keyPressed != 'q'))
					{
						keyPressed = scanner.nextLine().charAt(0);
						
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
			System.err.println("Sleep...");
			Thread.sleep(delay);
		}
		catch (InterruptedException ie)
		{
		}
	}
}
