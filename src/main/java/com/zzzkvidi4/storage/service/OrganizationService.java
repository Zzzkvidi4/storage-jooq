package com.zzzkvidi4.storage.service;

import com.zzzkvidi4.storage.generated.tables.pojos.Organization;
import com.zzzkvidi4.storage.model.ItemDto;
import com.zzzkvidi4.storage.model.OrganizationDto;
import com.zzzkvidi4.storage.model.OrganizationWithItem;
import com.zzzkvidi4.storage.repository.DataSource;
import com.zzzkvidi4.storage.repository.Pair;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static com.zzzkvidi4.storage.generated.tables.Invoice.INVOICE;
import static com.zzzkvidi4.storage.generated.tables.InvoiceItem.INVOICE_ITEM;
import static com.zzzkvidi4.storage.generated.tables.Item.ITEM;
import static com.zzzkvidi4.storage.generated.tables.Organization.ORGANIZATION;
import static java.util.stream.Collectors.*;
import static org.jooq.impl.DSL.*;

/**
 * Service to calculate reports.
 */
@RequiredArgsConstructor
public final class OrganizationService {
    @NotNull
    private final DataSource dataSource;

    @NotNull
    public List<Organization> findTenTheMostActiveOrganizations() {
        try (DSLContext create = DSL.using(dataSource.getUrl(), dataSource.getName(), dataSource.getPassword())) {
            return create.with("inc_count").as(
                    select(
                            ORGANIZATION.ORGANIZATION_ID,
                            ORGANIZATION.ACCOUNT,
                            ORGANIZATION.ITN,
                            ORGANIZATION.NAME,
                            sum(INVOICE_ITEM.VOLUME).as("volume")
                    )
                    .from(ORGANIZATION)
                    .innerJoin(INVOICE).on(INVOICE.ORGANIZATION_ID.eq(ORGANIZATION.ORGANIZATION_ID))
                    .innerJoin(INVOICE_ITEM).on(INVOICE_ITEM.INVOICE_ID.eq(INVOICE.INVOICE_ID))
                    .groupBy(
                            ORGANIZATION.ORGANIZATION_ID,
                            ORGANIZATION.ACCOUNT,
                            ORGANIZATION.ITN,
                            ORGANIZATION.NAME
                    )
            )
                    .select()
                    .from(table(name("inc_count")))
                    .orderBy(field(name("volume")).desc())
                    .limit(10)
                    .fetch()
                    .stream()
                    .map(r ->
                            new com.zzzkvidi4.storage.generated.tables.pojos.Organization(
                                    r.get(ORGANIZATION.ORGANIZATION_ID),
                                    r.get(ORGANIZATION.NAME),
                                    r.get(ORGANIZATION.ACCOUNT),
                                    r.get(ORGANIZATION.ITN)
                            )
                    )
                    .collect(toList());
        }
    }

    @NotNull
    public List<Organization> findOrganizationsWithItemsGreaterThan(@NotNull List<Pair<String, Double>> itemsWithVolume) {
        try (DSLContext create = DSL.using(dataSource.getUrl(), dataSource.getName(), dataSource.getPassword())) {
            SelectJoinStep<Record> from = create.select(ORGANIZATION.fields())
                    .from(ORGANIZATION);
            Table<Record6<String, String, String, String, String, BigDecimal>> cte = select(ORGANIZATION.ORGANIZATION_ID, ORGANIZATION.NAME, ORGANIZATION.ITN, ORGANIZATION.ACCOUNT, INVOICE_ITEM.ITEM_ID, sum(INVOICE_ITEM.VOLUME).as("volume_sum"))
                    .from(ORGANIZATION)
                    .innerJoin(INVOICE).on(ORGANIZATION.ORGANIZATION_ID.eq(INVOICE.ORGANIZATION_ID))
                    .innerJoin(INVOICE_ITEM).on(INVOICE_ITEM.INVOICE_ID.eq(INVOICE.INVOICE_ID))
                    .groupBy(ORGANIZATION.ORGANIZATION_ID, ORGANIZATION.NAME, ORGANIZATION.ITN, ORGANIZATION.ACCOUNT, INVOICE_ITEM.ITEM_ID)
                    .asTable("organizations_with_items_count");
            int index = 1;
            for (Pair<String, Double> item : itemsWithVolume) {
                Table<Record1<String>> subQuery = select(cte.field(ORGANIZATION.ORGANIZATION_ID))
                        .from(cte)
                        .where(
                                cte.field(INVOICE_ITEM.ITEM_ID).eq(item.getValue1())
                                        .and(cte.field("volume_sum").cast(Double.class).ge(item.getValue2())))
                        .asTable("organizations_with_item" + index++);
                from.innerJoin(subQuery).on(subQuery.field(ORGANIZATION.ORGANIZATION_ID).eq(ORGANIZATION.ORGANIZATION_ID));
            }
            return from.fetch()
                    .stream()
                    .map(r -> new Organization(r.get(ORGANIZATION.ORGANIZATION_ID), r.get(ORGANIZATION.NAME), r.get(ORGANIZATION.ITN), r.get(ORGANIZATION.ACCOUNT)))
                    .collect(toList());
        }
    }

    @NotNull
    public Map<OrganizationDto, Set<ItemDto>> getOrganizationsWithItems(@NotNull LocalDate startInclusive, @NotNull LocalDate endExclusive) {
        try (DSLContext create = DSL.using(dataSource.getUrl(), dataSource.getName(), dataSource.getPassword())) {
            Table<Record> subQuery = select()
                    .from(INVOICE)
                    .where(
                            INVOICE.DATE.ge(from(startInclusive))
                                    .and(INVOICE.DATE.lt(from(endExclusive)))
                    )
                    .asTable("inv");
            Map<OrganizationDto, Set<ItemDto>> organizationsWithItems = create.select(
                    ORGANIZATION.ORGANIZATION_ID,
                    ORGANIZATION.ACCOUNT,
                    ORGANIZATION.NAME,
                    ORGANIZATION.ITN,
                    ITEM.ITEM_ID,
                    ITEM.NAME,
                    ITEM.CODE
            )
                    .from(ORGANIZATION)
                    .leftJoin(subQuery).on(ORGANIZATION.ORGANIZATION_ID.eq(subQuery.field(INVOICE.ORGANIZATION_ID)))
                    .leftJoin(INVOICE_ITEM).on(subQuery.field(INVOICE.INVOICE_ID).eq(INVOICE_ITEM.INVOICE_ID))
                    .leftJoin(ITEM).on(ITEM.ITEM_ID.eq(INVOICE_ITEM.ITEM_ID))
                    .fetch()
                    .stream()
                    .map(r -> new OrganizationWithItem(r.value1(), r.value3(), r.value4(), r.value2(), r.value5(), r.value6(), r.value7()))
                    .collect(groupingBy(OrganizationWithItem::getOrganization, mapping(OrganizationWithItem::getItem, toSet())));
            for (Set<ItemDto> items : organizationsWithItems.values()) {
                items.remove(new ItemDto());
            }
            return organizationsWithItems;
        }
    }

    @NotNull
    private Timestamp from(@NotNull LocalDate date) {
        return new Timestamp(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
