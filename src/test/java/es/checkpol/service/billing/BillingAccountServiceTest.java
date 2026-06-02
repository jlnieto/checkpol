package es.checkpol.service.billing;

import es.checkpol.config.StripeBillingProperties;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.billing.BillingAccount;
import es.checkpol.domain.billing.BillingSubscriptionStatus;
import es.checkpol.repository.AccommodationRepository;
import es.checkpol.repository.billing.BillingAccountRepository;
import es.checkpol.repository.billing.BillingInvoiceRepository;
import es.checkpol.service.CurrentAppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingAccountServiceTest {

    @Mock
    private BillingAccountRepository billingAccountRepository;

    @Mock
    private BillingInvoiceRepository billingInvoiceRepository;

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private CurrentAppUserService currentAppUserService;

    @Mock
    private StripeBillingGateway stripeBillingGateway;

    private BillingAccountService service;

    @BeforeEach
    void setUp() {
        StripeBillingProperties properties = new StripeBillingProperties();
        properties.setGracePeriodDays(7);
        service = new BillingAccountService(
            billingAccountRepository,
            billingInvoiceRepository,
            accommodationRepository,
            currentAppUserService,
            stripeBillingGateway,
            properties
        );
    }

    @Test
    void allowsLegacyOwnerWithoutStripeBillingAccount() {
        AppUser owner = owner(10L);
        when(billingAccountRepository.findByOwnerId(10L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.assertCanCreateAccommodation(owner));
    }

    @Test
    void blocksCreatingAccommodationWhenPaidLimitIsReached() {
        AppUser owner = owner(10L);
        BillingAccount account = account(owner, 1, BillingSubscriptionStatus.ACTIVE);
        when(billingAccountRepository.findByOwnerId(10L)).thenReturn(Optional.of(account));
        when(accommodationRepository.countByOwnerId(10L)).thenReturn(1);

        BillingLimitExceededException exception = assertThrows(
            BillingLimitExceededException.class,
            () -> service.assertCanCreateAccommodation(owner)
        );

        assertEquals("Has contratado 1 vivienda(s). Para añadir otra, amplía la cantidad en facturación.", exception.getMessage());
    }

    @Test
    void blocksCreatingAccommodationWhenSubscriptionIsNotUsable() {
        AppUser owner = owner(10L);
        BillingAccount account = account(owner, 2, BillingSubscriptionStatus.CANCELED);
        when(billingAccountRepository.findByOwnerId(10L)).thenReturn(Optional.of(account));
        when(accommodationRepository.countByOwnerId(10L)).thenReturn(0);

        BillingLimitExceededException exception = assertThrows(
            BillingLimitExceededException.class,
            () -> service.assertCanCreateAccommodation(owner)
        );

        assertEquals("Tu suscripción no está activa. Revisa el pago antes de crear más viviendas.", exception.getMessage());
    }

    private BillingAccount account(AppUser owner, int limit, BillingSubscriptionStatus status) {
        BillingAccount account = new BillingAccount(owner, "cus_123", status, limit, OffsetDateTime.now());
        account.syncSubscription(
            "sub_123",
            "si_123",
            status,
            limit,
            OffsetDateTime.now().minusDays(1),
            OffsetDateTime.now().plusDays(20),
            false
        );
        return account;
    }

    private AppUser owner(Long id) {
        AppUser owner = new AppUser(
            "owner@example.com",
            "hash",
            "Owner",
            AppUserRole.OWNER,
            true,
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(owner, "id", id);
        return owner;
    }
}
