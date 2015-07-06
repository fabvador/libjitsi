package RoipPhone;



import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
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
import org.jitsi.service.neomedia.codec.Constants;
import org.jitsi.service.neomedia.device.MediaDevice;
import org.jitsi.service.neomedia.format.MediaFormat;

import com.bethecoder.ascii_table.ASCIITable;
import com.bethecoder.ascii_table.ASCIITableHeader;
import com.bethecoder.ascii_table.impl.CollectionASCIITableAware;
import com.bethecoder.ascii_table.spec.IASCIITable;
import com.bethecoder.ascii_table.spec.IASCIITableAware;

import RoipPhone.impl.neomedia.codec.audio.ulaw.Packetizer;
import RoipPhone.impl.neomedia.codec.audio.ulaw.Packetizer.MuteControl;
import RoipPhone.impl.neomedia.device.RoipPhoneMediaDevice;
import RoipPhone.impl.neomedia.transform.ABCD.AbcdTransformEngine;
import RoipPhone.service.neomedia.ABCDRtpSignal;
import RoipPhone.service.neomedia.event.ABCDListener;
import RoipPhone.service.neomedia.event.ABCDSignalEvent;
import RoipPhone.service.protocol.ABCDSignal;









public class RoipPhone
{
	private MediaService mediaService				= null;
	private MediaFormat format						= null;

