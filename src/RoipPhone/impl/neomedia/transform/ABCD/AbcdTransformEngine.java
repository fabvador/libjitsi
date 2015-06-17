package RoipPhone.impl.neomedia.transform.ABCD;


import java.util.Arrays;
import java.util.Vector;

import net.sf.fmj.media.rtp.RTPHeader;

import org.jitsi.impl.neomedia.AudioMediaStreamImpl;
import org.jitsi.impl.neomedia.RawPacket;
import org.jitsi.impl.neomedia.transform.PacketTransformer;
import org.jitsi.impl.neomedia.transform.TransformEngine;
import org.jitsi.service.neomedia.codec.Constants;

import RoipPhone.service.neomedia.ABCDRtpSignal;
import RoipPhone.service.neomedia.event.ABCDListener;
import RoipPhone.service.neomedia.event.ABCDSignalEvent;



public class AbcdTransformEngine implements PacketTransformer, TransformEngine
{
	/**
	 * The <tt>AudioMediaStreamImpl</tt> that this transform engine was created
	 * by and that it's going to deliver ABCD packets for.
	 */
	private final AudioMediaStreamImpl mediaStream;


	/**
	 * Array of all supported tones.
	 */
	private static final ABCDRtpSignal[] supportedTones = new ABCDRtpSignal[] {
		ABCDRtpSignal.ABCD_0, ABCDRtpSignal.ABCD_1, ABCDRtpSignal.ABCD_2, ABCDRtpSignal.ABCD_3,
		ABCDRtpSignal.ABCD_4, ABCDRtpSignal.ABCD_5, ABCDRtpSignal.ABCD_6, ABCDRtpSignal.ABCD_7,
		ABCDRtpSignal.ABCD_8, ABCDRtpSignal.ABCD_9, ABCDRtpSignal.ABCD_A, ABCDRtpSignal.ABCD_B,
		ABCDRtpSignal.ABCD_C, ABCDRtpSignal.ABCD_D, ABCDRtpSignal.ABCD_E, ABCDRtpSignal.ABCD_F };

	/**
	 * The dispatcher that is delivering tones to the media steam.
	 */
	private ABCDDispatcher abcdDispatcher = null;


	/**
	 * The list of signals that we are supposed to be currently transmitting.
	 */
	private Vector<ABCDRtpSignal> signalList = new Vector<ABCDRtpSignal>();

	/**
	 * A mutex used to control the start and the stop of a tone and thereby to
	 * control concurrent modification access to "currentTone",
	 * "nbToneToStop" and "toneTransmissionState".
	 */
	private Object startStopToneMutex = new Object();


	/**
	 * listener list
	 */
	private final Vector<ABCDListener> abcdListeners = new Vector<ABCDListener>();

	
	
	
	public AbcdTransformEngine(AudioMediaStreamImpl stream)
	{
		this.mediaStream = stream;
	}

	
	
	
	
	@Override
	public void close()
	{
	}
	@Override
	public PacketTransformer getRTCPTransformer()
	{
		return null;
	}
	@Override
	public PacketTransformer getRTPTransformer()
	{
		return this;
	}

	
	
	
	@Override
	public RawPacket[] reverseTransform(RawPacket[] pkt)
	{
		if ((pkt == null) || (pkt.length == 0) || (pkt[0].getVersion() != RTPHeader.VERSION))
		{
			return pkt;
		}

		byte currentAbcdPayload = mediaStream.getDynamicRTPPayloadType(Constants.TELEPHONE_EVENT);

		for (RawPacket packet : pkt)
		{
			Vector<RawPacket> ret = new Vector<RawPacket>();
			if (currentAbcdPayload == packet.getPayloadType())
			{
				AbcdRawPacket p = new AbcdRawPacket(packet);
				ret.add(p);
				
				if (abcdDispatcher == null)
				{
					abcdDispatcher = new ABCDDispatcher();
					new Thread(abcdDispatcher).start();
				}
				abcdDispatcher.addTonePacket(p);
			}			
			return ret.isEmpty() ? pkt : ret.toArray(new RawPacket[0]);
		}

		return pkt;
	}
	
	
	/*
	 * IETF-5244: chapter 2.4: ABCD Transitional Signalling for Digital Trunks
	 * -------------
	 * Since ABCD information is a state rather than a changing signal,
	 * implementations SHOULD use the following triple-redundancy mechanism,
	 * similar to the one specified in ITU-T Rec. I.366.2 [15], Annex L.  At
	 * the time of a transition, the same ABCD information is sent 3 times
	 * at an interval of 5 ms.  If another transition occurs during this
	 * time, then this continues.  After a period of no change, the ABCD
	 * information is sent every 5 seconds
	 * -------------
	 * That is not ideal. It should have different sequenceNumber
	 * But if I apply DtmfTranformEngine logic, then I have too much delay
	 * between consecutive packets.
	 */	
	@Override
	public RawPacket[] transform(RawPacket[] pkt)
	{
		if ((signalList.isEmpty()) || (pkt == null) || (pkt.length == 0) || (pkt[0].getVersion() != RTPHeader.VERSION))
		{
			return pkt;
		}

		byte currentDtmfPayload = mediaStream.getDynamicRTPPayloadType(Constants.TELEPHONE_EVENT);
		if ( currentDtmfPayload == -1 )
		{
			throw new IllegalStateException("Can't send ABCD Signals when no payload type has been negotiated for ABCD events.");
		}

		Vector<RawPacket> ret = new Vector<RawPacket>(Arrays.asList(pkt));
		RawPacket model = ret.remove(0);
		for (ABCDRtpSignal signal : signalList)
		{
			byte toneCode = signal.getCode();
	
			AbcdRawPacket abcdPkt = new AbcdRawPacket(
					model.getBuffer(),
					model.getOffset(),
					model.getLength(),
					currentDtmfPayload);
	
			abcdPkt.init(toneCode, true, abcdPkt.getTimestamp());
	
			ret.insertElementAt(abcdPkt, 0);
			ret.insertElementAt(abcdPkt, 0);
			ret.insertElementAt(abcdPkt, 0);
		}
		signalList.clear();
		
		return ret.toArray(new RawPacket[0]);
	}


	
	
	
	public void sendSignal(ABCDRtpSignal tone)
	{
		synchronized (startStopToneMutex)
		{
			signalList.add(tone);
		}
	}
	public void stop()
	{
		if (abcdDispatcher != null)
		{
			abcdDispatcher.stop();
		}
	}











