package com.electrahub.charger.elasticsearch.repository;

import com.electrahub.charger.elasticsearch.document.OcpiConnectorDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface OcpiConnectorDocumentRepository extends ElasticsearchRepository<OcpiConnectorDocument, String> {
}

