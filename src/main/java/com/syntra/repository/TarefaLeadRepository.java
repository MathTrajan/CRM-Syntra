package com.syntra.repository;

import com.syntra.model.TarefaLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TarefaLeadRepository extends JpaRepository<TarefaLead, String> {

    List<TarefaLead> findByLeadIdOrderByStatusAscVencimentoEmAscCriadoEmDesc(String leadId);

    Optional<TarefaLead> findByIdAndLeadId(String id, String leadId);
}
