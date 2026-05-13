package com.syntra.repository;

import com.syntra.model.HistoricoLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoLeadRepository extends JpaRepository<HistoricoLead, String> {

    List<HistoricoLead> findByLeadIdOrderByCriadoEmDesc(String leadId);
}
