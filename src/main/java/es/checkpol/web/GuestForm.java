package es.checkpol.web;

import es.checkpol.domain.DocumentType;
import es.checkpol.domain.GuestSex;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.time.LocalDate;

public record GuestForm(
    @NotBlank(message = "Indica el nombre.")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
    String firstName,

    @NotBlank(message = "Indica el primer apellido.")
    @Size(max = 50, message = "El primer apellido no puede superar los 50 caracteres.")
    String lastName1,

    @Size(max = 50, message = "El segundo apellido no puede superar los 50 caracteres.")
    String lastName2,

    DocumentType documentType,

    @Size(max = 15, message = "El numero del documento no puede superar los 15 caracteres.")
    String documentNumber,

    @Size(max = 9, message = "El numero de soporte no puede superar los 9 caracteres.")
    String documentSupport,

    @NotNull(message = "Indica la fecha de nacimiento.")
    @Past(message = "La fecha de nacimiento debe estar en el pasado.")
    @DateTimeFormat(iso = ISO.DATE)
    LocalDate birthDate,

    @NotBlank(message = "Selecciona la nacionalidad.")
    @Size(max = 3, message = "La nacionalidad debe tener 3 letras.")
    String nationality,

    @NotNull(message = "Selecciona el sexo.")
    GuestSex sex,

    @NotNull(message = "Selecciona una direccion.")
    Long addressId,

    @Size(max = 20, message = "El telefono no puede superar los 20 caracteres.")
    String phone,

    @Size(max = 20, message = "El otro telefono no puede superar los 20 caracteres.")
    String phone2,

    @Email(message = "Indica un correo valido.")
    @Size(max = 250, message = "El correo no puede superar los 250 caracteres.")
    String email,

    @Size(max = 2, message = "El parentesco no puede superar los 2 caracteres.")
    String relationship
) {

    public GuestForm() {
        this("", "", "", null, "", "", null, "ESP", null, null, "", "", "", "");
    }

    public GuestForm withAddressId(Long newAddressId) {
        return new GuestForm(
            firstName,
            lastName1,
            lastName2,
            documentType,
            documentNumber,
            documentSupport,
            birthDate,
            nationality,
            sex,
            newAddressId,
            phone,
            phone2,
            email,
            relationship
        );
    }
}
