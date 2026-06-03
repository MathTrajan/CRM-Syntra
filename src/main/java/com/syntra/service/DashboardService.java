package com.syntra.service;

import com.syntra.dto.DashboardFiltroDTO;
import com.syntra.model.enums.JornadaLead;
import com.syntra.model.enums.StatusLead;
import com.syntra.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final LeadRepository leadRepo;

    public DashboardService(LeadRepository leadRepo) {
        this.leadRepo = leadRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMetricas(DashboardFiltroDTO filtro) {
        LocalDate hoje = LocalDate.now();
        LocalDate dataInicio = filtro.getDataInicio();
        LocalDate dataFim    = filtro.getDataFim();

        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim    = dataFim    != null ? dataFim.atTime(23, 59, 59) : null;

        long total          = leadRepo.countNoPeriodo(inicio, fim);
        long novos          = leadRepo.countByStatusPeriodo(StatusLead.NOVO, inicio, fim);
        long emAtendimento  = leadRepo.countByStatusPeriodo(StatusLead.EM_ATENDIMENTO, inicio, fim);
        long emOutroAtendimento = leadRepo.countByStatusPeriodo(StatusLead.EM_OUTRO_ATENDIMENTO, inicio, fim);
        long aguardando     = leadRepo.countByStatusPeriodo(StatusLead.AGUARDANDO_RETORNO, inicio, fim);
        long convertidos    = leadRepo.countByStatusPeriodo(StatusLead.CONVERTIDO, inicio, fim);
        long perdidos       = leadRepo.countByStatusPeriodo(StatusLead.PERDIDO, inicio, fim);
        long cadastrados    = leadRepo.countByStatusPeriodo(StatusLead.CADASTRADO_NO_SITE, inicio, fim);
        long semLer         = leadRepo.countByLidoFalse();
        long recebidosHoje  = leadRepo.countRecentes(hoje.atStartOfDay());
        long semVendedor    = leadRepo.countSemVendedorAtivos();
        long agora          = 0;
        LocalDateTime nowLDT = LocalDateTime.now();
        long followUpAtrasado = leadRepo.countFollowUpAtrasado(nowLDT);
        long parados          = leadRepo.countParados(nowLDT, nowLDT.minusDays(3));

        double taxaConversao = total > 0 ? (double) convertidos / total * 100 : 0;

        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("total", total);
        metricas.put("novos", novos);
        metricas.put("emAtendimento", emAtendimento);
        metricas.put("emOutroAtendimento", emOutroAtendimento);
        metricas.put("aguardando", aguardando);
        metricas.put("convertidos", convertidos);
        metricas.put("perdidos", perdidos);
        metricas.put("cadastrados", cadastrados);
        metricas.put("semLer", semLer);
        metricas.put("hoje", recebidosHoje);
        metricas.put("semVendedor", semVendedor);
        metricas.put("followUpAtrasado", followUpAtrasado);
        metricas.put("parados", parados);
        metricas.put("taxaConversao", String.format("%.1f", taxaConversao));

        // Funil de etapas (taxa de conversão entre estágios)
        List<Map<String, Object>> funil = new ArrayList<>();
        long totalFunil = novos + emAtendimento + emOutroAtendimento + aguardando + convertidos;
        funil.add(etapaFunil("Novo", novos, totalFunil));
        funil.add(etapaFunil("Em Atendimento", emAtendimento, totalFunil));
        funil.add(etapaFunil("Em Outro Atendimento", emOutroAtendimento, totalFunil));
        funil.add(etapaFunil("Aguardando", aguardando, totalFunil));
        funil.add(etapaFunil("Convertido", convertidos, totalFunil));
        metricas.put("funil", funil);

        // Comparativo período atual vs período anterior (mesma duração)
        Map<String, Object> comparativo = calcularComparativo(inicio, fim, total, convertidos);
        metricas.put("comparativo", comparativo);

        // Conversão por jornada
        List<Map<String, Object>> porJornada = new ArrayList<>();
        for (Object[] row : leadRepo.conversaoPorJornada(inicio, fim)) {
            JornadaLead j      = (JornadaLead) row[0];
            long conv          = ((Number) row[1]).longValue();
            long tot           = ((Number) row[2]).longValue();
            double taxa        = tot > 0 ? (double) conv / tot * 100 : 0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("jornada", j != null ? j.getLabel() : "Sem jornada");
            item.put("jornadaKey", j != null ? j.name().toLowerCase() : "indefinida");
            item.put("convertidos", conv);
            item.put("total", tot);
            item.put("taxa", String.format("%.1f", taxa));
            porJornada.add(item);
        }
        metricas.put("porJornada", porJornada);

        // Top 5 vendedores por convertidos no período
        List<Map<String, Object>> topVendedores = new ArrayList<>();
        int limite = 5;
        for (Object[] row : leadRepo.topVendedores(inicio, fim)) {
            if (topVendedores.size() >= limite) break;
            long conv = ((Number) row[2]).longValue();
            long tot  = ((Number) row[3]).longValue();
            double taxa = tot > 0 ? (double) conv / tot * 100 : 0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("vendedorId", row[0]);
            item.put("nome", row[1]);
            item.put("convertidos", conv);
            item.put("total", tot);
            item.put("taxa", String.format("%.1f", taxa));
            topVendedores.add(item);
        }
        metricas.put("topVendedores", topVendedores);

        // Volume diário dos últimos 30 dias (independente do filtro)
        List<Map<String, Object>> volumeDiario = montarVolumeDiario(30);
        long volumeTotal = volumeDiario.stream().mapToLong(m -> (long) m.get("total")).sum();
        long volumeMaximo = volumeDiario.stream().mapToLong(m -> (long) m.get("total")).max().orElse(0);
        double volumeMedia = volumeDiario.isEmpty() ? 0 : (double) volumeTotal / volumeDiario.size();
        metricas.put("volumeDiario", volumeDiario);
        metricas.put("volumeMaximo", volumeMaximo);
        metricas.put("volumeTotal", volumeTotal);
        metricas.put("volumeMedia", String.format("%.1f", volumeMedia));

        // Tempo médio até conversão (período)
        metricas.put("tempoMedioConversao", calcularTempoMedio(inicio, fim));

        // Análise por origem (período)
        List<Map<String, Object>> porOrigem = new ArrayList<>();
        for (Object[] linha : leadRepo.contarPorOrigem(inicio, fim)) {
            String origem = (String) linha[0];
            long qtd      = ((Number) linha[1]).longValue();
            int pct       = total > 0 ? (int) Math.round(qtd * 100.0 / total) : 0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("origem", origem);
            item.put("total", qtd);
            item.put("percentual", pct);
            porOrigem.add(item);
        }
        metricas.put("porOrigem", porOrigem);

        return metricas;
    }

    private Map<String, Object> etapaFunil(String label, long valor, long total) {
        int pct = total > 0 ? (int) Math.round(valor * 100.0 / total) : 0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("total", valor);
        m.put("percentual", pct);
        return m;
    }

    private Map<String, Object> calcularComparativo(LocalDateTime inicio, LocalDateTime fim,
                                                    long totalAtual, long convertidosAtual) {
        Map<String, Object> c = new LinkedHashMap<>();
        if (inicio == null || fim == null) {
            c.put("disponivel", false);
            return c;
        }
        long dias = ChronoUnit.DAYS.between(inicio.toLocalDate(), fim.toLocalDate()) + 1;
        LocalDateTime inicioAnterior = inicio.minusDays(dias);
        LocalDateTime fimAnterior    = inicio.minusSeconds(1);

        long totalAnt        = leadRepo.countNoPeriodo(inicioAnterior, fimAnterior);
        long convertidosAnt  = leadRepo.countByStatusPeriodo(StatusLead.CONVERTIDO, inicioAnterior, fimAnterior);

        c.put("disponivel", true);
        c.put("totalAnterior", totalAnt);
        c.put("convertidosAnterior", convertidosAnt);
        c.put("variacaoTotal", variacao(totalAtual, totalAnt));
        c.put("variacaoConvertidos", variacao(convertidosAtual, convertidosAnt));
        return c;
    }

    private String variacao(long atual, long anterior) {
        if (anterior == 0) {
            return atual > 0 ? "+∞" : "0%";
        }
        double pct = ((double) atual - anterior) / anterior * 100;
        return String.format("%+.1f%%", pct);
    }

    private List<Map<String, Object>> montarVolumeDiario(int dias) {
        LocalDate hoje = LocalDate.now();
        LocalDate desde = hoje.minusDays(dias - 1L);
        Map<LocalDate, Long> agrupado = new LinkedHashMap<>();
        for (Object[] row : leadRepo.volumeDiario(desde.atStartOfDay())) {
            LocalDate dia;
            Object raw = row[0];
            if (raw instanceof java.sql.Date d) {
                dia = d.toLocalDate();
            } else if (raw instanceof LocalDate d) {
                dia = d;
            } else if (raw instanceof java.time.LocalDateTime dt) {
                dia = dt.toLocalDate();
            } else {
                dia = LocalDate.parse(raw.toString().substring(0, 10));
            }
            agrupado.put(dia, ((Number) row[1]).longValue());
        }

        List<Map<String, Object>> serie = new ArrayList<>();
        String[] diasSemana = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
        for (int i = 0; i < dias; i++) {
            LocalDate dia = desde.plusDays(i);
            long valor = agrupado.getOrDefault(dia, 0L);
            int diaSemanaIdx = dia.getDayOfWeek().getValue() % 7; // ISO: Mon=1..Sun=7 -> Sun=0..Sat=6
            boolean fimDeSemana = diaSemanaIdx == 0 || diaSemanaIdx == 6;
            Map<String, Object> ponto = new LinkedHashMap<>();
            ponto.put("dia", dia.toString());
            ponto.put("rotulo", String.format("%02d/%02d", dia.getDayOfMonth(), dia.getMonthValue()));
            ponto.put("rotuloLongo", String.format("%s %02d/%02d", diasSemana[diaSemanaIdx],
                    dia.getDayOfMonth(), dia.getMonthValue()));
            ponto.put("diaMes", dia.getDayOfMonth());
            ponto.put("total", valor);
            ponto.put("hoje", dia.equals(hoje));
            ponto.put("fimDeSemana", fimDeSemana);
            ponto.put("primeiroDoMes", dia.getDayOfMonth() == 1);
            serie.add(ponto);
        }
        return serie;
    }

    private String calcularTempoMedio(LocalDateTime inicio, LocalDateTime fim) {
        List<Object[]> pares = leadRepo.conversoesParaTempoMedio(inicio, fim);
        if (pares.isEmpty()) return "—";
        long somaSegundos = 0;
        int contador = 0;
        for (Object[] par : pares) {
            LocalDateTime criado = (LocalDateTime) par[0];
            LocalDateTime convertidoEm = (LocalDateTime) par[1];
            if (criado == null || convertidoEm == null) continue;
            somaSegundos += Duration.between(criado, convertidoEm).getSeconds();
            contador++;
        }
        if (contador == 0) return "—";
        long medio = somaSegundos / contador;
        long horasTotais = medio / 3600;
        long dias = horasTotais / 24;
        long horas = horasTotais % 24;
        if (dias >= 1) {
            return dias + "d " + horas + "h";
        }
        long minutos = (medio % 3600) / 60;
        if (horas >= 1) {
            return horas + "h " + minutos + "min";
        }
        return minutos + " min";
    }
}
