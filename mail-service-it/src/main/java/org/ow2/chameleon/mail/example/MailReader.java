package org.ow2.chameleon.mail.example;

import java.io.IOException;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailReceiverService;


@Component(immediate=true)
public class MailReader {

	@Requires
	private MailReceiverService reader;

	public MailReader() throws IOException {
		List<Mail> mails = reader.getAllMessages();
		for (Mail mail : mails) {
			System.out.println(mail.from() + " > " + mail.subject());
		}
	}

}
