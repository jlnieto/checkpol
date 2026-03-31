package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "accommodations")
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "ses_establishment_code", length = 10)
    private String sesEstablishmentCode;

    @Column(name = "registration_number", length = 40)
    private String registrationNumber;

    @Column(name = "room_count")
    private Integer roomCount;

    protected Accommodation() {
    }

    public Accommodation(AppUser owner, String name, String sesEstablishmentCode, String registrationNumber) {
        this(owner, name, sesEstablishmentCode, registrationNumber, null);
    }

    public Accommodation(AppUser owner, String name, String sesEstablishmentCode, String registrationNumber, Integer roomCount) {
        this.owner = owner;
        this.name = name;
        this.sesEstablishmentCode = sesEstablishmentCode;
        this.registrationNumber = registrationNumber;
        this.roomCount = roomCount;
    }

    public Accommodation(String name, String sesEstablishmentCode, String registrationNumber) {
        this(null, name, sesEstablishmentCode, registrationNumber, null);
    }

    public Accommodation(String name, String sesEstablishmentCode, String registrationNumber, Integer roomCount) {
        this(null, name, sesEstablishmentCode, registrationNumber, roomCount);
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
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

    public Integer getRoomCount() {
        return roomCount;
    }

    public void update(String name, String sesEstablishmentCode, String registrationNumber) {
        update(name, sesEstablishmentCode, registrationNumber, roomCount);
    }

    public void update(String name, String sesEstablishmentCode, String registrationNumber, Integer roomCount) {
        this.name = name;
        this.sesEstablishmentCode = sesEstablishmentCode;
        this.registrationNumber = registrationNumber;
        this.roomCount = roomCount;
    }
}
