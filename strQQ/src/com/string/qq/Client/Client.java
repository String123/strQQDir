package com.string.qq.Client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * 客户端类,能够按照IP地址和端口连接服务端,默认端口8088, 并且能够实时显示在线列表,发送消息群聊,通过/[name]:的方式进行私聊.
 * 能够发送单个文件,在发送文件过程中可以继续聊天
 * 
 * @author String
 * 
 */
@SuppressWarnings("serial")
public class Client {
	private Socket s = null;
	private static JFrame frame = new JFrame("QQ");
	private static JPanel panel = new JPanel() {
		@Override
		public void paint(Graphics g) {
			g.drawString("服务器IP地址:", 17, 35);
			g.drawString("端口:", 220, 35);
		}

	};
	private static JTextField inputText = new JTextField();
	private static JTextField ipText = new JTextField("176.24.3.50");
	private static JTextField portText = new JTextField("8088");
	private static JTextPane txt = new JTextPane();
	private static JScrollPane outputText = new JScrollPane(txt);
	private static StyledDocument outText = txt.getStyledDocument();
	private static JTextPane txt2 = new JTextPane();
	private static JScrollPane list = new JScrollPane(txt2);
	private static StyledDocument listText = txt2.getStyledDocument();
	private static SimpleAttributeSet attrRed = new SimpleAttributeSet();
	private static SimpleAttributeSet attrBlack = new SimpleAttributeSet();
	private static JButton connect = new JButton("连接服务器");
	private static JButton clear = new JButton("清屏");
	private static JButton send = new JButton("发送(Enter)");
	private static JButton sendFile = new JButton("发送文件");
	private static Integer port, sendFilePort = 8087;
	private static String IP;

	private OutputStream serverOut;
	private OutputStreamWriter serverOutWriter;
	private static PrintWriter serverPrinter = null;
	private InputStream serverInput;
	private InputStreamReader serverInputReader;
	private static BufferedReader serverReader = null;
	private static List<String> nameList = new ArrayList<String>();

	private static boolean connectCheck = false;
	private static String msg = null, name = null, fileName = null;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private ExecutorService execut = Executors.newFixedThreadPool(100);

	/**
	 * 连接服务器
	 */
	public Client() {
		try {
			s = new Socket(IP, port);
			// 创建文本IO流
			serverOut = s.getOutputStream();
			serverOutWriter = new OutputStreamWriter(serverOut,"utf-8");
			serverPrinter = new PrintWriter(serverOutWriter, true);
			serverInput = s.getInputStream();
			serverInputReader = new InputStreamReader(serverInput,"utf-8");
			serverReader = new BufferedReader(serverInputReader);

			connectCheck = true; // 连接标识
		} catch (Exception e) {
			connectCheck = false;
			printOutText("系统:服务器连接失败,请检查IP地址和端口,并重新连接!\n", attrRed);
		}
	}

