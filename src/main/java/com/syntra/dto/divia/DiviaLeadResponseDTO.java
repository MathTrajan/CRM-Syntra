package com.syntra.dto.divia;

import java.util.List;
import java.util.Map;

public class DiviaLeadResponseDTO {

    private List<DiviaLeadDTO> data;
    private DiviaMetaDTO meta;
    private Map<String, Object> filters;

    public List<DiviaLeadDTO> getData() { return data; }
    public void setData(List<DiviaLeadDTO> data) { this.data = data; }
    public DiviaMetaDTO getMeta() { return meta; }
    public void setMeta(DiviaMetaDTO meta) { this.meta = meta; }
    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }
}
