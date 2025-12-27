package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.TransferRequestDTO;
import com.example.onlyfanshop_be.dto.request.CreateTransferRequestDTO;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ITransferRequestService {

    TransferRequestDTO createRequest(Integer storeId, CreateTransferRequestDTO request);

    Page<TransferRequestDTO> getRequests(TransferRequestStatus status, Pageable pageable);

    Page<TransferRequestDTO> getRequestsByStore(Integer storeId, TransferRequestStatus status, Pageable pageable);

    TransferRequestDTO getRequest(Long id);

    TransferRequestDTO approveRequest(Long id);

    TransferRequestDTO rejectRequest(Long id, String reason);

    TransferRequestDTO completeRequest(Long id);

    TransferRequestDTO cancelRequest(Long id);
}
