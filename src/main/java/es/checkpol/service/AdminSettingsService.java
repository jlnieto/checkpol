package es.checkpol.service;

import es.checkpol.domain.AdminSetting;
import es.checkpol.repository.AdminSettingRepository;
import es.checkpol.web.AdminCatalogDefaultsForm;
import es.checkpol.web.AdminVerificationSettingsForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminSettingsService {

    private static final String MUNICIPALITY_DEFAULT_SOURCE = "municipality.admin.default-source";
    private static final String MUNICIPALITY_DEFAULT_MUNICIPALITIES_URL = "municipality.admin.default-municipalities-url";
    private static final String MUNICIPALITY_DEFAULT_POSTAL_MAPPINGS_URL = "municipality.admin.default-postal-mappings-url";
    private static final String VERIFICATION_ENABLED = "municipality.admin.verification.enabled";
    private static final String VERIFICATION_CRON = "municipality.admin.verification.cron";
    private static final String VERIFICATION_ZONE = "municipality.admin.verification.zone";
    private static final String VERIFICATION_TRIGGERED_BY = "municipality.admin.verification.triggered-by";

    private final AdminSettingRepository adminSettingRepository;
    private final String propertyDefaultSource;
    private final String propertyDefaultMunicipalitiesUrl;
    private final String propertyDefaultPostalMappingsUrl;
    private final boolean propertyVerificationEnabled;
    private final String propertyVerificationCron;
    private final String propertyVerificationZone;
    private final String propertyVerificationTriggeredBy;

    public AdminSettingsService(
        AdminSettingRepository adminSettingRepository,
        @Value("${checkpol.municipality.admin.default-source:ine-open-data}") String propertyDefaultSource,
        @Value("${checkpol.municipality.admin.default-municipalities-url:https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx}") String propertyDefaultMunicipalitiesUrl,
        @Value("${checkpol.municipality.admin.default-postal-mappings-url:https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip}") String propertyDefaultPostalMappingsUrl,
        @Value("${checkpol.municipality.admin.verification.enabled:false}") boolean propertyVerificationEnabled,
        @Value("${checkpol.municipality.admin.verification.cron:0 0 6 * * *}") String propertyVerificationCron,
        @Value("${checkpol.municipality.admin.verification.zone:Europe/Madrid}") String propertyVerificationZone,
        @Value("${checkpol.municipality.admin.verification.triggered-by:system-verifier}") String propertyVerificationTriggeredBy
    ) {
        this.adminSettingRepository = adminSettingRepository;
        this.propertyDefaultSource = propertyDefaultSource;
        this.propertyDefaultMunicipalitiesUrl = propertyDefaultMunicipalitiesUrl;
        this.propertyDefaultPostalMappingsUrl = propertyDefaultPostalMappingsUrl;
        this.propertyVerificationEnabled = propertyVerificationEnabled;
        this.propertyVerificationCron = propertyVerificationCron;
        this.propertyVerificationZone = propertyVerificationZone;
        this.propertyVerificationTriggeredBy = propertyVerificationTriggeredBy;
    }

    @Transactional(readOnly = true)
    public MunicipalityAdminDefaults getMunicipalityAdminDefaults() {
        Map<String, AdminSetting> settings = adminSettingRepository.findAllById(
            java.util.List.of(
                MUNICIPALITY_DEFAULT_SOURCE,
                MUNICIPALITY_DEFAULT_MUNICIPALITIES_URL,
                MUNICIPALITY_DEFAULT_POSTAL_MAPPINGS_URL
            )
        ).stream().collect(Collectors.toMap(AdminSetting::getSettingKey, Function.identity()));

        return new MunicipalityAdminDefaults(
            settingValue(settings, MUNICIPALITY_DEFAULT_SOURCE, propertyDefaultSource),
            settingValue(settings, MUNICIPALITY_DEFAULT_MUNICIPALITIES_URL, propertyDefaultMunicipalitiesUrl),
            settingValue(settings, MUNICIPALITY_DEFAULT_POSTAL_MAPPINGS_URL, propertyDefaultPostalMappingsUrl),
            settings.get(MUNICIPALITY_DEFAULT_SOURCE) != null ? settings.get(MUNICIPALITY_DEFAULT_SOURCE).getUpdatedAt() : null,
            settings.get(MUNICIPALITY_DEFAULT_SOURCE) != null ? settings.get(MUNICIPALITY_DEFAULT_SOURCE).getUpdatedByUsername() : null
        );
    }

    @Transactional(readOnly = true)
    public VerificationSettings getVerificationSettings() {
        Map<String, AdminSetting> settings = adminSettingRepository.findAllById(
            java.util.List.of(
                VERIFICATION_ENABLED,
                VERIFICATION_CRON,
                VERIFICATION_ZONE,
                VERIFICATION_TRIGGERED_BY
            )
        ).stream().collect(Collectors.toMap(AdminSetting::getSettingKey, Function.identity()));

        AdminSetting enabledSetting = settings.get(VERIFICATION_ENABLED);
        return new VerificationSettings(
            Boolean.parseBoolean(settingValue(settings, VERIFICATION_ENABLED, Boolean.toString(propertyVerificationEnabled))),
            settingValue(settings, VERIFICATION_CRON, propertyVerificationCron),
            settingValue(settings, VERIFICATION_ZONE, propertyVerificationZone),
            settingValue(settings, VERIFICATION_TRIGGERED_BY, propertyVerificationTriggeredBy),
            enabledSetting != null ? enabledSetting.getUpdatedAt() : null,
            enabledSetting != null ? enabledSetting.getUpdatedByUsername() : null
        );
    }

    @Transactional
    public void updateMunicipalityAdminDefaults(AdminCatalogDefaultsForm form, String updatedByUsername) {
        saveSetting(MUNICIPALITY_DEFAULT_SOURCE, form.source(), updatedByUsername);
        saveSetting(MUNICIPALITY_DEFAULT_MUNICIPALITIES_URL, form.municipalitiesUrl(), updatedByUsername);
        saveSetting(MUNICIPALITY_DEFAULT_POSTAL_MAPPINGS_URL, form.postalMappingsUrl(), updatedByUsername);
    }

    @Transactional
    public void updateVerificationSettings(AdminVerificationSettingsForm form, String updatedByUsername) {
        saveSetting(VERIFICATION_ENABLED, Boolean.toString(form.enabled()), updatedByUsername);
        saveSetting(VERIFICATION_CRON, form.cron(), updatedByUsername);
        saveSetting(VERIFICATION_ZONE, form.zone(), updatedByUsername);
        saveSetting(VERIFICATION_TRIGGERED_BY, form.triggeredByUsername(), updatedByUsername);
    }

    private void saveSetting(String settingKey, String settingValue, String updatedByUsername) {
        AdminSetting setting = adminSettingRepository.findById(settingKey).orElse(null);
        if (setting == null) {
            setting = new AdminSetting(settingKey, normalize(settingValue), updatedByUsername, OffsetDateTime.now());
        } else {
            setting.update(normalize(settingValue), updatedByUsername);
        }
        adminSettingRepository.save(setting);
    }

    private String settingValue(Map<String, AdminSetting> settings, String key, String fallback) {
        AdminSetting setting = settings.get(key);
        return setting != null ? setting.getSettingValue() : fallback;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record MunicipalityAdminDefaults(
        String source,
        String municipalitiesUrl,
        String postalMappingsUrl,
        OffsetDateTime updatedAt,
        String updatedByUsername
    ) {
    }

    public record VerificationSettings(
        boolean enabled,
        String cron,
        String zone,
        String triggeredByUsername,
        OffsetDateTime updatedAt,
        String updatedByUsername
    ) {
    }
}
