package org.bardes.mplayer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.bardes.mplayer.artnet.ArtNetListener;
import org.bardes.mplayer.citp.CITPServer;
import org.bardes.mplayer.cue.CueStack;
import org.bardes.mplayer.httpd.HTTPServer;
import org.bardes.mplayer.net.DMXProtocol;
import org.bardes.mplayer.net.NetworkListener;
import org.bardes.mplayer.net.Replication;
import org.bardes.mplayer.personality.InternalListener;
import org.bardes.mplayer.sacn.E131Listener;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 *
 * @author dean
 */
@SuppressWarnings("restriction")
public class Main extends Application
{

	public static Stage window;

	public static void main(String[] args)
	{
		System.out.println(System.getProperty("java.version"));
		launch(args);
	}

	public static Stage display;
	
//	public static List<Layer> layers = new ArrayList<>();
	
	private static NetworkListener listener;
	
	private static Config config;
	
	private static File configLocation;
	
	public static CueStack stack = new CueStack();

	public static Config getConfig()
	{
		return config;
	}

	public static MainController controller;

    static Window displayWindow;

	private CITPServer citpServer;

	public static StackPane displayPane;

	static LayerManager layoutManager;

	private NetServer httpServer;
	
	private static Main main;
	
	@Override
	public void start(Stage primaryStage)
	{
		try
		{
			main = this;
			
			initConfig();
			
			ClassLoader cl = getClass().getClassLoader();
			URL url = cl.getResource("main.fxml");
			
			stack = CueStack.load();

			displayPane = new StackPane();
			displayPane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
			Scene displayScene = new Scene(displayPane);
			
			display = new Stage();
			display.setScene(displayScene);
			display.setResizable(true);
			display.setTitle("Display");
			
			HWXY pos = config.getDisplayPosition();
			if (pos != null)
			{
				if (pos.x < -8000)
					pos.x = 0;
				if (pos.y < -8000)
					pos.y = 0;
    			display.setHeight(pos.height);
    			display.setWidth(pos.width);
    			display.setX(pos.x);
    			display.setY(pos.y);
    			displayScene.setCursor(Cursor.NONE);
			}
			else
			{
			    display.setWidth(1024);
			    display.setHeight(768);
			    display.setX(0);
			    display.setY(0);
			}
			display.show();
			
			displayPane.minWidthProperty().bind(display.widthProperty());
			displayPane.maxWidthProperty().bind(display.widthProperty());
			displayPane.minHeightProperty().bind(display.heightProperty());
			displayPane.maxHeightProperty().bind(display.heightProperty());
			
			window = primaryStage;
			pos = config.getEditorPosition();
			if (pos != null)
			{
				if (pos.x < -8000)
					pos.x = 0;
				if (pos.y < -8000)
					pos.y = 0;
    			primaryStage.setX(pos.x);
    			primaryStage.setY(pos.y);
    			primaryStage.setWidth(pos.width);
    			primaryStage.setHeight(pos.height);
			}
			else
			{
			    primaryStage.setWidth(1280);
			    primaryStage.setHeight(720);
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
            
			restartListener(config.getDmxProtocol(), config.getLayers());
			
			citpServer = new CITPServer();
			citpServer.start();
			
			Replication replication = new Replication();
			replication.start();
			
			httpServer = new HTTPServer();
			httpServer.startServer();
			
			if (config.isFullscreen())
				controller.showmode();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void initConfig() throws URISyntaxException, IOException
	{
		configLocation = new File(System.getProperty("user.home"));
		configLocation = new File(configLocation, "astmedia.xml");
		if (configLocation.exists())
		{
			config = Config.load(configLocation);
		}
		else
		{
		    Config.location = configLocation;
			config = Config.reset();
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
		citpServer.stop();
		
		httpServer.stopServer();
		
		super.stop();
	}

	/**
	 * @param selectedItem 
	 * 
	 */
	public static void restartListener(DMXProtocol protocol, List<LayerConfig> layers)
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

		layoutManager = new LayerManager(displayPane, layers); 
		listener.setReceiver(layoutManager);
		listener.start();
	}

	public static String normalize(File selectedFile, Slot slot)
	{
		String name = selectedFile.getName();
		File target = normalizeName(slot, name);
		slot.setTimestamp(selectedFile.lastModified());
		
		try
		{
			FileChannel from = FileChannel.open(selectedFile.toPath(), StandardOpenOption.READ);
			FileChannel to = FileChannel.open(target.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			
			slot.setLength(from.size());
			to.transferFrom(from, 0, from.size());
			
			from.close();
			to.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return target.toURI().toString();
	}

	public static File normalizeName(Slot slot, String name)
	{
		String ext = "";
		int n = name.lastIndexOf('.');
		if (n > 0)
			ext = name.substring(n);
		
		String workDirectory = config.getWorkDirectory();
		File target = new File(workDirectory);
		target = new File(target, String.format("group_%03d", slot.group));
		target.mkdirs();
		target = new File(target, String.format("slot_%03d%s", slot.id, ext));
		return target;
	}

	public static void Shutdown()
	{
		try
		{
			System.out.println("Attempt to stop");
			controller.Shutdown();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}