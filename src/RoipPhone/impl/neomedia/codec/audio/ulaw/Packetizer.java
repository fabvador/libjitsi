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
	/**
	 * Constructs a new ULaw <tt>Packetizer</tt>.
	 */
	public Packetizer()
	{
		// RFC 3551 4.5 Audio Encodings default ms/packet is 20
		packetSize = 320;
		setPacketSize(packetSize);

		PLUGIN_NAME = "ULaw Packetizer";
	}

	@Override
	public void open()
	{
		super.open();
		//-----------------------------
		//	super.open() does the following:
		//--------
		//	RFC 3551 4.5 Audio Encodings default ms/packet is 20
        //	TODO: add some sanity checks
        //	int sampleRate = (int) ((AudioFormat) getInputFormat()).getSampleRate();
        //	setPacketSize(sampleRate / 50);
		//-----------------------------
		// I don't see a reason why this should be replaced!
		//-----------------------------
		setPacketSize(packetSize);
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
			outputBuffer.setSequenceNumber(lastSequenceNumber++);
		}

		return ret;
	}
}
