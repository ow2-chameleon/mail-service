/*
 * Copyright 2009 OW2 Chameleon
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ow2.chameleon.mail.impl;

import java.io.File;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailSenderService;

/**
 * Mail Sender Service Implementation based on SMTP.
 */
@Component(immediate=true, name="org.ow2.chameleon.mail.smtp")
@Provides
public class SMTPMailSender implements MailSenderService {

	/**
	 * Type of connections.
	 */
	public enum Connection {
		NO_AUTH,
		TLS,
		SSL
	}

	/**
	 * The event admin if present.
	 */
	@Requires(optional=true, nullable=false, proxy=false)
	private EventAdmin m_eventAdmin;

	/**
	 * Configuration properties.
	 */
	private Properties m_properties;

	/**
	 * Enables / Disabled debugging.
	 */
	@Property(name="smtp.debug", value="false")
	private boolean m_debug;

	/**
	 * The mail address of the sender.
	 */
	@Property(name="smtp.from", mandatory=true)
	@ServiceProperty(name=MailSenderService.FROM_PROPERTY)
	private String m_from;

	/**
	 * The port.
	 */
	@Property(name="smtp.port", mandatory=true)
	private int m_port;

	/**
	 * The host.
	 */
	@Property(name="smtp.host", mandatory=true)
	private String m_host;

	/**
	 * Does quit should wait until termination.
	 */
	@Property(name="smtp.quitwait", value="false")
	private boolean m_quitWait;

	/**
	 * Enables /Disables SMTPS.
	 */
	@Property(name="smtp.useSMTPS", value="false")
	private boolean m_useSMTPS;


	/**
	 * The username.
	 */
	@Property(name="smtp.username")
	private String m_username;

	/**
	 * The password.
	 */
	@Property(name="smtp.password")
	private String m_password;

	/**
	 * The authenticator used for SSL.
	 */
	private Authenticator sslAuthentication;

	/**
	 * The connection type.
	 */
	@Property(name="smtp.connection", mandatory=true)
	private Connection m_connection;


	/**
	 * Creates a SMTPMailSender for testing purpose.
	 * @param connection the connection
	 * @param host the host
	 * @param port the port
	 * @param from the sender email address
	 * @param username the username
	 * @param password the password
	 * @param quitwait if the receiver must wait until termination completion
	 * @param useSMTPs enables SMTPs
	 */
	public SMTPMailSender(Connection connection, String host, int port, String from,
			final String username, final String password, boolean quitwait, boolean useSMTPs) {

		this.m_from = from;

		this.m_connection = connection;
		this.m_username = username;
		this.m_password = password;

		this.m_useSMTPS = useSMTPs;
		this.m_port = port;
		this.m_host = host;

		m_debug = true;
		this.m_quitWait = quitwait;
		this.m_useSMTPS = useSMTPs;
		configure();
	}

	/**
	 * Creates a SMTPMailSender (used by iPOJO).
	 */
	public SMTPMailSender() {
		configure();
	}

	/**
	 * Configures the sender.
	 */
	private void configure() {
		m_properties = new Properties();
		m_properties.put("mail.smtp.host", m_host);
		m_properties.put("mail.smtp.port", Integer.toString(m_port));

		m_properties.put("mail.smtps.quitwait", m_quitWait);
		switch (m_connection) {
		case SSL:
		    m_properties.put("mail.smtp.auth", Boolean.toString(true));
			m_properties.put("mail.smtp.socketFactory.port", Integer.toString(m_port));
			m_properties.put("mail.smtp.socketFactory.class", javax.net.ssl.SSLSocketFactory.class.getName());
		    sslAuthentication = new javax.mail.Authenticator() {
				protected javax.mail.PasswordAuthentication getPasswordAuthentication(){
					return new javax.mail.PasswordAuthentication(m_username, m_password);
				}
			};
		    break;
		case TLS:
			m_properties.put("mail.smtp.auth", Boolean.toString(true));
	    	m_properties.put("mail.smtp.starttls.enable", Boolean.toString(true));
	    	break;
		case NO_AUTH:
			m_properties.put("mail.smtp.auth", Boolean.toString(false));
		}
	}

	/**
	 * Sends a mail.
	 * @param to to
	 * @param cc cc
	 * @param subject subject
	 * @param body body
	 * @throws Exception if the mail cannot be sent.
	 * @see org.ow2.chameleon.mail.MailSenderService#send(java.lang.String, java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	public void send(String to, String cc, String subject, String body)
			throws Exception {
		send(to, cc, subject, body, null);
	}

	/**
	 * Sends a mail
	 * @param to to
	 * @param cc cc
	 * @param subject subject
	 * @param body body
	 * @param attachments list of attachments
	 * @throws Exception if the mail cannot be sent
	 * @see org.ow2.chameleon.mail.MailSenderService#send(java.lang.String, java.lang.String, java.lang.String,
	 * java.lang.String, java.util.List)
	 */
	public void send(String to, String cc, String subject, String body,
			List<File> attachments) throws Exception {
		if (attachments != null  && ! attachments.isEmpty()) {
			send(new Mail()
				.to(to)
				.cc(cc)
				.subject(subject)
				.body(body)
				.attach(attachments));
		} else {
			send(new Mail()
				.to(to)
				.cc(cc)
				.subject(subject)
				.body(body));
		}
	}

