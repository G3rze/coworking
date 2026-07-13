package com.gerson.coworking.controller;

import com.gerson.coworking.domain.dto.report.OccupancyReportResponse;
import com.gerson.coworking.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Get space occupancy report", description = "Calculates occupancy percentage for each space in a date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully",
                    content = @Content(schema = @Schema(implementation = OccupancyReportResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content(schema = @Schema(implementation = com.gerson.coworking.exception.ErrorResponse.class)))
    })
    @GetMapping("/occupancy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OccupancyReportResponse> getOccupancyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getOccupancyReport(dateFrom, dateTo));
    }
}
