package com.zzzkvidi4.storage.service;

import com.zzzkvidi4.storage.repository.DataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

import static com.zzzkvidi4.storage.generated.tables.Invoice.INVOICE;
import static com.zzzkvidi4.storage.generated.tables.InvoiceItem.INVOICE_ITEM;
import static java.util.stream.Collectors.*;
import static org.jooq.impl.DSL.*;

/**
 * Service to calculate reports.
 */
@RequiredArgsConstructor
public final class InvoiceService {
    @NotNull
    private final DataSource dataSource;

    @NotNull
    public DailyInvoiceSummary getDailyInvoiceSummary(@NotNull LocalDate startInclusive, @NotNull LocalDate endExclusive) {
        try (DSLContext create = DSL.using(dataSource.getUrl(), dataSource.getName(), dataSource.getPassword())) {
            Result<Record3<Date, BigDecimal, BigDecimal>> dailyInvoiceSummaryResults = create.select(date(INVOICE.DATE), round(sum(INVOICE_ITEM.PRICE.mul(INVOICE_ITEM.VOLUME)), 0), sum(INVOICE_ITEM.VOLUME))
                    .from(INVOICE)
                    .innerJoin(INVOICE_ITEM).on(INVOICE.INVOICE_ID.eq(INVOICE_ITEM.INVOICE_ID))
                    .where(
                            INVOICE.DATE.ge(Timestamp.valueOf(startInclusive.atStartOfDay()))
                                    .and(INVOICE.DATE.lt(Timestamp.valueOf(endExclusive.atStartOfDay()))))
                    .groupBy(date(INVOICE.DATE))
                    .fetch();
            return new DailyInvoiceSummary(dailyInvoiceSummaryResults.stream().collect(toMap(r -> r.component1().toLocalDate(), r -> Optional.of(new InvoiceSummary(r.component2().longValue(), r.component3().doubleValue())))));
        }
    }

    @Nullable
    public Double getAveragePrice(@NotNull LocalDate startInclusive, @NotNull LocalDate endExclusive) {
        try (DSLContext create = DSL.using(dataSource.getUrl(), dataSource.getName(), dataSource.getPassword())) {
            Result<Record1<BigDecimal>> averagePriceList = create.select(avg(INVOICE_ITEM.PRICE))
                    .from(INVOICE)
                    .innerJoin(INVOICE_ITEM).on(INVOICE.INVOICE_ID.eq(INVOICE_ITEM.INVOICE_ID))
                    .where(INVOICE.DATE.ge(Timestamp.valueOf(startInclusive.atStartOfDay())).and(INVOICE.DATE.lt(Timestamp.valueOf(endExclusive.atStartOfDay()))))
                    .fetch();
            BigDecimal averagePrice = averagePriceList.get(0).component1();
            return averagePrice == null ? null : averagePrice.doubleValue();
        }
    }

    @Getter
    @AllArgsConstructor
    public static final class InvoiceSummary {
        private long price;
        private double volume;

        @NotNull
        public InvoiceSummary sum(@NotNull InvoiceSummary invoiceSummary) {
            return new InvoiceSummary(this.price + invoiceSummary.price, this.volume + invoiceSummary.volume);
        }
    }

    @Getter
    public static final class DailyInvoiceSummary {
        @NotNull
        private final Map<LocalDate, Optional<InvoiceSummary>> summary;
        @NotNull
        private final InvoiceSummary invoiceSummary;

        public DailyInvoiceSummary(@NotNull Map<LocalDate, Optional<InvoiceSummary>> summary) {
            this.summary = summary;
            invoiceSummary = summary.values()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .reduce(InvoiceSummary::sum)
                    .orElseGet(() -> new InvoiceSummary(0, 0));
        }
    }
}
