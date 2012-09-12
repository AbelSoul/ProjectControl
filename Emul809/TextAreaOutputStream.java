import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * This class creates an output window showing system output
 * 
 * @author Robert Wilson
 *
 */
@SuppressWarnings("serial")
public class TextAreaOutputStream extends JFrame {

	private static JTextArea textArea = new JTextArea();

	public TextAreaOutputStream() {

		redirectSystemStreams();
		setVisible(true);
		this.setAlwaysOnTop(true);
		JPanel conPanel = new JPanel();
		add(conPanel, BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("Console Output");
		setSize(320, 300);
		setLocation(10, 350);
		textArea.setEditable(false);
		add(textArea, BorderLayout.CENTER);

		JScrollPane conScroll = new JScrollPane(textArea);

		add(conScroll, BorderLayout.CENTER);
		textArea.setFont(new Font("Courier", Font.PLAIN, 14));

	}

	public void updateTextArea(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				textArea.append(text);
			}
		});
	}

	public void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		System.setOut(new PrintStream(out, true));
	}

}