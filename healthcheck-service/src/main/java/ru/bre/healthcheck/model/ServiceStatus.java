package ru.bre.healthcheck.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceStatus {
    private String name;
    private String status;
    private String target;
}
