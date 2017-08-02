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
 * ���������,�򿪺��Զ����տͻ��˵�����,�ӿ�Ĭ��Ϊ8088, ���ڽ���ת���ͻ��˴�������Ϣ,������ܹ���ʾ�����б�,�����ܹ�����40���ͻ��˵�����Ⱥ��.
 * �ļ����͹���:������˿ڽ���ת���ļ�,������ת�������п����շ���Ϣ,���Դ������ļ�ת��
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
	private static JButton start = new JButton("����������");
	private Map<String, PrintWriter> outMap = new HashMap<String, PrintWriter>();
	private Map<String, Socket> outSocketMap = new HashMap<String, Socket>();
	private Map<String, Integer> outPortMap = new HashMap<String, Integer>();
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	ExecutorService execut = Executors.newFixedThreadPool(100);

	/**
	 * �����ߵĿͻ��˵����ֺ������������Ӧ��HashMap��,�������������Ⱥ������.
	 * 
	 * @param p
	 *            ���ߵ������
	 * @param br
	 *            ���ߵ�������
	 * @param name
	 *            ���ߵ�����
	 */
	public synchronized void addMap(PrintWriter p, BufferedReader br,
			String name, Socket s) {
		outMap.put(name, p);
		outSocketMap.put(name, s);
		allOut.add(p);
	}

	/**
	 * ���ͻ��˶�Ӧ�Ľ����ļ��Ķ˿ڴ���Map��ͻ������ֶ�Ӧ
	 */
	public synchronized void addPort(String name, String msg) {
		Integer getFilePort = Integer.valueOf(msg);
		outPortMap.put(name, getFilePort);
	}

	/**
	 * �����ߵĿͻ��˴�Ⱥ�������Ƴ�
	 * 
	 * @param name
	 *            ���ߵĿͻ��˵�����
	 * @param p
	 *            ���ߵĿͻ��˵������
	 */
	public synchronized void removeMsg(String name, PrintWriter p) {
		outMap.remove(name);
		allOut.remove(p);
	}

	/**
	 * Ⱥ����Ϣ����
	 * 
	 * @param msg
	 *            ҪȺ������Ϣ
	 */
	public synchronized void sendMsg(String msg) {
		for (PrintWriter p : allOut) {
			p.println(msg);
		}
	}

	/**
	 * ������Ϣ����
	 * 
	 * @param msg
	 *            Ҫ���͵���Ϣ
	 * @param p
	 *            Ҫ�����Ŀͻ��˵������
	 */
	public synchronized void sendMsg(String msg, PrintWriter p) {
		p.println(msg);
	}

	/**
	 * ���·���˺Ϳͻ��˵������б�
	 * 
	 * @throws Exception
	 */
	public void sendList() throws Exception {
		Set<String> names = outMap.keySet();
		listText.remove(0, listText.getLength());
		listText.insertString(listText.getLength(), "�����б�\n", null);
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
						+ "\n�ȴ��ͻ�������....\n", attr1);
				Socket s = receivePortSocket.accept();
				outText.insertString(outText.getLength(), sdf.format(new Date()).toString()
						+ "\n�ͻ���������\n", attr1);

				ClientHandler ch = new ClientHandler(s);
				execut.execute(ch);
				// ����8087�ļ�����˿�,ѭ������ת���ͻ��˴������ļ�
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
				+ "\n�����������\n", attr1);
		outText
				.insertString(outText.getLength(), sdf.format(new Date()).toString()
						+ "\n����IP��ַΪ:" + ServerUtil.getLocalInetAddress()
						+ "\n", attr1);
		outText.insertString(outText.getLength(), sdf.format(new Date()).toString()
				+ "\n�������ն˿�Ϊ:" + receivePort + "\n", attr1);

		listText.insertString(listText.getLength(), "�����б�\n", null);

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
	 * ���ڽ���ת���ļ�
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
				fs = filePortSocket.accept(); // �ȴ����Ϳͻ��˵�����
				fromNameClient = new DataInputStream(fs.getInputStream()); // ���Ϳͻ��˵�����������
				toNameClient = new DataOutputStream(fs.getOutputStream()); // ���Ϳͻ��˵����������

				name = fromNameClient.readUTF(); // ���շ��Ϳͻ��˵�����
				toname = fromNameClient.readUTF(); // ������Ҫ���͵�������
				fname = fromNameClient.readUTF(); // �����ļ���

				// sendMsg("F", outMap.get(toname)); //����Ҫ���͵����ֵĿͻ��˷����ļ�����

				String tonameIP = outSocketMap.get(toname).getInetAddress()
						.toString().substring(1); // ��ȡ��Ҫ���͵Ŀͻ��˵�IP��ַ
				Integer tonamePort = outPortMap.get(toname);

				tofs = new Socket(tonameIP, tonamePort); // ���Ӹõ�ַ�Ľ����ļ��˿�
				toToNameClient = new DataOutputStream(tofs.getOutputStream()); // �����͵Ŀͻ��˵����������
				fromToNameClient = new DataInputStream(tofs.getInputStream()); // �����͵Ŀͻ��˵�����������

				toToNameClient.writeUTF(name); // �򱻷��͵Ŀͻ�������ļ��ķ��Ϳͻ��˵�����
				toToNameClient.writeUTF(fname); // �򱻷��͵Ŀͻ�������ļ���

				synchronized (outText) { // �򴰿����ת����Ϣ
					outText.insertString(outText.getLength(), sdf.format(new Date())
							.toString()
							+ "\n"
							+ "����ת���ļ�"
							+ fname
							+ "��"
							+ name
							+ "��"
							+ toname + "...\n", attr1);
				}

				// ���ձ�ʶ,�ж��ļ��Ƿ���
				symbol = fromToNameClient.readUTF();
				toNameClient.writeUTF(symbol);
				if (symbol.equals("REFUSE")) {
					synchronized (outText) { // �򴰿����ת����Ϣ
						outText.insertString(outText.getLength(), sdf.format(new Date())
								.toString()
								+ "\n" + toname + "�����ļ�!\n", attr1);
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
					synchronized (outText) { // �򴰿����ת����Ϣ
						outText.insertString(outText.getLength(), sdf.format(new Date())
								.toString()
								+ "\n" + "�ļ���ʼ����!\n", attr1);
					}
				}
				len = 0;
				inByte = new byte[1024 * 8];
				while (true) { // ѭ���ӷ��Ͷ˶�ȡ�ļ�������������Ϳͻ���
					if (fromNameClient != null) {
						len = fromNameClient.read(inByte, 0, inByte.length);
					}
					if (len == -1) {
						break;
					}
					toToNameClient.write(inByte, 0, len);
					toToNameClient.flush();
				}
				synchronized (outText) { // �򴰿����ת����Ϣ
					outText.insertString(outText.getLength(), sdf.format(new Date())
							.toString()
							+ "\n" + "�ļ�ת�����!\n", attr1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally { // �ر�IO��������
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
	 * �������ת���ļ����߳�
	 */
	private class FileHandler implements Runnable {

		@Override
		public void run() {
			forward();
		}

	}

	/**
	 * ����ͻ������ߺ�ķ����߳��ڲ���,���̳߳�������ÿ���߳�.
	 * 
	 */
	private class ClientHandler implements Runnable {
		Socket s;

		/**
		 * �����ߵĿͻ��˵�Socket����
		 * 
		 * @param s
		 *            ���ߵĿͻ��˵�Socket
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
				name = br.readLine(); // ��ȡ�ͻ��˴���������

				// �ж������Ƿ�Ϸ������ظ�,���ؿͻ��˶�Ӧ����Ϣ
				while (name == null || outMap.containsKey(name)
						|| name.equals("")) {
					pw.println("N");
					name = br.readLine();
				}
				pw.println("Y");

				addMap(pw, br, name, s); // �����ߵĿͻ��˼��뼯��
				sendList(); // ���������б�
				// ��ʾ��Ӧ������������Ϣ
				outText.insertString(outText.getLength(), sdf.format(new Date()).toString()
						+ "\n" + name + "������!\n", attr1);
				sendMsg(sdf.format(new Date()).toString() + "\n" + name + "������!");

				// ѭ������ת���ͻ��˷�������Ϣ
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
								+ "����˵:" + msg.substring(msg.indexOf(":") + 1);
						String msgFrom = sdf.format(new Date()).toString() + "\n���"
								+ toName + "˵:"
								+ msg.substring(msg.indexOf(":") + 1);
						// ���������Ͷ�Ӧ��Ϣ
						sendMsg(msgFrom, outMap.get(name));
						sendMsg(msgTo, outMap.get(toName));
					} else {
						sendMsg(sdf.format(new Date()).toString() + "\n" + name + "�Դ��˵:"
								+ msg);
					}
				}
			} catch (Exception e) {
			} finally { // ���ͻ���ʧȥ����ʱ��Ϊ�ͻ�������,���ͻ��˴Ӽ����Ƴ����ҹرն�ӦSocket,������Ӧ�߳�
				removeMsg(name, pw);
				if (s != null) {
					try {
						s.close();
						sendList();
						outText.insertString(outText.getLength(), sdf.format(new Date())
								.toString()
								+ "\n" + name + "������!\n", attr1);
						sendMsg(sdf.format(new Date()).toString() + "\n" + name + "������!");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
