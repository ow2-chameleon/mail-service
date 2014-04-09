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

import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailReceiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.Message.RecipientType;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This abstract class contains the {@link MailReceiverService} implementation
 * based on one abstract method. It capitalizes code for IMAP and POP receivers.
 * Currently this class do not support attachments.
 */
public abstract class AbstractMailReceiver implements MailReceiverService {

    /**
     * The messages.
     * It's a map storing {@link Message} and the associated {@link Mail}.
     */
    protected Map<Message, Mail> m_messages;

    /**
     * The logger.
     */
    protected Logger m_logger = LoggerFactory.getLogger(this.getClass().getName());

    /**
     * Gets the list of messages.
     *
     * @return the messages
     * @throws MessagingException if the messages cannot be fetched.
     */
    protected abstract Message[] getMessages() throws MessagingException;

    /**
     * Gets all messages.
     *
     * @return the list of mails sorted by sent date
     * @throws IOException if the mail cannot be fetched
     * @see org.ow2.chameleon.mail.MailReceiverService#getAllMessages()
     */
    public synchronized List<Mail> getAllMessages() throws IOException {
        List<Mail> mails = new ArrayList<Mail>();
        mails.addAll(m_messages.values());
        Collections.sort(mails, new SentDateCompator());
        return mails;
    }

    /**
     * Gets unread mails.
     *
     * @return the list of unread mails, empty if all mails are read.
     * The list if sorted by sent date.
     * @throws IOException if the mail cannot be fetched
     * @see org.ow2.chameleon.mail.MailReceiverService#getUnreadMessages()
     */
    public synchronized List<Mail> getUnreadMessages() throws IOException {
        List<Mail> mails = new ArrayList<Mail>();
        for (Message msg : m_messages.keySet()) {
            Mail m = m_messages.get(msg);
            if (!m.read()) {
                mails.add(m);
            }
        }
        Collections.sort(mails, new SentDateCompator());
        return mails;
    }

    /**
     * Gets mails sent between the two given dates.
     *
     * @param fromDate the first date
     * @param toDate   the last date
     * @return the list of mails, sorted by sent date, empty if no mail match.
     * @throws IOException if the mail cannot be fetched
     * @see org.ow2.chameleon.mail.MailReceiverService#getMessages(java.util.Date, java.util.Date)
     */
    public synchronized List<Mail> getMessages(Date fromDate, Date toDate) throws IOException {
        List<Mail> mails = new ArrayList<Mail>();
        for (Message msg : m_messages.keySet()) {
            Date sent = m_messages.get(msg).sent();
            if (sent != null && sent.before(toDate) && sent.after(fromDate)) {
                mails.add(m_messages.get(msg));
            }
        }
        Collections.sort(mails, new SentDateCompator());
        return mails;
    }

    /**
     * Gets recent messages.
     *
     * @return the list of recent mail, sorted by sent date, empty if no recent mails.
     * @throws IOException if the mails cannot be fetched
     * @see org.ow2.chameleon.mail.MailReceiverService#getRecentMessages()
     */
    public synchronized List<Mail> getRecentMessages() throws IOException {
        List<Mail> mails = new ArrayList<Mail>();
        for (Message msg : m_messages.keySet()) {
            try {
                if (!msg.getFlags().contains(Flag.RECENT)) {
                    mails.add(m_messages.get(msg));
                }
            } catch (Exception e) {
                // Ignore the mail.
                m_logger.error("Cannot check the 'RECENT' flag of a message " +
                        "- ignoring mail");
            }
        }
        Collections.sort(mails, new SentDateCompator());
        return mails;

    }

    /**
     * Gets a specific mail by its id.
     *
     * @param id the id
     * @return the mail or <code>null</code> if not found
     * @throws IOException if the mail cannot be fetched
     * @see org.ow2.chameleon.mail.MailReceiverService#getMessageById(java.lang.String)
     */
    public synchronized Mail getMessageById(String id) throws IOException {
        List<Mail> list = getAllMessages();
        for (Mail m : list) {
            if (id.equals(m.id())) {
                return m;
            }
        }
        return null;
    }


