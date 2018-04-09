package fire.alarm.server;

import java.net.Socket;

public interface ISocketConnection {
	
	public void initSocketConnection(Socket serverSocket);
	public Object readSocketData();
	public Socket getServerSocket();
	public void closeSocket();
}
