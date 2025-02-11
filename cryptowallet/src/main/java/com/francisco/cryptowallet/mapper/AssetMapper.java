package com.francisco.cryptowallet.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import com.francisco.cryptowallet.domain.Asset;
import com.francisco.cryptowallet.domain.Token;
import com.francisco.cryptowallet.dto.AssetDTO;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    AssetMapper INSTANCE = Mappers.getMapper(AssetMapper.class);
    
    /**
     * Map an Asset to a AssetDTO.
     * 
     * symbol fetched from the respective Assets's symbol.
     * price fetched from the respective Assets's price.
     * value total value (quantity * Asset's Token's price) calculated with Asset's getValue.
     * 
     * @param asset
     * @return the corresponding AssetDTO
     */
    @Mappings({
        @Mapping(target = "symbol", expression = "java(asset.getToken().getSymbol())"),
        @Mapping(target = "price", expression = "java(asset.getToken().getPrice())"),
        @Mapping(target = "value", expression = "java(asset.getValue())")
    })
    AssetDTO assetToAssetDTO(Asset asset);

    /**
     * Map an AssetDTO to a Asset.
     * 
     * id and wallet are ignored (not needed).
     * token fetched using getTokenFromSymbol (mainly important for tests).
     * 
     * @param assetDto
     * @return
     */
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "wallet", ignore = true),
        @Mapping(target = "token", expression = "java(getTokenFromSymbol(assetDto.symbol()))")
    })
    Asset assetDTOToAsset(AssetDTO assetDto);

    /**
     * @param symbol
     * @return
     */
    default Token getTokenFromSymbol(String symbol) {
        return new Token();
    }
}
