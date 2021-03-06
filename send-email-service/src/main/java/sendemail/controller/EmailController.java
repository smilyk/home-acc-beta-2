package sendemail.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sendemail.dto.ChangePasswordDto;
import sendemail.dto.EmailDto;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@RestController
public class EmailController {
    public static final String APPLICATION_NAME = " Home Accounting ";
    @Autowired
    JavaMailSender emailSender;
    @Autowired
    Environment env;

    @PostMapping("/verification-email")
    String sendSimpleEmail(@RequestBody EmailDto mail) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        String email = mail.getEmail();
        String tokenValue = mail.getTokenValue();
        String lastName = mail.getUserLastName();
        String firstName = mail.getUserName();

        String VERIFY_LINK = env.getProperty("verification.link");

        boolean multipart = true;

        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "utf-8");
//        The HTML body for the email.
        final String htmlMsg =
                "<h1> Hi " + firstName + " " + lastName + ". Please verify your email address</h1>"
                        + "<p>Thank you for registering with our app. To complete registration process and be able to log in,"
                        + " click on the following link: "
                        + " <a href='"+VERIFY_LINK + "?token=" + tokenValue+"'>" + "<br/><br/>" +" Final step to complete your registration" +
                        "</a><br/><br/>"
                        + "Thank you! And we are waiting for you inside!";

        message.setContent(htmlMsg, "text/html");
        helper.setTo(email);
        helper.setSubject("Verification email from" + APPLICATION_NAME);
        try{
            this.emailSender.send(message);
        }catch (Exception ex){
            System.out.println(ex.getMessage());
        }
        return "Email send!";
    }

    @PostMapping("/change-password")
    String sendPasswordResetRequest(@RequestBody ChangePasswordDto mail) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();

        String VERIFY_LINK = env.getProperty("change.password.link");

        boolean multipart = true;
        String email = mail.getEmail();
        String token = mail.getToken();
        String firstName = mail.getFirstName();
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "utf-8");
//        The HTML body for the email.
        final String htmlMsg = "A request to reset your password "
                + "Hi, " +  firstName+ "! "
                + "Someone has requested to reset your password with our project. If it were not you, please ignore it."
                + " otherwise please open the link below in your browser window to set a new password:"
                + " <a href='"+VERIFY_LINK + "?token=" + token + "'>"
//                + " http://localhost:8080/verification-service/password-reset.html?token=$tokenValue"
                + " Thank you!";

        message.setContent(htmlMsg, "text/html");
        helper.setTo(email);
        helper.setSubject("Change password email from" + APPLICATION_NAME);
        this.emailSender.send(message);
        return "Email send!";
    }


    @PostMapping("/clean-not-confirmed-mail")
    String sendCleanNotConfirmEmails() throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();

        String ADMIN_EMAIL = env.getProperty("admin.email.address");

        boolean multipart = true;
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "utf-8");
//        The HTML body for the email.
        final String htmlMsg =
                 "Hi, admin ! "
                + "Users with not confirmed emails was deleted from data base";

        message.setContent(htmlMsg, "text/html");

        helper.setTo(ADMIN_EMAIL);
        helper.setSubject("Cleaning email from " + APPLICATION_NAME);
        this.emailSender.send(message);
        return "Email send!";
    }

    @PostMapping("/error-mail")
    String sendErrorEmail() throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();

        String ADMIN_EMAIL = env.getProperty("admin.email.address");

        boolean multipart = true;
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "utf-8");
//        The HTML body for the email.
        final String htmlMsg =
                 "Hi, admin ! "
                + "something was wrong during deleting users with not confirm" +
                         "emails from data base" +
                         "<p> Please, check it";

        message.setContent(htmlMsg, "text/html");

        helper.setTo(ADMIN_EMAIL);
        helper.setSubject("Cleaning email from " + APPLICATION_NAME);
        this.emailSender.send(message);
        return "Email send!";
    }

    @PostMapping("/parse/israell")
    String sendCParseIsrBank() throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();

        String ADMIN_EMAIL = env.getProperty("admin.email.address");

        boolean multipart = true;
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "utf-8");
//        The HTML body for the email.
        final String htmlMsg =
                "Hi, admin ! "
                        + "Currency from Israel Bank added to dataBase";

        message.setContent(htmlMsg, "text/html");

        helper.setTo(ADMIN_EMAIL);
        helper.setSubject("Parse Israel Bank " + APPLICATION_NAME);
        this.emailSender.send(message);
        return "Email send!";
    }

    @PostMapping("/parse/ukraine")
    String sendCParseUkrBank() throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();

        String ADMIN_EMAIL = env.getProperty("admin.email.address");

        boolean multipart = true;
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "utf-8");
//        The HTML body for the email.
        final String htmlMsg =
                "Hi, admin ! "
                        + "Currency from Ukrainian Bank added to dataBase";

        message.setContent(htmlMsg, "text/html");

        helper.setTo(ADMIN_EMAIL);
        helper.setSubject("Parse Ukr Bank " + APPLICATION_NAME);
        this.emailSender.send(message);
        return "Email send!";
    }

}

