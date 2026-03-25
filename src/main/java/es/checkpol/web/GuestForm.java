package es.checkpol.web;

import es.checkpol.domain.DocumentType;
import es.checkpol.domain.GuestSex;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

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
    LocalDate birthDate,

    @Size(max = 3, message = "La nacionalidad debe tener 3 letras.")
    String nationality,

    GuestSex sex,

    @NotBlank(message = "Indica la direccion.")
    @Size(max = 120, message = "La direccion no puede superar los 120 caracteres.")
    String addressLine,

    @Size(max = 120, message = "La informacion extra de direccion no puede superar los 120 caracteres.")
    String addressComplement,

    @Size(max = 5, message = "El codigo del municipio no puede superar los 5 caracteres.")
    String municipalityCode,

    @Size(max = 80, message = "El nombre de municipio no puede superar los 80 caracteres.")
    String municipalityName,

    @NotBlank(message = "Indica el codigo postal.")
    @Size(max = 12, message = "El codigo postal no puede superar los 12 caracteres.")
    String postalCode,

    @NotBlank(message = "Indica el pais con 3 letras, por ejemplo ESP.")
    @Size(min = 3, max = 3, message = "El pais debe tener 3 letras.")
    String country,

    @Size(max = 20, message = "El telefono no puede superar los 20 caracteres.")
    String phone,

    @Size(max = 20, message = "El otro telefono no puede superar los 20 caracteres.")
    String phone2,

    @Email(message = "Indica un correo valido.")
    @Size(max = 250, message = "El correo no puede superar los 250 caracteres.")
    String email,

    @Size(max = 5, message = "El parentesco no puede superar los 5 caracteres.")
    String relationship
) {

    public GuestForm() {
        this("", "", "", null, "", "", null, "ESP", null, "", "", "", "", "", "ESP", "", "", "", "");
    }
}
