package es.checkpol.web;

import es.checkpol.domain.CommunicationDispatchMode;
import es.checkpol.domain.CommunicationDispatchStatus;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.service.BookingDetails;
import es.checkpol.service.BookingOperationalStatus;

import java.util.Optional;

public record BookingDetailView(
    String statusLabel,
    String statusClass,
    String bannerClass,
    String heroTitle,
    String heroCopy,
    String primaryAction,
    Long pendingReviewGuestId,
    Long lastCommunicationId,
    boolean sesLocked,
    boolean showWhatsappAction,
    boolean showManualGuestAction,
    boolean showCancelSesAction,
    boolean showDownloadXmlSecondaryAction,
    boolean canAddGuestFromGuestsSection,
    boolean showSelfServiceOptions,
    boolean showCommunicationSummary,
    String communicationSummaryTitle,
    String communicationSummaryStatusLabel,
    String communicationSummaryStatusClass,
    String communicationSummaryCopy,
    String communicationSummaryMeta
) {

    public static BookingDetailView from(BookingDetails details, boolean shareMessagePrepared) {
        GeneratedCommunication communication = details.lastGeneratedCommunication().orElse(null);
        CommunicationDispatchStatus status = communication == null ? null : communication.getDispatchStatus();
        boolean duplicateFailureNeedsChanges = status == CommunicationDispatchStatus.SUBMISSION_FAILED
            && Integer.valueOf(10121).equals(communication.getSesResponseCode());
        boolean duplicateFailure = duplicateFailureNeedsChanges && details.lastGeneratedCommunicationMatchesCurrentXml();
        boolean duplicateFailureResolved = duplicateFailureNeedsChanges && !details.lastGeneratedCommunicationMatchesCurrentXml();
        boolean cancelledSameXml = status == CommunicationDispatchStatus.SES_CANCELLED
            && details.lastGeneratedCommunicationMatchesCurrentXml();
        boolean cancelledResolved = status == CommunicationDispatchStatus.SES_CANCELLED
            && !details.lastGeneratedCommunicationMatchesCurrentXml();
        boolean sameXmlBlocked = duplicateFailure || cancelledSameXml;
        boolean sesLocked = status == CommunicationDispatchStatus.SUBMITTED_TO_SES
            || status == CommunicationDispatchStatus.SES_PROCESSED
            || status == CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW;
        boolean canSubmitToSes = details.readyForTravelerPart()
            && details.sesSubmissionAvailable()
            && !sameXmlBlocked
            && (status == null || !sesLocked);
        boolean waitingForGuests = details.operationalStatus() == BookingOperationalStatus.WAITING_GUESTS
            || (details.operationalStatus() == BookingOperationalStatus.GUEST_COUNT_MISMATCH
            && details.guestCount() < details.expectedGuestCount());
        boolean guestCountNeedsReview = details.operationalStatus() == BookingOperationalStatus.GUEST_COUNT_MISMATCH
            && details.guestCount() >= details.expectedGuestCount();

        String primaryAction = primaryAction(
            details,
            status,
            shareMessagePrepared,
            waitingForGuests,
            guestCountNeedsReview,
            duplicateFailure,
            cancelledSameXml,
            canSubmitToSes
        );

        return new BookingDetailView(
            statusLabel(details, status, duplicateFailure, duplicateFailureResolved, cancelledResolved),
            statusClass(details, status, duplicateFailure),
            details.readyForTravelerPart() ? "owner-banner" : "owner-banner owner-banner-danger",
            heroTitle(details, status, shareMessagePrepared, duplicateFailure, duplicateFailureResolved, cancelledSameXml, cancelledResolved),
            heroCopy(details, status, shareMessagePrepared, duplicateFailure, duplicateFailureResolved, cancelledSameXml, cancelledResolved),
            primaryAction,
            firstPendingReviewGuestId(details.guests()).orElse(null),
            communication == null ? null : communication.getId(),
            sesLocked,
            waitingForGuests && shareMessagePrepared,
            waitingForGuests,
            details.readyForTravelerPart()
                && communication != null
                && communication.getSesLoteCode() != null
                && status != CommunicationDispatchStatus.SES_CANCELLED,
            details.readyForTravelerPart() && !details.sesSubmissionAvailable(),
            !sesLocked && !waitingForGuests,
            details.readyForTravelerPart() && !sesLocked,
            communication != null,
            communicationSummaryTitle(communication),
            communicationSummaryStatusLabel(communication),
            communicationSummaryStatusClass(communication),
            communicationSummaryCopy(communication, details),
            communicationSummaryMeta(communication)
        );
    }

    private static String communicationSummaryTitle(GeneratedCommunication communication) {
        if (communication == null) {
            return "";
        }
        return communication.getDispatchMode() == CommunicationDispatchMode.SES_WEB_SERVICE
            ? "Última comunicación SES"
            : "Último archivo XML";
    }

    private static String communicationSummaryStatusLabel(GeneratedCommunication communication) {
        if (communication == null) {
            return "";
        }
        return switch (communication.getDispatchStatus()) {
            case SUBMITTED_TO_SES -> "Pendiente en SES";
            case SES_PROCESSED -> "Presentada en SES";
            case SES_CANCELLED -> "Anulada en SES";
            case SES_RESPONSE_NEEDS_REVIEW -> "Revisión técnica";
            case SUBMISSION_FAILED, SES_PROCESSING_ERROR -> "Error en SES";
            case XML_READY -> communication.getDispatchMode() == CommunicationDispatchMode.SES_WEB_SERVICE
                ? "Pendiente de presentar"
                : "XML generado";
        };
    }

    private static String communicationSummaryStatusClass(GeneratedCommunication communication) {
        if (communication == null) {
            return "owner-status-neutral";
        }
        return switch (communication.getDispatchStatus()) {
            case SUBMISSION_FAILED, SES_PROCESSING_ERROR, SES_RESPONSE_NEEDS_REVIEW -> "owner-status-error";
            case SUBMITTED_TO_SES, XML_READY -> "owner-status-pending";
            case SES_CANCELLED -> "owner-status-neutral";
            case SES_PROCESSED -> "owner-status-ready";
        };
    }

    private static String communicationSummaryCopy(GeneratedCommunication communication, BookingDetails details) {
        if (communication == null) {
            return "";
        }
        return switch (communication.getDispatchStatus()) {
            case SUBMITTED_TO_SES -> "SES está procesando el envío. Puedes consultar el estado desde el botón principal.";
            case SES_PROCESSED -> "SES confirmó la comunicación de esta estancia.";
            case SES_CANCELLED -> details.lastGeneratedCommunicationMatchesCurrentXml()
                ? "La comunicación está anulada. Para presentar de nuevo, cambia algún dato antes."
                : "La comunicación está anulada y los datos ya han cambiado. Puedes preparar un nuevo envío.";
            case SES_RESPONSE_NEEDS_REVIEW -> "SES respondió de forma inesperada. La incidencia queda visible para administración.";
            case SUBMISSION_FAILED -> nonBlankOrDefault(
                communication.getSesResponseDescription(),
                "SES rechazó el envío. Revisa el siguiente paso indicado arriba."
            );
            case SES_PROCESSING_ERROR -> nonBlankOrDefault(
                communication.getSesProcessingErrorDescription(),
                "SES devolvió un error al procesar el lote. La incidencia queda visible para administración."
            );
            case XML_READY -> communication.getDispatchMode() == CommunicationDispatchMode.SES_WEB_SERVICE
                ? "El XML está preparado, pero todavía no se ha presentado en SES."
                : "Archivo preparado para presentación manual.";
        };
    }

    private static String communicationSummaryMeta(GeneratedCommunication communication) {
        if (communication == null) {
            return "";
        }
        if (communication.getSesCommunicationCode() != null && !communication.getSesCommunicationCode().isBlank()) {
            return "Código SES disponible";
        }
        if (communication.getSesLoteCode() != null && !communication.getSesLoteCode().isBlank()) {
            return "Lote SES registrado";
        }
        if (communication.getDispatchMode() == CommunicationDispatchMode.MANUAL_DOWNLOAD) {
            return communication.getDownloadCount() == 1 ? "1 descarga" : communication.getDownloadCount() + " descargas";
        }
        return "Versión " + communication.getVersion();
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String primaryAction(
        BookingDetails details,
        CommunicationDispatchStatus status,
        boolean shareMessagePrepared,
        boolean waitingForGuests,
        boolean guestCountNeedsReview,
        boolean duplicateFailure,
        boolean cancelledSameXml,
        boolean canSubmitToSes
    ) {
        if (details.operationalStatus() == BookingOperationalStatus.REVIEW_PENDING) {
            return "REVIEW_GUEST";
        }
        if (waitingForGuests) {
            return shareMessagePrepared ? "COPY_SHARE_MESSAGE" : "SEND_LINK";
        }
        if (guestCountNeedsReview) {
            return "REVIEW_BOOKING";
        }
        if (details.readyForTravelerPart()) {
            if ((status == CommunicationDispatchStatus.SUBMITTED_TO_SES
                || status == CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW)
                && details.lastGeneratedCommunication().map(GeneratedCommunication::getSesLoteCode).isPresent()) {
                return "REFRESH_SES";
            }
            if (duplicateFailure || cancelledSameXml) {
                return "EDIT_BOOKING";
            }
            if (canSubmitToSes) {
                return "SUBMIT_SES";
            }
            if (!details.sesSubmissionAvailable()) {
                return "DOWNLOAD_XML";
            }
            return "NONE";
        }
        return "REVIEW_BOOKING";
    }

    private static Optional<Long> firstPendingReviewGuestId(Iterable<Guest> guests) {
        for (Guest guest : guests) {
            if (guest.getReviewStatus() == GuestReviewStatus.PENDING_REVIEW) {
                return Optional.ofNullable(guest.getId());
            }
        }
        return Optional.empty();
    }

    private static String statusLabel(
        BookingDetails details,
        CommunicationDispatchStatus status,
        boolean duplicateFailure,
        boolean duplicateFailureResolved,
        boolean cancelledResolved
    ) {
        if (!details.readyForTravelerPart()) {
            return details.operationalStatus().getLabel();
        }
        if (duplicateFailure) {
            return "Revisar antes de reenviar";
        }
        if (duplicateFailureResolved || cancelledResolved) {
            return "Lista para reenviar";
        }
        if (status == CommunicationDispatchStatus.SUBMITTED_TO_SES) {
            return "Pendiente en SES";
        }
        if (status == CommunicationDispatchStatus.SES_PROCESSED) {
            return "Presentada en SES";
        }
        if (status == CommunicationDispatchStatus.SES_CANCELLED) {
            return "Anulada en SES";
        }
        if (status == CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW) {
            return "Revisión técnica SES";
        }
        if (status == CommunicationDispatchStatus.XML_READY && details.sesSubmissionAvailable()) {
            return "Pendiente de presentar";
        }
        return details.sesSubmissionAvailable() ? "Lista para presentar" : "Lista para XML";
    }

    private static String statusClass(BookingDetails details, CommunicationDispatchStatus status, boolean duplicateFailure) {
        if (duplicateFailure || status == CommunicationDispatchStatus.SUBMISSION_FAILED
            || status == CommunicationDispatchStatus.SES_PROCESSING_ERROR
            || status == CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW) {
            return "owner-status-error";
        }
        if (!details.readyForTravelerPart()
            || status == CommunicationDispatchStatus.SUBMITTED_TO_SES
            || status == CommunicationDispatchStatus.XML_READY) {
            return "owner-status-pending";
        }
        if (status == CommunicationDispatchStatus.SES_CANCELLED) {
            return "owner-status-neutral";
        }
        return "owner-status-ready";
    }

    private static String heroTitle(
        BookingDetails details,
        CommunicationDispatchStatus status,
        boolean shareMessagePrepared,
        boolean duplicateFailure,
        boolean duplicateFailureResolved,
        boolean cancelledSameXml,
        boolean cancelledResolved
    ) {
        if (cancelledSameXml) {
            return "Cambia algo antes de presentar";
        }
        if (duplicateFailure) {
            return "Cambia algo antes de reenviar";
        }
        if (cancelledResolved || duplicateFailureResolved) {
            return "Ya puedes reenviar a SES";
        }
        if (status == CommunicationDispatchStatus.SUBMITTED_TO_SES) {
            return "SES está procesando el envío";
        }
        if (status == CommunicationDispatchStatus.SES_PROCESSED) {
            return "La comunicación ya está presentada en SES";
        }
        if (status == CommunicationDispatchStatus.SES_CANCELLED) {
            return "La comunicación está anulada en SES";
        }
        if (status == CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW) {
            return "Respuesta SES pendiente de revisión";
        }
        if (status == CommunicationDispatchStatus.XML_READY && details.sesSubmissionAvailable()) {
            return "Falta presentar el parte en SES";
        }
        if (details.readyForTravelerPart()) {
            return details.sesSubmissionAvailable()
                ? "Todo está listo para presentar en SES"
                : "Todo está listo para descargar el archivo";
        }
        if (details.operationalStatus() == BookingOperationalStatus.REVIEW_PENDING) {
            return "Hay datos pendientes de revisar";
        }
        if (details.operationalStatus() == BookingOperationalStatus.WAITING_GUESTS
            || details.operationalStatus() == BookingOperationalStatus.GUEST_COUNT_MISMATCH) {
            return shareMessagePrepared ? "Enlace preparado" : "Faltan los datos de los huéspedes";
        }
        return "Todavía no se puede preparar el archivo";
    }

    private static String heroCopy(
        BookingDetails details,
        CommunicationDispatchStatus status,
        boolean shareMessagePrepared,
        boolean duplicateFailure,
        boolean duplicateFailureResolved,
        boolean cancelledSameXml,
        boolean cancelledResolved
    ) {
        if (details.operationalStatus() == BookingOperationalStatus.REVIEW_PENDING) {
            return details.pendingReviewGuestCount() == 1
                ? "Hay 1 huésped pendiente de revisión."
                : "Hay " + details.pendingReviewGuestCount() + " huéspedes pendientes de revisión.";
        }
        if (details.operationalStatus() == BookingOperationalStatus.WAITING_GUESTS
            || (details.operationalStatus() == BookingOperationalStatus.GUEST_COUNT_MISMATCH
            && details.guestCount() < details.expectedGuestCount())) {
            if (shareMessagePrepared) {
                return "Copia el mensaje o ábrelo en WhatsApp para que completen los datos.";
            }
            return "Envíales el enlace para que completen sus datos. Si no pueden, puedes añadirlos tú. Si no falta nadie, entra en Editar estancia y cambia el número de personas.";
        }
        if (cancelledSameXml) {
            return "SES aceptó la anulación. Para presentar de nuevo tienes que cambiar algún dato de la estancia o de un huésped. SES no acepta el mismo XML otra vez.";
        }
        if (cancelledResolved) {
            return "Los datos han cambiado respecto al XML anulado. El siguiente paso es presentar el nuevo parte en SES.";
        }
        if (duplicateFailure) {
            return "SES rechazó el reenvío porque el archivo es idéntico a un lote ya registrado. Edita la estancia o algún huésped antes de reenviar.";
        }
        if (duplicateFailureResolved) {
            return "Los datos han cambiado respecto al último XML rechazado. Ya puedes presentar de nuevo el parte.";
        }
        if (status == CommunicationDispatchStatus.SUBMITTED_TO_SES) {
            return "El envío ya salió hacia SES. Consulta el estado del lote para saber si quedó procesado.";
        }
        if (status == CommunicationDispatchStatus.SES_PROCESSED) {
            return "SES ya devolvió código de comunicación para esta estancia.";
        }
        if (status == CommunicationDispatchStatus.SES_CANCELLED) {
            return "SES aceptó la anulación. Si necesitas reenviar, primero cambia algún dato.";
        }
        if (status == CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW) {
            return "SES respondió de forma inesperada. Revisa la incidencia antes de repetir o anular.";
        }
        if (status == CommunicationDispatchStatus.XML_READY && details.sesSubmissionAvailable()) {
            return "El XML ya se generó, pero todavía no se ha enviado a SES desde Checkpol.";
        }
        if (details.readyForTravelerPart()) {
            return details.sesSubmissionAvailable()
                ? "Todos los huéspedes están revisados. El siguiente paso es presentar el parte en SES."
                : "Todos los huéspedes están revisados. El siguiente paso es descargar el XML.";
        }
        return details.blockingSummary() == null ? "Revisa la estancia para ver qué falta." : details.blockingSummary();
    }
}
