package com.prunation.mail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

public class MailFactory {
	public static final String USER_NUMBER_RECORDS_INPUT = "no_of_user_rows_input";
	public static final String USER_NUMBER_RECORDS_PROCESSED = "no_of_user_records_processed";
	public static final String ROLE_NUMBER_RECORDS_INPUT = "no_of_role_rows_input";
	public static final String ROLE_NUMBER_RECORDS_PROCESSED = "no_of_role_records_processed";
	
//	public static String ROOT_FOLDER = "/Users/nvan/Documents/EAP-6.3.0/jboss-eap-6.3/standalone/deployments/Struts2Example.war/WEB-INF/classes/";
	public static String LOG_FILE = "log.txt";
	public static String CONFIG_FILE = "config.ini";
	
	public static String getRootFolder() throws IOException{
		Properties prop = new Properties();
		String propFileName = "config.properties";
 
		InputStream inputStream = MailFactory.class.getClassLoader().getResourceAsStream("configRootFolder.ini");
 
		if (inputStream != null) {
			prop.load(inputStream);
		} else {
			throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
		}
 
		// get the property value and print it out
		String Test_ROOT_FOLDER = prop.getProperty("ROOT_FOLDER");
		System.out.println("Test_ROOT_FOLDER: "+Test_ROOT_FOLDER);
		return Test_ROOT_FOLDER;
	}
	
	public static Mail setupMailBox() {
		
		InputStream fstream;
		Mail emailContent = new Mail();
		try {
			fstream = MailFactory.class.getClassLoader().getResourceAsStream(LOG_FILE);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));
			// ProcessDuration, Remark/Error
			String strLine="";
			String error="";
			int iNumberInput = 0;
			int iNumberProcessed = 0;
			int waitNext = 0;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				if (strLine.contains(USER_NUMBER_RECORDS_INPUT)
						|| strLine.contains(ROLE_NUMBER_RECORDS_INPUT)) {
					waitNext = 1;
				} else if (strLine.contains(USER_NUMBER_RECORDS_PROCESSED)
						|| strLine.contains(ROLE_NUMBER_RECORDS_PROCESSED)) {
					waitNext = 2;
				} else if (strLine.toLowerCase().contains("error") || strLine.toLowerCase().contains("fatal")){
					waitNext = 0;
					error+= new Scanner(fstream).useDelimiter("\\Z").next();
					break;
				}else {
					if (waitNext == 1) {
						if(strLine.contains("---------") || strLine.contains("row") || strLine.equals("")){
							continue;
						}
						iNumberInput += Integer.parseInt(strLine.trim());
					}else if (waitNext == 2) {
						if(strLine.contains("---------") || strLine.contains("row") || strLine.equals("")){
							continue;
						}
						iNumberProcessed += Integer.parseInt(strLine.trim());
					}
				}
			}
			
			
			emailContent.setNoRecordInput(iNumberInput);
			emailContent.setNoRecordNotProcessed(iNumberInput - iNumberProcessed);
			emailContent.setNoRecordProcessed(iNumberProcessed);
			emailContent.setRemarkError(error);
			
			// Close the input stream
			br.close();
			
			
		} catch (Exception e) {
			System.out.println(e);
		}
		
		return emailContent;
	}
}
