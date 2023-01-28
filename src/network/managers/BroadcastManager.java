package network.managers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import cli.CLI;
import client.Intercom;
import general.Pair;
import network.Client;
import network.messages.Code;
import network.messages.Message;
import threads.ThreadController;

public class BroadcastManager extends AbstractManager {

	private DatagramSocket writeSocket;
	private DatagramSocket listenSocket;
	private ThreadController writer;
	private ThreadController listener;
	private ThreadController cleaner;
	private static Set<Client> potentialClients;

	public BroadcastManager() {
		super();
		potentialClients = new HashSet<Client>();
	}

	@Override
	protected void start() {
		startWriter();
		startCleaner();
		startListener();
	}

	public void startWriter() {
		writer = new ThreadController() {
			@Override
			public void run() {
				CLI.debug("Starting...");
				try {
					writeSocket = new DatagramSocket();
					writeSocket.setBroadcast(true);

					while (isRunning()) {
						//Update all addresses for this computer every 2 mins
						if (getIncrement()>0&&getIncrement()%24==0) NetworkManager.buildLocalAddresses();

						//Broadcast message
						byte[] m = new Message(Code.Broadcast).formatBytes();
						DatagramPacket packet = new DatagramPacket(m, m.length, InetAddress.getByName("255.255.255.255"), Intercom.getBroadcastPort());
						writeSocket.send(packet);

						iterate();
					}
				} 
				catch (IOException e) {
					if (shutdown||Intercom.isShuttingdown()) return;
					fatalError("There was a problem with the broadcast writer - "+e.getMessage(), false);
				}
			}
		};

		writer.setWait(5000);
		writer.start();
	}

	public void startListener() {
		if (listener!=null) listener.end();

		listener = new ThreadController() {
			@Override
			public void run() {
				while (isRunning()) {
					try {
						byte[] buffer = new byte[1024];
						DatagramPacket recieved = new DatagramPacket(buffer, buffer.length);
						listenSocket = new DatagramSocket(Intercom.getBroadcastPort());
						listenSocket.setSoTimeout(5000);
						listenSocket.receive(recieved);

						//Check sender wasn't this computer
						if (Intercom.isProduction()) {
							if (NetworkManager.isLocalAddress(recieved.getAddress())) {
								if (CLI.isVerbose()) CLI.debug("Local broadcast response recieved");
								continue;
							}
						}

						//Process data
						Message m = Message.decode(Arrays.copyOfRange(recieved.getData(), 0, recieved.getLength()));
						if (m==null) {
							CLI.error("Recieved bad message");
							continue;
						}

						switch (m.getCode()) {
						case Broadcast:
							byte[] r = new Message(Code.BroadcastAck, "cP="+Intercom.getConnectPort()+",lP="+Intercom.getListenPort()).formatBytes();
							DatagramPacket response = new DatagramPacket(r, r.length, InetAddress.getByName("255.255.255.255"), Intercom.getBroadcastListenPort());
							new DatagramSocket().send(response);
							break;

						case BroadcastAck:
							if (CLI.isVerbose()) CLI.debug("Recieved from "+recieved.getAddress().getHostAddress()+": "+m.toString());
							Pair<Integer, Integer> ports = m.splitPorts();

							//Check ports match before adding potential client
							if (ports.a!=Intercom.getListenPort()||ports.b!=Intercom.getConnectPort()) break;
							addPotentialClient(new Client(recieved.getAddress(), ports.a, ports.b));
							break;

						default:
							break;
						}
					}
					catch (IOException e) {
						if (shutdown||Intercom.isShuttingdown()) return;
						if (e.getClass()==SocketTimeoutException.class) {
							if (CLI.isVerbose()) CLI.debug("No responses - timeout");
						}
						else fatalError("There was a problem with the broadcast listener - "+e.getMessage(), false);
					}
					finally {if (listenSocket!=null) listenSocket.close();}
				}
			}
		};

		listener.start();
	}

	public void startCleaner() {
		if (cleaner!=null) cleaner.end();

		cleaner = new ThreadController() {
			@Override
			public void run() {
				while (isRunning()) {
					Set<Client> toRemove = new HashSet<>();
					for (Client c : potentialClients) {
						if (c.isExpired()
							|| c.getConnectPort()!=Intercom.getListenPort()
							|| c.getListenPort()!=Intercom.getConnectPort()
							|| c.failedRecently()) {
							toRemove.add(c);
						}
					}

					/*
					 * If in auto mode and a client being removed from potential
					 * clients is the current client then current client should be
					 * set to null to preserve 'auto' behaviour.
					 * 
					 * This should only happen if the cause of the
					 * client being removed is something other than just expired.
					 */
					if (Intercom.isAutoDetectEnabled()&&toRemove.contains(Intercom.getClient())) {
						Intercom.setClient(Client.nullClient);
					}

					potentialClients.removeAll(toRemove);
					iterate();
				}
			}
		};

		cleaner.setWait(2000);
		cleaner.start();
	}

	public static Set<Client> getPotentialClients() {return potentialClients;}

	public static void addPotentialClient(Client pC) {
		for (Client p : potentialClients) {
			if (p.equals(pC)) {
				p.resetTimestamp(); //So to give more time to this client
				return;
			}
		}
		potentialClients.add(pC);
	}

	public static void printPotentialClients() {
		if (potentialClients==null) {
			CLI.debug("Potential Clients is null");
			return;
		}
		if (potentialClients.isEmpty()) {
			CLI.debug("There are no current potential clients");
			return;
		}
		CLI.debug("Potential Clients:");
		for (Client pC : potentialClients) CLI.debug(pC.toString());
	}

	@Override
	public boolean hasShutdown() {
		return shutdown&&writer.hasEnded()&&listener.hasEnded()&&cleaner.hasEnded();
	}

	@Override
	public void shutdown() {
		if (shutdown) return;
		if (writer!=null) writer.end();
		if (listener!=null) listener.end();
		if (cleaner!=null) cleaner.end();
		if (writeSocket!=null) writeSocket.close();
		if (listenSocket!=null) listenSocket.close();
		CLI.debug("Shutdown");
	}
}
