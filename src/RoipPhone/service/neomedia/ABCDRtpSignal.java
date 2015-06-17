package RoipPhone.service.neomedia;

import RoipPhone.service.protocol.ABCDSignal;



/**
 * Represents all ABCD Signals for RTP method (RFC5244).
 */
public final class ABCDRtpSignal
{
    /**
     * The "0" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_0=new ABCDRtpSignal("0", (byte)0x90);

    /**
     * The "1" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_1=new ABCDRtpSignal("1", (byte)0x91);

    /**
     * The "2" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_2=new ABCDRtpSignal("2", (byte)0x92);

    /**
     * The "3" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_3=new ABCDRtpSignal("3", (byte)0x93);

    /**
     * The "4" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_4=new ABCDRtpSignal("4", (byte)0x94);

    /**
     * The "5" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_5=new ABCDRtpSignal("5", (byte)0x95);

    /**
     * The "6" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_6=new ABCDRtpSignal("6", (byte)0x96);

    /**
     * The "7" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_7=new ABCDRtpSignal("7", (byte)0x97);

    /**
     * The "8" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_8=new ABCDRtpSignal("8", (byte)0x98);

    /**
     * The "9" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_9=new ABCDRtpSignal("9", (byte)0x99);

    /**
     * The "A" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_A=new ABCDRtpSignal("A", (byte)0x9A);

    /**
     * The "B" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_B=new ABCDRtpSignal("B", (byte)0x9B);

    /**
     * The "C" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_C=new ABCDRtpSignal("C", (byte)0x9C);

    /**
     * The "D" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_D=new ABCDRtpSignal("D", (byte)0x9D);

    /**
     * The "D" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_E=new ABCDRtpSignal("E", (byte)0x9E);

    /**
     * The "D" DTMF Tone
     */
    public static final ABCDRtpSignal ABCD_F=new ABCDRtpSignal("F", (byte)0x9F);

    /**
     * The value of the DTMF tone
     */
    private final String value;

    /**
     * The code of the tone, as specified by RFC 5244, and the we'll actually
     * be sending over the wire.
     */
    private final byte code;

    /**
     * Creates a DTMF instance with the specified tone value. The method is
     * private since one would only have to use predefined static instances.
     *
     * @param value one of the ABCD_XXX fields, indicating the value of the tone.
     * @param code the of the DTMF tone that we'll actually be sending over the
     * wire, as specified by RFC 4733.
     */
    private ABCDRtpSignal(String value, byte code)
    {
        this.value = value;
        this.code = code;
    }

    /**
     * Returns the string representation of this DTMF tone.
     *
     * @return the <tt>String</tt> representation of this DTMF tone.
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * Indicates whether some other object is "equal to" this tone.
     *
     * @param target the reference object with which to compare.
     *
     * @return  <tt>true</tt> if target represents the same tone as this
     * object.
     */
    @Override
    public boolean equals(Object target)
    {
        if(!(target instanceof ABCDRtpSignal))
        {
            return false;
        }
        ABCDRtpSignal targetABCDSignal = (ABCDRtpSignal)(target);

        return targetABCDSignal.value.equals(this.value);
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>. The method would actually return the
     * hashcode of the string representation of this DTMF tone.
     *
     * @return  a hash code value for this object (same as calling
     * getValue().hashCode()).
     */
    @Override
    public int hashCode()
    {
        return getValue().hashCode();
    }

    /**
     * Returns the RFC 4733 code of this DTMF tone.
     *
     * @return the RFC 4733 code of this DTMF tone.
     */
    public byte getCode()
    {
        return code;
    }

    /**
     * Maps between protocol and media DTMF objects.
     * @param tone The ABCDSignal to be mapped to an DTMFRtpTone.
     * @return The DTMFRtpTone corresponding to the tone specified.
     */
    public static ABCDRtpSignal mapSignal(ABCDSignal tone)
    {
        if(tone.equals(ABCDSignal.ABCD_0))
            return ABCDRtpSignal.ABCD_0;
        else if(tone.equals(ABCDSignal.ABCD_1))
            return ABCDRtpSignal.ABCD_1;
        else if(tone.equals(ABCDSignal.ABCD_2))
            return ABCDRtpSignal.ABCD_2;
        else if(tone.equals(ABCDSignal.ABCD_3))
            return ABCDRtpSignal.ABCD_3;
        else if(tone.equals(ABCDSignal.ABCD_4))
            return ABCDRtpSignal.ABCD_4;
        else if(tone.equals(ABCDSignal.ABCD_5))
            return ABCDRtpSignal.ABCD_5;
        else if(tone.equals(ABCDSignal.ABCD_6))
            return ABCDRtpSignal.ABCD_6;
        else if(tone.equals(ABCDSignal.ABCD_7))
            return ABCDRtpSignal.ABCD_7;
        else if(tone.equals(ABCDSignal.ABCD_8))
            return ABCDRtpSignal.ABCD_8;
        else if(tone.equals(ABCDSignal.ABCD_9))
            return ABCDRtpSignal.ABCD_9;
        else if(tone.equals(ABCDSignal.ABCD_A))
            return ABCDRtpSignal.ABCD_A;
        else if(tone.equals(ABCDSignal.ABCD_B))
            return ABCDRtpSignal.ABCD_B;
        else if(tone.equals(ABCDSignal.ABCD_C))
            return ABCDRtpSignal.ABCD_C;
        else if(tone.equals(ABCDSignal.ABCD_D))
            return ABCDRtpSignal.ABCD_D;
        else if(tone.equals(ABCDSignal.ABCD_E))
            return ABCDRtpSignal.ABCD_E;
        else if(tone.equals(ABCDSignal.ABCD_F))
            return ABCDRtpSignal.ABCD_F;

        return null;
    }
}
