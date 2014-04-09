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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Mail class to quickly create Mail object without any check.
 * Be aware that implementations may override this class
 * to improve checking (such as unmodifiable objects)
 * <p/>
 * This class follow a kind of <tt>fluent API</tt> approach.
 */
public class Mail {

    /**
     * The 'to' list of address
     */
    protected List<String> m_to = new ArrayList<String>(1);

    /**
     * The 'cc' list of address
     */
    protected List<String> m_cc = new ArrayList<String>(0);

    /**
     * The 'replyTo' list of address
     */
    protected List<String> m_replyTo = new ArrayList<String>(0);

    /**
     * The list of attachments.
     */
    protected List<File> m_attachments = new ArrayList<File>();

    /**
     * The subject.
     */
    protected String m_subject = "no subject";

    /**
     * The body.
     */
    protected String m_body = "";

    /**
     * Is the mail read?
     */
    protected boolean m_read = false;

    /**
     * The 'from' address. For simplicity, only one
     * address is supported.
     */
    protected String m_from;

    /**
     * The sent date if sent.
     */
    protected Date m_sent;

    /**
     * The mail id if set.
     */
    protected String m_id;

    /**
     * The body's charset.
     */
    private String m_charset;

    /**
     * The body's TEXT/ sub-mime-type.
     */
    private String m_mime;

    /**
     * Creates a new Mail object
     *
     * @param to          addressee
     * @param subject     subject
     * @param body        body (text)
     * @param attachments file attachments
     */
    public Mail(String to, String subject, String body, List<File> attachments) {
        m_to.add(to);
        m_subject = subject;
        m_body = body;
        if (attachments != null && !attachments.isEmpty()) {
            m_attachments.addAll(attachments);
        }
    }

    /**
     * Creates a new Mail object
     *
     * @param to      addressee
     * @param subject subject
     * @param body    body (texT)
     */
    public Mail(String to, String subject, String body) {
        this(to, subject, body, null);
    }

    /**
     * Creates a new empty Mail object.
     */
    public Mail() {
    }

    /**
     * Creates a new Mail object by copying an exiting mail.
     *
     * @param mail the mail to copy.
     * @throws FileNotFoundException if a file attached to the existing mail does
     *                               not exist.
     */
    public Mail(Mail mail) throws FileNotFoundException {
        this
                .to(mail.to())
                .cc(mail.cc())
                .replyTo(mail.replyTo())
                .subject(mail.subject())
                .body(mail.body())
                .charset(mail.charset())
                .subType(mail.subType())
                .attach(mail.attachments())
                .read(mail.read())
                .sent(mail.sent())
                .from(mail.from())
                .id(mail.id());
    }

    /**
     * Sets the 'from' attribute.
     *
     * @param from the from address.
     * @return the current {@link Mail}
     */
    public Mail from(String from) {
        m_from = from;
        return this;
    }

    /**
     * Gets the 'from' attribute
     *
     * @return the from attribute value
     */
    public String from() {
        return m_from;
    }

    /**
     * Adds an address to the 'to' list.
     *
     * @param to the address to add.
     * @return the current {@link Mail}
     */
    public Mail to(String to) {
        m_to.add(to);
        return this;
    }

    /**
     * Adds a list of addresses to the 'to' list.
     *
     * @param to the list to add.
     * @return the current {@link Mail}
     */
    public Mail to(List<String> to) {
        m_to.addAll(to);
        return this;
    }

    /**
     * Gets the list of addresses.
     *
     * @return the 'to' list
     */
    public List<String> to() {
        return new ArrayList<String>(m_to);
    }

    /**
     * Removes an address from the 'to' list.
     *
     * @param to the address to remove
     * @return the current {@link Mail}
     */
    public Mail removeTo(String to) {
        m_to.remove(to);
        return this;
    }

    /**
     * Adds addresses to the 'cc' list.
     *
     * @param cc the list of addresses to add.
     * @return the current {@link Mail}
     */
    public Mail cc(List<String> cc) {
        m_cc.addAll(cc);
        return this;
    }

    /**
     * Removes an address from the 'cc' list.
     *
     * @param cc the address to remove
     * @return the current {@link Mail}
     */
    public Mail removeCC(String cc) {
        m_cc.remove(cc);
        return this;
    }

    /**
     * Adds an address to the 'cc' list.
     *
     * @param cc the address to add.
     * @return the current {@link Mail}
     */
    public Mail cc(String cc) {
        if (cc != null) {
            m_cc.add(cc);
        }
        return this;
    }

    /**
     * Gets the list of 'cc' addresses.
     *
     * @return the 'cc' list
     */
    public List<String> cc() {
        return new ArrayList<String>(m_cc);
    }

