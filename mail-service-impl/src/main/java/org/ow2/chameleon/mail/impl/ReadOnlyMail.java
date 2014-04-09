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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * Unmodifiable mail.
 * All setter methods throw an {@link UnsupportedOperationException}.
 */
public class ReadOnlyMail extends Mail {

    public ReadOnlyMail(String to, String subject, String body, List<File> attachments) throws IOException {
        super(to, subject, body, attachments);
    }

    public ReadOnlyMail(String to, String subject, String body) throws IOException {
        this(to, subject, body, null);
    }

    public ReadOnlyMail() {
        // Default constructor.
    }

    public ReadOnlyMail(Mail mail) throws IOException {
        super.to(mail.to());
        super.cc(mail.cc());
        super.replyTo(mail.replyTo());
        super.subject(mail.subject());
        super.body(mail.body());
        super.attach(mail.attachments());
        super.charset(mail.charset());
        super.subType(mail.subType());
        super.sent(mail.sent());
        super.read(mail.read());
        super.from(mail.from());
        super.id(mail.id());
    }

    public ReadOnlyMail from(String from) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail to(String to) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail to(List<String> to) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail removeTo(String to) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail cc(List<String> cc) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail removeCC(String cc) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail cc(String cc) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail replyTo(String to) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail replyTo(List<String> reply) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail removeReplyTo(String reply) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail attach(File file) throws NullPointerException, FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail attach(List<File> files) throws NullPointerException, FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail removeAttachment(File attachment) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail subject(String subject) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail body(String body) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail read(boolean r) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail sent(Date s) {
        throw new UnsupportedOperationException();
    }

    public ReadOnlyMail id(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mail subType(String mime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mail charset(String charset) {
        throw new UnsupportedOperationException();
    }
}
