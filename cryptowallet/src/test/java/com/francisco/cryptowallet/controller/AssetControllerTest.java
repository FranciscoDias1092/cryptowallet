package com.francisco.cryptowallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francisco.cryptowallet.dto.AssetDTO;
import com.francisco.cryptowallet.exception.TokenPriceException;
import com.francisco.cryptowallet.exception.WalletNotFoundException;
import com.francisco.cryptowallet.mapper.AssetMapper;
import com.francisco.cryptowallet.service.AssetService;
import com.francisco.cryptowallet.service.TokenService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(AssetController.class)
public class AssetControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Mock
    private AssetMapper assetMapper;

    @MockitoBean
    private AssetService assetService;

    @MockitoBean
    private TokenService tokenService;

    private AssetDTO assetDto;

    @BeforeEach
    public void setUp() {
        assetDto = new AssetDTO("BTC", 1.5, 0, 0);
    }

    @Test
    public void whenAddAsset_thenReturnAsset() throws Exception {
        when(assetService.addAsset(anyString(), any(AssetDTO.class))).thenReturn(assetDto);

        mockMvc.perform(post("/api/assets/email/{email}", "test@email.com")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(assetDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.quantity").value(1.5));

        verify(assetService, times(1)).addAsset(anyString(), any(AssetDTO.class));
    }

    @Test
    public void whenAddAssetInvalidEmail_thenThrowWalletNotFoundException() throws Exception {
        when(assetService.addAsset(anyString(), any(AssetDTO.class))).thenThrow(new WalletNotFoundException("bad@email.com"));

        mockMvc.perform(post("/api/assets/email/{email}", "bad@email.com")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(assetDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Wallet not found for email: bad@email.com!"));

        verify(assetService, times(1)).addAsset(anyString(), any(AssetDTO.class));
    }

    @Test
    public void whenAddAssetInvalidEmailFormat_thenThrowWalletNotFoundException() throws Exception {
        mockMvc.perform(post("/api/assets/email/{email}", "bademail.com")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(assetDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Argument validation failed!"));

        verify(assetService, never()).addAsset(anyString(), any(AssetDTO.class));
    }

    @Test
    public void whenAddAssetAndWalletNotFound_thenThrowWalletNotFoundException() throws Exception {
        when(assetService.addAsset(anyString(), any(AssetDTO.class))).thenThrow(new WalletNotFoundException());

        mockMvc.perform(post("/api/assets/email/{email}", "test@email.com")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(assetDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Wallet not found!"));

        verify(assetService, times(1)).addAsset(anyString(), any(AssetDTO.class));
        verify(assetMapper, never()).assetDTOToAsset(any(AssetDTO.class));
    }

    @Test
    public void whenAddAssetAndTokenDetailsNotFetched_thenReturnInternalServerError() throws Exception {
        when(assetService.addAsset(anyString(), any(AssetDTO.class))).thenThrow(new TokenPriceException("Could not fetch token details for Bitcoin!"));

        mockMvc.perform(post("/api/assets/email/{email}", "test@email.com")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(assetDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Could not fetch token details for Bitcoin!"));

        verify(assetService, times(1)).addAsset(anyString(), any(AssetDTO.class));
    }
}
