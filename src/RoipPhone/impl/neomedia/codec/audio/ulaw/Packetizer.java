package RoipPhone.impl.neomedia.codec.audio.ulaw;

import java.awt.Component;

import javax.media.Buffer;
import javax.media.Control;




/**
 * Overrides the ULaw Packetizer with a different packet size.
 * Also removes the payload when mute
 * @author fabvador
 */
public class Packetizer extends org.jitsi.impl.neomedia.codec.audio.ulaw.Packetizer
{
	private static final byte[] EMPTY_BUFFER	= new byte[] {};
	
	private Long lastSequenceNumber				= null;
	private boolean mute						= true;
	
	
	
	public Packetizer()
	{
		addControl(new MuteControl());
	}
	
	@Override
	public int process(Buffer inputBuffer, Buffer outputBuffer)
	{
		int ret = super.process(inputBuffer, outputBuffer);
		
		if (ret == BUFFER_PROCESSED_OK)
		{
			if (lastSequenceNumber == null)
			{
				lastSequenceNumber = inputBuffer.getSequenceNumber();
			}
			//-----------------
			// remove the payload
			if (mute)
			{
				outputBuffer.setData(EMPTY_BUFFER);
				outputBuffer.setOffset(0);
				outputBuffer.setLength(0);
			}
			outputBuffer.setSequenceNumber(lastSequenceNumber++);
		}
		return ret;
	}
	
	
	
	/**
	 * When in mute, it removes the payload (decrease bandwidth)
	 * My idea was to simply discard the packet but it seems impossible.
	 * 40ms: 374 reduced to 54 bytes
	 * 20ms: 214 reduced to 54 bytes
	 * @author fabvador
	 */
	public class MuteControl implements Control
	{
		public void setMute(boolean mute)
		{
			Packetizer.this.mute = mute;
		}
		public boolean isMute()
		{
			return mute;
		}
		@Override
		public Component getControlComponent()
		{
			return null;
		}
	}
}
