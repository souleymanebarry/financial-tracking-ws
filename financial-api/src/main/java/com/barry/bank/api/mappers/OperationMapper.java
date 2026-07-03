package com.barry.bank.api.mappers;

import com.barry.bank.api.dtos.OperationDTO;
import com.barry.bank.domain.model.Operation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OperationMapper {

    OperationDTO operationToOperationDto(Operation operation);
    @Mapping(target = "account", ignore = true)
    Operation operationDtoToOperation(OperationDTO operationDTO);
    List<OperationDTO> operationsToOperationDtos(List<Operation> operation);
}
