package RoipPhone.service.protocol;



public final class ABCDSignal
{
    /**
     * The "0000" ABCD Tone
     */
    public static final ABCDSignal ABCD_0=new ABCDSignal("0");

    /**
     * The "0001" ABCD Tone
     */
    public static final ABCDSignal ABCD_1=new ABCDSignal("1");

    /**
     * The "0010" ABCD Tone
     */
    public static final ABCDSignal ABCD_2=new ABCDSignal("2");

    /**
     * The "0011" ABCD Tone
     */
    public static final ABCDSignal ABCD_3=new ABCDSignal("3");

    /**
     * The "0100" ABCD Tone
     */
    public static final ABCDSignal ABCD_4=new ABCDSignal("4");

    /**
     * The "0101" ABCD Tone
     */
    public static final ABCDSignal ABCD_5=new ABCDSignal("5");

    /**
     * The "0110" ABCD Tone
     */
    public static final ABCDSignal ABCD_6=new ABCDSignal("6");

    /**
     * The "0111" ABCD Tone
     */
    public static final ABCDSignal ABCD_7=new ABCDSignal("7");

    /**
     * The "1000" ABCD Tone
     */
    public static final ABCDSignal ABCD_8=new ABCDSignal("8");

    /**
     * The "1001" ABCD Tone
     */
    public static final ABCDSignal ABCD_9=new ABCDSignal("9");

    /**
     * The "1010" ABCD Tone
     */
    public static final ABCDSignal ABCD_A=new ABCDSignal("A");

    /**
     * The "1011" ABCD Tone
     */
    public static final ABCDSignal ABCD_B=new ABCDSignal("B");

    /**
     * The "1100" ABCD Tone
     */
    public static final ABCDSignal ABCD_C=new ABCDSignal("C");

    /**
     * The "1101" ABCD Tone
     */
    public static final ABCDSignal ABCD_D=new ABCDSignal("D");

    /**
     * The "1110" ABCD Tone
     */
    public static final ABCDSignal ABCD_E=new ABCDSignal("E");

    /**
     * The "1111" ABCD Tone
     */
    public static final ABCDSignal ABCD_F=new ABCDSignal("F");

    /**
     * The value of the ABCD tone
     */
    private final String value;

    /**
     * Creates a ABCD instance with the specified tone value. The method is
     * private since one would only have to use predefined static instances.
     *
     * @param value one of te ABCD_XXX fields, indicating the value of the tone.
     */
    private ABCDSignal(String value)
    {
        this.value = value;
    }

    /**
     * Returns the string representation of this ABCD tone.
     *
     * @return the <tt>String</tt> representation of this ABCD tone.
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * Indicates whether some other object is "equal to" this tone.
     * <p>
     * @param target the reference object with which to compare.
     *
     * @return  <tt>true</tt> if target represents the same tone as this
     * object.
     */
    @Override
    public boolean equals(Object target)
    {
        if (!(target instanceof ABCDSignal))
        {
            return false;
        }
        ABCDSignal targetABCDTone = (ABCDSignal)(target);

        return targetABCDTone.value.equals(this.value);
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>. The method would actually return the
     * hashcode of the string representation of this ABCD tone.
     * <p>
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
     * Parses input <tt>value</tt> and return the corresponding tone.
     * If unknown will return null;
     * @param value the input value.
     * @return the corresponding tone, <tt>null</tt> for unknown.
     */
    public static ABCDSignal getABCDTone(String value)
    {
        if (value == null)
            return null;
        else if (value.equals(ABCD_0.getValue()))
            return ABCD_0;
        else if (value.equals(ABCD_1.getValue()))
            return ABCD_1;
        else if (value.equals(ABCD_2.getValue()))
            return ABCD_2;
        else if (value.equals(ABCD_3.getValue()))
            return ABCD_3;
        else if (value.equals(ABCD_4.getValue()))
            return ABCD_4;
        else if (value.equals(ABCD_5.getValue()))
            return ABCD_5;
        else if (value.equals(ABCD_6.getValue()))
            return ABCD_6;
        else if (value.equals(ABCD_7.getValue()))
            return ABCD_7;
        else if (value.equals(ABCD_8.getValue()))
            return ABCD_8;
        else if (value.equals(ABCD_9.getValue()))
            return ABCD_9;
        else if (value.equals(ABCD_A.getValue()))
            return ABCD_A;
        else if (value.equals(ABCD_B.getValue()))
            return ABCD_B;
        else if (value.equals(ABCD_C.getValue()))
            return ABCD_C;
        else if (value.equals(ABCD_D.getValue()))
            return ABCD_D;
        else if (value.equals(ABCD_E.getValue()))
            return ABCD_E;
        else if (value.equals(ABCD_F.getValue()))
            return ABCD_F;
        else
            return null;
    }
}
