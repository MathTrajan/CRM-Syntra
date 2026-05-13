package com.syntra.service;

import com.syntra.model.enums.StatusLead;
import com.syntra.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final LeadRepository leadRepo;

    public DashboardService(LeadRepository leadRepo) {
        this.leadRepo = leadRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMetricas() {
        Map<String, Object> metricas = new LinkedHashMap<>();

        long total        = leadRepo.count();
        long novos        = leadRepo.countByStatus(StatusLead.NOVO);
        long emAtendimento= leadRepo.countByStatus(StatusLead.EM_ATENDIMENTO);
        long aguardando   = leadRepo.countByStatus(StatusLead.AGUARDANDO_RETORNO);
        long convertidos  = leadRepo.countByStatus(StatusLead.CONVERTIDO);
        long perdidos     = leadRepo.countByStatus(StatusLead.PERDIDO);
        long semLer       = leadRepo.countByLidoFalse();
        long hoje         = leadRepo.countRecentes(LocalDateTime.now().toLocalDate().atStartOfDay());

        double taxaConversao = total > 0 ? (double) convertidos / total * 100 : 0;

        metricas.put("total", total);
        metricas.put("novos", novos);
        metricas.put("emAtendimento", emAtendimento);
        metricas.put("aguardando", aguardando);
        metricas.put("convertidos", convertidos);
        metricas.put("perdidos", perdidos);
        metricas.put("semLer", semLer);
        metricas.put("hoje", hoje);
        metricas.put("taxaConversao", String.format("%.1f", taxaConversao));

        return metricas;
    }
}
