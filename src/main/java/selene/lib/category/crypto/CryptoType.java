package selene.lib.category.crypto;

import lombok.Getter;

import java.util.*;
import java.util.regex.Pattern;

@Getter
public enum CryptoType {
    // Bitcoin
    MD5("md5", 32),
    SHA1("sha1", 40),
    SHA224("sha224", 56),
    SHA256("sha256", 64),
    SHA384("sha384", 96),
    SHA512("sha512", 128),

    // Bitcoin
    BITCOIN_LEGACY("bitcoin-legacy", "^[1][a-km-zA-HJ-NP-Z1-9]{25,34}$"),
    BITCOIN_SCRIPT("bitcoin-script", "^[3][a-km-zA-HJ-NP-Z1-9]{25,34}$"),
    BITCOIN_SEGWIT("bitcoin-segwit", "^(bc1)[a-z0-9]{39,59}$"),
    BITCOIN_TAPROOT("bitcoin-taproot", "^(bc1p)[a-z0-9]{58}$"),

    // Bitcoin Testnet
    BITCOIN_TESTNET("bitcoin-testnet", "^[mn2][a-km-zA-HJ-NP-Z1-9]{25,34}$"),
    BITCOIN_TESTNET_SEGWIT("bitcoin-testnet-segwit", "^(tb1)[a-z0-9]{39,59}$"),

    // Ethereum & EVM Chains
    ETHEREUM_ADDRESS("ethereum-address", "^0x[a-fA-F0-9]{40}$"),
    ETHEREUM_TX_HASH("ethereum-tx-hash", "^0x[a-fA-F0-9]{64}$"),

    // Litecoin
    LITECOIN_LEGACY("litecoin-legacy", "^[L][a-km-zA-HJ-NP-Z1-9]{26,33}$"),
    LITECOIN_SCRIPT("litecoin-script", "^[M][a-km-zA-HJ-NP-Z1-9]{26,33}$"),
    LITECOIN_SEGWIT("litecoin-segwit", "^(ltc1)[a-z0-9]{39,59}$"),

    // Dogecoin
    DOGECOIN("dogecoin", "^[D][5-9A-HJ-NP-U][1-9A-HJ-NP-Za-km-z]{32}$"),
    DOGECOIN_ALT("dogecoin-alt", "^[A][5-9A-HJ-NP-U][1-9A-HJ-NP-Za-km-z]{32}$"),

    // Ripple (XRP)
    RIPPLE_CLASSIC("ripple-classic", "^r[1-9A-HJ-NP-Za-km-z]{25,34}$"),
    RIPPLE_X("ripple-x", "^X[1-9A-HJ-NP-Za-km-z]{46}$"),

    // Cardano
    CARDANO_SHELLEY("cardano-shelley", "^addr1[a-z0-9]{58,}$"),
    CARDANO_BYRON("cardano-byron", "^Ae2[a-km-zA-HJ-NP-Z1-9]{93,}$"),

    // Monero
    MONERO("monero", "^[48][0-9AB][1-9A-HJ-NP-Za-km-z]{93}$"),
    MONERO_SUB("monero-sub", "^[8][0-9AB][1-9A-HJ-NP-Za-km-z]{93}$"),
    MONERO_INTEGRATED("monero-integrated", "^[4][0-9AB][1-9A-HJ-NP-Za-km-z]{105}$"),

    // Stellar
    STELLAR("stellar", "^G[A-Z2-7]{55}$"),

    // Tron
    TRON("tron", "^T[1-9A-HJ-NP-Za-km-z]{33}$"),

    // EOS
    EOS("eos", "^[a-z1-5\\.]{1,12}$"),

    // Polkadot
    POLKADOT("polkadot", "^[1-9A-HJ-NP-Za-km-z]{47,48}$"),

    // Solana
    SOLANA("solana", "^[1-9A-HJ-NP-Za-km-z]{32,44}$"),

    // Binance
    BINANCE("binance", "^bnb1[a-z0-9]{38}$"),

    // Cosmos
    COSMOS("cosmos", "^cosmos1[a-z0-9]{38}$"),

    // Tezos
    TEZOS_TZ1("tezos-tz1", "^tz1[1-9A-HJ-NP-Za-km-z]{33}$"),
    TEZOS_TZ2("tezos-tz2", "^tz2[1-9A-HJ-NP-Za-km-z]{33}$"),
    TEZOS_TZ3("tezos-tz3", "^tz3[1-9A-HJ-NP-Za-km-z]{33}$"),

    // Zcash
    ZCASH_TADDR("zcash-taddr", "^t[13][a-km-zA-HJ-NP-Z1-9]{33}$"),
    ZCASH_ZADDR("zcash-zaddr", "^zs1[a-z0-9]{75}$"),

    // Dash
    DASH("dash", "^X[1-9A-HJ-NP-Za-km-z]{33}$"),

    // NEO
    NEO("neo", "^A[0-9a-zA-Z]{33}$"),

    // Algorand
    ALGORAND("algorand", "^[A-Z2-7]{58}$"),

    // Avalanche
    AVALANCHE_X("avalanche-x", "^X-[a-zA-Z0-9]{39}$"),
    AVALANCHE_C("avalanche-c", "^0x[a-fA-F0-9]{40}$"),

    // Hedera
    HEDERA("hedera", "^0\\.0\\.[0-9]+$"),

    // IOTA
    IOTA("iota", "^[A-Z9]{90}$"),

    // VeChain
    VECHAIN("vechain", "^0x[a-fA-F0-9]{40}$"),

    // Filecoin
    FILECOIN_F1("filecoin-f1", "^f1[a-z0-9]{38,}$"),
    FILECOIN_F3("filecoin-f3", "^f3[a-z0-9]{38,}$"),

    // Near
    NEAR("near", "^[a-z0-9_\\-\\.]{2,64}\\.near$"),

    // Kusama
    KUSAMA("kusama", "^[C-H][1-9A-HJ-NP-Za-km-z]{46,47}$"),

    // Elrond
    ELROND("elrond", "^erd1[a-z0-9]{58}$"),

    // Fantom
    FANTOM("fantom", "^0x[a-fA-F0-9]{40}$"),

    // Polygon
    POLYGON("polygon", "^0x[a-fA-F0-9]{40}$"),

    // Bitcoin Cash
    BCH_LEGACY("bch-legacy", "^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$"),
    BCH_CASHADDR("bch-cashaddr", "^((bitcoincash|bchreg|bchtest):)?[qp][a-z0-9]{41}$");

    private final String name;
    private final Pattern regex;
    private final Integer size;

     CryptoType(String name, String regex){
        this.name = name;
        this.regex = Pattern.compile(regex);
        size = null;
    }

    CryptoType(String name, Integer size){
         this.name = name;
         this.size = size;
         this.regex = null;
    }

    public static List<CryptoType> getTypes(String candidate){
         List<CryptoType> types = new ArrayList<>();
         for(CryptoType type : CryptoType.values()){
             if(type.isMatch(candidate)){
                 types.add(type);
             }
         }
        return types;
    }

    public boolean isMatch(String candidate){
         if(regex == null){
             return  candidate.length() == size;
         }
         return regex.matcher(candidate.trim()).matches();
    }


}
