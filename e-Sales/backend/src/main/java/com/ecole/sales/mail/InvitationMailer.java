package com.ecole.sales.mail;

import com.ecole.sales.config.AppProperties;
import com.ecole.sales.user.SalesUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class InvitationMailer {
    private final JavaMailSender sender; private final AppProperties properties; private final String host;
    public InvitationMailer(JavaMailSender sender,AppProperties properties,@Value("${spring.mail.host:}") String host){this.sender=sender;this.properties=properties;this.host=host;}
    public Result send(SalesUser user,String link){
        if(host==null||host.isBlank())return new Result(false,"Email delivery is not configured; share the invitation link directly.");
        try{
            SimpleMailMessage message=new SimpleMailMessage();message.setFrom(properties.getMailFrom());message.setTo(user.getEmail());
            message.setSubject("Your e-Sales account invitation");message.setText("Set your password using this one-time link (expires in 7 days):\n"+link);
            sender.send(message);return new Result(true,null);
        }catch(MailException ex){return new Result(false,"Invitation email could not be sent; share the invitation link directly.");}
    }
    public record Result(boolean sent,String message){}
}
