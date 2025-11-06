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

/**
 * Configuração responsável por carregar chaves RSA do sistema de arquivos.
 * 
 * Esta classe carrega as chaves RSA (pública e privada) a partir de arquivos PEM
 * localizados no classpath ou filesystem. As chaves são usadas para:
 * - Assinar tokens JWT (chave privada)
 * - Validar tokens JWT (chave pública)
 * 
 * Localização das chaves (configurável via application.properties):
 * - jwt.public.key: caminho para arquivo da chave pública (ex: classpath:keys/app.pub)
 * - jwt.private.key: caminho para arquivo da chave privada (ex: classpath:keys/app.key)
 * 
 * Formato esperado: arquivos PEM (Privacy-Enhanced Mail) contendo chaves RSA.
 * 
 * IMPORTANTE: A chave privada deve ser mantida em segredo e nunca commitada
 * em repositórios públicos. Use variáveis de ambiente ou arquivos externos
 * em produção.
 */
@Configuration
public class RsaKeyLoader {

    private static final Logger logger = LoggerFactory.getLogger(RsaKeyLoader.class);

    /**
     * Recurso (arquivo) contendo a chave pública RSA.
     * Carregado do caminho especificado em jwt.public.key.
     */
    @Value("${jwt.public.key}")
    private Resource publicKeyResource;

    /**
     * Recurso (arquivo) contendo a chave privada RSA.
     * Carregado do caminho especificado em jwt.private.key.
     */
    @Value("${jwt.private.key}")
    private Resource privateKeyResource;

    /**
     * Carrega a chave pública RSA do arquivo especificado.
     * 
     * O arquivo é lido do classpath ou filesystem, parseado como PEM,
     * e convertido para um objeto RSAPublicKey que pode ser usado
     * para validar tokens JWT.
     * 
     * @return RSAPublicKey carregada do arquivo
     * @throws Exception se ocorrer erro ao ler ou parsear o arquivo
     */
    @Bean
    public RSAPublicKey publicKey() throws Exception {
        logger.info("Carregando chave pública RSA de: {}", publicKeyResource.getURI());
        RSAPublicKey key = loadPublicKey(publicKeyResource);
        logger.info("Chave pública RSA carregada com sucesso");
        return key;
    }

    /**
     * Carrega a chave privada RSA do arquivo especificado.
     * 
     * O arquivo é lido do classpath ou filesystem, parseado como PEM,
     * e convertido para um objeto RSAPrivateKey que pode ser usado
     * para assinar tokens JWT.
     * 
     * IMPORTANTE: Esta chave deve ser mantida em segredo.
     * 
     * @return RSAPrivateKey carregada do arquivo
     * @throws Exception se ocorrer erro ao ler ou parsear o arquivo
     */
    @Bean
    public RSAPrivateKey privateKey() throws Exception {
        logger.info("Carregando chave privada RSA de: {}", privateKeyResource.getURI());
        RSAPrivateKey key = loadPrivateKey(privateKeyResource);
        logger.info("Chave privada RSA carregada com sucesso");
        return key;
    }

    /**
     * Método auxiliar para carregar chave pública de um arquivo PEM.
     * 
     * Usa BouncyCastle para fazer o parsing do formato PEM e converter
     * para um objeto Java RSAPublicKey.
     * 
     * @param resource Recurso (arquivo) contendo a chave pública em formato PEM
     * @return RSAPublicKey parseada do arquivo
     * @throws Exception se ocorrer erro ao ler ou parsear o arquivo
     */
    private RSAPublicKey loadPublicKey(Resource resource) throws Exception {
        logger.debug("Parseando chave pública do formato PEM...");
        
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(resource.getInputStream()))) {
            // Lê o objeto PEM do arquivo
            SubjectPublicKeyInfo publicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
            
            // Converte para objeto Java RSAPublicKey
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            RSAPublicKey key = (RSAPublicKey) converter.getPublicKey(publicKeyInfo);
            
            logger.debug("Chave pública parseada com sucesso");
            return key;
        }
    }

    /**
     * Método auxiliar para carregar chave privada de um arquivo PEM.
     * 
     * Usa BouncyCastle para fazer o parsing do formato PEM e converter
     * para um objeto Java RSAPrivateKey.
     * 
     * @param resource Recurso (arquivo) contendo a chave privada em formato PEM
     * @return RSAPrivateKey parseada do arquivo
     * @throws Exception se ocorrer erro ao ler ou parsear o arquivo
     */
    private RSAPrivateKey loadPrivateKey(Resource resource) throws Exception {
        logger.debug("Parseando chave privada do formato PEM...");
        
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(resource.getInputStream()))) {
            // Lê o objeto PEM do arquivo
            PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) pemParser.readObject();
            
            // Converte para objeto Java RSAPrivateKey
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            RSAPrivateKey key = (RSAPrivateKey) converter.getPrivateKey(privateKeyInfo);
            
            logger.debug("Chave privada parseada com sucesso");
            return key;
        }
    }
}

