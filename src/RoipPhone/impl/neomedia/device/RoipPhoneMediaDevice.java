package RoipPhone.impl.neomedia.device;




import javax.media.CaptureDeviceInfo;
import org.jitsi.impl.neomedia.device.AudioMediaDeviceImpl;
import org.jitsi.impl.neomedia.device.MediaDeviceSession;



public class RoipPhoneMediaDevice extends AudioMediaDeviceImpl
{
	public CaptureDeviceInfo playbackMediaDeviceInfo;

	public RoipPhoneMediaDevice(CaptureDeviceInfo captureDeviceInfo, CaptureDeviceInfo playbackMediaDeviceInfo)
	{
		super(captureDeviceInfo);
		this.playbackMediaDeviceInfo = playbackMediaDeviceInfo;
	}
	
	
	@Override
    public MediaDeviceSession createSession()
    {
        switch (getMediaType())
        {
        case VIDEO:
            return super.createSession();
        default:
            return new RoipAudioMediaDeviceSession(this);
        }
    }
}