	/**
	 * 向输出框输出带时间的文字信息
	 * 
	 * @param msg
	 *            要输出的信息
	 * @param attr
	 *            输出的信息的格式
	 */
	public synchronized static void printOutText(String msg,
			SimpleAttributeSet attr) {
		try {
			outText.insertString(outText.getLength(), sdf.format(new Date())
					+ "\n" + msg, attr);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		try {
			// 从用户处输入客户端名字
			name = JOptionPane.showInputDialog("请输入你的名字:");

			serverPrinter.println(name); // 将名字传给服务端
			// 接收从服务端发来的名字不合法或者重复的信息
			while (serverReader.readLine().equals("N")) {
				name = JOptionPane
						.showInputDialog("你所输入的名字不合法或者已存在,请重新输入你的名字:");
				serverPrinter.println(name);
			}
			frame.setTitle(name); // 将客户端窗口设置成对应的名字
			// 将监听文本线程设置为守护线程并且开始线程
			ServerHandle ser = new ServerHandle();
			Thread thread = new Thread(ser);
			thread.setDaemon(true);
			execut.execute(thread);

			printOutText("系统:你好!" + name + ",开始聊天吧!\n", attrRed);

			// 开始文件接收监听
			GetFileHandle fh = new GetFileHandle();
			execut.execute(fh);
			command();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 发送文件
	 */
	public void sendFile() {
		File file = null;
		String fileName, toName, symbol;
		DataOutputStream toServer = null;
		DataInputStream fromServer = null;
		FileInputStream fileInput = null;
		Socket serverSocket = null;
		byte[] outByte = null;
		JFileChooser jfc;
		int len = 0;
		try {
			jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.showDialog(new JLabel(), "选择文件");
			file = jfc.getSelectedFile();
			if (file == null) {
				return;
			}
			fileName = file.getName();
			toName = JOptionPane.showInputDialog("请输入你要发送文件给谁(在线列表中的才能发送):");
			while ((!nameList.contains(toName) || toName.equals("") || toName
					.equals(name))
					&& toName != null) {
				toName = JOptionPane.showInputDialog("请输入正确的,在线的名字:");
			}
			if (toName == null) {
				return;
			}
			outByte = new byte[1024*8];

			printOutText("系统:正在发送文件" + fileName + "给" + toName + "...\n",
					attrRed);

			serverSocket = new Socket(IP, sendFilePort);
			toServer = new DataOutputStream(serverSocket.getOutputStream());
			toServer.writeUTF(name);
			toServer.writeUTF(toName);
			toServer.writeUTF(fileName);

			fromServer = new DataInputStream(serverSocket.getInputStream());
			symbol = fromServer.readUTF();
			if (symbol.equals("REFUSE")) {
				printOutText("系统:文件发送失败!对方拒收文件!\n", attrRed);
				file = null;
			} else {
				printOutText("系统:" + toName + "开始接收文件!\n", attrRed);
			}

			fileInput = new FileInputStream(file);
			while ((len = fileInput.read(outByte, 0, outByte.length)) > 0) {
				toServer.write(outByte, 0, len);
				toServer.flush();
			}
			printOutText("系统:文件发送完毕!\n", attrRed);
		} catch (Exception e) {
		} finally {
			try {
				if (toServer != null) {
					toServer.close();
				}
				if (fileInput != null) {
					fileInput.close();
				}
				if (serverSocket != null) {
					serverSocket.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 循环检测可用端口,
	 */
	@SuppressWarnings("resource")
	public void getFile() {
		DataInputStream fromServer = null;
		DataOutputStream toServer = null;
		FileOutputStream fos = null;
		ServerSocket receviePort = null;
		Socket fs = null;
		File file = null;
		String fromName;
		Random r = null;
		boolean portCheck = false;
		int getFilePort = 0;
		byte[] inByte = new byte[1024*8];
		JFileChooser jfc;
		int len = 0;

		r = new Random();
		while (!portCheck) {
			try {
				getFilePort = r.nextInt(65535) + 1;
				receviePort = new ServerSocket(getFilePort);
				portCheck = true;
			} catch (Exception e) {
				portCheck = false;
			}
		}
		serverPrinter.println("PORT");
		serverPrinter.println(getFilePort);

		while (true) {
			try {
				fs = receviePort.accept();
				fromServer = new DataInputStream(fs.getInputStream());
				toServer = new DataOutputStream(fs.getOutputStream());
				fromName = fromServer.readUTF();
				fileName = fromServer.readUTF();
				printOutText("系统:正在从" + fromName + "接收" + fileName
						+ ",请选择保存位置:\n", attrRed);
				jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jfc.showDialog(new JLabel(), "保存到");
				file = new File(jfc.getSelectedFile().getPath()
						+ File.separator + fileName);
				if (file != null) {
					toServer.writeUTF("ACCEPT");
				}
				if (!file.exists()) {
					file.createNewFile();
				}
				fos = new FileOutputStream(file);
				len = 0;
				while (true) {
					if (fromServer != null) {
						len = fromServer.read(inByte, 0, inByte.length);
					}
					if (len == -1) {
						break;
					}
					fos.write(inByte, 0, len);
					fos.flush();
				}
				printOutText("系统:文件" + fileName + "接收成功\n", attrRed);
			} catch (Exception e) {
				printOutText("系统:拒收文件!\n", attrRed);
				try {
					toServer.writeUTF("REFUSE");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} finally {
				try {
					if (fromServer != null) {
						fromServer.close();
					}
					if (fos != null) {
						fos.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * 接收服务器传来的在线列表,收到的信息如果为L则开始接收列表
	 * 
	 * @param msg
	 *            从服务端接收到的信息
	 * @param serverReader
	 *            服务端的输入流
	 * @return 是否接收到列表
	 */
	public boolean getList() {
		boolean b = false;
		try {
			if (msg.equals("L")) {
				nameList = new ArrayList<String>();
				b = true;
				listText.remove(0, listText.getLength());
				listText.insertString(listText.getLength(), "在线列表\n", null);
				msg = serverReader.readLine();
				while (!msg.equals("E")) {
					nameList.add(msg);
					listText.insertString(listText.getLength(), msg + "\n",
							null);
					msg = serverReader.readLine();
				}
				msg = serverReader.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return b;
	}

	/**
	 * 在显示窗口显示接收到的信息
	 * 
	 * @param serverReader
	 *            服务器的输入流
	 * @throws Exception
	 */
	public synchronized void showMsg() throws Exception {
		outText.insertString(outText.getLength(), msg + "\n", attrBlack);
		// bar.setValue(bar.getMaximum());
	}

	// "欢迎来到聊天室,请按连接按钮连接服务器!"
	public static void main(String[] args) throws Exception {
		StyleConstants.setForeground(attrRed, Color.RED);
		StyleConstants.setForeground(attrBlack, Color.BLACK);
		printOutText("系统:欢迎来到聊天室,请按连接按钮连接服务器!\n", attrRed);
		listText.insertString(listText.getLength(), "在线列表\n", null);

		frame.setSize(500, 600);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.add(panel);
		frame.setVisible(true);

		Font f = new Font(Font.SANS_SERIF, 0, 20);
		Font f2 = new Font(Font.SANS_SERIF, 0, 15);
		inputText.setBounds(17, 480, 350, 40);
		inputText.setFont(f);
		ipText.setBounds(100, 20, 100, 30);
		portText.setBounds(255, 20, 50, 30);
		outputText.setBounds(17, 60, 350, 400);
		txt.setFont(f);
		txt.setEditable(false);
		list.setBounds(375, 60, 110, 400);
		txt2.setFont(f2);
		txt2.setEditable(false);
		connect.setBounds(17, 525, 100, 30);
		send.setBounds(250, 525, 117, 30);
		clear.setBounds(150, 525, 80, 30);
		sendFile.setBounds(375, 480, 110, 40);

		frame.setLayout(null);
		frame.add(inputText);
		frame.add(ipText);
		frame.add(portText);
		frame.add(outputText);
		frame.add(list);
		frame.add(connect);
		frame.add(send);
		frame.add(clear);
		frame.add(sendFile);

		clear.setEnabled(false);
		send.setEnabled(false);
		sendFile.setEnabled(false);

		/**
		 * 连接服务器的按钮
		 */
		connect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!(ipText.getText().equals("") || portText.getText().equals(
						""))) {
					IP = ipText.getText();
					port = Integer.valueOf(portText.getText());
					Client c = new Client();
					if (!connectCheck) {
						return;
					}
					c.start();
					connect.setEnabled(false);
					ipText.setEditable(false);
					portText.setEditable(false);
					clear.setEnabled(true);
					send.setEnabled(true);
					sendFile.setEnabled(true);
				} else {
					printOutText("系统:请输入IP地址和端口号!\n", attrRed);
				}
			}
		});

		JRootPane jr = frame.getRootPane();
		jr.setDefaultButton(send); // 回车模拟按发送按钮
	}

	/**
	 * 处理各个按钮的功能实现
	 */
	public void command() {
		/**
		 * send按钮发送消息给服务端
		 */
		send.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if ((inputText.getText() != null)
						&& (!inputText.getText().equals(""))) {
					serverPrinter.println(inputText.getText());
					inputText.setText("");
				}
			}
		});

		/**
		 * 清屏按钮
		 */
		clear.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					synchronized (outText) {
						outText.remove(0, outText.getLength());
					}
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
		});

		// 按下发送文件按钮选择文件并发送给对方
		sendFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SendFileHandle sh = new SendFileHandle();
				execut.execute(sh);
			}
		});

	}

	/**
	 * 实时处理服务器传来的消息的线程
	 * 
	 * @author nbtarena
	 * 
	 */
	public class ServerHandle implements Runnable {

		@Override
		public void run() {
			try {
				// 循环接收服务端的信息并分类处理
				while (true) {
					msg = serverReader.readLine();
					getList();
					showMsg();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 循环监听接服务器,接收传来的文件的线程
	 */
	private class GetFileHandle implements Runnable {

		@Override
		public void run() {
			getFile();
		}
	}

	/**
	 * 发送文件的线程
	 */
	private class SendFileHandle implements Runnable {

		@Override
		public void run() {
			System.out.println(1);
			sendFile();
			System.gc();
		}
	}
}
