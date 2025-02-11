package com.francisco.cryptowallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "tokens")
public class Token {
    @Id
    @Column(
        unique = true,
        nullable = false
    )
    private String id;

    @Column(
        unique = true,
        nullable = false
    )
    private String symbol;

    @Column(nullable = false)
    private Double price;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String symbol;
        private Double price;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder price(Double price) {
            this.price = price;
            return this;
        }

        public Token build() {
            Token token = new Token();
            
            token.setId(this.id);
            token.setSymbol(this.symbol);
            token.setPrice(this.price);
            
            return token;
        }
    }
}
