package es.checkpol.web;

public record PublicGuestCard(
    String title,
    String detail,
    String actionLabel,
    String href,
    String statusLabel,
    String statusClass,
    boolean fullWidth
) {
}
