package com.syntra.dto.divia;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiviaMetaDTO {

    @JsonProperty("current_page")
    private int currentPage;

    @JsonProperty("per_page")
    private int perPage;

    private int total;

    @JsonProperty("last_page")
    private int lastPage;

    private Integer from;
    private Integer to;

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getPerPage() { return perPage; }
    public void setPerPage(int perPage) { this.perPage = perPage; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getLastPage() { return lastPage; }
    public void setLastPage(int lastPage) { this.lastPage = lastPage; }
    public Integer getFrom() { return from; }
    public void setFrom(Integer from) { this.from = from; }
    public Integer getTo() { return to; }
    public void setTo(Integer to) { this.to = to; }
}
