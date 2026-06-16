package com.humanizar.acolhimento.application.http.mapper.central;

import java.util.List;

import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.application.http.dto.central.PendingTargetStatusDTO;
import com.humanizar.acolhimento.domain.model.peding.PendingTargetStatus;

@Component
public class PendingTargetStatusDtoMapper {

    public List<PendingTargetStatusDTO> toDTOList(List<PendingTargetStatus> targets) {
        return targets.stream()
                .map(target -> new PendingTargetStatusDTO(
                        target.getTargetService(),
                        target.getStatus().name()))
                .toList();
    }
}
