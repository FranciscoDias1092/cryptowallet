package com.francisco.cryptowallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.francisco.cryptowallet.dto.CreateWalletRequestDTO;
import com.francisco.cryptowallet.dto.WalletDTO;
import com.francisco.cryptowallet.dto.WalletEvaluationResponseDTO;
import com.francisco.cryptowallet.exception.NotFoundException;
import com.francisco.cryptowallet.exception.WalletAlreadyExistsException;
import com.francisco.cryptowallet.exception.WalletNotFoundException;
import com.francisco.cryptowallet.mapper.WalletMapper;
import com.francisco.cryptowallet.service.WalletService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(WalletController.class)
public class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private WalletMapper assetMapper;

    @MockitoBean
    private WalletService walletService;

    private WalletDTO walletDto;

    @BeforeEach
    public void setUp() {
        walletDto = new WalletDTO(UUID.randomUUID(), "test@email.com", 0.0, null);
    }

    @Test
    public void whenCreateWallet_thenReturnEmptyWallet() throws Exception {
        when(walletService.createWallet(anyString())).thenReturn(walletDto);

        mockMvc.perform(post("/api/wallets/create/{email}", "test@email.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.total").value("0.0"));

        verify(walletService, times(1)).createWallet("test@email.com");
    }

    @Test
    public void whenCreateWalletThatAlreadyExists_thenThrowWalletAlreadyExistsException() throws Exception {
        when(walletService.createWallet(anyString())).thenThrow(new WalletAlreadyExistsException("test@email.com"));

        mockMvc.perform(post("/api/wallets/create/{email}", "test@email.com"))
                .andExpect(status().isConflict())
                .andExpect(content().string("A wallet already exists for the email: test@email.com!"));

        verify(walletService, times(1)).createWallet("test@email.com");
    }

    @Test
    public void whenCreateWalletInvalidEmail_thenThrowHandlerMethodValidationException() throws Exception {
        mockMvc.perform(post("/api/wallets/create/{email}", "test.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Argument validation failed!"));

        verify(walletService, never()).createWallet("test@email.com");
    }

    @Test
    public void whenCreateWalletRequestBody_thenReturnEmptyWallet() throws Exception {
        CreateWalletRequestDTO request = new CreateWalletRequestDTO("test@email.com");
        when(walletService.createWallet(anyString())).thenReturn(walletDto);

        mockMvc.perform(post("/api/wallets/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.total").value("0.0"));

        verify(walletService, times(1)).createWallet("test@email.com");
    }

    @Test
    public void whenCreateWalletRequestBodyWithInvalidEmail_thenThrowMethodArgumentNotValidException() throws Exception {
        CreateWalletRequestDTO request = new CreateWalletRequestDTO("testemail.com");

        when(walletService.createWallet(anyString())).thenReturn(walletDto);

        mockMvc.perform(post("/api/wallets/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("must be a well-formed email address"));

        verify(walletService, never()).createWallet(anyString());
    }

    @Test
    public void whenCreateWalletRequestBodyWithNoEmail_thenThrowMethodArgumentNotValidException() throws Exception {
        CreateWalletRequestDTO request = new CreateWalletRequestDTO("");

        when(walletService.createWallet(anyString())).thenReturn(walletDto);

        mockMvc.perform(post("/api/wallets/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("must not be blank"));

        verify(walletService, never()).createWallet(anyString());
    }

    @Test
    public void whenGetWalletById_thenReturnWallet() throws Exception {
        UUID walletId = UUID.randomUUID();

        when(walletService.getWallet(walletId)).thenReturn(walletDto);

        mockMvc.perform(get("/api/wallets/id/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@email.com"));

        verify(walletService, times(1)).getWallet(walletId);
    }

    @Test
    public void whenGetWalletByIdNotExists_thenThrowWalletNotFoundException() throws Exception {
        UUID walletId = UUID.randomUUID();

        when(walletService.getWallet(walletId)).thenThrow(new WalletNotFoundException());

        mockMvc.perform(get("/api/wallets/id/{id}", walletId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Wallet not found!"));

        verify(walletService, times(1)).getWallet(walletId);
    }

    @Test
    public void whenGetWalletByEmail_thenReturnWallet() throws Exception {
        when(walletService.getWallet("test@email.com")).thenReturn(walletDto);

        mockMvc.perform(get("/api/wallets/email/{email}", "test@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@email.com"));

        verify(walletService, times(1)).getWallet("test@email.com");
    }

    @Test
    public void whenGetWalletByEmailNotExists_thenThrowWalletNotFoundException() throws Exception {
        UUID walletId = UUID.randomUUID();

        when(walletService.getWallet(walletId)).thenThrow(new WalletNotFoundException());

        mockMvc.perform(get("/api/wallets/id/{id}", walletId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Wallet not found!"));

        verify(walletService, times(1)).getWallet(walletId);
    }

    @Test
    public void whenEvaluateWallet_thenReturnEvaluation() throws Exception {
        UUID walletId = UUID.randomUUID();

        WalletEvaluationResponseDTO evaluationResponseDTO = new WalletEvaluationResponseDTO(5000.00, "BTC", 1.0, "ETH",
                0.9);

        when(walletService.evaluateWallet(eq(walletId), any())).thenReturn(evaluationResponseDTO);

        mockMvc.perform(get("/api/wallets/evaluate/{id}", walletId)
                .param("date", "2025-02-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5000.00));

        verify(walletService, times(1)).evaluateWallet(eq(walletId), any());
    }

    @Test
    public void whenEvaluateEmptyWallet_thenThrowNotFoundException() throws Exception {
        UUID walletId = UUID.randomUUID();

        when(walletService.evaluateWallet(eq(walletId), any())).thenThrow(new NotFoundException("Wallet is empty!"));

        mockMvc.perform(get("/api/wallets/evaluate/{id}", walletId)
                    .param("date", "2025-02-07"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Wallet is empty!"));

        verify(walletService, times(1)).evaluateWallet(eq(walletId), any());
    }
}
