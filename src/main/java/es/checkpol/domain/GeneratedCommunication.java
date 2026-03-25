package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "generated_communications")
public class GeneratedCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "last_downloaded_at")
    private OffsetDateTime lastDownloadedAt;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount;

    @Lob
    @Column(name = "xml_content", nullable = false)
    private String xmlContent;

    protected GeneratedCommunication() {
    }

    public GeneratedCommunication(Booking booking, Integer version, OffsetDateTime generatedAt, String xmlContent) {
        this.booking = booking;
        this.version = version;
        this.generatedAt = generatedAt;
        this.downloadCount = 0;
        this.xmlContent = xmlContent;
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public Integer getVersion() {
        return version;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public OffsetDateTime getLastDownloadedAt() {
        return lastDownloadedAt;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public void registerDownload(OffsetDateTime downloadedAt) {
        this.lastDownloadedAt = downloadedAt;
        this.downloadCount = this.downloadCount + 1;
    }
}
