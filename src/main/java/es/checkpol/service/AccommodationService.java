package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.repository.AccommodationRepository;
import es.checkpol.web.AccommodationForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;

    public AccommodationService(AccommodationRepository accommodationRepository) {
        this.accommodationRepository = accommodationRepository;
    }

    @Transactional(readOnly = true)
    public List<Accommodation> findAll() {
        return accommodationRepository.findAll();
    }

    @Transactional
    public Accommodation create(AccommodationForm form) {
        Accommodation accommodation = new Accommodation(
            form.name().trim(),
            form.sesEstablishmentCode().trim(),
            normalize(form.registrationNumber())
        );
        return accommodationRepository.save(accommodation);
    }

    @Transactional(readOnly = true)
    public AccommodationForm getForm(Long id) {
        Accommodation accommodation = accommodationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("La vivienda seleccionada no existe."));
        return new AccommodationForm(
            accommodation.getName(),
            accommodation.getSesEstablishmentCode(),
            accommodation.getRegistrationNumber() == null ? "" : accommodation.getRegistrationNumber()
        );
    }

    @Transactional
    public Accommodation update(Long id, AccommodationForm form) {
        Accommodation accommodation = accommodationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("La vivienda seleccionada no existe."));
        accommodation.update(
            form.name().trim(),
            form.sesEstablishmentCode().trim(),
            normalize(form.registrationNumber())
        );
        return accommodation;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
