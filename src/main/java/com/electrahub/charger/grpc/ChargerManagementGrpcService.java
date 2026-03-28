package com.electrahub.charger.grpc;

import com.electrahub.proto.charger.v1.ChargerManagementServiceGrpc;
import com.electrahub.proto.charger.v1.CreateChargerRequest;
import com.electrahub.proto.charger.v1.ListChargersRequest;
import com.electrahub.proto.charger.v1.GetChargerRequest;
import com.electrahub.proto.charger.v1.UpdateChargerRequest;
import com.electrahub.proto.charger.v1.UpdateChargerStatusRequest;
import com.electrahub.proto.charger.v1.DeleteChargerRequest;
import com.electrahub.proto.charger.v1.ChargerResponse;
import com.electrahub.proto.charger.v1.ChargerListResponse;
import com.electrahub.charger.api.dto.ChargerDtos;
import com.electrahub.charger.service.ChargerManagementService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class ChargerManagementGrpcService extends ChargerManagementServiceGrpc.ChargerManagementServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerManagementGrpcService.class);

    private final ChargerManagementService chargerManagementService;

    public ChargerManagementGrpcService(ChargerManagementService chargerManagementService) {
        this.chargerManagementService = chargerManagementService;
    }

    @Override
    public void createCharger(CreateChargerRequest request, StreamObserver<ChargerResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Creating charger: {}", request.getChargerId());

            ChargerDtos.ChargerUpsertRequest upsertRequest = new ChargerDtos.ChargerUpsertRequest(
                    request.getChargerId(),
                    request.getSiteName(),
                    request.getOcppVersion(),
                    request.getStatus()
            );

            ChargerDtos.ChargerResponse charger = chargerManagementService.create(upsertRequest);
            ChargerResponse response = convertToProto(charger);

            responseObserver.onNext(response);
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
    public void listChargers(ListChargersRequest request, StreamObserver<ChargerListResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Listing chargers with filters");

            ChargerDtos.ChargerListResponse listResponse = chargerManagementService.list(
                    request.getStatus(),
                    request.getOcppVersion(),
                    request.getSiteName(),
                    request.getEnabled()
            );

            ChargerListResponse response = convertToListProto(listResponse);
            responseObserver.onNext(response);
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
    public void getCharger(GetChargerRequest request, StreamObserver<ChargerResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Getting charger: {}", request.getChargerId());

            ChargerDtos.ChargerResponse charger = chargerManagementService.get(request.getChargerId());
            ChargerResponse response = convertToProto(charger);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.warn("Charger not found: {}", request.getChargerId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Charger not found")
                            .asException()
            );
        }
    }

    @Override
    public void updateCharger(UpdateChargerRequest request, StreamObserver<ChargerResponse> responseObserver) {
        try {
            LOGGER.debug("gRPC: Updating charger: {}", request.getChargerId());

            ChargerDtos.ChargerUpsertRequest upsertRequest = new ChargerDtos.ChargerUpsertRequest(
                    request.getChargerId(),
                    request.getSiteName(),
                    request.getOcppVersion(),
                    request.getStatus()
            );

            ChargerDtos.ChargerResponse charger = chargerManagementService.update(request.getChargerId(), upsertRequest);
            ChargerResponse response = convertToProto(charger);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in updateCharger", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.warn("Charger not found during update: {}", request.getChargerId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Charger not found")
                            .asException()
            );
        }
    }

    @Override
    public void updateChargerStatus(
            UpdateChargerStatusRequest request,
            StreamObserver<ChargerResponse> responseObserver
    ) {
        try {
            LOGGER.debug("gRPC: Updating charger status: {}", request.getChargerId());

            ChargerDtos.ChargerStatusUpdateRequest statusRequest = new ChargerDtos.ChargerStatusUpdateRequest(
                    request.getStatus()
            );

            ChargerDtos.ChargerResponse charger = chargerManagementService.updateStatus(request.getChargerId(), statusRequest);
            ChargerResponse response = convertToProto(charger);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid argument in updateChargerStatus", e);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asException()
            );
        } catch (Exception e) {
            LOGGER.warn("Charger not found during status update: {}", request.getChargerId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Charger not found")
                            .asException()
            );
        }
    }

    @Override
    public void deleteCharger(DeleteChargerRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            LOGGER.debug("gRPC: Deleting charger: {}", request.getChargerId());

            chargerManagementService.delete(request.getChargerId());

            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.warn("Charger not found during deletion: {}", request.getChargerId());
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Charger not found")
                            .asException()
            );
        }
    }

    private ChargerResponse convertToProto(ChargerDtos.ChargerResponse charger) {
        return ChargerResponse.newBuilder()
                .setChargerId(charger.chargerId() != null ? charger.chargerId() : "")
                .setSiteName(charger.siteName() != null ? charger.siteName() : "")
                .setOcppVersion(charger.ocppVersion() != null ? charger.ocppVersion() : "")
                .setStatus(charger.status() != null ? charger.status() : "")
                .setEnabled(charger.enabled())
                .build();
    }

    private ChargerListResponse convertToListProto(ChargerDtos.ChargerListResponse listResponse) {
        var builder = ChargerListResponse.newBuilder();

        if (listResponse.chargers() != null) {
            listResponse.chargers().forEach(charger ->
                    builder.addChargers(convertToProto(charger))
            );
        }

        builder.setTotal(listResponse.total());
        return builder.build();
    }
}
