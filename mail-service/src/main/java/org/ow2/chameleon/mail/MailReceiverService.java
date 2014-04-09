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
package org.ow2.chameleon.mail;

import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * Mail Receiver defines a service to receive mail.
 * Providers must exposed the <code>org.ow2.chameleon.mail.receiver</code>
 * property indicating the mail address of the 'to'.
 *
 * Returned Mail object should not be modifiable.
 *
 * Providers must also publish events into the event admin to notify of the
 * availability of new mails. Those events are publish asynchronously to the
 * <code>org/ow2/chameleon/mail/folder</code> topic. Events must contain following
 * properties:
 * <ul>
 * <li><code>receiver</code> : the <code>org.ow2.chameleon.mail.receiver</code> value</li>
 * <li><code>from</code> : from</li>
 * <li><code>to</code> : to (List of String)</li>
 * <li><code>cc</code> : cc (List of String)</li>
 * <li><code>subject</code> : subject</li>
 * <li><code>date</code> : the mail date</li>
 * <li><code>id</code> : an ID (long) allowing to retrieve the mail with the {@link MailReceiverService#getMessageById()} method</li>
 * </ul>
 *
 */
public interface MailReceiverService {

    /**
     * Service Property indicating who is receiving mails.
     */
    public static final String TO_PROPERTY = "chameleon.mail.receiver";

    /**
     * Event Admin topic prefix on which received mail are published (notifications)
     * The topic is completed using the folder name.
     */
    public static final String RECEIVE_TOPIC = "org/ow2/chameleon/mail";

    /**
     * Event Property Key: the address sending the mail.
     */
    public static final String FROM_KEY = "from";

    /**
     * Event Property Key: the addresses receiving the mail.
     */
    public static final String TO_KEY = "to";

    /**
     * Event Property Key: the CC addresses receiving the mail.
     */
    public static final String CC_KEY = "cc";

    /**
     * Event Property Key: the mail subject if any.
     */
    public static final String SUBJECT_KEY = "subject";

    /**
     * Event Property Key: the sent date.
     */
    public static final String DATE_KEY = "date";

    /**
     * Event Property Key: the message id.
     */
    public static final String ID_KEY = "message.id";

    /**
     * Gets all mails
     * @return the list of mails
     * @throws IOException if the mails cannot be fetched
     */
    public List<Mail> getAllMessages() throws IOException;

    /**
     * Gets unread mails.
     * @return the list of unread mails.
     * @throws IOException if the mails cannot be fetched
     */
    public List<Mail> getUnreadMessages() throws IOException;

    /**
     * Gets the mails sent between 'fromDate' and 'toDate'
     * @param fromDate first date
     * @param toDate last date
     * @return the list of mails, empty if no mails match
     * @throws IOException if the mails cannot be fetched
     */
    public List<Mail> getMessages(Date fromDate, Date toDate) throws IOException;

    /**
     * Gets the recents mails.
     * @return the list of recent mail, empty if no recent mail.
     * @throws IOException the mails cannot be fetched
     */
    public List<Mail> getRecentMessages() throws IOException;

    /**
     * Gets a specific mail by id. Ids are implementation specific, so
     * this method must be call on the correct provider
     * @param id the id
     * @return the Mail or <code>null</code> if not found.
     * @throws IOException if the mail cannot be fetched
     */
    public Mail getMessageById(String id) throws IOException;

}
