package com.rtb.k8gen.model;

import lombok.Data;

import java.util.List;

@Data
public class Migrations {
    private String tool;
    private String image;
    private List<String> args;
}