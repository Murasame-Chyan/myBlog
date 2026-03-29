package com.murasame.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class TagWrapper {
    private List<Integer> tagList;
}