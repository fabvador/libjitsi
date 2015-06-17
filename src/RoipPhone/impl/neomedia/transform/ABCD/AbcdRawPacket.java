package RoipPhone.impl.neomedia.transform.ABCD;

import org.jitsi.impl.neomedia.*;
import org.jitsi.util.*;


public class AbcdRawPacket extends RawPacket
{
	/**
	 * Our class logger.
	 */
	private static final Logger logger = Logger.getLogger(AbcdRawPacket.class);

	/**
	 * The event code to send.
	 */
	private int code;

	public AbcdRawPacket(byte[] buffer, int offset, int length, byte payload)
	{
		super(buffer, offset, length);

		setPayloadType(payload);
	}


	public AbcdRawPacket(RawPacket pkt)
	{
		super(pkt.getBuffer(), pkt.getOffset(), pkt.getLength());

		int at = getHeaderLength();

		code = readByte(at++);
	}

	public void init(int code, boolean marker, long timestamp)
	{
		if(logger.isTraceEnabled())
		{
			logger.trace("ABCD send on RTP, code : " + code + " timestamps = " + timestamp + " Marker = " + marker);
		}

		// Set the marker
		setMarker(marker);

		// set the Timestamp
		setTimestamp(timestamp);

		// Clear any RTP header extensions
		removeExtension();

		// Create the RTP data
		setAbcdPayload(code);
	}

	/**
	 * Initializes the  a DTMF raw data using event, E and duration field.
	 * Event : the digits to transmit (0-15).
	 * E : End field, used to mark the two last packets.
	 * R always = 0.
	 * Volume always = 0.
	 * Duration : duration increments for each dtmf sending updates,
	 * stay unchanged at the end for the 3 last packets.
	 * <pre>
	 *  0                   1                   2                   3
	 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |     event     |                                               |
	 *  |       ?       |                                               |
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>
	 *
	 * @param code the digit to transmit 0-15
	 */
	private void setAbcdPayload(int code)
	{
		this.code = code;

		int at = getHeaderLength();

		writeByte(at++, (byte)code);
		writeByte(at++, (byte)0x80);	// => DTMF: end boolean used to mark the two last packets
		writeByte(at++, (byte)0);
		writeByte(at++, (byte)0);

		//packet finished setting its payload, set correct length
		setLength(at);
	}

	/**
	 * The event code of the current packet.
	 * @return the code
	 */
	public int getCode()
	{
		return code;
	}
}
