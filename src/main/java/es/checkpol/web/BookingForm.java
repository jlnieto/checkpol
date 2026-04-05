package es.checkpol.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.time.LocalDate;

public record BookingForm(
    @NotNull(message = "Selecciona una vivienda.")
    Long accommodationId,

    @Size(min = 3, max = 80, message = "La referencia debe tener entre 3 y 80 caracteres.")
    String referenceCode,

    @NotNull(message = "Indica cuántas personas vienen en la reserva.")
    @Min(value = 1, message = "Indica al menos 1 persona.")
    @Max(value = 20, message = "El número de personas no puede superar 20.")
    Integer personCount,

    @NotNull(message = "Indica la fecha en la que cerraste la estancia.")
    @DateTimeFormat(iso = ISO.DATE)
    LocalDate contractDate,

    @NotNull(message = "Indica la fecha de entrada.")
    @DateTimeFormat(iso = ISO.DATE)
    LocalDate checkInDate,

    @NotNull(message = "Indica la fecha de salida.")
    @DateTimeFormat(iso = ISO.DATE)
    LocalDate checkOutDate
) {

    public BookingForm() {
        this(null, "", null, null, null, null);
    }
}
