package com.pgh.api_practice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DirectChatMessagePageDTO {

    private List<DirectChatMessageDTO> content;
    private long totalElements;
    private int totalPages;
}