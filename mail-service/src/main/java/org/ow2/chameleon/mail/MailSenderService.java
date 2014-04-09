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

import java.io.File;
import java.util.List;


/**
 * Mail Service defines a service to send mails.
 * Providers must exposed the <code>org.ow2.chameleon.mail.sender</code>
 * property indicating the mail address of the 'from'.
 * Providers must also send event admin message (if present) on
 * the <code>org/ow2/chameleon/mail/username/sent</code>. Events must contains the following key:
 * <ul>
 * <li><code>from</code> : from (<code>org.ow2.chameleon.mail.sender</code> value)</li>
 * <li><code>to</code> : to (List of String)</li>
 * <li><code>cc</code> : cc (List of String)</li>
 * <li><code>subject</code> : subject</li>
 * <li><code>body</code> : body</li>
 * <li><code>status</code> : boolean set to true if the mail was sent successfully, false otherwise</li>
 * <li><code>error</code> : error message if the mail was not sent correctly, can be null</li>
 * </ul>
 *
 * If a message cannot be delivered, another event similar to the first one is sent with the <code>status</code>
 * and <code>error</code> entries updated.
 *
 */
public interface MailSenderService {

    /**
     * Service PRoperty indicating who is sending mails.
     */
    public static final String FROM_PROPERTY = "org.ow2.chameleon.mail.sender";

    /**
     * Event Admin topic where sent mail are propagated.
     */
    public static final String SENT_TOPIC = "org/ow2/chameleon/mail/sent";

    /**
     * Event Property Key: the address sending the mail.
     */
    public static final String FROM_KEY = "from";

    /**
     * Event Property Key: the addresses receiving the mail.
     */
    public static final String TO_KEY = "to";

    /**
     * Event Property Key: the addresses receiving the mail in CC.
     */
    public static final String CC_KEY = "cc";

    /**
     * Event Property Key: the mail subject if any.
     */
    public static final String SUBJECT_KEY = "subject";

    /**
     * Event Property Key: the mail body if any.
     */
    public static final String BODY_KEY = "body";

    /**
     * Event Property Key: was the mail sent successfully or not.
     */
    public static final String STATUS_KEY = "status";

    /**
     * Event Property Key: if the mail was not sent correctly, the error
     * message.
     */
    public static final String ERROR_KEY = "error";

    /**
     * Sends a mail.
     * @param to address
     * @param cc the cc address
     * @param subject the subject
     * @param body the body
     * @throws Exception if the mail cannot be sent.
     */
    public void send(String to, String cc, String subject, String body) throws Exception;

    /**
     * Sends a mail.
     * @param to the address
     * @param cc the cc address
     * @param subject the subject
     * @param body the body
     * @param attachments the files to attach
     * @throws Exception if the mail cannot be sent.
     */
    public void send(String to, String cc, String subject, String body, List<File> attachments) throws Exception;

    /**
     * Sends a mail.
     * @param mail the Mail to send
     * @throws Exception if the mail cannot be sent.
     */
    public void send(Mail mail) throws Exception;

}
