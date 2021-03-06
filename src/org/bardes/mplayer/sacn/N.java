package org.bardes.mplayer.sacn;

import java.nio.ByteBuffer;

public class N
{

	public static int getInt(ByteBuffer buffer, Framing field, int addOffset)
	{
		int offset = field.getOffset() + addOffset;
		int size = field.getSize();
		
		switch (size)
		{
		case 1:
			return u(buffer.get(offset));
			
		case 2:
			return buffer.getShort(offset);
			
		case 4:
			return buffer.getInt(offset);
			
		default:
			return -1;
		}
		
	}

    public static int u(int b)
    {
        if (b < 0)
            b += 256;
        return b;
    }
    
    public static int us(int s)
    {
        if (s < 0)
            s += 65536;
        return s;
    }

}
