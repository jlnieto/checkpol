package es.checkpol.web;

public record GuestLinkShareMessage(
    String linkUrl,
    String messageText,
    String whatsappUrl
) {
}
