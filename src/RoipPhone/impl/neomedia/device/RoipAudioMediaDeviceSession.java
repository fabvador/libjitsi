package RoipPhone.impl.neomedia.device;



import javax.media.CaptureDeviceInfo;
import javax.media.Player;
import javax.media.Renderer;
import javax.media.control.TrackControl;

import org.jitsi.impl.neomedia.device.AbstractMediaDevice;
import org.jitsi.impl.neomedia.device.AudioMediaDeviceSession;
import org.jitsi.impl.neomedia.jmfext.media.renderer.audio.PulseAudioRenderer;



public class RoipAudioMediaDeviceSession extends AudioMediaDeviceSession
{
	protected RoipAudioMediaDeviceSession(AbstractMediaDevice device) 
	{
		super(device);
	}
	
	
	
    @Override
    protected Renderer createRenderer(Player player, TrackControl trackControl)
    {
        Renderer renderer = super.createRenderer(player, trackControl);
        if (getDevice() instanceof RoipPhoneMediaDevice)
        {
        	CaptureDeviceInfo playback = ((RoipPhoneMediaDevice) getDevice()).playbackMediaDeviceInfo;
        	if ((playback != null) && (renderer instanceof PulseAudioRenderer))
        	{
        		((PulseAudioRenderer) renderer).setLocator(playback.getLocator());
        	}
        }

        return renderer;
    }
}
