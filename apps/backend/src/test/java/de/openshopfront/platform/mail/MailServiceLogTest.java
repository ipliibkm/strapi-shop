package de.openshopfront.platform.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/** Verifies that mail delivery logs do not contain recipient addresses. */
class MailServiceLogTest {
  @Test
  void successLogDoesNotContainRecipientAddress() {
    JavaMailSender smtp = mock(JavaMailSender.class);
    when(smtp.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    MailService mail = service(smtp);

    ListAppender<ILoggingEvent> logs = startCapture();
    try {
      assertThat(mail.send("private.customer@example.com", "Subject", "Body")).isTrue();
      assertThat(messages(logs)).doesNotContain("private.customer@example.com");
    } finally {
      stopCapture(logs);
    }
  }

  @Test
  void failureLogDoesNotContainRecipientAddress() {
    JavaMailSender smtp = mock(JavaMailSender.class);
    when(smtp.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    doThrow(new MailSendException("Provider rejected private.customer@example.com"))
        .when(smtp)
        .send(any(MimeMessage.class));
    MailService mail = service(smtp);

    ListAppender<ILoggingEvent> logs = startCapture();
    try {
      assertThat(mail.send("private.customer@example.com", "Subject", "Body")).isFalse();
      assertThat(messages(logs)).doesNotContain("private.customer@example.com");
      assertThat(logs.list).allMatch(event -> event.getThrowableProxy() == null);
    } finally {
      stopCapture(logs);
    }
  }

  @SuppressWarnings("unchecked")
  private static MailService service(JavaMailSender smtp) {
    return new MailService(
        smtp,
        mock(ObjectProvider.class),
        mock(ObjectProvider.class),
        "smtp.example.com",
        "sender@example.com",
        "https://www.googleapis.com/gmail/v1",
        new SimpleMeterRegistry());
  }

  private static ListAppender<ILoggingEvent> startCapture() {
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger().addAppender(appender);
    return appender;
  }

  private static void stopCapture(ListAppender<ILoggingEvent> appender) {
    logger().detachAppender(appender);
    appender.stop();
  }

  private static Logger logger() {
    return (Logger) LoggerFactory.getLogger(MailService.class);
  }

  private static String messages(ListAppender<ILoggingEvent> appender) {
    return appender.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .reduce("", (left, right) -> left + '\n' + right);
  }
}
