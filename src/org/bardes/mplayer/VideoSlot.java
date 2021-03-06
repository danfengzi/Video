package org.bardes.mplayer;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlType;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

@SuppressWarnings("restriction")
@XmlType(name="video")
public class VideoSlot extends Slot
{
    private MediaPlayer mp;
    
    public VideoSlot()
    {
    }
    
    @Override
    public void setReference(String reference)
    {
        super.setReference(reference);
        
        Media media = new Media(reference);
        mp = new MediaPlayer(media);
        mp.setAutoPlay(false);
    }
    
    @Override
    public String toString()
    {
    	return String.format("Slot %03d - %s", id, getDescription());
    }

    @Override
    public Type getType()
    {
        return Type.VIDEO;
    }

    @Override
    public Node getNode(Stage stage)
    {
        MediaView mediaView = new MediaView(mp);
        mediaView.setPreserveRatio(true);
        mediaView.fitWidthProperty().bind(stage.widthProperty());
        mediaView.fitHeightProperty().bind(stage.heightProperty());
        return mediaView;
    }
    
    @Override
    public Node getPreview(Node owner)
    {
    	MediaView mediaView = new MediaView(mp);
    	return mediaView;
    }

    @Override
    public Node getThumbNail()
    {
        MediaView mediaView = new MediaView(mp);
        mediaView.setPreserveRatio(true);
        mediaView.setFitHeight(128);
        mediaView.setFitWidth(128);
        return mediaView;
    }
    
    @Override
    public void makeThumbNail(Parent parent)
    {
        if (this.previewImage == null || !new File(this.previewImage).exists())
        {
        	try
			{
        		String ref = URI.create(reference).getPath();
        		File out = new File(ref + MainController.MOVIE_THUMB);
				WritableImage image = parent.snapshot(null, null);
				ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", out);
				
				this.previewImage = out.getCanonicalPath();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
        }
        
    }

}
