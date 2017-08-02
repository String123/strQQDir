package com.string.qq.Server;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * 服务端主类,打开后自动接收客户端的连接,接口默认为8088, 用于接收转发客户端传来的信息,服务端能够显示在线列表,并且能够容纳40个客户端的连接群聊.
 * 文件发送功能:用随机端口接收转发文件,并且在转发过程中可以收发信息,可以处理多个文件转发
 * 
 * @author String
 * 
 */
public class Server {
	private static ServerSocket receivePortSocket;
	private static ServerSocket filePortSocket;
	private static int receivePort = 8088, filePort = 8087;
	private static List<PrintWriter> allOut;
	private static JFrame frame = new JFrame("Server");
	private static JTextPane txt = new JTextPane();
	private static JTextPane txt2 = new JTextPane();
	private static JScrollPane outputText = new JScrollPane(txt);
	private static JScrollPane liveList = new JScrollPane(txt2);
	private static StyledDocument outText = txt.getStyledDocument();
	private static StyledDocument listText = txt2.getStyledDocument();
	private static SimpleAttributeSet attr1 = new SimpleAttributeSet();
	private static JButton start = new JButton("启动服务器");
	private Map<String, PrintWriter> outMap = new HashMap<String, PrintWriter>();
	private Map<String, Socket> outSocketMap = new HashMap<String, Socket>();
	private Map<String, Integer> outPortMap = new HashMap<String, Integer>();
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	ExecutorService execut = Executors.newFixedThreadPool(100);

	/**
	 * 将上线的客户端的名字和输入输出流对应到HashMap中,并将输出流加入群发集合.
	 * 
	 * @param p
	 *            上线的输出流
	 * @param br
	 *            上线的输入流
	 * @param name
	 *            上线的名字
	 */
	public synchronized void addMap(PrintWriter p, BufferedReader br,
			String name, Socket s) {
		outMap.put(name, p);
		outSocketMap.put(name, s);
		allOut.add(p);
	}

	/**
	 * 将客户端对应的接收文件的端口存入Map与客户端名字对应
	 */
	public synchronized void addPort(String name, String msg) {
		Integer getFilePort = Integer.valueOf(msg);
		outPortMap.put(name, getFilePort);
	}

	/**
	 * 将下线的客户端从群发集合移除
	 * 
	 * @param name
	 *            下线的客户端的名字
	 * @param p
	 *            下线的客户端的输出流
	 */
	public synchronized void removeMsg(String name, PrintWriter p) {
		outMap.remove(name);
		allOut.remove(p);
	}

	/**
	 * 群发消息功能
	 * 
	 * @param msg
	 *            要群发的消息
	 */
	public synchronized void sendMsg(String msg) {
		for (PrintWriter p : allOut) {
			p.println(msg);
		}
	}

	/**
	 * 单发消息功能
	 * 
	 * @param msg
	 *            要发送的消息
	 * @param p
	 *            要单发的客户端的输出流
	 */
	public synchronized void sendMsg(String msg, PrintWriter p) {
		p.println(msg);
	}

	/**
	 * 更新服务端和客户端的在线列表
	 * 
	 * @throws Exception
	 */
	public void sendList() throws Exception {
		Set<String> names = outMap.keySet();
		listText.remove(0, listText.getLength());
		listText.insertString(listText.getLength(), "在线列表\n", null);
		for (PrintWriter pw : allOut)
			pw.println("L");
		for (String str : names) {
			for (PrintWriter pw : allOut)
				pw.println(str);
			listText.insertString(listText.getLength(), str + "\n", null);
		}
		for (PrintWriter pw : allOut)
			pw.println("E");
	}

