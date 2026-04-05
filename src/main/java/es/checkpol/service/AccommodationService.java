package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.repository.AccommodationRepository;
import es.checkpol.web.AccommodationForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final CurrentAppUserService currentAppUserService;

    public AccommodationService(
        AccommodationRepository accommodationRepository,
        CurrentAppUserService currentAppUserService
    ) {
        this.accommodationRepository = accommodationRepository;
        this.currentAppUserService = currentAppUserService;
    }

    @Transactional(readOnly = true)
    public List<Accommodation> findAll() {
        return accommodationRepository.findAllByOwnerIdOrderByNameAsc(currentAppUserService.requireCurrentUserId());
    }

    @Transactional
    public Accommodation create(AccommodationForm form) {
        var currentUser = currentAppUserService.requireCurrentUserEntity();
        Accommodation accommodation = new Accommodation(
            currentUser,
            form.name().trim(),
            normalizeSesCode(form.sesEstablishmentCode()),
            normalize(form.registrationNumber()),
            form.roomCount()
        );
        return accommodationRepository.save(accommodation);
    }

    @Transactional(readOnly = true)
    public AccommodationForm getForm(Long id) {
        Accommodation accommodation = accommodationRepository.findByIdAndOwnerId(id, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("La vivienda seleccionada no existe."));
        return new AccommodationForm(
            accommodation.getName(),
            accommodation.getSesEstablishmentCode(),
            accommodation.getRegistrationNumber() == null ? "" : accommodation.getRegistrationNumber(),
            accommodation.getRoomCount()
        );
    }

    @Transactional
    public Accommodation update(Long id, AccommodationForm form) {
        Accommodation accommodation = accommodationRepository.findByIdAndOwnerId(id, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("La vivienda seleccionada no existe."));
        accommodation.update(
            form.name().trim(),
            normalizeSesCode(form.sesEstablishmentCode()),
            normalize(form.registrationNumber()),
            form.roomCount()
        );
        return accommodation;
    }

    private String normalizeSesCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
