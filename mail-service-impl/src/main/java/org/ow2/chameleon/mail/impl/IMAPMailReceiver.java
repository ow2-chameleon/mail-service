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
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

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
 * Mail Receiver Implementation for IMAP.
 * This implementation is notified when new mails are arriving, however, so avoid some issue with IMAP server,
 * a polling is also done.
 * Returned mails are not modifiable.
 */
@Component(immediate=true, name="org.ow2.chameleon.mail.imap")
@Provides(specifications={MailReceiverService.class})
public class IMAPMailReceiver extends AbstractMailReceiver implements MailReceiverService, MessageCountListener {

	/**
	 * Username / Password authenticator.
	 */
	private Authenticator m_authenticator;

	/**
	 * The Event Admin is present.
	 */
	@Requires(optional=true, nullable=false, proxy=false)
	private EventAdmin m_eventAdmin;

	/**
	 * The username.
	 */
	@Property(name="imap.username", mandatory=true)
	@ServiceProperty(name=MailReceiverService.TO_PROPERTY)
	private String m_username;

	/**
	 * The password.
	 */
	@Property(name="imap.password", mandatory=true)
	private String m_password;

	/**
	 * The port.
	 */
	@Property(name="imap.port", value="-1")
	private int m_port;

	/**
	 * The host.
	 */
	@Property(name="imap.host", mandatory=true)
	private String m_host;

	/**
	 * Enable / Disable IMAPS.
	 */
	@Property(name="imap.useIMAPS", value="false")
	private boolean m_useIMAPS;

	/**
	 * The desired folder name, if not set use <tt>inbox</tt>
	 */
	@Property(name="imap.folder", value="inbox")
	private String m_folderName;

	/**
	 * Polling interval, 10 minutes by default.
	 */
	@Property(name="imap.polling", value="60000")
	private long m_polling;

	/**
	 * Enable/Disable debugging.
	 */
	@Property(name="imap.debug")
	private boolean m_debug;

	/**
	 * The Folder.
	 */
	private Folder m_folder;
	/**
	 * The store.
	 */
	private Store m_store;

	/**
	 * Controller of the fetch thread.
	 */
	private volatile boolean m_run;

	/**
	 * Creates a IMAPMailReceiver for testing purpose.
	 * @param host the host
	 * @param port the port
	 * @param username the username
	 * @param password the password
	 * @param useIMAPS use imaps
	 * @param folder the folder
	 * @param polling the polling
	 * @throws MessagingException if mails cannot be fetched.
	 * @throws IOException if mails cannot be read
	 */
	public IMAPMailReceiver(String host, int port,
			final String username, final String password, final boolean useIMAPS,
			final String folder, final int polling) throws MessagingException, IOException {

		this.m_username = username;
		this.m_password = password;

		this.m_port = port;
		this.m_host = host;

		m_useIMAPS = useIMAPS;

		m_folderName = folder;

		m_polling = polling;

		m_debug = false;
		configure();
	}

	/**
	 * Creates a IMAPMailReceiver (used by iPOJO)
	 * @throws MessagingException if mails cannot be fetched.
	 * @throws IOException if mails cannot be read
	 */
	public IMAPMailReceiver() throws MessagingException, IOException {
		configure();
	}

	/**
	 * Configures the receiver.
	 * @throws MessagingException if mails cannot be fetched.
	 * @throws IOException if mails cannot be read
	 */
	private void configure() throws MessagingException, IOException {
		Properties props = new Properties();

		String protocol = "imap";
		if (m_useIMAPS) {
			protocol = "imaps";
		}

		props.setProperty("mail." + protocol + ".host",  m_host);
		if (m_port != -1) {
			props.setProperty("mail." + protocol + ".port",
					Integer.toString(m_port));
		}
		props.setProperty("mail." + protocol + ".user",  m_username);


		m_authenticator = new javax.mail.Authenticator() {
			protected javax.mail.PasswordAuthentication getPasswordAuthentication(){
				return new javax.mail.PasswordAuthentication(m_username, m_password);
			}
		};


		Session session = Session.getInstance(props, m_authenticator);
		session.setDebug(m_debug);
		m_store = session.getStore(protocol);
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


        m_folder.addMessageCountListener(this);

        // Set polling frequence
        m_run = true;

        fetch();
        Runnable runnable = new Runnable() {
        	public void run() {
        		while(m_run) {
        			try {
        				// This will trigger the listener.
        				m_folder.getMessageCount();
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
	 * Fetches mails.
	 * @throws MessagingException if mails cannot be fetches
	 */
	private void fetch() throws MessagingException {
		if (m_messages == null) {
			m_messages = new HashMap<Message, Mail>();
		}

		Message[] messages = getMessages();
		for (Message msg : messages) {
			try {
				if (! m_messages.containsKey(msg)) {
					Mail mail = createMail(msg);
					m_messages.put(msg, mail);
					propagateToEventAdmin(mail);
				}
			} catch (Exception e) {
				m_logger.error("Cannot fetch mails", e);
			}
		}
	}

	/**
	 * Stops the receiver.
	 * @throws MessagingException can be ignored.
	 */
	public void stop() throws MessagingException {
		m_run = false;
		m_folder.close(false);
		m_store.close();
	}

	/**
	 * Listeners called when a new mails arrive.
	 * @param e the event
	 * @see javax.mail.event.MessageCountListener#messagesAdded(javax.mail.event.MessageCountEvent)
	 */
	public void messagesAdded(MessageCountEvent e) {
		synchronized (this) {
			Message[] messages = e.getMessages();
			if (messages != null) {
				for (Message msg : messages) {
					try {
						Mail mail = createMail(msg);
						m_messages.put(msg, mail);
						propagateToEventAdmin(mail);
					} catch (MessagingException e1) {
						m_logger.error("Cannot read new message", e1);
					} catch (IOException e1) {
						m_logger.error("Cannot read new message", e1);
					}
				}
			}
		}

	}

	/**
	 * Listeners called when mails are deleted.
	 * @param e the event
	 * @see javax.mail.event.MessageCountListener#messagesRemoved(javax.mail.event.MessageCountEvent)
	 */
	public void messagesRemoved(MessageCountEvent e) {
		synchronized (this) {
			Message[] messages = e.getMessages();
			if (messages != null) {
				for (Message msg : messages) {
					m_messages.remove(msg);
				}
			}
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
