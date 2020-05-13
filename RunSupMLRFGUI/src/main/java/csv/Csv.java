package csv;

import gui.Frame;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class Csv implements Runnable
{
	String pathOfFile;
	String fileName;

	public Csv(String pathOfFile,String fileName)
	{
		this.pathOfFile = pathOfFile;
		this.fileName = fileName;
	}
	
	public void run()
	{
		try
		{
			this.changeDateAndLabel();
		}
		catch(ArrayIndexOutOfBoundsException aioobe)
		{
			Frame.changeLabel("Incorrect file selected.");
			//aioobe.printStackTrace();
		}
		catch(ParseException pe)
		{
			Frame.changeLabel("File already modified.");
			//pe.printStackTrace();
		}
		catch(FileNotFoundException fnfe)
		{
			Frame.changeLabel("Couldn't find the file.");
			//fnfe.printStackTrace();
		}
		catch(IOException ioe)
		{
			Frame.changeLabel("IOException.");
			//ioe.printStackTrace();
		}
		
	}
	
	public void start()
	{
		Thread t = new Thread(this);
		t.start();
	}

	public void printData(List<String[]> allData)
	{
		// print Data 
		for (String[] row : allData) 
		{ 
			for (String cell : row) 
			{ 
				System.out.print(cell + "\t"); 
			} 
			System.out.println(); 
		} 
	}

	public void changeDateAndLabel() throws ParseException, FileNotFoundException, IOException
	{
		int counter = 0;
		FileInputStream inputStream = null;
		FileOutputStream outputStream = new FileOutputStream("C:\\Users\\ricar\\Documents\\Research_SupervisedMLRF\\Testing_data\\" + "modified_" + fileName);
		Scanner sc = null;
		byte bytesArray[];
		try 
		{
			inputStream = new FileInputStream(this.pathOfFile);
			sc = new Scanner(inputStream, "UTF-8");
			
			String header = sc.nextLine() + "\n";
			bytesArray = header.getBytes();
			outputStream.write(bytesArray);
			
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				//System.out.println(counter);
				//System.out.println(line);
				String[] cells = line.split(",");
				
				//Convert String data to long data from Jan,1,1970
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				Date date = dateFormat.parse(cells[2]);
				long millis = date.getTime();
				cells[2] = String.valueOf(millis);

				//Change Benign for 0, Bot 1
				if(cells[79].equals("Benign"))
				{
					cells[79] = String.valueOf(0);
				}
				else if(cells[79].equals("Bot"))
				{
					cells[79] = String.valueOf(1);
				}
				else if(cells[79].equals("DoS attacks-SlowHTTPTest"))
				{
					cells[79] = String.valueOf(2);
				}
				else if(cells[79].equals("DoS attacks-Hulk"))
				{
					cells[79] = String.valueOf(3);
				}
				else if(cells[79].equals("Infilteration"))
				{
					cells[79] = String.valueOf(4);
				}
				else if(cells[79].equals("DoS attacks-GoldenEye"))
				{
					cells[79] = String.valueOf(5);
				}
				else if(cells[79].equals("FTP-BruteForce"))
				{
					cells[79] = String.valueOf(6);
				}
				else if(cells[79].equals("SSH-Bruteforce"))
				{
					cells[79] = String.valueOf(7);
				}
				else if(cells[79].equals("DDOS attack-HOIC"))
				{
					cells[79] = String.valueOf(8);
				}
				else if(cells[79].equals("DoS attacks-Slowloris"))
				{
					cells[79] = String.valueOf(9);
				}
				else if(cells[79].equals("Brute Force -Web"))
				{
					cells[79] = String.valueOf(10);
				}
				else if(cells[79].equals("Brute Force -XSS"))
				{
					cells[79] = String.valueOf(11);
				}
				else if(cells[79].equals("SQL Injection"))
				{
					cells[79] = String.valueOf(12);
				}
				else if(cells[79].equals("DDOS attack-LOIC-UDP"))
				{
					cells[79] = String.valueOf(13);
				}
				else
				{
					System.out.println(cells[79]);
				}
				
				
				
				String modifiedLine = String.join(",", cells) + "\n";
				bytesArray = modifiedLine.getBytes();
				outputStream.write(bytesArray);
				counter++;
			}
			// note that Scanner suppresses exceptions
			if (sc.ioException() != null) {
				throw sc.ioException();
			}
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (sc != null) {
				sc.close();
			}
			
		}
		Frame.changeLabel("Done..");
	}
}

