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

import org.junit.Assert;
import org.junit.Test;
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailSenderService;
import org.ow2.chameleon.mail.impl.IMAPMailReceiver;
import org.ow2.chameleon.mail.impl.SMTPMailSender;
import org.ow2.chameleon.mail.impl.SMTPMailSender.Connection;

import java.util.Date;
import java.util.List;


public class IMAPReceiverTest {

	public static final String USERNAME = "ow2.chameleon.test@googlemail.com";
	public static final String PASSWORD = "chameleon";


	@Test
	public void testAllMessages() throws Exception {
		IMAPMailReceiver receiver = new IMAPMailReceiver(
				"imap.gmail.com",
				-1,
				USERNAME,
				PASSWORD,
				true,
				null,
				5000);

		List<Mail> list = receiver.getAllMessages();
		//Assert.assertFalse(list.isEmpty());

		for (Mail mail : list) {
			System.out.println(mail.subject());
			Assert.assertNotNull(mail.subject());
			Assert.assertNotNull(mail.body());
			Assert.assertNotNull(mail.id());
			Assert.assertNotNull(mail.from());
			Assert.assertNotNull(mail.sent());
		}

		receiver.stop();
	}

	@Test
	public void testGetRecentMails() throws Exception {
		IMAPMailReceiver receiver = new IMAPMailReceiver(
				"imap.gmail.com",
				-1,
				USERNAME,
				PASSWORD,
				true,
				null,
				5000);



		List<Mail> recentMessages = receiver.getRecentMessages();
		int count = recentMessages.size();
		for (Mail mail : recentMessages) {
			Assert.assertNotNull(mail.subject());
			Assert.assertNotNull(mail.body());
			Assert.assertNotNull(mail.id());
			Assert.assertNotNull(mail.from());
			Assert.assertNotNull(mail.sent());
		}

		MailSenderService sender = new SMTPMailSender(
				Connection.SSL,
				"smtp.gmail.com",
				465,
				USERNAME,
				USERNAME,
				PASSWORD,
				false,
				false);

		Date date = new Date();
		sender.send(USERNAME, null, "[TEST] - " + date , "This is the mail " + date + " \n C.");

		Thread.sleep(5000); // Wait for delivery
		recentMessages = receiver.getRecentMessages();
		Assert.assertTrue(count < recentMessages.size());
	}

	@Test
	public void testGetUnreadMails() throws Exception {
		IMAPMailReceiver receiver = new IMAPMailReceiver(
				"imap.gmail.com",
				-1,
				USERNAME,
				PASSWORD,
				true,
				null,
				5000);

		List<Mail> unread = receiver.getUnreadMessages();
		int count = unread.size();
		for (Mail mail : unread) {
			Assert.assertNotNull(mail.subject());
			Assert.assertNotNull(mail.body());
			Assert.assertNotNull(mail.id());
			Assert.assertNotNull(mail.from());
			Assert.assertNotNull(mail.sent());
		}

		MailSenderService sender = new SMTPMailSender(
				Connection.SSL,
				"smtp.gmail.com",
				465,
				USERNAME,
				USERNAME,
				PASSWORD,
				false,
				false);

		Date date = new Date();
		sender.send(USERNAME, null, "[TEST] - " + date , "This is the mail " + date + " \n C.");

		int i = 0;
		while (count == unread.size()  && i < 5) {
			Thread.sleep(10000); // Wait for delivery
			System.out.println("Try " + i);
			unread = receiver.getUnreadMessages();
			i++;
		}

//		System.out.println("count " + count);
//		System.out.println("after " + unread.size());
//		Assert.assertTrue(count < unread.size());
//
//		Mail m = unread.get(0);
//		Assert.assertEquals(m.subject(), "[TEST] - " + date);
	}
}
