package es.checkpol.web;

import es.checkpol.service.GuestService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuestAddressController {

    private final GuestService guestService;

    public GuestAddressController(GuestService guestService) {
        this.guestService = guestService;
    }

    @PutMapping("/guests/{guestId}/address")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateGuestAddress(@PathVariable Long guestId, @RequestBody GuestAddressUpdateRequest request) {
        guestService.assignAddress(guestId, request.addressId());
    }

    public record GuestAddressUpdateRequest(Long addressId) {
    }
}
