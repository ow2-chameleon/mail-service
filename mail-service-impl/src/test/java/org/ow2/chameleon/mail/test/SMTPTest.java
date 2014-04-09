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
package org.ow2.chameleon.mail.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailSenderService;
import org.ow2.chameleon.mail.impl.SMTPMailSender;
import org.ow2.chameleon.mail.impl.SMTPMailSender.Connection;


public class SMTPTest {

	public static final String USERNAME = "ow2.chameleon.test@googlemail.com";
	public static final String PASSWORD = "chameleon";

	@Test
	public void testSendMessageWithSSL() throws Exception {
		MailSenderService sender = new SMTPMailSender(
				Connection.SSL,
				"smtp.gmail.com",
				465,
				USERNAME,
				USERNAME,
				PASSWORD,
				false,
				false);
		sender.send(USERNAME, null, "This is a test", "This is a test using SSL");
	}

	@Test
	public void testSendMessageWithTLS() throws Exception {
		MailSenderService sender = new SMTPMailSender(
				Connection.TLS,
				"smtp.gmail.com",
				587,
				USERNAME,
				USERNAME,
				PASSWORD,
				false,
				false);
		sender.send(USERNAME, null, "This is a test", "This is a test using TLS");
	}

	@Test
	public void testSendMessageWithAttachments() throws Exception {
		MailSenderService sender = new SMTPMailSender(
				Connection.SSL,
				"smtp.gmail.com",
				465,
				USERNAME,
				USERNAME,
				PASSWORD,
				false,
				false);
		List<File> attachments = new ArrayList<File>();
		attachments.add(new File("src/test/resources/doc.pdf"));
		attachments.add(new File("src/test/resources/text.txt"));

		sender.send(USERNAME, null, "[TEST] Mail with attachments", "Two files should be attached to this mail", attachments);
	}

	@Test
	public void testSendMailwithAttachments() throws Exception {
		MailSenderService sender = new SMTPMailSender(
				Connection.SSL,
				"smtp.gmail.com",
				465,
				USERNAME,
				USERNAME,
				PASSWORD,
				false,
				false);
		List<File> attachments = new ArrayList<File>();
		attachments.add(new File("src/test/resources/doc.pdf"));
		attachments.add(new File("src/test/resources/text.txt"));
		Mail mail = new Mail(USERNAME,
				"[TEST] Mail with attachments",
				"Two files are attached to this mail, \n Regards, \n C.",
				attachments);
		sender.send(mail);
	}


	@Test
	public void testSendMessageToAnInvalidAddress() throws Exception {
		MailSenderService sender = new SMTPMailSender(
				Connection.SSL,
				"smtp.gmail.com",
				465,
				USERNAME,
				USERNAME,
				PASSWORD,
				false,
				false);
		sender.send("this_does_not_exist_98663@gmail.com", null, "This is a test", "This is a test using SSL");
	}




}
