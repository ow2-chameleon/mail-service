package org.ow2.chameleon.mail.it;

import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;

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
import org.ow2.chameleon.mail.Mail;
import org.ow2.chameleon.mail.MailSenderService;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith(JUnit4TestRunner.class)
public class MailSenderServiceTest {
	public static final String USERNAME = "ow2.chameleon.test@googlemail.com";
	public static final String PASSWORD = "chameleon";

	@Inject
    private BundleContext context;

    private OSGiHelper osgi;
    private IPOJOHelper ipojo;

    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
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
                        CoreOptions.mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple").versionAsInProject()
                ), CoreOptions.systemPackage("sun.security.util"));

        return OptionUtils.combine(platform, bundles);
    }

    @Test
    public void testSendingMailUsingGmail() throws Exception {
    	Dictionary<String, String> conf = new Hashtable<String, String>();
    	conf.put("smtp.connection", "SSL");
    	conf.put("smtp.host", "smtp.gmail.com");
    	conf.put("smtp.port", "465");
    	conf.put("smtp.username", USERNAME);
    	conf.put("smtp.from", USERNAME);
    	conf.put("smtp.password", PASSWORD);

    	ComponentInstance ci = ipojo.createComponentInstance("org.ow2.chameleon.mail.smtp", conf);
    	Assert.assertTrue(ci.getState() == ComponentInstance.VALID);

    	osgi.waitForService(MailSenderService.class.getName(), null, 5000);
    	ServiceReference ref = osgi.getServiceReference(MailSenderService.class.getName());
    	Assert.assertNotNull(ref);

    	Assert.assertEquals(USERNAME, ref.getProperty(MailSenderService.FROM_PROPERTY));
    	MailSenderService svc = (MailSenderService) osgi.getServiceObject(ref);
    	Date date = new Date();
    	svc.send(new Mail()
    		.to(USERNAME)
    		.subject("[IT-TEST] Sent from IT Tests - " + date)
    		.body("This is a test. \n C."));
    }

}
