package org.ow2.chameleon.mail.it;

import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailReceiverService;
import org.ow2.chameleon.mail.MailSenderService;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith(JUnit4TestRunner.class)
public class EventAdminTest {
	public static final String USERNAME = "ow2.chameleon.test@googlemail.com";
	public static final String PASSWORD = "chameleon";

	@Inject
    private BundleContext context;

    private OSGiHelper osgi;
    private IPOJOHelper ipojo;

    private List<Event> events = new ArrayList<Event>();

    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);

        String[] topics = new String[] {EventConstants.EVENT_TOPIC, "org/ow2/chameleon/mail/*"};
        Hashtable ht = new Hashtable();
        ht.put(EventConstants.EVENT_TOPIC, topics);
        context.registerService(EventHandler.class.getName(), new Collector(), ht);
    }

    @After
    public void tearDown() {
        osgi.dispose();
        ipojo.dispose();
    }

    @Configuration
    public static Option[] configure() {
        Option[] platform = CoreOptions.options(
                CoreOptions.felix());

        Option[] bundles = CoreOptions.options(
                CoreOptions.provision(
                        CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").versionAsInProject(),
                        CoreOptions.mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject(),
                        CoreOptions.mavenBundle().groupId("org.ow2.chameleon.mail").artifactId("mail-service").versionAsInProject(),
                        CoreOptions.mavenBundle().groupId("org.ow2.chameleon.mail").artifactId("mail-service-impl").versionAsInProject(),
                        CoreOptions.mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.2.0"),
                        CoreOptions.mavenBundle().groupId("org.slf4j").artifactId("slf4j-api").versionAsInProject(),
                        CoreOptions.mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple").versionAsInProject(),
                        CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.eventadmin").version("1.2.8")
                ), CoreOptions.systemPackage("sun.security.util"));

        return OptionUtils.combine(platform, bundles);
    }

    @Test
    public void testIMAPUsingGmail() throws Exception {
    	Dictionary<String, String> conf = new Hashtable<String, String>();
    	conf.put("imap.host", "imap.gmail.com");
    	conf.put("imap.username", USERNAME);
    	conf.put("imap.password", PASSWORD);
    	conf.put("imap.polling", "5000");
    	conf.put("imap.useIMAPS", "true");

    	ComponentInstance ci = ipojo.createComponentInstance("org.ow2.chameleon.mail.imap", conf);
    	Assert.assertTrue(ci.getState() == ComponentInstance.VALID);

    	osgi.waitForService(MailReceiverService.class.getName(), null, 5000);
    	ServiceReference ref = osgi.getServiceReference(MailReceiverService.class.getName());
    	Assert.assertNotNull(ref);

    	Assert.assertEquals(USERNAME, ref.getProperty(MailReceiverService.TO_PROPERTY));

    	MailReceiverService svc = (MailReceiverService) osgi.getServiceObject(ref);
    	List<Mail> list = svc.getAllMessages();
    	int count = list.size();
    	Assert.assertNotNull(list);

    	conf = new Hashtable<String, String>();
    	conf.put("smtp.connection", "SSL");
    	conf.put("smtp.host", "smtp.gmail.com");
    	conf.put("smtp.port", "465");
    	conf.put("smtp.username", USERNAME);
    	conf.put("smtp.from", USERNAME);
    	conf.put("smtp.password", PASSWORD);

    	ComponentInstance ci2 = ipojo.createComponentInstance("org.ow2.chameleon.mail.smtp", conf);
    	Assert.assertTrue(ci2.getState() == ComponentInstance.VALID);

    	osgi.waitForService(MailSenderService.class.getName(), null, 5000);
    	ref = osgi.getServiceReference(MailSenderService.class.getName());
    	MailSenderService sender = (MailSenderService) osgi.getServiceObject(ref);
    	Date date = new Date();
    	String subject = "[IT-TEST] Sent from IT Tests - " + date;
    	sender.send(new Mail()
    		.to(USERNAME)
    		.subject(subject)
    		.body("This is a test. \n C."));

    	Thread.sleep(10000); // Wait for delivery.

    	list = svc.getAllMessages();
    	int tries = 0;
    	while (list.size() == count  && tries < 5) {
    		Thread.sleep(10000); // Wait for delivery.
    		System.out.println("IMAP - Try " + tries);
        	list = svc.getAllMessages();
        	tries++;
    	}

    	Assert.assertFalse(list.isEmpty());

    	Assert.assertFalse(events.isEmpty());
    	for (Event ev: events) {
    		System.out.println("===");
    		for (String n : ev.getPropertyNames()) {
    			System.out.println(n + "=" + ev.getProperty(n));
    		}
    	}
    }

    @Test
    public void testPOP3UsingGmail() throws Exception {
    	Dictionary<String, String> conf = new Hashtable<String, String>();
    	conf.put("pop3.host", "pop.gmail.com");
    	conf.put("pop3.port", "995");
    	conf.put("pop3.username", USERNAME);
    	conf.put("pop3.password", PASSWORD);
    	conf.put("pop3.polling", "5000");
    	conf.put("pop3.debug", "true");


    	ComponentInstance ci = ipojo.createComponentInstance("org.ow2.chameleon.mail.pop3", conf);
    	Assert.assertTrue(ci.getState() == ComponentInstance.VALID);

    	osgi.waitForService(MailReceiverService.class.getName(), null, 5000);
    	ServiceReference ref = osgi.getServiceReference(MailReceiverService.class.getName());
    	Assert.assertNotNull(ref);

    	Assert.assertEquals(USERNAME, ref.getProperty(MailReceiverService.TO_PROPERTY));

    	MailReceiverService svc = (MailReceiverService) osgi.getServiceObject(ref);
    	List<Mail> list = svc.getAllMessages();
    	int count = list.size();
    	Assert.assertNotNull(list);

    	conf = new Hashtable<String, String>();
    	conf.put("smtp.connection", "SSL");
    	conf.put("smtp.host", "smtp.gmail.com");
    	conf.put("smtp.port", "465");
    	conf.put("smtp.username", USERNAME);
    	conf.put("smtp.from", USERNAME);
    	conf.put("smtp.password", PASSWORD);

    	ComponentInstance ci2 = ipojo.createComponentInstance("org.ow2.chameleon.mail.smtp", conf);
    	Assert.assertTrue(ci2.getState() == ComponentInstance.VALID);

    	osgi.waitForService(MailSenderService.class.getName(), null, 5000);
    	ref = osgi.getServiceReference(MailSenderService.class.getName());
    	MailSenderService sender = (MailSenderService) osgi.getServiceObject(ref);
    	Date date = new Date();
    	String subject = "[IT-TEST] Sent from IT Tests - " + date;
    	sender.send(new Mail()
    		.to(USERNAME)
    		.subject(subject)
    		.body("This is a test. \n C."));

    	Thread.sleep(10000); // Wait for delivery.

    	list = svc.getAllMessages();
    	int tries = 0;
    	while (list.size() == count  && tries < 5) {
    		Thread.sleep(10000); // Wait for delivery.
    		System.out.println("POP3 - Try " + tries);
        	list = svc.getAllMessages();
        	tries++;
    	}

    	Assert.assertFalse(list.isEmpty());

    	Assert.assertFalse(events.isEmpty());
    }

    private class Collector implements EventHandler {

		public void handleEvent(Event event) {
			events.add(event);
		}

    }

}
