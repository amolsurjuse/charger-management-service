package com.electrahub.charger.elasticsearch.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch document that stores connector records in an OCPI-shaped payload.
 *
 * <p>Each document represents one connector and carries location, EVSE, and connector
 * sections that mirror the OCPI data hierarchy for search and downstream sync use cases.
 */
@Document(indexName = "ocpi-connectors")
public class OcpiConnectorDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String countryCode;

    @Field(type = FieldType.Keyword)
    private String partyId;

    @Field(type = FieldType.Object)
    private Location location;

    @Field(type = FieldType.Object)
    private Evse evse;

    @Field(type = FieldType.Object)
    private Connector connector;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date)
    private Instant lastUpdated;

    @Field(type = FieldType.Date)
    private Instant indexedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Evse getEvse() {
        return evse;
    }

    public void setEvse(Evse evse) {
        this.evse = evse;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Instant getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(Instant indexedAt) {
        this.indexedAt = indexedAt;
    }

    public static class Location {
        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Keyword)
        private String ocpiLocationId;

        @Field(type = FieldType.Text)
        private String name;

        @Field(type = FieldType.Text)
        private String address;

        @Field(type = FieldType.Keyword)
        private String city;

        @Field(type = FieldType.Keyword)
        private String networkId;

        @Field(type = FieldType.Text)
        private String networkName;

        @Field(type = FieldType.Keyword)
        private String enterpriseId;

        @Field(type = FieldType.Text)
        private String enterpriseName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getOcpiLocationId() {
            return ocpiLocationId;
        }

        public void setOcpiLocationId(String ocpiLocationId) {
            this.ocpiLocationId = ocpiLocationId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getNetworkId() {
            return networkId;
        }

        public void setNetworkId(String networkId) {
            this.networkId = networkId;
        }

        public String getNetworkName() {
            return networkName;
        }

        public void setNetworkName(String networkName) {
            this.networkName = networkName;
        }

        public String getEnterpriseId() {
            return enterpriseId;
        }

        public void setEnterpriseId(String enterpriseId) {
            this.enterpriseId = enterpriseId;
        }

        public String getEnterpriseName() {
            return enterpriseName;
        }

        public void setEnterpriseName(String enterpriseName) {
            this.enterpriseName = enterpriseName;
        }
    }

    public static class Evse {
        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Keyword)
        private String uid;

        @Field(type = FieldType.Keyword)
        private String chargerId;

        @Field(type = FieldType.Text)
        private String chargerName;

        @Field(type = FieldType.Keyword)
        private String zone;

        @Field(type = FieldType.Keyword)
        private List<String> capabilities = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getChargerId() {
            return chargerId;
        }

        public void setChargerId(String chargerId) {
            this.chargerId = chargerId;
        }

        public String getChargerName() {
            return chargerName;
        }

        public void setChargerName(String chargerName) {
            this.chargerName = chargerName;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public List<String> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(List<String> capabilities) {
            this.capabilities = capabilities == null ? new ArrayList<>() : capabilities;
        }
    }

    public static class Connector {
        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Keyword)
        private String standard;

        @Field(type = FieldType.Keyword)
        private String format;

        @Field(type = FieldType.Keyword)
        private String powerType;

        @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
        private BigDecimal maxPowerKw;

        @Field(type = FieldType.Keyword)
        private List<String> tariffIds = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStandard() {
            return standard;
        }

        public void setStandard(String standard) {
            this.standard = standard;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getPowerType() {
            return powerType;
        }

        public void setPowerType(String powerType) {
            this.powerType = powerType;
        }

        public BigDecimal getMaxPowerKw() {
            return maxPowerKw;
        }

        public void setMaxPowerKw(BigDecimal maxPowerKw) {
            this.maxPowerKw = maxPowerKw;
        }

        public List<String> getTariffIds() {
            return tariffIds;
        }

        public void setTariffIds(List<String> tariffIds) {
            this.tariffIds = tariffIds == null ? new ArrayList<>() : tariffIds;
        }
    }
}