	/**
	 * A simple thread that waits for new tones to be reported from incoming
	 * RTP packets and then delivers them to the <tt>AudioMediaStream</tt>
	 * associated with this engine. The reason we need to do this in a separate
	 * thread is of course the time sensitive nature of incoming RTP packets.
	 */
	private class ABCDDispatcher implements Runnable
	{
		/** Indicates whether this thread is supposed to be running */
		private boolean isRunning							= false;

		/** The tone that we last received from the reverseTransform thread*/
		private Vector<ABCDRtpSignal> lastReceivedToneList	= new Vector<ABCDRtpSignal>();


		public void run()
		{
			long lastFiredEvent = 0;
			ABCDRtpSignal lastSent = null;

			isRunning = true;
			while(isRunning)
			{
				synchronized(this)
				{
					if (lastReceivedToneList.isEmpty())
					{
						try
						{
							wait();
						}
						catch (InterruptedException ie)
						{
							// DO NOTHING
						}
					}
					else
					{
						long currentTime = System.currentTimeMillis();

						while (!lastReceivedToneList.isEmpty())
						{
							ABCDRtpSignal current = lastReceivedToneList.remove(0);

							// I should receive 3 signals consecutively. They should be considered as one transmission.
							// Wireshark recording provides 3 msg in 9micro-seconds
							if ((lastSent == null) || (current.getCode() != lastSent.getCode()) || (currentTime - lastFiredEvent > 500))
							{
								lastFiredEvent = currentTime;
								lastSent = current;
								
								if ((abcdListeners != null) && (mediaStream != null))
								{
									ABCDSignalEvent ev = new ABCDSignalEvent(mediaStream, current);
									for (ABCDListener listener : abcdListeners)
									{
										listener.abcdSignalReception(ev);
									}
								}
							}
						}
					}
				}
			}
		}

		/**
		 * A packet that we should convert to tone and deliver
		 * to our media stream and its listeners in a separate thread.
		 *
		 * @param p the packet we will convert and deliver.
		 */
		public void addTonePacket(AbcdRawPacket p)
		{
			synchronized(this)
			{
				ABCDRtpSignal signal = getToneFromPacket(p);
				if (signal != null)
				{
					lastReceivedToneList.add(signal); 
				}

				notifyAll();
			}
		}

		/**
		 * Causes our run method to exit so that this thread would stop
		 * handling levels.
		 */
		public void stop()
		{
			synchronized(this)
			{
				this.lastReceivedToneList = null;
				isRunning = false;

				notifyAll();
			}
		}

		/**
		 * Maps DTMF packet codes to our ABCDRtpSignal objects.
		 * @param p the packet
		 * @return the corresponding tone.
		 */
		private ABCDRtpSignal getToneFromPacket(AbcdRawPacket p)
		{
			for (int i = 0; i < supportedTones.length; i++)
			{
				ABCDRtpSignal t = supportedTones[i];
				if (t.getCode() == p.getCode())
				{
					return t;
				}
			}

			return null;
		}
	}





	public void addAbcdListener(ABCDListener listener)
	{
		if ((listener != null) && (!abcdListeners.contains(listener)))
		{
			abcdListeners.add(listener);
		}
	}
	public void removeAbcdListener(ABCDListener listener)
	{
		abcdListeners.remove(listener);
	}
}
