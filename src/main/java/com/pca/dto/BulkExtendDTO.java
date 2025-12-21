package com.pca.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BulkExtendDTO {
    private Integer groupId;        // Jar Specific Group hava asel tar ID, nahitar NULL (Saglyansathi)
    private LocalDate holidayStart; // Sutti chalu (e.g., 2025-12-25)
    private LocalDate holidayEnd;   // Sutti sampli (e.g., 2025-12-28)
}