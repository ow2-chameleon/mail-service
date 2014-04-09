package org.ow2.chameleon.mail.example;

import java.io.File;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailSenderService;


@Component(immediate=true)
public class MailSender {

	@Requires
	private MailSenderService sender;

	public MailSender() throws Exception {
		sender.send(new Mail()
			.to("me@me.com")
			.subject("This is a test")
			.body("This is a test. \n The Chameleon Team")
			);

		sender.send(new Mail()
			.to("me@me.com")
			.subject("This is a test")
			.body("This is a test using attachments. \n The Chameleon Team")
			.attach(new File("my-attachment.txt"))
		);
	}

}
