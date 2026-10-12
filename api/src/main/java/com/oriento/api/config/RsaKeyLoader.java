package com.oriento.api.config;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStreamReader;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class RsaKeyLoader {

    private static final Logger logger = LoggerFactory.getLogger(RsaKeyLoader.class);

    @Value("${jwt.public.key}")
    private Resource publicKeyResource;
    @Value("${jwt.private.key}")
    private Resource privateKeyResource;

    @Bean
    public RSAPublicKey publicKey() throws Exception {
        logger.info("Carregando chave pública RSA de: {}", publicKeyResource.getURI());
        RSAPublicKey key = loadPublicKey(publicKeyResource);
        logger.info("Chave pública RSA carregada com sucesso");
        return key;
    }

    @Bean
    public RSAPrivateKey privateKey() throws Exception {
        logger.info("Carregando chave privada RSA de: {}", privateKeyResource.getURI());
        RSAPrivateKey key = loadPrivateKey(privateKeyResource);
        logger.info("Chave privada RSA carregada com sucesso");
        return key;
    }

    private RSAPublicKey loadPublicKey(Resource resource) throws Exception {
        logger.debug("Parseando chave pública do formato PEM...");
        
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(resource.getInputStream()))) {
            SubjectPublicKeyInfo publicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            RSAPublicKey key = (RSAPublicKey) converter.getPublicKey(publicKeyInfo);
            
            logger.debug("Chave pública parseada com sucesso");
            return key;
        }
    }

    private RSAPrivateKey loadPrivateKey(Resource resource) throws Exception {
        logger.debug("Parseando chave privada do formato PEM...");
        
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(resource.getInputStream()))) {
            PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            RSAPrivateKey key = (RSAPrivateKey) converter.getPrivateKey(privateKeyInfo);
            
            logger.debug("Chave privada parseada com sucesso");
            return key;
        }
    }
}

