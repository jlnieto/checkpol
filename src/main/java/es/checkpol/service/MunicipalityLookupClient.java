package es.checkpol.service;

import java.util.List;

public interface MunicipalityLookupClient {

    List<MunicipalityCandidate> search(String query);
}
