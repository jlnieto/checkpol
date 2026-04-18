package es.checkpol.web;

import es.checkpol.domain.AppIncident;
import es.checkpol.domain.AppIncidentArea;
import es.checkpol.service.AppIncidentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
class GlobalExceptionAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionAdvice.class);

    private final ObjectProvider<AppIncidentService> appIncidentServiceProvider;

    GlobalExceptionAdvice(ObjectProvider<AppIncidentService> appIncidentServiceProvider) {
        this.appIncidentServiceProvider = appIncidentServiceProvider;
    }

    @ExceptionHandler(Exception.class)
    String handleUnexpectedException(
        Exception exception,
        HttpServletRequest request,
        HttpServletResponse response,
        Model model
    ) throws Exception {
        if (exception instanceof NoResourceFoundException) {
            throw exception;
        }

        AppIncident incident = registerIncident(request, exception);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        boolean publicGuestArea = incident != null && incident.getArea() == AppIncidentArea.PUBLIC_GUEST;
        model.addAttribute("title", publicGuestArea ? "No hemos podido completar la acción" : "Ha ocurrido un problema");
        model.addAttribute("message", publicGuestArea
            ? "Pide a la persona responsable del alojamiento que revise la incidencia."
            : "Hemos guardado la incidencia para revisarla desde administración.");
        model.addAttribute("nextStep", publicGuestArea
            ? "Indica este código si necesitas ayuda: " + code(incident)
            : "Código de incidencia: " + code(incident));
        model.addAttribute("incidentCode", code(incident));
        model.addAttribute("homeHref", incident != null && incident.getArea() == AppIncidentArea.ADMIN ? "/admin" : "/bookings");
        model.addAttribute("homeLabel", incident != null && incident.getArea() == AppIncidentArea.ADMIN ? "Volver al admin" : "Volver al inicio");
        return publicGuestArea ? "public/guest-access-unavailable" : "error";
    }

    private AppIncident registerIncident(HttpServletRequest request, Exception exception) {
        try {
            AppIncidentService appIncidentService = appIncidentServiceProvider.getIfAvailable();
            if (appIncidentService == null) {
                LOGGER.error("Unexpected application error and incident service is not available.", exception);
                return null;
            }
            AppIncident incident = appIncidentService.registerUnexpectedException(request, exception);
            LOGGER.error("Unexpected application incident {}", incident.getCode(), exception);
            return incident;
        } catch (RuntimeException registerException) {
            LOGGER.error("Unexpected application error and incident registration failed.", exception);
            LOGGER.error("Incident registration failure.", registerException);
            return null;
        }
    }

    private String code(AppIncident incident) {
        return incident == null ? "INC-NO-GUARDADA" : incident.getCode();
    }
}
