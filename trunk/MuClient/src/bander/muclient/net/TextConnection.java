package bander.muclient.net;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.util.LinkedList;

import javax.net.ssl.SSLSocket;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.io.SocketInputBuffer;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.CharArrayBuffer;

import android.os.Handler;
import android.os.Message;

public class TextConnection extends Thread {
	public static final int			MESSAGE_CLEAR	= 1;
	public static final int			MESSAGE_LINE	= 2;
	public static final int			MESSAGE_SYSTEM	= 3;

	private final String			mHost;
	private final int				mPort;
	private final boolean			mUseSsl;
	private final String			mPostConnect;

	private Handler 				mHandler;
	private Socket					mSocket;

	private final Object			mHandlerLock	= new Object();

	private boolean 				mNewLine		= false;
	
	private class BufferEntry {
		public final int messageType;
		public final String text;
		public BufferEntry(int messageType, String text) {
			this.messageType = messageType; 
			this.text = text;
		}
	}
	private LinkedList<BufferEntry>	mBuffer			= new LinkedList<BufferEntry>();

	private final int				BUFFER_SIZE		= 200;

	public TextConnection(Handler handler, String host, int port, boolean useSsl, String postConnect) {
		mHandler = handler;
		mHost = host;
		mPort = port;
		mUseSsl = useSsl;
		mPostConnect = postConnect;
	}

	/** Sets the message handler of this connection.
	 * @param handler The handler to set.
	 */
	public void setHandler(Handler handler) {
		synchronized (mHandlerLock) {
			if (mHandler != handler) {
				mHandler = handler;
				if ((mHandler != null) && (mBuffer.size() > 0)) {
					Message clear = mHandler.obtainMessage();
					clear.arg1 = MESSAGE_CLEAR;
					clear.sendToTarget();
					
					for (BufferEntry entry : mBuffer) {
						Message message = mHandler.obtainMessage();
						message.arg1 = entry.messageType;
						message.obj = entry.text;
						message.sendToTarget();
					}
				}
			}
		}
	}

	/** Disconnects this connection. */
	public void disconnect() {
		try {
			mSocket.close();
		} catch (IOException e) { }
	}

	/** Writes a string to this connection.
	 * @param text The string to write to the connection.
	 * @throws IOException
	 */
	public void write(final String text) throws IOException {
		if (mSocket != null) {
			PrintWriter writer = new PrintWriter(mSocket.getOutputStream(), true);
			writer.println(text);
		}
	}

	/** Determines whether the connection can connect to a host.
	 * @return True if this connection can connect, false otherwise.
	 */
	public boolean canConnect() { return !this.isAlive(); }
	
	/** Determines whether the connection can disconnect from a host.
	 * @return True if this connection can disconnect, false otherwise.
	 */
	public boolean canDisconnect() { return (mSocket != null) && !mSocket.isClosed(); }

	@Override
	public void run() {
		try {
			appendOutput("> Resolving " + mHost + "...\n", MESSAGE_SYSTEM);
			InetSocketAddress address = new InetSocketAddress(mHost, mPort);			
			if ((address == null) || (address.getAddress() == null)) {
				appendOutput("> error: Could not resolve, check you have an active data connection.\n", MESSAGE_SYSTEM);
				return;
			}

			appendOutput("> Connecting to " + address.getAddress().getHostAddress() + "...\n", MESSAGE_SYSTEM);
						
			SocketFactory socketFactory;
			if (mUseSsl) {
				KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				trustStore.load(null, null);
				SSLSocketFactory sslSocketFactory = new TrustingSocketFactory(trustStore);
				sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				socketFactory = sslSocketFactory;
			} else {
				socketFactory = PlainSocketFactory.getSocketFactory();
			}
			
			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setHttpElementCharset(params, "US-ASCII");
			
			mSocket = socketFactory.createSocket();
			mSocket = socketFactory.connectSocket(
				mSocket, address.getAddress().getHostAddress(), address.getPort(), null, 0, params
			);
			mSocket.setTcpNoDelay(true);
			mSocket.setSoTimeout(0);

			appendOutput("> Connected.\n", MESSAGE_SYSTEM);

			if (mUseSsl) {
				SSLSocket socket = (SSLSocket) mSocket;
				socket.startHandshake();
			}

			SessionInputBuffer inputBuffer = new SocketInputBuffer(mSocket, 8192, params);
			
			if (mPostConnect.length() > 0) {
				write(mPostConnect);
			}
			
			mSocket.setSoTimeout(500);
			CharArrayBuffer charBuffer = new CharArrayBuffer(1024);
			while ((!mSocket.isClosed()) && (!interrupted())) {
				try {
					while ((inputBuffer.readLine(charBuffer)) != -1) {
						if (!charBuffer.isEmpty()) {
							appendOutput(charBuffer.toString() + "\n", MESSAGE_LINE);
							charBuffer.clear();
						}
						yield();
					}
					break;
				} catch (InterruptedIOException e) { yield(); }
			}
			
			appendOutput("> Disconnected.\n", MESSAGE_SYSTEM);
			disconnect();
		} catch (IOException e) {
			appendOutput("> error: " + e.getMessage() + "\n", MESSAGE_SYSTEM);
		} catch (Exception e) {
			appendOutput("> error: " + e.toString() + "\n", MESSAGE_SYSTEM);
		}
	}

	private void appendOutput(String text, int messageType) {
		synchronized (mHandlerLock) {
			text = text.replace("\r\n", "\n");
			if (mNewLine) text = "\n" + text;
			if (text.endsWith("\n")) {
				text = text.substring(0, text.length()-1);
				mNewLine = true;
			} else mNewLine = false;

			BufferEntry entry = new BufferEntry(messageType, text);
			mBuffer.addLast(entry);
			while (mBuffer.size() > BUFFER_SIZE) mBuffer.removeFirst();
			
			if (mHandler != null) {
				Message message = mHandler.obtainMessage();
				message.arg1 = messageType;
				message.obj = text;
				message.sendToTarget();
			}
		}
	}

}
