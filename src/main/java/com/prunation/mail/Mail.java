package com.prunation.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Mail {
	protected int noRecordInput;
	protected int noRecordProcessed;
	protected int noRecordNotProcessed;
	protected String remarkError;
	protected String processDuration;

	private String emailFrom;
	private String emailTo;
	private String subject;
	private String host;
	private String port;
	private String username;
	private String password;

	public Mail() {
	}

	public Mail(int noRecordInput, int noRecordProcessed,
			int noRecordNotProcessed, String remarkError, String processDuration) {
		super();
		this.noRecordInput = noRecordInput;
		this.noRecordProcessed = noRecordProcessed;
		this.noRecordNotProcessed = noRecordNotProcessed;
		this.remarkError = remarkError;
		this.processDuration = processDuration;
	}

	public Mail(int noRecordInput, int noRecordProcessed,
			int noRecordNotProcessed, String remarkError,
			String processDuration, String emailFrom, String emailTo,
			String subject, String host, String port, String username,
			String password) {
		super();
		this.noRecordInput = noRecordInput;
		this.noRecordProcessed = noRecordProcessed;
		this.noRecordNotProcessed = noRecordNotProcessed;
		this.remarkError = remarkError;
		this.processDuration = processDuration;
		this.emailFrom = emailFrom;
		this.emailTo = emailTo;
		this.subject = subject;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public String getProcessDuration() {
		return processDuration;
	}

	public void setprocessDuration(String processDuration) {
		this.processDuration = processDuration;
	}

	public int getNoRecordInput() {
		return noRecordInput;
	}

	public void setNoRecordInput(int noRecordInput) {
		this.noRecordInput = noRecordInput;
	}

	public int getNoRecordProcessed() {
		return noRecordProcessed;
	}

	public void setNoRecordProcessed(int noRecordProcessed) {
		this.noRecordProcessed = noRecordProcessed;
	}

	public int getNoRecordNotProcessed() {
		return noRecordNotProcessed;
	}

	public void setNoRecordNotProcessed(int noRecordNotProcessed) {
		this.noRecordNotProcessed = noRecordNotProcessed;
	}

	public String getRemarkError() {
		return remarkError;
	}

	public void setRemarkError(String remarkError) {
		this.remarkError = remarkError;
	}

	public String getEmailFrom() {
		return emailFrom;
	}

	public void setEmailFrom(String emailFrom) {
		this.emailFrom = emailFrom;
	}

	public String getEmailTo() {
		return emailTo;
	}

	public void setEmailTo(String emailTo) {
		this.emailTo = emailTo;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String toString() {
		String content = "No of rows (Input): " + noRecordInput + "\n";
		content += "No of Records (Processed): " + noRecordProcessed + "\n";
		content += "No of Records (Not Processed): "
				+ (noRecordInput - noRecordProcessed) + "\n";
		content += "ProcessDuration: " + processDuration + "\n";

		content += "Remark/Error: " + remarkError + "\n";

		return content;
	}

	
	private class SMTPAuthenticator extends Authenticator
	{
	    public PasswordAuthentication getPasswordAuthentication()
	    {
	        return new PasswordAuthentication(username, password);
	    }
	}
	
	public void send() {
		readConfig();
		try {

		    Properties props = new Properties();
			props.put("mail.smtp.user", this.username);
			props.put("mail.smtp.host", this.host);
			props.put("mail.smtp.port", this.port);
			props.put("mail.smtp.starttls.enable","true");
			props.put("mail.smtp.debug", "true");
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.socketFactory.port", this.port);
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.socketFactory.fallback", "false");

			SMTPAuthenticator auth = new SMTPAuthenticator();
			Session session = Session.getInstance(props, auth);
			session.setDebug(true);

			MimeMessage msg = new MimeMessage(session);
			
			msg.setSubject("[SSO] Updating ODS data into Keycloak DB status.");
			msg.setFrom(new InternetAddress(this.emailFrom));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(this.emailTo));
			
			BodyPart messageBodyPart = new MimeBodyPart();
			String content = "Dear admin, \n\nPls, See about status of updating ODS data into Keycloak DB:\n\n"+ this.toString();
			content+= "\n\n\n\nThanks & BR,\nSSO Team.";
			messageBodyPart.setText(content);
	        Multipart multipart = new MimeMultipart();
	        multipart.addBodyPart(messageBodyPart);
	        messageBodyPart = new MimeBodyPart();
	        DataSource source = new FileDataSource(new File(MailFactory.getRootFolder()+MailFactory.LOG_FILE));
	        messageBodyPart.setDataHandler(new DataHandler(source));
	        messageBodyPart.setFileName("log.txt");
	        multipart.addBodyPart(messageBodyPart);
	        
	        msg.setContent(multipart);
	        
			Transport transport = session.getTransport("smtps");
			transport.connect(this.host, 465, this.username, this.password);
			transport.sendMessage(msg, msg.getAllRecipients());
			transport.close();

		} catch (MessagingException e) {
			System.out.println("MessagingException - error : "+e.getMessage());
		} catch (Exception e) {
			System.out.println("MessagingException: "+e);
		}
	}

	public void readConfig() {
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(new File(MailFactory.getRootFolder()+MailFactory.CONFIG_FILE)));
			host = p.getProperty("SMTP_HOST");
			port = p.getProperty("SMTP_PORT");
			password = p.getProperty("SMTP_PASSWORD");
			emailFrom = p.getProperty("FROM");
			emailTo = p.getProperty("TO");
			subject = p.getProperty("SUBJECT");
			username = p.getProperty("SMTP_USER");

			System.out.println("SMTP_HOST = " + host);
			System.out.println("SMTP_PORT = " + port);
			// System.out.println("SMTP_PASSWORD = " + password);
			System.out.println("FROM = " + emailFrom);
			System.out.println("TO = " + emailTo);
			System.out.println("SUBJECT = " + subject);
		} catch (Exception e) {
			System.out.println("readConfig: "+ e);
		}
	}

}
