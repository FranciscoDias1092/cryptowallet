package com.francisco.cryptowallet.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import com.francisco.cryptowallet.domain.Wallet;
import com.francisco.cryptowallet.dto.WalletDTO;

@Mapper(componentModel = "spring", uses = AssetMapper.class)
public interface WalletMapper {
    WalletMapper INSTANCE = Mappers.getMapper(WalletMapper.class);

    /**
     * Map an Wallet to a WalletDTO.
     * 
     * assets mapped as is using AssetMapper.
     * email is ignored (not needed).
     * total (sum of all assets' values) calculated using Wallet's getTotal method.
     * 
     * @param wallet
     * @return the corresponding WalletDTO
     */
    @Mappings({
        @Mapping(source = "assets", target = "assets"),
        @Mapping(target = "email", ignore = true),
        @Mapping(target = "total", expression = "java(wallet.getTotal())")
    })
    WalletDTO walletToWalletDTO(Wallet wallet);

    /**
     * Map an WalletDTO to a Wallet.
     * 
     * assets mapped as is using AssetMapper.
     * 
     * @param walletDTO
     * @return the corresponding Wallet
     */
    @Mappings({
        @Mapping(source = "assets", target = "assets")
    })
    Wallet walletDTOToWallet(WalletDTO walletDTO);
}
