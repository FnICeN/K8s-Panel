package com.ficn.panel.model.dto.cluster;

import com.ficn.panel.model.entity.vo.PodVO;
import lombok.Data;

import java.util.List;

@Data
public class PodListResponse {
    private String kind;
    private String apiVersion;
    private List<PodVO> items;
}
