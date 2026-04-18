package es.checkpol.service;

public class SesCommunicationException extends RuntimeException {

    private final Integer responseCode;
    private final String rawResponse;

    public SesCommunicationException(String message, Integer responseCode, String rawResponse) {
        super(message);
        this.responseCode = responseCode;
        this.rawResponse = rawResponse;
    }

    public SesCommunicationException(String message, Integer responseCode, String rawResponse, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
        this.rawResponse = rawResponse;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
