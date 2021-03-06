package org.bardes.mplayer.personality;

import static org.bardes.mplayer.sacn.N.u;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.bardes.mplayer.Layer;


/**
 * @author eric
 * The most simple mode.
 */
public class LayerLitePersonality implements Personality
{
    protected Layer layer;
	private int dimmer;
	private int groupId;
	private int slotId;
	private int volume;
    
    @Override
	public int getFootprint()
	{
		return 4;
	}

	@Override
	public void decode(ByteBuffer d)
	{
	    d.order(ByteOrder.BIG_ENDIAN);
	    
		dimmer = u(d.get());
		groupId = u(d.get());
		slotId = u(d.get());
		volume = u(d.get());
	}
	
	@Override
	public void activate(Layer layer)
	{
		layer.setItem(groupId, slotId);
		layer.setDimmer(dimmer);
		layer.setVolume(volume, 128);
	}
}
