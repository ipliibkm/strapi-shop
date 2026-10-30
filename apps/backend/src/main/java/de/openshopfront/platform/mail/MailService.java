package de.openshopfront.platform.mail;

import de.openshopfront.platform.auth.GoogleAccessTokenService;
import de.openshopfront.platform.auth.GoogleAccessTokenService.AuthorizedToken;
import de.openshopfront.platform.auth.GoogleServiceConnection;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Sends platform emails through SMTP or the connected Google account. */
@Service
public class MailService {
  private static final Logger log = LoggerFactory.getLogger(MailService.class);

  /** Contains one text email to send. */
  public record OutgoingMail(String to, String subject, String body) {}

  private final JavaMailSender smtp;
  private final RestClient gmailApi;
  private final ObjectProvider<GoogleAccessTokenService> tokens;
  private final ObjectProvider<GoogleServiceConnection> connection;
  private final MeterRegistry metrics;
  private final String smtpHost;
  private final String fromAddress;

  public MailService(
      JavaMailSender smtp,
      ObjectProvider<GoogleAccessTokenService> tokens,
      ObjectProvider<GoogleServiceConnection> connection,
      @Value("${platform.mail.smtp-host:}") String smtpHost,
      @Value("${platform.mail.from-address:}") String fromAddress,
      @Value("${platform.mail.gmail-api-base-url:https://www.googleapis.com/gmail/v1}")
          String gmailBaseUrl,
      MeterRegistry metrics) {
    this.smtp = smtp;
    this.tokens = tokens;
    this.connection = connection;
    this.metrics = metrics;
    this.smtpHost = smtpHost == null ? "" : smtpHost.strip();
    this.fromAddress = fromAddress == null || fromAddress.isBlank() ? null : fromAddress.strip();
    this.gmailApi = RestClient.builder().baseUrl(gmailBaseUrl).build();
  }

  /** Sends one text email. A delivery failure never breaks the calling flow. */
  public boolean send(String to, String subject, String body) {
    return deliver(to, subject, body == null ? "" : body, false);
  }

  /** Sends one HTML email. A delivery failure never breaks the calling flow. */
  public boolean sendHtml(String to, String subject, String html) {
    return deliver(to, subject, html == null ? "" : html, true);
  }

  private boolean deliver(String to, String subject, String content, boolean html) {
    if (to == null || to.isBlank()) {
      log.warn("The mail was not sent: no recipient address is set.");
      return false;
    }
    if (!smtpHost.isEmpty()) {
      try {
        sendViaSmtp(to, subject, content, html);
        metrics.counter("platform.mail.sent", "provider", "smtp").increment();
        log.info("Sent mail over SMTP.");
        return true;
      } catch (Exception error) {
        metrics.counter("platform.mail.failed", "provider", "smtp").increment();
        log.warn(
            "The mail could not be sent over SMTP. Error type: {}.",
            error.getClass().getSimpleName());
        return false;
      }
    }
    if (tokens.getIfAvailable() == null || connection.getIfAvailable() == null) {
      log.warn(
          "The mail was not sent: set MAIL_HOST or connect a Google account with the gmail.send scope.");
      metrics.counter("platform.mail.failed", "provider", "none").increment();
      return false;
    }
    try {
      sendViaGoogle(to, subject, content, html);
      metrics.counter("platform.mail.sent", "provider", "google").increment();
      log.info("Sent mail through Google.");
      return true;
    } catch (Exception error) {
      metrics.counter("platform.mail.failed", "provider", "google").increment();
      log.warn(
          "The mail could not be sent through Google. Error type: {}.",
          error.getClass().getSimpleName());
      return false;
    }
  }

  private void sendViaSmtp(String to, String subject, String content, boolean html)
      throws Exception {
    MimeMessage message = smtp.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
    if (fromAddress != null) helper.setFrom(fromAddress);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(content, html);
    smtp.send(message);
  }

  private void sendViaGoogle(String to, String subject, String body, boolean html) {
    GoogleServiceConnection account = connection.getObject();
    AuthorizedToken token = tokens.getObject().authorize(account.owner());
    // Prefer the stored profile email; fall back to a live lookup when unset.
    String from = account.email();
    if (from.isBlank()) {
      from = tokens.getObject().profileEmail(account.owner(), token.value());
    }
    if (from.isBlank())
      throw new IllegalStateException("Connect a Google account before sending mail.");
    String raw = buildRawMime(from, to, subject, body, html);
    String encoded =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    gmailApi
        .post()
        .uri("/users/me/messages/send")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.value())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("raw", encoded))
        .retrieve()
        .toBodilessEntity();
  }

  /** Builds one RFC 5322 message accepted by the Gmail send API. */
  static String buildRawMime(String from, String to, String subject, String body, boolean html) {
    StringBuilder raw = new StringBuilder();
    raw.append("MIME-Version: 1.0\r\n");
    header(raw, "From", from);
    header(raw, "To", to);
    header(raw, "Subject", encodeHeaderValue(subject));
    raw.append(
        html
            ? "Content-Type: text/html; charset=UTF-8\r\n"
            : "Content-Type: text/plain; charset=UTF-8\r\n");
    raw.append("\r\n");
    if (body != null) {
      if (html) {
        raw.append(body).append("\r\n");
      } else {
        for (String line : body.replace("\r\n", "\n").split("\n", -1))
          raw.append(line).append("\r\n");
      }
    }
    return raw.toString();
  }

  private static void header(StringBuilder raw, String name, String value) {
    raw.append(name).append(": ").append(value == null ? "" : value).append("\r\n");
  }

  /** Encodes non-ASCII header values with RFC 2047 B-encoding. */
  private static String encodeHeaderValue(String value) {
    if (value == null || value.isEmpty()) return "";
    boolean plain = true;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c < 32 || c >= 127) {
        plain = false;
        break;
      }
    }
    if (plain) return value;
    String encoded =
        Base64.getEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    return "=?UTF-8?B?" + encoded + "?=";
  }
}
