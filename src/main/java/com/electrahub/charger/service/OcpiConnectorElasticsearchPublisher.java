package com.electrahub.charger.service;

import com.electrahub.charger.config.ElasticsearchProperties;
import com.electrahub.charger.elasticsearch.document.OcpiConnectorDocument;
import com.electrahub.charger.elasticsearch.repository.OcpiConnectorDocumentRepository;
import com.electrahub.charger.repository.OcpiConnectorIndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OcpiConnectorElasticsearchPublisher {
    private static final Logger log = LoggerFactory.getLogger(OcpiConnectorElasticsearchPublisher.class);
    private static final String INDEX_NAME = "ocpi-connectors";
    private static final int BATCH_SIZE = 200;

    private final ElasticsearchProperties elasticsearchProperties;
    private final ObjectProvider<OcpiConnectorDocumentRepository> repositoryProvider;
    private final OcpiConnectorIndexRepository ocpiConnectorIndexRepository;

    public OcpiConnectorElasticsearchPublisher(
            ElasticsearchProperties elasticsearchProperties,
            ObjectProvider<OcpiConnectorDocumentRepository> repositoryProvider,
            OcpiConnectorIndexRepository ocpiConnectorIndexRepository
    ) {
        this.elasticsearchProperties = elasticsearchProperties;
        this.repositoryProvider = repositoryProvider;
        this.ocpiConnectorIndexRepository = ocpiConnectorIndexRepository;
    }

    public void publishConnector(String connectorId) {
        syncByConnectorId(connectorId);
    }

    public SyncResult syncByConnectorId(String connectorId) {
        return syncRows(
                "CONNECTOR",
                connectorId,
                ocpiConnectorIndexRepository.findConnectorRowById(connectorId).stream().toList()
        );
    }

    public SyncResult syncByEvseId(String evseId) {
        return syncRows("EVSE", evseId, ocpiConnectorIndexRepository.listConnectorRowsByEvseId(evseId));
    }

    public SyncResult syncByChargerId(String chargerId) {
        return syncRows("CHARGER", chargerId, ocpiConnectorIndexRepository.listConnectorRowsByChargerId(chargerId));
    }

    public ReindexResult reindexAllConnectors() {
        OcpiConnectorDocumentRepository repository = getRepositoryOrNull();
        long total = ocpiConnectorIndexRepository.countConnectorRows();
        long indexed = 0;
        long failed = 0;

        if (repository == null) {
            return new ReindexResult(INDEX_NAME, total, 0, total, OffsetDateTime.now());
        }

        for (int offset = 0; offset < total; offset += BATCH_SIZE) {
            for (OcpiConnectorIndexRepository.OcpiConnectorIndexRow row : ocpiConnectorIndexRepository.listConnectorRows(BATCH_SIZE, offset)) {
                try {
                    repository.save(toDocument(row));
                    indexed++;
                } catch (Exception ex) {
                    failed++;
                    log.warn("Failed to index connector {} to Elasticsearch", row.connectorId(), ex);
                }
            }
        }

        log.info("Completed OCPI connector reindex to Elasticsearch index {}. total={}, indexed={}, failed={}",
                INDEX_NAME, total, indexed, failed);
        return new ReindexResult(INDEX_NAME, total, indexed, failed, OffsetDateTime.now());
    }

    private SyncResult syncRows(String syncType, String syncValue, List<OcpiConnectorIndexRepository.OcpiConnectorIndexRow> rows) {
        OcpiConnectorDocumentRepository repository = getRepositoryOrNull();
        long total = rows.size();
        long indexed = 0;
        long failed = 0;

        if (repository == null) {
            return new SyncResult(INDEX_NAME, syncType, syncValue, total, 0, total, OffsetDateTime.now());
        }

        for (OcpiConnectorIndexRepository.OcpiConnectorIndexRow row : rows) {
            try {
                repository.save(toDocument(row));
                indexed++;
            } catch (Exception ex) {
                failed++;
                log.warn("Failed to index connector {} to Elasticsearch during {} selective sync",
                        row.connectorId(), syncType, ex);
            }
        }

        log.info("Completed {} selective OCPI connector sync for value {}. total={}, indexed={}, failed={}",
                syncType, syncValue, total, indexed, failed);
        return new SyncResult(INDEX_NAME, syncType, syncValue, total, indexed, failed, OffsetDateTime.now());
    }

    private OcpiConnectorDocumentRepository getRepositoryOrNull() {
        if (!elasticsearchProperties.isEnabled()) {
            log.info("Elasticsearch publish is disabled for charger-management-service");
            return null;
        }
        OcpiConnectorDocumentRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            log.info("Elasticsearch repository is unavailable, skipping connector publish");
        }
        return repository;
    }

    private OcpiConnectorDocument toDocument(OcpiConnectorIndexRepository.OcpiConnectorIndexRow row) {
        OcpiConnectorDocument document = new OcpiConnectorDocument();
        document.setId(row.connectorId());
        document.setCountryCode(row.countryCode());
        document.setPartyId(row.partyId());
        document.setActive(isFullyEnabled(row));
        document.setStatus(isFullyEnabled(row) ? "AVAILABLE" : "UNAVAILABLE");
        document.setLastUpdated(row.lastUpdated() == null ? null : row.lastUpdated().toInstant());
        document.setIndexedAt(Instant.now());

        OcpiConnectorDocument.Location location = new OcpiConnectorDocument.Location();
        location.setId(row.locationId());
        location.setOcpiLocationId(row.ocpiLocationId());
        location.setName(row.locationName());
        location.setAddress(row.locationAddress());
        location.setCity(row.locationCity());
        location.setNetworkId(row.networkId());
        location.setNetworkName(row.networkName());
        location.setEnterpriseId(row.enterpriseId());
        location.setEnterpriseName(row.enterpriseName());
        document.setLocation(location);

        OcpiConnectorDocument.Evse evse = new OcpiConnectorDocument.Evse();
        evse.setId(row.evseId());
        evse.setUid(row.evseUid());
        evse.setZone(row.evseZone());
        evse.setCapabilities(row.evseCapabilities());
        evse.setChargerId(row.chargerId());
        evse.setChargerName(row.chargerDisplayName());
        document.setEvse(evse);

        OcpiConnectorDocument.Connector connector = new OcpiConnectorDocument.Connector();
        connector.setId(row.connectorId());
        connector.setStandard(row.standard());
        connector.setFormat(row.format());
        connector.setPowerType(row.powerType());
        connector.setMaxPowerKw(row.maxPowerKw());
        connector.setTariffIds(row.tariffIds());
        document.setConnector(connector);

        return document;
    }

    private boolean isFullyEnabled(OcpiConnectorIndexRepository.OcpiConnectorIndexRow row) {
        return row.connectorEnabled()
                && row.evseEnabled()
                && row.chargerEnabled()
                && row.locationEnabled()
                && row.networkEnabled()
                && row.enterpriseEnabled();
    }

    public record ReindexResult(
            String indexName,
            long totalConnectors,
            long indexedConnectors,
            long failedConnectors,
            OffsetDateTime executedAt
    ) {
    }

    public record SyncResult(
            String indexName,
            String syncType,
            String syncValue,
            long candidateConnectors,
            long indexedConnectors,
            long failedConnectors,
            OffsetDateTime executedAt
    ) {
    }
}
