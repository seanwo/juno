package us.wohlgemuth;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Date;

import javax.mail.*;

import javax.mail.internet.*;

import com.sun.mail.smtp.*;

public class Notifier {

    String host;
    String user;
    String password;

    public Notifier(String host, String user, String password) {
        this.host = host;
        this.user = user;
        this.password = password;
    }

    public void sendEmail(String subject, String body, ArrayList<String> emailAddresses) {
        Properties props = System.getProperties();
        props.put("mail.smtps.host", host);
        props.put("mail.smtps.auth", "true");
        Session session = Session.getInstance(props, null);
        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(user));
            StringBuilder recipients = new StringBuilder();
            Boolean first = true;
            for (String emailAddress : emailAddresses) {
                if (!first) recipients.append(",");
                recipients.append(emailAddress);
                first = false;
            }
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients.toString(), false));
            msg.setSubject(subject);
            msg.setText(body);
            msg.setHeader("X-Mailer", "juno");
            msg.setSentDate(new Date());
            SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
            t.connect(host, user, password);
            t.sendMessage(msg, msg.getAllRecipients());
            System.out.println("Response: " + t.getLastServerResponse());
            t.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
