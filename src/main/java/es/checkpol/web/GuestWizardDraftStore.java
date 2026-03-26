package es.checkpol.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GuestWizardDraftStore {

    public Optional<GuestForm> getBookingDraft(HttpSession session, Long bookingId, Long guestId) {
        return read(session, bookingKey(bookingId, guestId));
    }

    public void saveBookingDraft(HttpSession session, Long bookingId, Long guestId, GuestForm form) {
        session.setAttribute(bookingKey(bookingId, guestId), form);
    }

    public void clearBookingDraft(HttpSession session, Long bookingId, Long guestId) {
        session.removeAttribute(bookingKey(bookingId, guestId));
    }

    public Optional<GuestForm> getPublicDraft(HttpSession session, String token, Long guestId, Integer slot, boolean extra) {
        return read(session, publicKey(token, guestId, slot, extra));
    }

    public void savePublicDraft(HttpSession session, String token, Long guestId, Integer slot, boolean extra, GuestForm form) {
        session.setAttribute(publicKey(token, guestId, slot, extra), form);
    }

    public void clearPublicDraft(HttpSession session, String token, Long guestId, Integer slot, boolean extra) {
        session.removeAttribute(publicKey(token, guestId, slot, extra));
    }

    private Optional<GuestForm> read(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        if (value instanceof GuestForm guestForm) {
            return Optional.of(guestForm);
        }
        return Optional.empty();
    }

    private String bookingKey(Long bookingId, Long guestId) {
        return "guest-wizard-draft:booking:" + bookingId + ":" + (guestId == null ? "new" : guestId);
    }

    private String publicKey(String token, Long guestId, Integer slot, boolean extra) {
        String guestPart = guestId == null ? "new" : guestId.toString();
        String slotPart = slot == null ? "none" : slot.toString();
        return "guest-wizard-draft:public:" + token + ":" + guestPart + ":" + slotPart + ":" + extra;
    }
}
