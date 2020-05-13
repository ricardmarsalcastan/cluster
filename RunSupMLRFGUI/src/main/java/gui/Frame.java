package gui;

import csv.Csv;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import java.io.File;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.*;
import java.text.ParseException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;


public class Frame extends JFrame implements ActionListener
{
	JPanel gui;
	JPanel topComponents;
	JPanel southComponents;
	
	JButton openFileButton;
	JButton modifyFileButton;
	JButton startSupMLRFButton;
	
	public static boolean csvHasFinished;
	
	static JLabel statusLabel;
	
	Csv csv;
	
	Frame()
	{
		csvHasFinished = false;
		
		gui = new JPanel(new BorderLayout(5,5));
		
		topComponents = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 3,3));
		topComponents.setBorder(
                new TitledBorder("Preprocess") );
		
		openFileButton = new JButton("Open File");
		openFileButton.setActionCommand("OPENFILE");
		openFileButton.addActionListener(this);
		topComponents.add(openFileButton);
		
		modifyFileButton = new JButton("Modify File");
		modifyFileButton.setActionCommand("MODIFYFILE");
		modifyFileButton.addActionListener(this);
		modifyFileButton.setEnabled(false);
		topComponents.add(modifyFileButton);
		
		
		startSupMLRFButton = new JButton("Start SupMLRF");
		startSupMLRFButton.setActionCommand("STARTSUPMLRF");
		startSupMLRFButton.addActionListener(this);
		startSupMLRFButton.setEnabled(false);
		topComponents.add(startSupMLRFButton);
		
		gui.add(topComponents, BorderLayout.NORTH);
		
		southComponents = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 3,3));
		southComponents.setBorder(
                new TitledBorder("Status") );
		
		statusLabel = new JLabel();
		statusLabel.setEnabled(false);
		southComponents.add(statusLabel);
		
		gui.add(southComponents, BorderLayout.SOUTH);
		
		setContentPane(gui);
		
		setUpFrame();
	}
	
	private void setUpFrame()
	{
		Toolkit tk;
		Dimension d;

		tk = Toolkit.getDefaultToolkit();
		d = tk.getScreenSize();
		setSize(60 * d.width/100,  60 * d.height/100);
		setLocation(d.width/2-this.getSize().width/2, d.height/2-this.getSize().height/2);
		setTitle("SupervisedMLRF");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		
	}
	
	public static void changeLabel(String text)
	{
		statusLabel.setText(text);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getActionCommand().equals("OPENFILE"))
		{
			changeLabel("Openin File...");
            JFileChooser jFileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory()); 

            int result = jFileChooser.showOpenDialog(null); 
            
            if (result == JFileChooser.APPROVE_OPTION)   
            { 
            	String pathOfFileSelected = jFileChooser.getSelectedFile().getPath();
            	String nameOfFileSelected = jFileChooser.getSelectedFile().getName();

                csv = new Csv(pathOfFileSelected,nameOfFileSelected);
                modifyFileButton.setEnabled(true);
            } 
            changeLabel("Done.");
		}
		else if(e.getActionCommand().equals("MODIFYFILE"))
		{
			modifyFileButton.setEnabled(false);
			changeLabel("Modifing file...");
			csv.start();
			startSupMLRFButton.setEnabled(true);
		}
		else if(e.getActionCommand().equals("STARTSUPMLRF"))
		{
			try
			{
				
				Runtime.getRuntime().exec("cmd /c start cmd.exe /K \" cd C:\\Users\\ricar\\Documents\\Research_SupervisedMLRF\\Bash_File && bash\"");
				
			}
			catch(Exception ew)
			{
				ew.printStackTrace();
			}
		}
	}
}