	private Vector<AudioLine> audioLineList			= new Vector<AudioLine>();
	private Controller controller					= null;
	
	
	
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
		audioLineList.add(new AudioLine(6666, "10.0.5.20", 5004));
		//audioLineList.add(new AudioLine(6668, "10.0.5.20", 5006));
	}
	private void stop()
	{
		for (AudioLine line : audioLineList)
		{
			line.stop();
		}		
	}
	private void start()
	{
		for (AudioLine line : audioLineList)
		{
			line.start();
		}


		controller = new Controller();
		controller.start();
	}


	
	
	
	private class AudioLine
	{
		private MediaStream mediaStream					= null;
		private boolean pttActive						= false;
		private AbcdTransformEngine abcdTransformEngine	= null;

		//=====================
		// set the audio payload size
		//--------
		// 160 - 20ms of audio @8000 Samples/seconds & 1 byte/sample (like PCMU/PCMA)
		// 320 - 40ms of audio @8000 Samples/seconds & 1 byte/sample (like PCMU/PCMA)
		private int currentRtpAudioPayloadSize			= 320;
	
		private String remoteAddress					= null;
		
		
		private AudioLine(int localPort, String remoteIP, int remotePort) throws Exception
		{
			remoteAddress = remoteIP + ":" + remotePort;

			//=====================
			// set the audioSystem
			//AudioSystem[] list = ((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().getAvailableAudioSystems();
			//((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().setAudioSystem(list[0], true);
			
			//=====================
			// creates the stream
			mediaStream = mediaService.createMediaStream(MediaType.AUDIO);

			//=====================
			// connector
			mediaStream.setConnector(new DefaultStreamConnector(new DatagramSocket(localPort), new DatagramSocket(localPort + 1)));
			
			//=====================
			// direction
			mediaStream.setDirection(MediaDirection.SENDRECV);

			//=====================
			// set the device
			//------
			// This works for the audio input (microphone)
			AudioSystem audioSystem = ((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().getAudioSystem();
			mediaStream.setDevice(new RoipPhoneMediaDevice(
					audioSystem.getSelectedDevice(DataFlow.CAPTURE),
					audioSystem.getSelectedDevice(DataFlow.PLAYBACK)
					));
			
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
						System.out.println("\nabcdSignalReception from " + remoteAddress);
						sendPttCommand(pttActive);
					}
				});
				mediaStream.setExternalTransformer(abcdTransformEngine);
			}
			
			//=====================
			// target
			mediaStream.setTarget(new MediaStreamTarget(new InetSocketAddress(remoteIP, remotePort), new InetSocketAddress(remoteIP, remotePort + 1)));		
		}
		
		
		public void start() 
		{
			if (mediaStream != null)
			{
				mediaStream.start();
				
				setRtpAudioPayloadSize(currentRtpAudioPayloadSize);
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
		private void sendPttCommand(boolean pttActive)
		{
			this.pttActive = pttActive;
			mediaStream.setMute(!this.pttActive);
			updateMuteControls();
			if (abcdTransformEngine != null)
			{
				abcdTransformEngine.sendSignal(ABCDRtpSignal.mapSignal(this.pttActive ? ABCDSignal.ABCD_8 : ABCDSignal.ABCD_0));
			}
		}
		private void updateMuteControls()
		{
			if (mediaStream instanceof AudioMediaStreamImpl)
			{
				Set<MuteControl> muteControlList = ((AudioMediaStreamImpl) mediaStream).getDeviceSession().getEncoderControls(MuteControl.class);
				if (muteControlList != null)
				{
					Iterator<MuteControl> iterator = muteControlList.iterator();
					while (iterator.hasNext())
					{
						MuteControl control = iterator.next();
						control.setMute(!pttActive);
					}
				}
			}
		}
		private void setRtpAudioPayloadSize(int currentRtpAudioPayloadSize)
		{
			this.currentRtpAudioPayloadSize = currentRtpAudioPayloadSize;
			if (mediaStream instanceof AudioMediaStreamImpl)
			{
				Set<PacketSizeControl> packetControlList = ((AudioMediaStreamImpl) mediaStream).getDeviceSession().getEncoderControls(PacketSizeControl.class);
				if (packetControlList != null)
				{
					Iterator<PacketSizeControl> iterator = packetControlList.iterator();
					while (iterator.hasNext())
					{
						PacketSizeControl control = iterator.next();
						control.setPacketSize(this.currentRtpAudioPayloadSize);
					}
				}
			}
		}
		public void changeDevice(DataFlow flow, CaptureDeviceInfo captureDeviceInfo) 
		{
			MediaDevice device = mediaStream.getDevice();
			if (device instanceof RoipPhoneMediaDevice)
			{
				switch (flow)
				{
				case CAPTURE:
					mediaStream.setDevice(new RoipPhoneMediaDevice(captureDeviceInfo, ((RoipPhoneMediaDevice) device).playbackMediaDeviceInfo));
					setRtpAudioPayloadSize(currentRtpAudioPayloadSize);	
					break;
				case PLAYBACK:
					mediaStream.setDevice(new RoipPhoneMediaDevice(((RoipPhoneMediaDevice) device).getCaptureDeviceInfo(), captureDeviceInfo));
					setRtpAudioPayloadSize(currentRtpAudioPayloadSize);	
					break;
				}
			}
		}
	}
	
	
	
	
	
	private class Controller implements Runnable
	{
		private char keyPressed		= 0;
		private boolean stopApp		= false;

		private int selectedIndex	= 0;
		
		@Override
		public void run() 
		{
			Scanner scanner = new Scanner(System.in);
			while ((keyPressed != 'x') && (keyPressed != 'q'))
			{
				String line = scanner.nextLine().trim(); 
				keyPressed = line.length() > 0 ? line.charAt(0) : 0;
				
				if (keyPressed == 't')
				{
					audioLineList.get(selectedIndex).sendPttCommand(true);
					keyPressed = 0;
				}
				else if (keyPressed == 's')
				{
					audioLineList.get(selectedIndex).sendPttCommand(false);
					keyPressed = 0;
				}
				else if (keyPressed == 'i')
				{
					if (line.length() > 1)
					{
						try
						{
							int i = Integer.parseInt(line.substring(1));
							if ((i >= 0) && (i < audioLineList.size()))
							{
								selectedIndex = i;
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					System.out.println("\nCurrent Index:" + selectedIndex);
					keyPressed = 0;
				}
				else if (keyPressed == 'r')
				{
					if (line.length() > 1)
					{
						try
						{
							int i = Integer.parseInt(line.substring(1));
							audioLineList.get(selectedIndex).setRtpAudioPayloadSize(160 * i);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					else
					{
						System.out.println("Current RTP Audio Payload:" + audioLineList.get(selectedIndex).currentRtpAudioPayloadSize);
					}
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
							audioLineList.get(selectedIndex).changeDevice(DataFlow.PLAYBACK, audioSystem.getDevices(DataFlow.PLAYBACK).get(i));
							processed = true;
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					if (!processed)
					{
						CaptureDeviceInfo selected = null;
						MediaDevice device = audioLineList.get(selectedIndex).mediaStream.getDevice();
						if (device instanceof RoipPhoneMediaDevice)
						{
							selected = ((RoipPhoneMediaDevice) device).playbackMediaDeviceInfo;
						}
						//---------
						// list the devices
						AudioSystem audioSystem = ((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().getAudioSystem();
						List<CaptureDeviceInfo2> list = audioSystem.getDevices(DataFlow.PLAYBACK);
						Vector<DeviceDesc> deviceList = new Vector<DeviceDesc>();
						if ((list != null) && (!list.isEmpty()))
						{
							for (int i = 0; i < list.size(); i++)
							{
								CaptureDeviceInfo2 cur = list.get(i);
								deviceList.add(new DeviceDesc(i, selected == cur ? "*" : "", cur.getName(), cur.getLocator().toString()));
							}
						}
						//---------
						// create the table
						String title = "PLAYBACK DEVICE";
						if (list.isEmpty())
						{
							ASCIITableHeader[] header = { new ASCIITableHeader(title, ASCIITable.ALIGN_CENTER) };
							String[][] data = { { "VOID" } };
							System.out.println(ASCIITable.getInstance().getTable(header, data));
						}
						else
						{
							IASCIITableAware asciiTable = new CollectionASCIITableAware<DeviceDesc>(deviceList, "index", "selected", "name", "locator");
							asciiTable.getHeaders().get(1).setDataAlign(IASCIITable.ALIGN_LEFT);
							asciiTable.getHeaders().get(2).setDataAlign(IASCIITable.ALIGN_LEFT);
							asciiTable.getHeaders().get(3).setDataAlign(IASCIITable.ALIGN_LEFT);
							String res = ASCIITable.getInstance().getTable(asciiTable);
							int length = res.indexOf("\n");
							System.out.println(createLine(length) + "|" + getFormattedData(length - 2, title) + "|\n" + res + "\n");
						}
					}
				}
				else if (keyPressed == 'c')
				{
					boolean processed = false;
					if (line.length() > 1)
					{
						try
						{
							int i = Integer.parseInt(line.substring(1));							
							AudioSystem audioSystem = ((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().getAudioSystem();
							audioLineList.get(selectedIndex).changeDevice(DataFlow.CAPTURE, audioSystem.getDevices(DataFlow.CAPTURE).get(i));									
							processed = true;
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					if (!processed)
					{
						CaptureDeviceInfo selected = null;
						MediaDevice device = audioLineList.get(selectedIndex).mediaStream.getDevice();
						if (device instanceof RoipPhoneMediaDevice)
						{
							selected = ((RoipPhoneMediaDevice) device).getCaptureDeviceInfo();
						}
						//---------
						// list the devices
						AudioSystem audioSystem = ((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().getAudioSystem();
						List<CaptureDeviceInfo2> list = audioSystem.getDevices(DataFlow.CAPTURE);
						Vector<DeviceDesc> deviceList = new Vector<DeviceDesc>();
						if ((list != null) && (!list.isEmpty()))
						{
							for (int i = 0; i < list.size(); i++)
							{
								CaptureDeviceInfo2 cur = list.get(i);
								deviceList.add(new DeviceDesc(i, selected == cur ? "*" : "", cur.getName(), cur.getLocator().toString()));
							}
						}
						//---------
						// create the table
						String title = "CATURE DEVICE";
						if (list.isEmpty())
						{
							ASCIITableHeader[] header = { new ASCIITableHeader(title, ASCIITable.ALIGN_CENTER) };
							String[][] data = { { "VOID" } };
							System.out.println(ASCIITable.getInstance().getTable(header, data));
						}
						else
						{
							IASCIITableAware asciiTable = new CollectionASCIITableAware<DeviceDesc>(deviceList, "index", "selected", "name", "locator");
							asciiTable.getHeaders().get(1).setDataAlign(IASCIITable.ALIGN_LEFT);
							asciiTable.getHeaders().get(2).setDataAlign(IASCIITable.ALIGN_LEFT);
							asciiTable.getHeaders().get(3).setDataAlign(IASCIITable.ALIGN_LEFT);
							String res = ASCIITable.getInstance().getTable(asciiTable);
							int length = res.indexOf("\n");
							System.out.println(createLine(length) + "|" + getFormattedData(length - 2, title) + "|\n" + res + "\n");
						}
					}
				}
			}
			stopApp = true;
		}

		public void start()
		{
			new Thread(this).start();
		}	
	}


	
	
	
	
	public static class DeviceDesc
	{
		private int index;
		private String selected;
		private String name;
		private String locator;

		public DeviceDesc(int index, String selected, String name, String locator) 
		{
			this.index = index;
			this.selected = selected;
			this.name = name;
			this.locator = locator;
		}
		public int getIndex()
		{
			return index;
		}
		public String getSelected()
		{
			return selected;
		}
		public String getName()
		{
			return name;
		}
		public String getLocator()
		{
			return locator;
		}
	}
	private static String createLine(int length)
	{
		String data = "+";
		while (data.length() < length - 1)
		{
			data += "-";
		}
		return data + "+\n";
	}
	private static String getFormattedData(int maxLength, String data)
	{
		if (data.length() > maxLength)
		{
			return data;
		}

		boolean toggle = true;
		while (data.length() < maxLength)
		{
			if (toggle)
			{
				data = " " + data;
				toggle = false;
			}
			else
			{
				data = data + " ";
				toggle = true;
			}
		}

		return data;
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
			while (!phone.controller.stopApp)
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
