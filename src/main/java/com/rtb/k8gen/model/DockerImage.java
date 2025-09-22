package com.rtb.k8gen.model;

import lombok.Data;

import java.util.List;

@Data
public class DockerImage {
    private String name;
    private String image;
    private String role;
    private List<String> ports;
}