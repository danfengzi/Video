package org.bardes.mplayer;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.bardes.mplayer.artnet.ArtNetListener;
import org.bardes.mplayer.cue.CueStack;
import org.bardes.mplayer.net.DMXProtocol;
import org.bardes.mplayer.net.NetworkListener;
import org.bardes.mplayer.personality.DMXPersonality;
import org.bardes.mplayer.personality.InternalListener;
import org.bardes.mplayer.personality.MasterLitePersonality;
import org.bardes.mplayer.personality.MasterRegularPersonality;
import org.bardes.mplayer.personality.Personality;
import org.bardes.mplayer.sacn.E131Listener;

/**
 *
 * @author dean
 */
public class Main extends Application
{

	public static Stage window;

	public static void main(String[] args)
	{
		System.out.println(System.getProperty("java.version"));
		launch(args);
	}

	public static Stage display;
	
	public static List<Layer> layers = new ArrayList<>();
	
	private static NetworkListener listener;
	
	private static Config config;
	
	private static File configLocation = new File("config.xml");
	
	public static CueStack stack = new CueStack();

	public static Config getConfig()
	{
		return config;
	}

	public static MainController controller;

    static Window displayWindow;
	
	@Override
	public void start(Stage primaryStage)
	{
		try
		{
			ClassLoader cl = getClass().getClassLoader();
			URL url = cl.getResource("main.fxml");
			
			if (configLocation.exists())
			{
				config = Config.load(configLocation);
			}
			else
			{
			    Config.location = configLocation;
				config = Config.reset(url);
			}
			
			stack = CueStack.load();

			StackPane displayPane = new StackPane();
			displayPane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
			Scene displayScene = new Scene(displayPane);
			
			display = new Stage();
			display.setScene(displayScene);
			display.setResizable(true);
			display.setTitle("Display");
			
			HWXY pos = config.getDisplayPosition();
			if (pos != null)
			{
    			display.setHeight(pos.height);
    			display.setWidth(pos.width);
    			display.setX(pos.x);
    			display.setY(pos.y);
			}
			else
			{
			    display.setWidth(1024);
			    display.setHeight(768);
			    display.setX(0);
			    display.setY(0);
			}
			display.show();
			
			for (int i = 0; i < 4; i++)
			{
				BorderPane e = new BorderPane();
				displayPane.getChildren().add(0, e);
				Layer l = new BasicLayer(i,display,e);
				layers.add(l);
			}
			
			displayPane.minWidthProperty().bind(display.widthProperty());
			displayPane.maxWidthProperty().bind(display.widthProperty());
			displayPane.minHeightProperty().bind(display.heightProperty());
			displayPane.maxHeightProperty().bind(display.heightProperty());
			
			window = primaryStage;
			pos = config.getEditorPosition();
			if (pos != null)
			{
    			primaryStage.setX(pos.x);
    			primaryStage.setY(pos.y);
    			primaryStage.setWidth(pos.width);
    			primaryStage.setHeight(pos.height);
			}
			else
			{
			    primaryStage.setWidth(1024);
			    primaryStage.setHeight(768);
			}
			
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(url);
			Parent p = loader.load();
			Scene myScene = new Scene(p);
			primaryStage.setTitle("Astraeus Media System");
			primaryStage.setScene(myScene);
			
			primaryStage.show();
			
			controller = loader.getController();
			controller.initializeCueList();

            Tab cueTab = controller.cueTab;
            controller.tabBar.getTabs().remove(cueTab);
            
			restartListener(config.getDmxProtocol(), config.getDmxPersonality());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void stop() throws Exception
	{
		HWXY pos = new HWXY();
		pos.height = display.getHeight();
		pos.width = display.getWidth();
		pos.x = display.getX();
		pos.y = display.getY();
		config.setDisplayPosition(pos);
		
		pos = new HWXY();
		pos.height = window.getHeight();
		pos.width = window.getWidth();
		pos.x = window.getX();
		pos.y = window.getY();
		config.setEditorPosition(pos);
		
		config.save();
		
		super.stop();
	}

	/**
	 * @param selectedItem 
	 * 
	 */
	public static void restartListener(DMXProtocol protocol, DMXPersonality personality)
	{
		if (listener != null)
			listener.stop();
		
		switch (protocol)
		{
		case INTERNAL:
			listener = new InternalListener(stack);
			break;
		case ARTNET:
		    listener = new ArtNetListener();
		    break;
		case SACN:
		default:
			listener = new E131Listener();
			break;
		}
		
		Personality p;
		switch (personality)
		{
		case REGULAR:
		    p = new MasterRegularPersonality(new MasterLayer(display), layers);
		    break;
		case LITE:
		default:
			p = new MasterLitePersonality(new MasterLayer(display), layers); 
			break;
		}
		listener.setPersonality(p);
		listener.start();
	}
}