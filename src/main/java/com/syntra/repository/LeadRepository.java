package com.syntra.repository;

import com.syntra.model.Lead;
import com.syntra.model.enums.JornadaLead;
import com.syntra.model.enums.StatusLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, String>, JpaSpecificationExecutor<Lead> {

    /**
     * Busca leads de um vendedor específico cujo status esteja na coleção fornecida,
     * ordenados por ultimaInteracaoEm crescente (mais parados primeiro).
     * Usado pelo AlertaService para encontrar candidatos a alerta.
     *
     * @param vendedorId ID do usuario/vendedor
     * @param statuses   conjunto de statuses candidatos
     * @return lista de leads, ordenada por ultimaInteracaoEm ASC
     */
    List<Lead> findByVendedorIdAndStatusInOrderByUltimaInteracaoEmAsc(
            String vendedorId, Collection<StatusLead> statuses);

    Optional<Lead> findByOrigemExternaAndLeadExternoId(String origemExterna, String leadExternoId);

    Optional<Lead> findFirstByVendedorIsNotNullOrderByCriadoEmDesc();

    /**
     * Busca o lead mais recente que ja existe para o mesmo cliente, comparando
     * por e-mail (case-insensitive) OU pelos ultimos digitos do telefone. Usado
     * para evitar duplicidade de tratamento: se o mesmo cliente chegar de novo,
     * o vendedor e a jornada anteriores sao reaplicados.
     *
     * Os parametros podem ser null: nesse caso a clausula correspondente e' ignorada.
     * O telefone deve ser passado ja' como string de digitos (sem formatacao).
     */
    @Query("""
        SELECT l FROM Lead l
        WHERE (CAST(:emailNorm AS string) IS NOT NULL AND LOWER(TRIM(l.email)) = :emailNorm)
           OR (CAST(:telDigits AS string) IS NOT NULL
               AND CAST(FUNCTION('regexp_replace', l.telefone, '[^0-9]', '', 'g') AS string)
                   LIKE CONCAT('%', CAST(:telDigits AS string)))
        ORDER BY l.criadoEm DESC
        """)
    List<Lead> findClienteExistente(@Param("emailNorm") String emailNorm,
                                    @Param("telDigits") String telDigits);

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
          AND (CAST(:jornada AS string) IS NULL OR l.jornada = :jornada)
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
            @Param("jornada") JornadaLead jornada,
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

    @Query("""
        SELECT COALESCE(NULLIF(TRIM(l.origem), ''), 'Sem origem') AS origem,
               COUNT(l)
        FROM Lead l
        WHERE (CAST(:inicio AS timestamp) IS NULL OR l.criadoEm >= :inicio)
          AND (CAST(:fim    AS timestamp) IS NULL OR l.criadoEm <= :fim)
        GROUP BY COALESCE(NULLIF(TRIM(l.origem), ''), 'Sem origem')
        ORDER BY COUNT(l) DESC
        """)
    List<Object[]> contarPorOrigem(@Param("inicio") LocalDateTime inicio,
                                   @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT l.status, COUNT(l)
        FROM Lead l
        WHERE (CAST(:inicio AS timestamp) IS NULL OR l.criadoEm >= :inicio)
          AND (CAST(:fim    AS timestamp) IS NULL OR l.criadoEm <= :fim)
        GROUP BY l.status
        """)
    List<Object[]> contarPorStatusPeriodo(@Param("inicio") LocalDateTime inicio,
                                          @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE (CAST(:inicio AS timestamp) IS NULL OR l.criadoEm >= :inicio)
          AND (CAST(:fim    AS timestamp) IS NULL OR l.criadoEm <= :fim)
        """)
    long countNoPeriodo(@Param("inicio") LocalDateTime inicio,
                        @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.status = :status
          AND (CAST(:inicio AS timestamp) IS NULL OR l.criadoEm >= :inicio)
          AND (CAST(:fim    AS timestamp) IS NULL OR l.criadoEm <= :fim)
        """)
    long countByStatusPeriodo(@Param("status") StatusLead status,
                              @Param("inicio") LocalDateTime inicio,
                              @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT COALESCE(l.jornada, NULL) AS jornada,
               SUM(CASE WHEN l.status = com.syntra.model.enums.StatusLead.CONVERTIDO THEN 1 ELSE 0 END) AS convertidos,
               COUNT(l) AS total
        FROM Lead l
        WHERE (CAST(:inicio AS timestamp) IS NULL OR l.criadoEm >= :inicio)
          AND (CAST(:fim    AS timestamp) IS NULL OR l.criadoEm <= :fim)
        GROUP BY l.jornada
        ORDER BY COUNT(l) DESC
        """)
    List<Object[]> conversaoPorJornada(@Param("inicio") LocalDateTime inicio,
                                       @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT l.vendedor.id, l.vendedor.nome,
               SUM(CASE WHEN l.status = com.syntra.model.enums.StatusLead.CONVERTIDO THEN 1 ELSE 0 END) AS convertidos,
               COUNT(l) AS total
        FROM Lead l
        WHERE l.vendedor IS NOT NULL
          AND (CAST(:inicio AS timestamp) IS NULL OR l.criadoEm >= :inicio)
          AND (CAST(:fim    AS timestamp) IS NULL OR l.criadoEm <= :fim)
        GROUP BY l.vendedor.id, l.vendedor.nome
        ORDER BY SUM(CASE WHEN l.status = com.syntra.model.enums.StatusLead.CONVERTIDO THEN 1 ELSE 0 END) DESC,
                 COUNT(l) DESC
        """)
    List<Object[]> topVendedores(@Param("inicio") LocalDateTime inicio,
                                 @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.vendedor IS NULL
          AND l.status NOT IN (com.syntra.model.enums.StatusLead.CONVERTIDO, com.syntra.model.enums.StatusLead.PERDIDO)
        """)
    long countSemVendedorAtivos();

    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.proximoContatoEm IS NOT NULL
          AND l.proximoContatoEm < :agora
          AND l.status NOT IN (com.syntra.model.enums.StatusLead.CONVERTIDO, com.syntra.model.enums.StatusLead.PERDIDO)
        """)
    long countFollowUpAtrasado(@Param("agora") LocalDateTime agora);

    @Query("""
        SELECT COUNT(l) FROM Lead l
        WHERE l.status NOT IN (com.syntra.model.enums.StatusLead.CONVERTIDO, com.syntra.model.enums.StatusLead.PERDIDO)
          AND ((l.proximoContatoEm IS NOT NULL AND l.proximoContatoEm < :agora)
               OR (l.ultimaInteracaoEm IS NOT NULL AND l.ultimaInteracaoEm < :corte))
        """)
    long countParados(@Param("agora") LocalDateTime agora,
                      @Param("corte") LocalDateTime corte);

    @Query("""
        SELECT CAST(l.criadoEm AS date) AS dia, COUNT(l)
        FROM Lead l
        WHERE l.criadoEm >= :desde
        GROUP BY CAST(l.criadoEm AS date)
        ORDER BY CAST(l.criadoEm AS date) ASC
        """)
    List<Object[]> volumeDiario(@Param("desde") LocalDateTime desde);

    @Query("""
        SELECT l.criadoEm, h.criadoEm
        FROM HistoricoLead h
        JOIN h.lead l
        WHERE h.campo = 'status'
          AND h.valorDepois = 'Convertido'
          AND (CAST(:inicio AS timestamp) IS NULL OR h.criadoEm >= :inicio)
          AND (CAST(:fim    AS timestamp) IS NULL OR h.criadoEm <= :fim)
        """)
    List<Object[]> conversoesParaTempoMedio(@Param("inicio") LocalDateTime inicio,
                                            @Param("fim") LocalDateTime fim);
}
