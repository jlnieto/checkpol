package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accommodations")
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "ses_establishment_code", length = 10)
    private String sesEstablishmentCode;

    @Column(name = "registration_number", length = 40)
    private String registrationNumber;

    protected Accommodation() {
    }

    public Accommodation(String name, String sesEstablishmentCode, String registrationNumber) {
        this.name = name;
        this.sesEstablishmentCode = sesEstablishmentCode;
        this.registrationNumber = registrationNumber;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSesEstablishmentCode() {
        return sesEstablishmentCode;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void update(String name, String sesEstablishmentCode, String registrationNumber) {
        this.name = name;
        this.sesEstablishmentCode = sesEstablishmentCode;
        this.registrationNumber = registrationNumber;
    }
}
