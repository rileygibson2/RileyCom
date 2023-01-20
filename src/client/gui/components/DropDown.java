package client.gui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import client.gui.Element;
import client.gui.ScreenUtils;
import general.CLI;
import general.Point;
import general.Rectangle;

public class DropDown<E> extends Component {

	private LinkedHashMap<String, E> options; //LinkedHashMap maintains an order
	private Future<LinkedHashMap<String, E>> update; //Executor that can be called to update options list
	Map.Entry<String, E> selectedItem; //The selected item and it's name
	private Map.Entry<String, E> noneItem =new AbstractMap.SimpleEntry<>("None", null); //A default item
	private Runnable onSelect; //Action to happen on click
	
	public Label selectedLabel; //The gui selected item component

	public DropDown(Rectangle rectangle, Element parent) {
		super(rectangle, parent);
		options = new LinkedHashMap<String, E>();
		selectedItem = noneItem;
		
		//Make top selected label
		selectedLabel = new Label(new Point(8, 65), selectedItem.getKey(), new Font("Geneva", Font.ITALIC, 14), new Color(200, 200, 200), this); 
		components.add(selectedLabel);
	}
	
	public void setSelectAction(Runnable onSelect) {this.onSelect = onSelect;}
	
	public void setUpdateAction(Future<LinkedHashMap<String, E>> update) {this.update = update;}
	
	public void setSelected(Map.Entry<String, E> selected) {this.selectedItem = selected;}
	
	public E getSelected() {return selectedItem.getValue();}
	
	public LinkedHashMap<String, E> getOptions() {return options;}
	
	@Override
	public void doClick(Point p) {
		if (selected) close(p);
		else open();
	}
	
	@Override
	public void doDeselect() {
		close(null);
		super.doDeselect();
	}

	private void open() {
		if (update!=null) {
			try {options = update.get();}
			catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
		}
		if (options.isEmpty()) return;
		
		selected = true;
		increasePriority();

		//Add labels
		int y = 160;
		for (Map.Entry<String, E> m : options.entrySet()) {
			components.add(new Label(new Point(8, y), m.getKey(), new Font("Geneva", Font.ITALIC, 13), new Color(200, 200, 200), this));
			y += 100;
		}
	}

	private void close(Point p) {
		if (p!=null) {
			//Get clicked element
			int i = (int) Math.floor(p.y)-1;
			if (i>=0&&i<options.size()) selectedItem = getOptionAt(i);
			if (selectedItem!=null) selectedLabel.text = selectedItem.getKey();
			
			if (onSelect!=null) onSelect.run();
		}

		selected = false;
		decreasePriority();
		Set<Component> toRemove = new HashSet<>();
		for (Component c : components) {
			if (c instanceof Label && c!= selectedLabel) toRemove.add(c);
		}
		components.removeAll(toRemove);
	}

	@Override
	public boolean isOver(Point p) {
		//Overridden as this allows a different isOver result for an open selector then for a closed one
		if (!selected) return super.isOver(p);
		else return super.isOver(p, new Rectangle(r.x, r.y, r.width, r.height*(options.size()+1)));
	}
	
	private Map.Entry<String, E> getOptionAt(int i) {
		int z = 0;
		for (Map.Entry<String, E> m : options.entrySet()) {
			if (z==i) return m;
			z++;
		}
		return null;
	}

	@Override
	public void doHover() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doUnhover() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doKeyPress(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void draw(Graphics2D g) {
		ScreenUtils.drawDropDown(g, this);
		super.draw(g);
	}
}
