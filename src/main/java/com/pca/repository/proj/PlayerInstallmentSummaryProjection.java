package com.pca.repository.proj;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PlayerInstallmentSummaryProjection {
    Long getPlayerId();
    String getPlayerName();
    String getPhone();
    String getGroupName();
    LocalDate getJoinDate();

    BigDecimal getInstallmentAmount();
    BigDecimal getTotalPaid();

    LocalDate getDueDate();
    Long getInstallmentId();
}