	/**
	 * Sends the given mail. This method really send the mail, others
	 * are just front-ends.
	 * @param mail the mail
	 * @throws Exception if the mail cannot be sent.
	 */
	private void process(Mail mail) throws Exception {
		if (mail.to() == null  || mail.to().isEmpty()) {
			throw new NullPointerException("The given 'to' is null or empty");
		}

		Session session = Session.getInstance(m_properties, sslAuthentication);

		session.setDebug(m_debug);
		// create a message
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(m_from));

		// Manage to.
		List<String> to = mail.to();
		InternetAddress[] address = new InternetAddress[to.size()];
		for (int index = 0; index < to.size(); index++) {
			String t = to.get(index);
			if (t == null) {
				throw new NullPointerException("A 'to' address is null");
			} else {
				address[index] = new InternetAddress(t);
			}
		}
		msg.setRecipients(Message.RecipientType.TO, address);

		// Manage cc.
		List<String> cc = mail.cc();
		InternetAddress[] addressCC = new InternetAddress[cc.size()];
		for (int index = 0; index < cc.size(); index++) {
			String t = cc.get(index);
			if (t == null) {
				throw new NullPointerException("A 'cc' address is null");
			} else {
				addressCC[index] = new InternetAddress(t);
			}
		}
		msg.setRecipients(Message.RecipientType.CC, addressCC);


		msg.setSubject(mail.subject());

		Date sent = new Date();
		msg.setSentDate(sent);
		if (mail != null) {
			mail.sent(sent);
		}


		// create the Multipart and its parts to it
		Multipart mp = new MimeMultipart();

		// create and fill the first message part
		MimeBodyPart mbp1 = new MimeBodyPart();
		mbp1.setText(mail.body());
		mp.addBodyPart(mbp1);

		List<File> attachments = mail.attachments();
		if (attachments != null  && ! attachments.isEmpty()) {
			for (File file : attachments) {
				MimeBodyPart part = new MimeBodyPart();
				DataSource source = new FileDataSource(file);
	            part.setDataHandler(new DataHandler(source));
	            part.setFileName(file.getName());
	            mp.addBodyPart(part);
			}
		}


		// add the Multipart to the message
		msg.setContent(mp);
		// send the message
		Transport transport;
		if (m_useSMTPS) {
			transport = session.getTransport("smtps");
		} else {
			transport = session.getTransport("smtp");
		}

		if (m_connection == Connection.TLS) {
		    transport.connect("smtp.gmail.com",
		    		m_port, m_username, m_password);
		} else {
			transport.connect();
		}

		try {
			transport.sendMessage(msg, msg.getAllRecipients());
			propagateToEventAdmin(mail, msg, null);
		} catch (SendFailedException ex) {
			propagateToEventAdmin(mail, msg, ex);
			throw ex;
		} finally {
			transport.close();
		}
	}

	/**
	 * Propagates the given mail to the event admin if available
	 * @param mail the mail
	 * @param msg the message
	 * @param ex the exception if any
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void propagateToEventAdmin(Mail mail, MimeMessage msg, SendFailedException ex) {
		if (m_eventAdmin != null) {
			Dictionary m = new Hashtable();
			m.put(MailSenderService.FROM_KEY, m_from);
			m.put(MailSenderService.TO_KEY, mail.to());
			m.put(MailSenderService.CC_KEY, mail.cc());

			if (mail.subject() != null) {
				m.put(MailSenderService.SUBJECT_KEY, mail.subject());
			} else {
				m.put(MailSenderService.SUBJECT_KEY, "");
			}

			if (mail.body() != null) {
				m.put(MailSenderService.BODY_KEY, mail.body());
			} else {
				m.put(MailSenderService.BODY_KEY, "");
			}

			if (ex == null) {
				m.put(MailSenderService.STATUS_KEY, true);
			} else {
				m.put(MailSenderService.STATUS_KEY, false);
				m.put(MailSenderService.ERROR_KEY, ex.getMessage());
			}

			Event event = new Event(MailSenderService.SENT_TOPIC, m);
			m_eventAdmin.postEvent(event);
		}
		// Else ignore.
	}

	/**
	 * Sends the given mail
	 * @param mail the mail
	 * @throws Exception the mail cannot be sent.
	 * @see org.ow2.chameleon.mail.MailSenderService#send(org.ow2.chameleon.mail.Mail)
	 */
	public void send(Mail mail) throws Exception {
		process(mail);
	}

}
