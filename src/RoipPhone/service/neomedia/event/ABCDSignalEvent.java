package RoipPhone.service.neomedia.event;

import java.util.EventObject;

import org.jitsi.service.neomedia.AudioMediaStream;

import RoipPhone.service.neomedia.ABCDRtpSignal;



public class ABCDSignalEvent extends EventObject
{
	private static final long serialVersionUID	= 0L;
	
	private final ABCDRtpSignal abcdSignal;

	public ABCDSignalEvent(AudioMediaStream source, ABCDRtpSignal abcdSignal)
	{
		super(source);

		this.abcdSignal = abcdSignal;
	}

	public ABCDRtpSignal getAbcdSignal()
	{
		return abcdSignal;
	}
}
