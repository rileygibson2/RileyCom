package client.gui.views;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;

import client.AudioManager;
import client.Client;
import client.gui.GUI;
import client.gui.components.Button;
import client.gui.components.DropDown;
import client.gui.components.Image;
import client.gui.components.Label;
import client.gui.components.TextBox;
import general.Functional;
import general.Point;
import general.Rectangle;

public class SettingsView extends View {

	private static SettingsView singleton;

	DropDown<Mixer.Info> audioIn;
	DropDown<Mixer.Info> audioOut;
	DropDown<Line.Info> remote;
	Color bg = new Color(50, 50, 50);

	private SettingsView() {
		super(ViewType.Settings, new Rectangle(10, 10, 80, 80));
		int x = 6;	
		
		//Input dropdown
		addComponent(new Label(new Point(x, 12), "Microphone", new Font("Geneva", Font.BOLD, 18), new Color(200, 200, 200)));
		audioIn = new DropDown<>(new Rectangle(x, 18, 40, 15));
		audioIn.addComponent(new Image(new Rectangle(85, 25, 8, 50), "closedselector.png"));
		addComponent(audioIn);

		//Actions
		audioIn.setSelected(AudioManager.getInstance().getDefaultMic());
		audioIn.setActions(new Functional<LinkedHashMap<String, Mixer.Info>, Mixer.Info>() {
			public void submit(Mixer.Info s) {
				AudioManager.getInstance().setMicLineInfo(s);
			}

			public LinkedHashMap<String, Info> get() {
				return AudioManager.getInstance().listMicrophones();
			}
			
		});

		//Output dropdown
		addComponent(new Label(new Point(x, 42), "Speaker", new Font("Geneva", Font.BOLD, 18), new Color(200, 200, 200)));
		audioOut = new DropDown<Mixer.Info>(new Rectangle(x, 48, 40, 15));
		audioOut.addComponent(new Image(new Rectangle(85, 25, 8, 50), "closedselector.png"));
		addComponent(audioOut);

		//Actions
		audioOut.setSelected(AudioManager.getInstance().getDefaultSpeaker());
		audioOut.setActions(new Functional<LinkedHashMap<String, Mixer.Info>, Mixer.Info>() {
			public void submit(Mixer.Info s) {
				AudioManager.getInstance().setSpeakerLineInfo(s);
			}

			public LinkedHashMap<String, Info> get() {
				return AudioManager.getInstance().listSpeakers();
			}
			
		});

		//Remote dropdown
		addComponent(new Label(new Point(x, 72), "Remote", new Font("Geneva", Font.BOLD, 18), new Color(200, 200, 200)));
		remote = new DropDown<>(new Rectangle(x, 78, 40, 15));
		remote.addComponent(new Image(new Rectangle(85, 25, 8, 50), "closedselector.png"));
		addComponent(remote);

		//Port textbox
		x = 55;
		addComponent(new Label(new Point(x, 12), "Port", new Font("Geneva", Font.BOLD, 18), new Color(200, 200, 200)));
		addComponent(new TextBox(new Rectangle(x, 18, 40, 15), ""+Client.getConnectPort()));

		//Exit button
		Button b = new Button(new Rectangle(92, 4, 6, 12), new Color(100, 100, 100));
		b.setClickAction(() -> Client.cGUI.changeView(HomeView.getInstance()));
		b.addComponent(new Image(new Rectangle(10, 10, 80, 80), "exit.png"));
		addComponent(b);
	}

	public static SettingsView getInstance() {
		if (singleton==null) singleton = new SettingsView();
		return singleton;
	};

	@Override
	public void enter() {
		// TODO Auto-generated method stub

	}

	@Override
	public void draw(Graphics2D g) {
		GUI.getInstance().getScreenUtils().fillRoundRect(g, bg, getRec());
		super.draw(g);
	}
}
