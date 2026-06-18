package org.elis.ericsson.datathon.user_management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopUserDto {
    private String email;
    private long actionCount;
}
