package de.openshopfront.platform.page;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Removes markup that is not on the approved list before rich text reaches a browser. */
public final class RichTextSanitizer {
  private static final Set<String> ALLOWED_ELEMENTS =
      Set.of(
          "a",
          "b",
          "blockquote",
          "br",
          "em",
          "h2",
          "h3",
          "h4",
          "i",
          "li",
          "ol",
          "p",
          "s",
          "strong",
          "sub",
          "sup",
          "u",
          "ul");
  private static final Pattern TAG =
      Pattern.compile("(<!--.*?-->)|(<\\/?[a-zA-Z][^<>]*?>)", Pattern.DOTALL);
  private static final Pattern NAME = Pattern.compile("^</?([a-zA-Z][a-zA-Z0-9]*)");
  private static final Pattern ATTRIBUTE = Pattern.compile("\\s([a-zA-Z-]+)\\s*=\\s*\"([^\"]*)\"");

  private RichTextSanitizer() {}

  /** Returns only approved elements and a validated link href. */
  public static String sanitize(String input) {
    if (input == null || input.isBlank()) return "";
    StringBuilder output = new StringBuilder();
    Matcher matcher = TAG.matcher(input);
    int position = 0;
    while (matcher.find()) {
      output.append(input, position, matcher.start());
      String tag = matcher.group(2);
      if (tag != null) appendTag(output, tag);
      position = matcher.end();
    }
    output.append(input, position, input.length());
    return output.toString().strip();
  }

  private static void appendTag(StringBuilder output, String tag) {
    Matcher nameMatcher = NAME.matcher(tag);
    if (!nameMatcher.find()) return;
    String element = nameMatcher.group(1).toLowerCase();
    if (!ALLOWED_ELEMENTS.contains(element)) return;
    boolean closing = tag.startsWith("</");
    output.append(closing ? "</" + element : "<" + element);
    if ("a".equals(element) && !closing) {
      String href = findHref(tag);
      if (href != null) output.append(" href=\"").append(href).append('"');
    }
    output.append(closing || tag.endsWith("/>") ? ">" : ">");
  }

  /** Keeps only http(s), relative, and plain-fragment link values. */
  public static String findHref(String tag) {
    Matcher attribute = ATTRIBUTE.matcher(tag);
    while (attribute.find()) {
      if ("href".equalsIgnoreCase(attribute.group(1))) {
        String value = attribute.group(2).strip();
        if ((value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/"))
            && !value.contains("\u0000")) return value;
      }
    }
    return null;
  }
}