	public void start() {
		try {
			while (true) {
				outText.insertString(outText.getLength(), sdf.format(new Date()).toString()
						+ "\n等待客户端链接....\n", attr1);
				Socket s = receivePortSocket.accept();
				outText.insertString(outText.getLength(), sdf.format(new Date()).toString()
						+ "\n客户端已连接\n", attr1);

				ClientHandler ch = new ClientHandler(s);
				execut.execute(ch);
				// 监听8087文件传输端口,循环接收转发客户端传来的文件
				FileHandler fh = new FileHandler();
				execut.execute(fh);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		receivePortSocket = new ServerSocket(receivePort);
		filePortSocket = new ServerSocket(filePort);
		
		allOut = new ArrayList<PrintWriter>();
		StyleConstants.setForeground(attr1, Color.BLACK);
		outText.insertString(outText.getLength(), sdf.format(new Date()).toString()
				+ "\n服务端已启动\n", attr1);
		outText
				.insertString(outText.getLength(), sdf.format(new Date()).toString()
						+ "\n本机IP地址为:" + ServerUtil.getLocalInetAddress()
						+ "\n", attr1);
		outText.insertString(outText.getLength(), sdf.format(new Date()).toString()
				+ "\n本机接收端口为:" + receivePort + "\n", attr1);

		listText.insertString(listText.getLength(), "在线列表\n", null);

		frame.setSize(500, 300);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setAlwaysOnTop(true);
		frame.setVisible(true);

		Font f = new Font(Font.SANS_SERIF, 0, 20);
		Font f2 = new Font(Font.SANS_SERIF, 0, 15);
		outputText.setBounds(17, 20, 350, 230);
		liveList.setBounds(375, 20, 110, 230);
		txt.setFont(f);
		txt.setEditable(false);
		txt2.setFont(f2);
		txt2.setEditable(false);
		start.setBounds(17, 10, 100, 30);

		frame.setLayout(null);
		frame.add(outputText);
		frame.add(liveList);

		Server server = new Server();
		server.start();
	}

	/**
	 * 用于接收转发文件
	 */
	public void forward() {
		String fname, toname, symbol, name;
		DataInputStream fromNameClient = null;
		DataOutputStream toNameClient = null;
		DataOutputStream toToNameClient = null;
		DataInputStream fromToNameClient = null;
		Socket fs = null;
		Socket tofs = null;
		byte[] inByte = null;
		int len = 0;

		while (true) {
			try {
				fs = filePortSocket.accept(); // 等待发送客户端的连接
				fromNameClient = new DataInputStream(fs.getInputStream()); // 发送客户端的数据输入流
				toNameClient = new DataOutputStream(fs.getOutputStream()); // 发送客户端的数据输出流

				name = fromNameClient.readUTF(); // 接收发送客户端的名字
				toname = fromNameClient.readUTF(); // 接收需要发送到的名字
				fname = fromNameClient.readUTF(); // 接收文件名

				// sendMsg("F", outMap.get(toname)); //向需要发送的名字的客户端发送文件请求

				String tonameIP = outSocketMap.get(toname).getInetAddress()
						.toString().substring(1); // 获取需要发送的客户端的IP地址
				Integer tonamePort = outPortMap.get(toname);

				tofs = new Socket(tonameIP, tonamePort); // 连接该地址的接收文件端口
				toToNameClient = new DataOutputStream(tofs.getOutputStream()); // 被发送的客户端的数据输出流
				fromToNameClient = new DataInputStream(tofs.getInputStream()); // 被发送的客户端的数据输入流

				toToNameClient.writeUTF(name); // 向被发送的客户端输出文件的发送客户端的名字
				toToNameClient.writeUTF(fname); // 向被发送的客户端输出文件名

				synchronized (outText) { // 向窗口输出转发信息
					outText.insertString(outText.getLength(), sdf.format(new Date())
							.toString()
							+ "\n"
							+ "正在转发文件"
							+ fname
							+ "从"
							+ name
							+ "到"
							+ toname + "...\n", attr1);
				}

				// 接收标识,判断文件是否传输
				symbol = fromToNameClient.readUTF();
				toNameClient.writeUTF(symbol);
				if (symbol.equals("REFUSE")) {
					synchronized (outText) { // 向窗口输出转发信息
						outText.insertString(outText.getLength(), sdf.format(new Date())
								.toString()
								+ "\n" + toname + "拒收文件!\n", attr1);
					}
					try {
						if (toToNameClient != null) {
							toToNameClient.close();
						}
						if (tofs != null) {
							tofs.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					continue;
				} else {
					synchronized (outText) { // 向窗口输出转发信息
						outText.insertString(outText.getLength(), sdf.format(new Date())
								.toString()
								+ "\n" + "文件开始传输!\n", attr1);
					}
				}
				len = 0;
				inByte = new byte[1024 * 8];
				while (true) { // 循环从发送端读取文件并输出到被发送客户端
					if (fromNameClient != null) {
						len = fromNameClient.read(inByte, 0, inByte.length);
					}
					if (len == -1) {
						break;
					}
					toToNameClient.write(inByte, 0, len);
					toToNameClient.flush();
				}
				synchronized (outText) { // 向窗口输出转发信息
					outText.insertString(outText.getLength(), sdf.format(new Date())
							.toString()
							+ "\n" + "文件转发完毕!\n", attr1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally { // 关闭IO流和连接
				try {
					if (toToNameClient != null) {
						toToNameClient.close();
					}
					if (tofs != null) {
						tofs.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 处理接收转发文件的线程
	 */
	private class FileHandler implements Runnable {

		@Override
		public void run() {
			forward();
		}

	}

	/**
	 * 处理客户端上线后的发送线程内部类,用线程池来加载每个线程.
	 * 
	 */
	private class ClientHandler implements Runnable {
		Socket s;

		/**
		 * 将上线的客户端的Socket传入
		 * 
		 * @param s
		 *            上线的客户端的Socket
		 */
		public ClientHandler(Socket s) {
			this.s = s;
		}

		@Override
		public void run() {
			PrintWriter pw = null;
			String name = null;
			try {
				OutputStream os = s.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os,"utf-8");
				pw = new PrintWriter(osw, true);

				InputStream is = s.getInputStream();
				InputStreamReader isr = new InputStreamReader(is, "utf-8");
				BufferedReader br = new BufferedReader(isr);
				name = br.readLine(); // 获取客户端传来的名字

				// 判断名字是否合法或者重复,传回客户端对应的信息
				while (name == null || outMap.containsKey(name)
						|| name.equals("")) {
					pw.println("N");
					name = br.readLine();
				}
				pw.println("Y");

				addMap(pw, br, name, s); // 将上线的客户端加入集合
				sendList(); // 更新在线列表
				// 显示对应的名字上线信息
				outText.insertString(outText.getLength(), sdf.format(new Date()).toString()
						+ "\n" + name + "上线了!\n", attr1);
				sendMsg(sdf.format(new Date()).toString() + "\n" + name + "上线了!");

				// 循环接收转发客户端发来的信息
				String msg;
				while ((msg = br.readLine()) != null) {
					if (msg.equals("PORT")) {
						msg = br.readLine();
						addPort(name, msg);
						msg = br.readLine();
					}
					if (msg.substring(0, 1).equals("/")) {
						String toName = msg.substring(1, msg.indexOf(":"));
						String msgTo = sdf.format(new Date()).toString() + "\n" + name
								+ "对你说:" + msg.substring(msg.indexOf(":") + 1);
						String msgFrom = sdf.format(new Date()).toString() + "\n你对"
								+ toName + "说:"
								+ msg.substring(msg.indexOf(":") + 1);
						// 向两方发送对应信息
						sendMsg(msgFrom, outMap.get(name));
						sendMsg(msgTo, outMap.get(toName));
					} else {
						sendMsg(sdf.format(new Date()).toString() + "\n" + name + "对大家说:"
								+ msg);
					}
				}
			} catch (Exception e) {
			} finally { // 当客户端失去连接时即为客户端下线,将客户端从集合移除并且关闭对应Socket,结束对应线程
				removeMsg(name, pw);
				if (s != null) {
					try {
						s.close();
						sendList();
						outText.insertString(outText.getLength(), sdf.format(new Date())
								.toString()
								+ "\n" + name + "下线了!\n", attr1);
						sendMsg(sdf.format(new Date()).toString() + "\n" + name + "下线了!");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
