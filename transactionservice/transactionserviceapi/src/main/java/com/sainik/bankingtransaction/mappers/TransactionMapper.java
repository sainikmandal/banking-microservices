package com.sainik.bankingtransaction.mappers;

import com.sainik.bankingtransaction.dtos.TransactionDTO;
import com.sainik.bankingtransaction.models.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionDTO toDTO(Transaction transaction);

    Transaction toEntity(TransactionDTO transactionDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionDate", ignore = true)
    void updateEntityFromDTO(TransactionDTO dto, @MappingTarget Transaction entity);
}
