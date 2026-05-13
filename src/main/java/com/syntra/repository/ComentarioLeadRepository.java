package com.syntra.repository;

import com.syntra.model.ComentarioLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioLeadRepository extends JpaRepository<ComentarioLead, String> {

    List<ComentarioLead> findByLeadIdOrderByCriadoEmDesc(String leadId);
}
