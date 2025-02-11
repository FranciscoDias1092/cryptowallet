package com.francisco.cryptowallet.domain;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import lombok.Data;

@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"
)
@Entity
@Data
@Table(name = "wallets")
@NamedEntityGraph(
    name = "Wallet.assets",
    attributeNodes = @NamedAttributeNode("assets")
)
public class Wallet {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(
        unique = true,
        nullable = false
    )
    private String email;

    @OneToMany(
        mappedBy = "wallet", 
        cascade = CascadeType.ALL,
        fetch = FetchType.LAZY
    )
    private List<Asset> assets;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String email;
        private List<Asset> assets;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder assets(List<Asset> assets) {
            this.assets = assets;
            return this;
        }

        public Wallet build() {
            Wallet wallet = new Wallet();
            
            wallet.setId(this.id);
            wallet.setEmail(this.email);
            wallet.setAssets(this.assets);
            
            return wallet;
        }
    }

    public Double getTotal() {
        if (assets == null || assets.isEmpty()) {
            return 0.0;
        }

        return assets.stream()
                        .mapToDouble(Asset::getValue)
                        .sum();
    }
}
