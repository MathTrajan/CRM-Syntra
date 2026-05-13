package com.syntra.repository;

import com.syntra.model.Lead;
import com.syntra.model.enums.StatusLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, String>, JpaSpecificationExecutor<Lead> {

    Optional<Lead> findByOrigemExternaAndLeadExternoId(String origemExterna, String leadExternoId);

    long countByStatus(StatusLead status);

    long countByLidoFalse();

    void deleteByOrigemExternaNot(String origemExterna);

    // Sem JOIN FETCH — paginação funciona corretamente; lazy loading via open-in-view
    //
    // Busca varre TODOS os campos textuais do lead (nome, email, telefone, origem,
    // campanha, mensagem, dadosExtras - que armazena o JSON da API externa).
    // Quando o usuario digita apenas digitos (numero de telefone) o LeadService
    // tambem passa :buscaDigits com so digitos para casar com a coluna telefone
    // independente de espacos, parenteses ou tracos.
    //
    // CAST(:param AS tipo) e' obrigatorio em TODOS os parametros nullable porque
    // o Hibernate 6 nao envia tipo do bind quando o valor e' null; sem o CAST
    // o Postgres infere bytea e o statement falha na preparacao.
    @Query("""
        SELECT l FROM Lead l
        WHERE (CAST(:status AS string) IS NULL OR l.status = :status)
          AND (:semVendedor = FALSE OR l.vendedor IS NULL)
          AND (:semVendedor = TRUE  OR CAST(:vendedorId AS string) IS NULL OR l.vendedor.id = :vendedorId)
          AND (CAST(:busca AS string) IS NULL
               OR LOWER(l.nome)        LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
               OR LOWER(l.email)       LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
               OR LOWER(l.origem)      LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
               OR LOWER(l.campanha)    LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
               OR LOWER(l.mensagem)    LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
               OR LOWER(l.dadosExtras) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
               OR l.telefone           LIKE CONCAT('%', CAST(:busca AS string), '%')
               OR (CAST(:buscaDigits AS string) IS NOT NULL
                   AND CAST(FUNCTION('regexp_replace', l.telefone, '[^0-9]', '', 'g') AS string)
                       LIKE CONCAT('%', CAST(:buscaDigits AS string), '%')))
          AND (CAST(:dataInicio AS timestamp) IS NULL OR l.criadoEm >= :dataInicio)
          AND (CAST(:dataFim    AS timestamp) IS NULL OR l.criadoEm <= :dataFim)
        ORDER BY l.criadoEm DESC
        """)
    Page<Lead> buscar(
            @Param("status") StatusLead status,
            @Param("semVendedor") boolean semVendedor,
            @Param("vendedorId") String vendedorId,
            @Param("busca") String busca,
            @Param("buscaDigits") String buscaDigits,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable
    );

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.criadoEm >= :desde")
    long countRecentes(@Param("desde") LocalDateTime desde);

    @Query("""
        SELECT l.status, COUNT(l)
        FROM Lead l
        GROUP BY l.status
        """)
    List<Object[]> contarPorStatus();
}
