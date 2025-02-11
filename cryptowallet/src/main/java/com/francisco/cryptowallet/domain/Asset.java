package com.francisco.cryptowallet.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"
)
@Entity
@Data
@Table(name = "assets")
public class Asset {
    @Id
    @SequenceGenerator(
        name = "user_sequence",
        sequenceName = "user_sequence",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "user_sequence"
    )
    private Long id;

    @ManyToOne
    @JoinColumn(
        name = "wallet_id",
        nullable = false
    )
    private Wallet wallet;

    @ManyToOne
    @JoinColumn(
        name = "token_id",
        nullable = false
    )
    private Token token;

    private double quantity;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Wallet wallet;
        private Token token;
        private double quantity;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder wallet(Wallet wallet) {
            this.wallet = wallet;
            return this;
        }

        public Builder token(Token token) {
            this.token = token;
            return this;
        }

        public Builder quantity(double quantity) {
            this.quantity = quantity;
            return this;
        }

        public Asset build() {
            Asset asset = new Asset();
            
            asset.setId(this.id);
            asset.setWallet(this.wallet);
            asset.setToken(this.token);
            asset.setQuantity(this.quantity);
            
            return asset;
        }
    }

    public Double getValue() {
        return this.quantity * this.token.getPrice();
    }
}
