package com.barry.bank.financial.tracking_ws.mappers;

import com.barry.bank.financial.tracking_ws.dtos.OperationDTO;
import com.barry.bank.financial.tracking_ws.entities.Operation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OperationMapper {

    OperationDTO operationToOperationDto(Operation operation);
    @Mapping(target = "account", ignore = true)
    Operation operationDtoToOperation(OperationDTO operationDTO);
    List<OperationDTO> operationsToOperationDtos(List<Operation> operation);
}
