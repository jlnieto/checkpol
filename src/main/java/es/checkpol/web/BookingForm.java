package es.checkpol.web;

import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BookingForm(
    @NotNull(message = "Selecciona una vivienda.")
    Long accommodationId,

    @NotNull(message = "Selecciona un canal.")
    BookingChannel channel,

    @Size(min = 3, max = 80, message = "La referencia debe tener entre 3 y 80 caracteres.")
    String referenceCode,

    @NotNull(message = "Indica la fecha en la que cerraste la estancia.")
    LocalDate contractDate,

    @NotNull(message = "Indica la fecha de entrada.")
    LocalDate checkInDate,

    @NotNull(message = "Indica la fecha de salida.")
    LocalDate checkOutDate,

    @NotNull(message = "Selecciona un tipo de pago.")
    PaymentType paymentType,

    LocalDate paymentDate,

    @Size(max = 50, message = "La forma de pago no puede superar los 50 caracteres.")
    String paymentMethod,

    @Size(max = 100, message = "El nombre del pagador no puede superar los 100 caracteres.")
    String paymentHolder,

    @Size(max = 7, message = "La caducidad no puede superar los 7 caracteres.")
    String cardExpiry
) {

    public BookingForm() {
        this(null, BookingChannel.DIRECT, "", null, null, null, PaymentType.EFECT, null, "", "", "");
    }
}
