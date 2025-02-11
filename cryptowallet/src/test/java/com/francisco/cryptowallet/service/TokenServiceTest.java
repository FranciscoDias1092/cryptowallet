package com.francisco.cryptowallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import com.francisco.cryptowallet.domain.Token;
import com.francisco.cryptowallet.dto.TokenDataDTO;
import com.francisco.cryptowallet.dto.TokenResponseDTO;
import com.francisco.cryptowallet.repository.TokenRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TokenServiceTest {

    @MockitoBean
    private TokenRepository tokenRepository;

    @MockitoBean
    private RestTemplate restTemplate;
    
    @Autowired
    private TokenService tokenService;

    private Token token;

    @BeforeEach
    void setUp() {
        tokenService = Mockito.spy(tokenService);
        token = Token.builder()
                        .id("bitcoin")
                        .symbol("BTC")
                        .price(1500.00)
                        .build();
    }

    @Test
    public void whenFetchAndUpdateTokenPrices_UpdateTokenPrices() {
        when(tokenRepository.findAll()).thenReturn(Arrays.asList(token));
        when(tokenRepository.findById(anyString())).thenReturn(null);

        TokenDataDTO dataDTO = new TokenDataDTO("bitcoin", null, "BTC", null, null, null, null, null, "5000.00", null, null);
        TokenResponseDTO responseDTO = new TokenResponseDTO(dataDTO);

        when(restTemplate.getForObject(anyString(), eq(TokenResponseDTO.class))).thenReturn(responseDTO);

        tokenService.updateAllTokenPrices();

        verify(tokenRepository, times(1)).save(token);
        assertEquals(5000.00, token.getPrice());
    }

    @Test
    public void whenUpdateAllTokenPricesNoToken_NoUpdate() {
        when(tokenRepository.findAll()).thenReturn(Arrays.asList());
        
        tokenService.updateAllTokenPrices();

        verify(tokenService, never()).fetchAndUpdateTokenPrice(eq(token));
        verify(tokenRepository, never()).save(token);
    }

    @Test
    public void whenFetchTokenPricesWithRetry_PriceFetched() {
        when(restTemplate.getForObject(anyString(), eq(TokenResponseDTO.class)))
            .thenReturn(new TokenResponseDTO(
                new TokenDataDTO("bitcoin", null, "BTC", null, null, null, null, null, "5000.00", null, null)));
        
        Double price = tokenService.fetchTokenPriceWithRetry(token);

        assertEquals(5000.00, price);
    }
}
