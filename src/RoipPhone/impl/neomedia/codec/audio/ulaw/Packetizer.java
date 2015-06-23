package RoipPhone.impl.neomedia.codec.audio.ulaw;

import javax.media.Buffer;




/**
 * Overrides the ULaw Packetizer with a different packet size.
 *
 * @author fabvador
 */
public class Packetizer extends org.jitsi.impl.neomedia.codec.audio.ulaw.Packetizer
{
	private Long lastSequenceNumber	= null;


	
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
			outputBuffer.setSequenceNumber(lastSequenceNumber++);
		}

		return ret;
	}
}
