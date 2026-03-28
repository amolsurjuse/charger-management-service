package com.electrahub.charger.grpc;

import com.electrahub.proto.charger.v1.ChargerAdminServiceGrpc;
import com.electrahub.proto.charger.v1.ListEnterprisesRequest;
import com.electrahub.proto.charger.v1.CreateEnterpriseRequest;
import com.electrahub.proto.charger.v1.ListNetworksRequest;
import com.electrahub.proto.charger.v1.CreateNetworkRequest;
import com.electrahub.proto.charger.v1.ListLocationsRequest;
import com.electrahub.proto.charger.v1.CreateLocationRequest;
import com.electrahub.proto.charger.v1.ListAdminChargersRequest;
import com.electrahub.proto.charger.v1.CreateAdminChargerRequest;
import com.electrahub.proto.charger.v1.ListEvsesRequest;
import com.electrahub.proto.charger.v1.CreateEvseRequest;
import com.electrahub.proto.charger.v1.ListConnectorsRequest;
import com.electrahub.proto.charger.v1.CreateConnectorRequest;
import com.electrahub.proto.charger.v1.PublishEvsesToSearchRequest;
import com.electrahub.proto.charger.v1.EnterpriseListResponse;
import com.electrahub.proto.charger.v1.NetworkListResponse;
import com.electrahub.proto.charger.v1.LocationListResponse;
import com.electrahub.proto.charger.v1.ChargerAdminListResponse;
import com.electrahub.proto.charger.v1.EvseListResponse;
import com.electrahub.proto.charger.v1.ConnectorListResponse;
import com.electrahub.proto.charger.v1.PublishEvsesToSearchResponse;
import com.electrahub.charger.api.dto.ChargerAdminDtos;
import com.electrahub.charger.service.ChargerAdminService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class ChargerAdminGrpcService extends ChargerAdminServiceGrpc.ChargerAdminServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerAdminGrpcService.class);

    private final ChargerAdminService chargerAdminService;

    public ChargerAdminGrpcService(ChargerAdminService chargerAdminService) {
        this.chargerAdminService = chargerAdminService;
    }

    @Override
    public void listEnterprises(ListEnterprisesRequest request, StreamObserver<EnterpriseListResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Listing enterprises");

            ChargerAdminDtos.PagedResponse<ChargerAdminDtos.EnterpriseResponse> response =
                    chargerAdminService.listEnterprises(
                            request.getSearch(),
                            request.getLimit(),
                            request.getOffset()
                    );

            responseObserver.onNext(convertToEnterpriseListProto(response));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in listEnterprises", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in listEnterprises", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void createEnterprise(CreateEnterpriseRequest request, StreamObserver<com.electrahub.proto.charger.v1.EnterpriseResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Creating enterprise: {}", request.getName());

            ChargerAdminDtos.EnterpriseCreateRequest createRequest = new ChargerAdminDtos.EnterpriseCreateRequest(
                    request.getName()
            );

            ChargerAdminDtos.EnterpriseResponse enterprise = chargerAdminService.createEnterprise(createRequest);
            responseObserver.onNext(convertToEnterpriseProto(enterprise));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in createEnterprise", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in createEnterprise", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void listNetworks(ListNetworksRequest request, StreamObserver<NetworkListResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Listing networks");

            ChargerAdminDtos.PagedResponse<ChargerAdminDtos.NetworkResponse> response =
                    chargerAdminService.listNetworks(
                            request.getSearch(),
                            request.getEnterpriseId(),
                            request.getLimit(),
                            request.getOffset()
                    );

            responseObserver.onNext(convertToNetworkListProto(response));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in listNetworks", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in listNetworks", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void createNetwork(CreateNetworkRequest request, StreamObserver<com.electrahub.proto.charger.v1.NetworkResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Creating network: {}", request.getName());

            ChargerAdminDtos.NetworkCreateRequest createRequest = new ChargerAdminDtos.NetworkCreateRequest(
                    request.getName(),
                    request.getEnterpriseId()
            );

            ChargerAdminDtos.NetworkResponse network = chargerAdminService.createNetwork(createRequest);
            responseObserver.onNext(convertToNetworkProto(network));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in createNetwork", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in createNetwork", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void listLocations(ListLocationsRequest request, StreamObserver<LocationListResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Listing locations");

            ChargerAdminDtos.PagedResponse<ChargerAdminDtos.LocationResponse> response =
                    chargerAdminService.listLocations(
                            request.getSearch(),
                            request.getNetworkId(),
                            request.getLimit(),
                            request.getOffset()
                    );

            responseObserver.onNext(convertToLocationListProto(response));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in listLocations", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in listLocations", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void createLocation(CreateLocationRequest request, StreamObserver<com.electrahub.proto.charger.v1.LocationResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Creating location: {}", request.getName());

            ChargerAdminDtos.LocationCreateRequest createRequest = new ChargerAdminDtos.LocationCreateRequest(
                    request.getName(),
                    request.getNetworkId()
            );

            ChargerAdminDtos.LocationResponse location = chargerAdminService.createLocation(createRequest);
            responseObserver.onNext(convertToLocationProto(location));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in createLocation", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in createLocation", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void listChargers(ListAdminChargersRequest request, StreamObserver<ChargerAdminListResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Listing admin chargers");

            ChargerAdminDtos.PagedResponse<ChargerAdminDtos.ChargerResponse> response =
                    chargerAdminService.listChargers(
                            request.getSearch(),
                            request.getLocationId(),
                            request.getLimit(),
                            request.getOffset()
                    );

            responseObserver.onNext(convertToChargerListProto(response));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in listChargers", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in listChargers", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void createCharger(CreateAdminChargerRequest request, StreamObserver<com.electrahub.proto.charger.v1.ChargerResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Creating admin charger");

            ChargerAdminDtos.ChargerCreateRequest createRequest = new ChargerAdminDtos.ChargerCreateRequest(
                    request.getChargerId(),
                    request.getLocationId()
            );

            ChargerAdminDtos.ChargerResponse charger = chargerAdminService.createCharger(createRequest);
            responseObserver.onNext(convertToChargerProto(charger));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in createCharger", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in createCharger", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void listEvses(ListEvsesRequest request, StreamObserver<EvseListResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Listing EVSEs");

            ChargerAdminDtos.PagedResponse<ChargerAdminDtos.EvseResponse> response =
                    chargerAdminService.listEvses(
                            request.getSearch(),
                            request.getChargerId(),
                            request.getLimit(),
                            request.getOffset()
                    );

            responseObserver.onNext(convertToEvseListProto(response));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in listEvses", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in listEvses", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void createEvse(CreateEvseRequest request, StreamObserver<com.electrahub.proto.charger.v1.EvseResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Creating EVSE");

            ChargerAdminDtos.EvseCreateRequest createRequest = new ChargerAdminDtos.EvseCreateRequest(
                    request.getEvseId(),
                    request.getChargerId()
            );

            ChargerAdminDtos.EvseResponse evse = chargerAdminService.createEvse(createRequest);
            responseObserver.onNext(convertToEvseProto(evse));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in createEvse", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in createEvse", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void listConnectors(ListConnectorsRequest request, StreamObserver<ConnectorListResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Listing connectors");

            ChargerAdminDtos.PagedResponse<ChargerAdminDtos.ConnectorResponse> response =
                    chargerAdminService.listConnectors(
                            request.getSearch(),
                            request.getEvseId(),
                            request.getLimit(),
                            request.getOffset()
                    );

            responseObserver.onNext(convertToConnectorListProto(response));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in listConnectors", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in listConnectors", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void createConnector(CreateConnectorRequest request, StreamObserver<com.electrahub.proto.charger.v1.ConnectorResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Creating connector");

            ChargerAdminDtos.ConnectorCreateRequest createRequest = new ChargerAdminDtos.ConnectorCreateRequest(
                    request.getConnectorId(),
                    request.getEvseId()
            );

            ChargerAdminDtos.ConnectorResponse connector = chargerAdminService.createConnector(createRequest);
            responseObserver.onNext(convertToConnectorProto(connector));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in createConnector", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.error("Unexpected error in createConnector", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    @Override
    public void publishEvsesToSearch(
            PublishEvsesToSearchRequest request,
            StreamObserver<PublishEvsesToSearchResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Publishing EVSEs to search");

            ChargerAdminDtos.EvseSearchPublishRequest publishRequest = new ChargerAdminDtos.EvseSearchPublishRequest(
                    request.getLocationIdsList()
            );

            ChargerAdminDtos.EvseSearchPublishResponse publishResponse =
                    chargerAdminService.publishCurrentEvsesToSearch(publishRequest);

            PublishEvsesToSearchResponse response = PublishEvsesToSearchResponse.newBuilder()
                    .setPublished(publishResponse.published())
                    .setFailed(publishResponse.failed())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Unexpected error in publishEvsesToSearch", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asException()
            );
        }
    }

    private EnterpriseListResponse convertToEnterpriseListProto(ChargerAdminDtos.PagedResponse<ChargerAdminDtos.EnterpriseResponse> response) {
        var builder = EnterpriseListResponse.newBuilder();
        if (response.items() != null) {
            response.items().forEach(enterprise ->
                    builder.addEnterprises(convertToEnterpriseProto(enterprise))
            );
        }
        builder.setTotal(response.total());
        return builder.build();
    }

    private com.electrahub.proto.charger.v1.EnterpriseResponse convertToEnterpriseProto(ChargerAdminDtos.EnterpriseResponse enterprise) {
        return com.electrahub.proto.charger.v1.EnterpriseResponse.newBuilder()
                .setEnterpriseId(enterprise.enterpriseId() != null ? enterprise.enterpriseId() : "")
                .setName(enterprise.name() != null ? enterprise.name() : "")
                .build();
    }

    private NetworkListResponse convertToNetworkListProto(ChargerAdminDtos.PagedResponse<ChargerAdminDtos.NetworkResponse> response) {
        var builder = NetworkListResponse.newBuilder();
        if (response.items() != null) {
            response.items().forEach(network ->
                    builder.addNetworks(convertToNetworkProto(network))
            );
        }
        builder.setTotal(response.total());
        return builder.build();
    }

    private com.electrahub.proto.charger.v1.NetworkResponse convertToNetworkProto(ChargerAdminDtos.NetworkResponse network) {
        return com.electrahub.proto.charger.v1.NetworkResponse.newBuilder()
                .setNetworkId(network.networkId() != null ? network.networkId() : "")
                .setName(network.name() != null ? network.name() : "")
                .setEnterpriseId(network.enterpriseId() != null ? network.enterpriseId() : "")
                .build();
    }

    private LocationListResponse convertToLocationListProto(ChargerAdminDtos.PagedResponse<ChargerAdminDtos.LocationResponse> response) {
        var builder = LocationListResponse.newBuilder();
        if (response.items() != null) {
            response.items().forEach(location ->
                    builder.addLocations(convertToLocationProto(location))
            );
        }
        builder.setTotal(response.total());
        return builder.build();
    }

    private com.electrahub.proto.charger.v1.LocationResponse convertToLocationProto(ChargerAdminDtos.LocationResponse location) {
        return com.electrahub.proto.charger.v1.LocationResponse.newBuilder()
                .setLocationId(location.locationId() != null ? location.locationId() : "")
                .setName(location.name() != null ? location.name() : "")
                .setNetworkId(location.networkId() != null ? location.networkId() : "")
                .build();
    }

    private ChargerAdminListResponse convertToChargerListProto(ChargerAdminDtos.PagedResponse<ChargerAdminDtos.ChargerResponse> response) {
        var builder = ChargerAdminListResponse.newBuilder();
        if (response.items() != null) {
            response.items().forEach(charger ->
                    builder.addChargers(convertToChargerProto(charger))
            );
        }
        builder.setTotal(response.total());
        return builder.build();
    }

    private com.electrahub.proto.charger.v1.ChargerResponse convertToChargerProto(ChargerAdminDtos.ChargerResponse charger) {
        return com.electrahub.proto.charger.v1.ChargerResponse.newBuilder()
                .setChargerId(charger.chargerId() != null ? charger.chargerId() : "")
                .setLocationId(charger.locationId() != null ? charger.locationId() : "")
                .build();
    }

    private EvseListResponse convertToEvseListProto(ChargerAdminDtos.PagedResponse<ChargerAdminDtos.EvseResponse> response) {
        var builder = EvseListResponse.newBuilder();
        if (response.items() != null) {
            response.items().forEach(evse ->
                    builder.addEvses(convertToEvseProto(evse))
            );
        }
        builder.setTotal(response.total());
        return builder.build();
    }

    private com.electrahub.proto.charger.v1.EvseResponse convertToEvseProto(ChargerAdminDtos.EvseResponse evse) {
        return com.electrahub.proto.charger.v1.EvseResponse.newBuilder()
                .setEvseId(evse.evseId() != null ? evse.evseId() : "")
                .setChargerId(evse.chargerId() != null ? evse.chargerId() : "")
                .build();
    }

    private ConnectorListResponse convertToConnectorListProto(ChargerAdminDtos.PagedResponse<ChargerAdminDtos.ConnectorResponse> response) {
        var builder = ConnectorListResponse.newBuilder();
        if (response.items() != null) {
            response.items().forEach(connector ->
                    builder.addConnectors(convertToConnectorProto(connector))
            );
        }
        builder.setTotal(response.total());
        return builder.build();
    }

    private com.electrahub.proto.charger.v1.ConnectorResponse convertToConnectorProto(ChargerAdminDtos.ConnectorResponse connector) {
        return com.electrahub.proto.charger.v1.ConnectorResponse.newBuilder()
                .setConnectorId(connector.connectorId() != null ? connector.connectorId() : "")
                .setEvseId(connector.evseId() != null ? connector.evseId() : "")
                .build();
    }
}
