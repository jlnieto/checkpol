package es.checkpol.service;

import es.checkpol.domain.AppUser;

public interface SesCommunicationGateway {

    SesConnectionTestResult testConnection(AppUser owner);

    SesSubmissionResult submitTravelerPart(AppUser owner, String xmlContent);

    SesLoteStatusResult queryLoteStatus(AppUser owner, String loteCode);

    SesSubmissionResult cancelLote(AppUser owner, String loteCode);
}