    /**
     * Adds an address to the 'reply-to' list
     *
     * @param to the address to add.
     * @return the current {@link Mail}
     */
    public Mail replyTo(String to) {
        m_replyTo.add(to);
        return this;
    }

    /**
     * Adds a list of addresses to the 'reply-to' list
     *
     * @param reply the list of addresses to add.
     * @return the current {@link Mail}
     */
    public Mail replyTo(List<String> reply) {
        m_replyTo.addAll(reply);
        return this;
    }

    /**
     * Removes an address from the 'reply-to' list.
     *
     * @param reply the address to remove
     * @return the current {@link Mail}
     */
    public Mail removeReplyTo(String reply) {
        m_replyTo.remove(reply);
        return this;
    }

    /**
     * Gets the 'reply-to' list of addresses.
     *
     * @return the reply-to list
     */
    public List<String> replyTo() {
        return new ArrayList<String>(m_replyTo);
    }

    /**
     * Attachs a file to the current {@link Mail}.
     *
     * @param file the file to attach
     * @return the current {@link Mail}
     * @throws NullPointerException  the file is <code>null</code>
     * @throws FileNotFoundException the file does not exist.
     */
    public Mail attach(File file) throws NullPointerException, FileNotFoundException {
        if (file != null && file.exists()) {
            m_attachments.add(file);
        } else if (file == null) {
            throw new NullPointerException("The given file is null");
        } else {
            // The file does not exist
            throw new FileNotFoundException("The file " + file.getAbsolutePath() + " does not exist");
        }

        return this;
    }

    /**
     * Attach a list of files to the current {@link Mail}
     *
     * @param files the files to attach
     * @return the current {@link Mail}
     * @throws NullPointerException  if one of the file is <code>null</code>
     * @throws FileNotFoundException if one of the file does not exist
     */
    public Mail attach(List<File> files) throws NullPointerException, FileNotFoundException {
        if (files != null) {
            for (File f : files) {
                attach(f);
            }
        } else {
            throw new NullPointerException("The given file list is null");
        }

        return this;
    }

    /**
     * Removes an attached file.
     *
     * @param attachment the file to remove
     * @return the current {@link Mail}
     */
    public Mail removeAttachment(File attachment) {
        m_attachments.remove(attachment);
        return this;
    }

    /**
     * Gets the list of attached files.
     *
     * @return the attached files
     */
    public List<File> attachments() {
        return new ArrayList<File>(m_attachments);
    }

    /**
     * Sets the mail's subject.
     *
     * @param subject the subject
     * @return the current {@link Mail}
     */
    public Mail subject(String subject) {
        m_subject = subject;
        return this;
    }

    /**
     * Gets the subjects
     *
     * @return the subject
     */
    public String subject() {
        return m_subject;
    }

    /**
     * Sets the mail's body
     *
     * @param body the body
     * @return the current {@link Mail}
     */
    public Mail body(String body) {
        m_body = body;
        return this;
    }

    /**
     * Gets the mail body.
     *
     * @return the mail body
     */
    public String body() {
        return m_body;
    }

    /**
     * Sets the mail's body charset
     *
     * @param charset a valid charset
     * @return the current mail
     */
    public Mail charset(String charset) {
        m_charset = charset;
        return this;
    }

    /**
     * Gets the mail's charset.
     *
     * @return the charset, {@literal null} if not set
     */
    public String charset() {
        return m_charset;
    }

    /**
     * Sets the mail's body sub-type.
     *
     * @param mime a valid mime sub-type for TEXT/
     * @return the current mail
     */
    public Mail subType(String mime) {
        m_mime = mime;
        return this;
    }

    /**
     * Gets the mail's sub-mime-type.
     *
     * @return the sub mime-type of TEXT/, {@literal null} if not set
     */
    public String subType() {
        return m_mime;
    }

    /**
     * Is the mail read?
     *
     * @return <code>true</code> if the mail was read,
     * <code>false</code> otherwise
     */
    public boolean read() {
        return m_read;
    }

    /**
     * Sets the read flag.
     *
     * @param r read flag
     * @return the current {@link Mail}
     */
    public Mail read(boolean r) {
        m_read = r;
        return this;
    }

    /**
     * Gets the sent date if any.
     *
     * @return the sent date of <code>null</code>
     * if the mail was not sent.
     */
    public Date sent() {
        return m_sent;
    }

    /**
     * Sets the sent date.
     *
     * @param s the sent date
     * @return the current {@link Mail}
     */
    public Mail sent(Date s) {
        m_sent = s;
        return this;
    }

    /**
     * Gets the mail id if computed.
     *
     * @return the mail id.
     */
    public String id() {
        return m_id;
    }

    /**
     * Sets the mail id.
     *
     * @param id the id
     * @return the current {@link Mail}
     */
    public Mail id(String id) {
        m_id = id;
        return this;
    }

}
