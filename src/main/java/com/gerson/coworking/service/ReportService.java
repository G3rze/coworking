package com.gerson.coworking.service;

import com.gerson.coworking.domain.dto.report.OccupancyReportResponse;

import java.time.LocalDate;

public interface ReportService {
    OccupancyReportResponse getOccupancyReport(LocalDate dateFrom, LocalDate dateTo);
}
