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

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailReceiverService;

/**
 * Mail Receiver Implementation for POP3.
 * This implementation fetches mails regularly.
 * Returned mails are not modifiable.
 */
@Component(immediate=true, name="org.ow2.chameleon.mail.pop3")
@Provides(specifications={MailReceiverService.class})
public class POP3MailReceiver extends AbstractMailReceiver implements MailReceiverService {

	/**
	 * The username / password authenticator.
	 */
	private Authenticator m_authenticator;

	/**
	 * The event admin if present.
	 */
	@Requires(optional=true, nullable=false, proxy=false)
	private EventAdmin m_eventAdmin;

	/**
	 * The username.
	 */
	@Property(name="pop3.username", mandatory=true)
	@ServiceProperty(name=MailReceiverService.TO_PROPERTY)
	private String m_username;

	/**
	 * The password.
	 */
	@Property(name="pop3.password", mandatory=true)
	private String m_password;

	/**
	 * The port.
	 */
	@Property(name="pop3.port", value="-1")
	private int m_port;

	/**
	 * The host.
	 */
	@Property(name="pop3.host", mandatory=true)
	private String m_host;

	/**
	 * The folder name, if not set use <tt>inbox</tt>.
	 */
	@Property(name="pop3.folder", value="inbox")
	private String m_folderName;

	/**
	 * The polling period, 10 minutes by default.
	 */
	@Property(name="pop3.polling", value="60000")
	private long m_polling;

	/**
	 * Enables / Disables debugging.
	 */
	@Property(name="pop3.debug")
	private boolean m_debug;

	/**
	 * The folder.
	 */
	private Folder m_folder;
	/**
	 * The store.
	 */
	private Store m_store;

	/**
	 * Fetch Thread controller.
	 */
	private volatile boolean m_run;


	/**
	 * Creates a POP3MailReceiver used for testing purposes
	 * @param host the host
	 * @param port the port
	 * @param username the username
	 * @param password the password
	 * @param folder the folder name
	 * @param polling the polling period
	 * @throws MessagingException if the messages cannot be fetched
	 * @throws IOException if the messages cannot be read
	 */
	public POP3MailReceiver(String host, int port,
			final String username, final String password,
			final String folder, final int polling) throws MessagingException, IOException {

		this.m_username = username;
		this.m_password = password;

		this.m_port = port;
		this.m_host = host;

		m_folderName = folder;

		m_polling = polling;

		m_debug = true;
		configure();
	}

	/**
	 * Creates a POP3MailReceiver (used by iPOJO)
	 * @throws MessagingException if the messages cannot be fetched
	 * @throws IOException if the messages cannot be read
	 */
	public POP3MailReceiver() throws MessagingException, IOException {
		configure();
	}

	/**
	 * Configures the receiver.
	 * @throws MessagingException if the messages cannot be fetched
	 * @throws IOException if the messages cannot be read
	 */
	private void configure() throws MessagingException, IOException {
		Properties props = new Properties();

		String protocol = "pop3";


		props.setProperty("mail." + protocol + ".host",  m_host);
		if (m_port != -1) {
			props.setProperty("mail." + protocol + ".port",
					Integer.toString(m_port));
			props.setProperty("mail." + protocol + ".socketFactory.port",
					Integer.toString(m_port));
		}

		props.setProperty("mail." + protocol + ".socketFactory.class", javax.net.ssl.SSLSocketFactory.class.getName());
		props.setProperty("mail." + protocol + ".socketFactory.fallback", "false");

		m_authenticator = new javax.mail.Authenticator() {
			protected javax.mail.PasswordAuthentication getPasswordAuthentication(){
				return new javax.mail.PasswordAuthentication(m_username, m_password);
			}
		};

		Session session = Session.getInstance(props, m_authenticator);
		session.setDebug(m_debug);
		m_store = session.getStore(protocol);

		m_messages = new HashMap<Message, Mail>();
		fetch();
        // Set polling
        m_run = true;

        Runnable runnable = new Runnable() {
        	public void run() {
        		while(m_run) {
        			try {
        				fetch();
        				Thread.sleep(m_polling);
        			} catch (Exception e) {
        				m_logger.error("Cannot fetch mails", e);
        			}
        		}
        	}
        };
        new Thread(runnable).start();
	}

	/**
	 * Stops the receiver
	 * @throws MessagingException
	 */
	public void stop() throws MessagingException {
		m_run = false;
		m_folder.close(false);
		m_store.close();
	}

	/**
	 * Fetches mails.
	 * @throws MessagingException if the mails cannot be fetched.
	 */
	private synchronized void fetch() throws MessagingException {
		m_store.connect();

        if (m_folderName == null) {
        	m_folder = m_store.getFolder("INBOX");
        } else {
        	m_folder = m_store.getFolder(m_folderName);
        	if (m_folder == null) {
        		throw new IllegalArgumentException("Cannot find folder " + m_folderName);
        	}
        }

        // try to open read/write and if that fails try read-only
        try {
        	m_folder.open(Folder.READ_WRITE);
    	} catch (MessagingException ex) {
    		m_folder.open(Folder.READ_ONLY);
        }

		try {
			Message[] messages = m_folder.getMessages();
			for (Message msg : messages) {
				try {
					if (! m_messages.containsKey(msg)) {
						Mail mail = createMail(msg);
						m_messages.put(msg, mail);
						propagateToEventAdmin(mail);
					}
				} catch (Exception e) {
					m_logger.error("Cannot read new message", e);
				}
			}
		} finally {
			m_store.close();
		}
	}

	/**
	 * Gets the list of messages.
	 * @return the list of messages
	 * @throws MessagingException if the messages cannot be fetched
	 * @see org.ow2.chameleon.mail.impl.AbstractMailReceiver#getMessages()
	 */
	@Override
	protected Message[] getMessages() throws MessagingException {
		return m_folder.getMessages();
	}

	/**
	 * Propagates the newly arrived mail to the event admin.
	 * @param mail the mail
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void propagateToEventAdmin(Mail mail) {
		if (m_eventAdmin != null) {
			Dictionary props = new Hashtable();
			props.put(MailReceiverService.TO_KEY, mail.to());
			props.put(MailReceiverService.FROM_KEY, mail.from());
			props.put(MailReceiverService.CC_KEY, mail.cc());

			if (mail.subject() != null) {
				props.put(MailReceiverService.SUBJECT_KEY, mail.subject());
			} else {
				props.put(MailReceiverService.SUBJECT_KEY, "");
			}

			props.put(MailReceiverService.ID_KEY, mail.id());

			m_eventAdmin.postEvent(new Event(MailReceiverService.RECEIVE_TOPIC + "/" + m_folder.getName(), props));
		}
		// Else ignore
	}

}