    /**
     * Creates a mail from a {@link Part}.
     *
     * @param p the part
     * @return the mail
     * @throws MessagingException if the part cannot be fetched
     * @throws IOException        if the part cannot be fetched
     */
    protected Mail createMail(Part p) throws MessagingException, IOException {
        Mail mail = new Mail();
        if (p instanceof Message) {
            convertMessageEnvelope((Message) p, mail);
        }

        if (p.isMimeType("text/*")) {
            extractSubTypeAndCharset(p, mail);
            mail.body((String) p.getContent());
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                if (i == 0) {
                    extractSubTypeAndCharset(mp.getBodyPart(i), mail);
                    mail.body(mp.getBodyPart(i).getContent().toString());
                } else {
                    // TODO Attachments
                }
            }
        } else if (p.isMimeType("message/rfc822")) {
            // TODO Support nested messages
        } else {
            //TODO Attachments.
        }

        return new ReadOnlyMail(mail);
    }

    /**
     * Extracts the charset from the content-type header.
     * The extracted charset is in the group #1.
     */
    private static final Pattern CHARSET_PATTERN = Pattern.compile(".*charset=\"?([a-zA-Z0-9_-]*)\"?.*");

    /**
     * Extracts the sub-mime-type of the content-type header.
     * The extracted sub-type is in the ground #1.
     */
    private static final Pattern SUBTYPE_PATTERN = Pattern.compile(".*/([A-Za-z]+).*");

    private void extractSubTypeAndCharset(Part part, Mail mail) throws MessagingException {
        String content = part.getContentType();
        if (content == null) {
            return;
        }
        Matcher matcher = CHARSET_PATTERN.matcher(content);
        if (matcher.matches()) {
            mail.charset(matcher.group(1));
        }
        matcher = SUBTYPE_PATTERN.matcher(content);
        if (matcher.matches()) {
            mail.subType(matcher.group(1));
        }
    }

    /**
     * converts a message envelope to a mail
     *
     * @param message the message
     * @param mail    the mail
     * @throws MessagingException if the message cannot be read.
     */
    private void convertMessageEnvelope(Message message, Mail mail) throws MessagingException {
        // From
        Address[] addresses = message.getFrom();
        if (addresses.length > 0 && addresses[0] != null) {
            // Use first
            mail.from(addresses[0].toString());
        }
        // To
        addresses = message.getRecipients(RecipientType.TO);
        if (addresses != null) {
            for (Address add : addresses) {
                mail.to(add.toString());
            }
        }
        // CC
        addresses = message.getRecipients(RecipientType.CC);
        if (addresses != null) {
            for (Address add : addresses) {
                mail.cc(add.toString());
            }
        }
        // ReplyTo
        addresses = message.getReplyTo();
        if (addresses != null) {
            for (Address add : addresses) {
                mail.replyTo(add.toString());
            }
        }
        // SUBJECT
        mail.subject(message.getSubject());

        Date d = message.getSentDate();
        mail.sent(d);
        mail.read(message.getFlags().contains(Flag.SEEN));
        mail.id(getIdForMail(message, mail));
    }


    /**
     * Computes the ids for the given mail.
     *
     * @param msg  the message
     * @param mail the mail
     * @return the id
     */
    private String getIdForMail(Message msg, Mail mail) {
        String folder = "";
        if (msg.getFolder() != null) {
            folder = msg.getFolder().getName();
        }
        Date d = mail.sent();
        String sub = mail.subject();

        String id = folder + "/" + d.getTime();

        if (sub != null) {
            id += "-" + sub;
        }
        return id;

    }


    /**
     * Compare Mails based on there sent dates.
     */
    private class SentDateCompator implements Comparator<Mail> {

        /**
         * Compares mails
         *
         * @param o1 a mail
         * @param o2 another mail
         * @return <tt>(o2.sent).compareTo(o1.sent)</tt>
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Mail o1, Mail o2) {
            if (o1.sent() != null && o2.sent() != null) {
                return o2.sent().compareTo(o1.sent());
            } else {
                return 0;
            }
        }
    }

}
