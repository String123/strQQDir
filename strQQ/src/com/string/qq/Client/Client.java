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
 * �ͻ�����,�ܹ�����IP��ַ�Ͷ˿����ӷ����,Ĭ�϶˿�8088, �����ܹ�ʵʱ��ʾ�����б�,������ϢȺ��,ͨ��/[name]:�ķ�ʽ����˽��.
 * �ܹ����͵����ļ�,�ڷ����ļ������п��Լ�������
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
			g.drawString("������IP��ַ:", 17, 35);
			g.drawString("�˿�:", 220, 35);
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
	private static JButton connect = new JButton("���ӷ�����");
	private static JButton clear = new JButton("����");
	private static JButton send = new JButton("����(Enter)");
	private static JButton sendFile = new JButton("�����ļ�");
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
	 * ���ӷ�����
	 */
	public Client() {
		try {
			s = new Socket(IP, port);
			// �����ı�IO��
			serverOut = s.getOutputStream();
			serverOutWriter = new OutputStreamWriter(serverOut,"utf-8");
			serverPrinter = new PrintWriter(serverOutWriter, true);
			serverInput = s.getInputStream();
			serverInputReader = new InputStreamReader(serverInput,"utf-8");
			serverReader = new BufferedReader(serverInputReader);

			connectCheck = true; // ���ӱ�ʶ
		} catch (Exception e) {
			connectCheck = false;
			printOutText("ϵͳ:����������ʧ��,����IP��ַ�Ͷ˿�,����������!\n", attrRed);
		}
	}

	/**
	 * ������������ʱ���������Ϣ
	 * 
	 * @param msg
	 *            Ҫ�������Ϣ
	 * @param attr
	 *            �������Ϣ�ĸ�ʽ
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
			// ���û�������ͻ�������
			name = JOptionPane.showInputDialog("�������������:");

			serverPrinter.println(name); // �����ִ��������
			// ���մӷ���˷��������ֲ��Ϸ������ظ�����Ϣ
			while (serverReader.readLine().equals("N")) {
				name = JOptionPane
						.showInputDialog("������������ֲ��Ϸ������Ѵ���,�����������������:");
				serverPrinter.println(name);
			}
			frame.setTitle(name); // ���ͻ��˴������óɶ�Ӧ������
			// �������ı��߳�����Ϊ�ػ��̲߳��ҿ�ʼ�߳�
			ServerHandle ser = new ServerHandle();
			Thread thread = new Thread(ser);
			thread.setDaemon(true);
			execut.execute(thread);

			printOutText("ϵͳ:���!" + name + ",��ʼ�����!\n", attrRed);

			// ��ʼ�ļ����ռ���
			GetFileHandle fh = new GetFileHandle();
			execut.execute(fh);
			command();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * �����ļ�
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
			jfc.showDialog(new JLabel(), "ѡ���ļ�");
			file = jfc.getSelectedFile();
			if (file == null) {
				return;
			}
			fileName = file.getName();
			toName = JOptionPane.showInputDialog("��������Ҫ�����ļ���˭(�����б��еĲ��ܷ���):");
			while ((!nameList.contains(toName) || toName.equals("") || toName
					.equals(name))
					&& toName != null) {
				toName = JOptionPane.showInputDialog("��������ȷ��,���ߵ�����:");
			}
			if (toName == null) {
				return;
			}
			outByte = new byte[1024*8];

			printOutText("ϵͳ:���ڷ����ļ�" + fileName + "��" + toName + "...\n",
					attrRed);

			serverSocket = new Socket(IP, sendFilePort);
			toServer = new DataOutputStream(serverSocket.getOutputStream());
			toServer.writeUTF(name);
			toServer.writeUTF(toName);
			toServer.writeUTF(fileName);

			fromServer = new DataInputStream(serverSocket.getInputStream());
			symbol = fromServer.readUTF();
			if (symbol.equals("REFUSE")) {
				printOutText("ϵͳ:�ļ�����ʧ��!�Է������ļ�!\n", attrRed);
				file = null;
			} else {
				printOutText("ϵͳ:" + toName + "��ʼ�����ļ�!\n", attrRed);
			}

			fileInput = new FileInputStream(file);
			while ((len = fileInput.read(outByte, 0, outByte.length)) > 0) {
				toServer.write(outByte, 0, len);
				toServer.flush();
			}
			printOutText("ϵͳ:�ļ��������!\n", attrRed);
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
	 * ѭ�������ö˿�,
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
				printOutText("ϵͳ:���ڴ�" + fromName + "����" + fileName
						+ ",��ѡ�񱣴�λ��:\n", attrRed);
				jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jfc.showDialog(new JLabel(), "���浽");
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
				printOutText("ϵͳ:�ļ�" + fileName + "���ճɹ�\n", attrRed);
			} catch (Exception e) {
				printOutText("ϵͳ:�����ļ�!\n", attrRed);
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
	 * ���շ����������������б�,�յ�����Ϣ���ΪL��ʼ�����б�
	 * 
	 * @param msg
	 *            �ӷ���˽��յ�����Ϣ
	 * @param serverReader
	 *            ����˵�������
	 * @return �Ƿ���յ��б�
	 */
	public boolean getList() {
		boolean b = false;
		try {
			if (msg.equals("L")) {
				nameList = new ArrayList<String>();
				b = true;
				listText.remove(0, listText.getLength());
				listText.insertString(listText.getLength(), "�����б�\n", null);
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
	 * ����ʾ������ʾ���յ�����Ϣ
	 * 
	 * @param serverReader
	 *            ��������������
	 * @throws Exception
	 */
	public synchronized void showMsg() throws Exception {
		outText.insertString(outText.getLength(), msg + "\n", attrBlack);
		// bar.setValue(bar.getMaximum());
	}

	// "��ӭ����������,�밴���Ӱ�ť���ӷ�����!"
	public static void main(String[] args) throws Exception {
		StyleConstants.setForeground(attrRed, Color.RED);
		StyleConstants.setForeground(attrBlack, Color.BLACK);
		printOutText("ϵͳ:��ӭ����������,�밴���Ӱ�ť���ӷ�����!\n", attrRed);
		listText.insertString(listText.getLength(), "�����б�\n", null);

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
		 * ���ӷ������İ�ť
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
					printOutText("ϵͳ:������IP��ַ�Ͷ˿ں�!\n", attrRed);
				}
			}
		});

		JRootPane jr = frame.getRootPane();
		jr.setDefaultButton(send); // �س�ģ�ⰴ���Ͱ�ť
	}

	/**
	 * ���������ť�Ĺ���ʵ��
	 */
	public void command() {
		/**
		 * send��ť������Ϣ�������
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
		 * ������ť
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

		// ���·����ļ���ťѡ���ļ������͸��Է�
		sendFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SendFileHandle sh = new SendFileHandle();
				execut.execute(sh);
			}
		});

	}

	/**
	 * ʵʱ�����������������Ϣ���߳�
	 * 
	 * @author nbtarena
	 * 
	 */
	public class ServerHandle implements Runnable {

		@Override
		public void run() {
			try {
				// ѭ�����շ���˵���Ϣ�����ദ��
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
	 * ѭ�������ӷ�����,���մ������ļ����߳�
	 */
	private class GetFileHandle implements Runnable {

		@Override
		public void run() {
			getFile();
		}
	}

	/**
	 * �����ļ����߳�
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
