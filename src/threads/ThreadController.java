package threads;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import client.Client;

public class ThreadController extends Thread {

	private boolean stop;
	private boolean hasRun; //Stops same thread attempting to run twice and throwing an error
	private int initialDelay;
	private int wait = 20; //Wait for each iteration
	private boolean iteratePaint; //Whether to paint upon iteration

	private Object target; //Target object of animation
	List<Object> extras; //A list of extra parameters that can be passed in beyond the target if needed
	Set<Object> elements; //Generic list of thread created elements a thread can use
	private int i; //Thread clock
	
	private Runnable finishAction;

	public ThreadController() {
		this.stop = false;
		this.hasRun = false;
		initialDelay = 0;
		iteratePaint = true;
	}

	@Override
	public void start() {
		if (!hasRun()) {
			hasRun = true;
			i = 0;
			super.start();
		}
	}
	
	public void end() {this.stop = true;}

	public boolean isRunning() {return !this.stop;}

	public boolean hasRun() {return this.stop&&this.hasRun;}

	public void setWait(int wait) {this.wait = wait;}
	
	public void setInitialDelay(int d) {this.initialDelay = d;}

	public boolean hasElements() {return elements!=null;}

	public Set<?> getElements() {return Collections.unmodifiableSet(elements);}

	public Object getTarget() {return this.target;}
	
	public void setTarget(Object t) {this.target = t;}
	
	public void setExtras(List<Object> extras) {this.extras = extras;}
	
	public int getIncrement() {return this.i;}
	
	public void setFinishAction(Runnable r) {this.finishAction = r;}
	
	public void setPaintOnIterate(boolean p) {iteratePaint = p;}

	public void doInitialDelay() {
		if (initialDelay>0) {
			try {Thread.sleep(initialDelay);}
			catch (InterruptedException e) {e.printStackTrace();}
		}
	}
	
	public void iterate() {
		i++;
		if (iteratePaint) Client.cGUI.repaint();
		sleep(wait);
	}

	public void finish() {
		if (finishAction!=null) finishAction.run();
		Client.cGUI.repaint();
	}
	
	public void sleep(int wait) {
		try {Thread.sleep(wait);}
		catch (InterruptedException e) {e.printStackTrace();}
	}
}
