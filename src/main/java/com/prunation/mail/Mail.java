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
	private String emailFrom;
	private String emailTo;
	private String subject;
	private String host;
	private String port;
	private String username;
	private String password;
	private String infomation;
	private String logfilePath;

	public Mail() {
	}


	public Mail(String emailFrom, String emailTo,
			String subject, String host, String port, String username,
			String password, String infomation, String logfilePath) {
		this.emailFrom = emailFrom;
		this.emailTo = emailTo;
		this.subject = subject;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.infomation = infomation;
		this.logfilePath = logfilePath;
		System.out.println("emailfrom = " + emailFrom);
		System.out.println("emailto = " + emailTo);
		System.out.println("username = " + username);
		System.out.println("password = " + password);
		System.out.println("infomation = " + infomation);
		System.out.println("subject = " + subject);
		System.out.println("logfilePath = " + logfilePath);
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

	
	private class SMTPAuthenticator extends Authenticator
	{
	    public PasswordAuthentication getPasswordAuthentication()
	    {
	        return new PasswordAuthentication(username, password);
	    }
	}
	
	public void send() {
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
			String content = "Dear admin, \n\nPls, See about status of updating ODS data into Keycloak DB:\n\n"+ infomation;
			content+= "\n\n\n\nThanks & BR,\nSSO Team.";
			messageBodyPart.setText(content);
	        Multipart multipart = new MimeMultipart();
	        multipart.addBodyPart(messageBodyPart);
	        messageBodyPart = new MimeBodyPart();
	        DataSource source = new FileDataSource(new File(logfilePath));
	        messageBodyPart.setDataHandler(new DataHandler(source));
	        messageBodyPart.setFileName("server.log");
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


}
