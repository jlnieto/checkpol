package es.checkpol.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = {GuestSelfServiceController.class, AddressController.class})
class GuestAccessExceptionAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    String handleGuestAccessError(
        IllegalArgumentException exception,
        HttpServletRequest request,
        HttpServletResponse response,
        Model model
    ) {
        if (!request.getRequestURI().startsWith("/guest-access/")) {
            throw exception;
        }

        boolean expired = exception.getMessage() != null && exception.getMessage().toLowerCase().contains("caduc");
        response.setStatus(expired ? HttpStatus.GONE.value() : HttpStatus.NOT_FOUND.value());
        model.addAttribute("title", expired ? "Enlace caducado" : "Enlace no disponible");
        model.addAttribute("message", expired
            ? "Este enlace ya no está activo."
            : "No hemos podido encontrar este enlace de check-in.");
        model.addAttribute("nextStep", "Pide a la persona responsable del alojamiento que te envíe un enlace nuevo.");
        return "public/guest-access-unavailable";
    }
}
