package es.checkpol.service;

import es.checkpol.domain.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SesConnectionTestingService {

    private final SesCommunicationGateway sesCommunicationGateway;

    public SesConnectionTestingService(SesCommunicationGateway sesCommunicationGateway) {
        this.sesCommunicationGateway = sesCommunicationGateway;
    }

    @Transactional
    public SesConnectionTestResult testAndStore(AppUser owner) {
        SesConnectionTestResult result = sesCommunicationGateway.testConnection(owner);
        owner.registerSesConnectionTest(
            result.status(),
            result.testedAt(),
            result.ownerMessage(),
            result.adminMessage(),
            result.endpointUrl(),
            result.httpStatus(),
            result.errorType(),
            result.rawDetail()
        );
        return result;
    }
}
