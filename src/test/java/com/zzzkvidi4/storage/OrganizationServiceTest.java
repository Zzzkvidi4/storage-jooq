package com.zzzkvidi4.storage;

import com.opentable.db.postgres.embedded.ConnectionInfo;
import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.PreparedDbRule;
import com.zzzkvidi4.storage.generated.tables.daos.OrganizationDao;
import com.zzzkvidi4.storage.generated.tables.pojos.Organization;
import com.zzzkvidi4.storage.model.ItemDto;
import com.zzzkvidi4.storage.model.OrganizationDto;
import com.zzzkvidi4.storage.repository.DataSource;
import com.zzzkvidi4.storage.repository.Pair;
import com.zzzkvidi4.storage.service.OrganizationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

public final class OrganizationServiceTest {
    @Rule
    @NotNull
    public final PreparedDbRule db = EmbeddedPostgresRules.preparedDatabase(FlywayPreparer.forClasspathLocation("db/migration"));
    @Nullable
    private OrganizationService organizationService;
    @Nullable
    private OrganizationDao organizationDao;

    @Before
    public void setUp() {
        ConnectionInfo connectionInfo = db.getConnectionInfo();
        DataSource dataSource = new DataSource("jdbc:postgresql://localhost:" + connectionInfo.getPort() + "/" + connectionInfo.getDbName(), "postgres", "postgres");
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl("jdbc:postgresql://localhost:" + connectionInfo.getPort() + "/" + connectionInfo.getDbName());
        pgDataSource.setUser("postgres");
        pgDataSource.setPassword("postgres");
        organizationService = new OrganizationService(dataSource);
        organizationDao = new OrganizationDao(
                new DefaultConfiguration()
                        .set(pgDataSource)
                        .set(SQLDialect.POSTGRES_9_4)
        );
    }

    @Test
    public void whenGetOrganizationsWithItemsForPeriodResultIsCorrect() {
        Map<OrganizationDto, Set<ItemDto>> organizationsWithItems = organizationService.getOrganizationsWithItems(
                LocalDate.of(2018, 12, 1),
                LocalDate.of(2019, 7, 20)
        );
        Set<OrganizationDto> organizations = organizationDao.findAll()
                .stream()
                .map(OrganizationDto::toDto)
                .collect(toSet());
        assertTrue(organizationsWithItems.keySet().containsAll(organizations));
        for (Map.Entry<OrganizationDto, Set<ItemDto>> organizationWithItems : organizationsWithItems.entrySet()) {
            OrganizationDto organization = organizationWithItems.getKey();
            if ("1".equals(organization.getOrganizationId())) {
                assertEquals(2, organizationWithItems.getValue().size());
                assertEquals(new HashSet<>(asList("1", "2")), organizationWithItems.getValue().stream().map(ItemDto::getId).collect(toSet()));
            } else {
                assertEquals(0, organizationWithItems.getValue().size());
            }
        }
    }

    @Test
    public void whenGetTenMostActiveOrganizationsListIsCorrect() {
        List<Organization> organizations = organizationService.findTenTheMostActiveOrganizations();
        List<String> organizationIds = organizations.stream().map(Organization::getOrganizationId).collect(Collectors.toList());
        List<String> expectedOrder = asList("3", "6", "5", "2", "4", "1", "9", "8", "7");
        assertEquals(expectedOrder.size(), organizationIds.size());
        for (int i = 0; i < organizationIds.size(); i++) {
            String id = organizationIds.get(i);
            String expectedId = expectedOrder.get(i);
            assertEquals(expectedId, id);
        }
    }

    @Test
    public void whenGetOrganizationsWithMoreItemsItIsCorrect() {
        List<Organization> organizationsWithItemsGreaterThan = organizationService.findOrganizationsWithItemsGreaterThan(singletonList(new Pair<>("3", 15.0)));
        assertEquals(2, organizationsWithItemsGreaterThan.size());
        assertTrue(organizationsWithItemsGreaterThan.stream().map(Organization::getOrganizationId).collect(toSet()).containsAll(asList("2", "3")));

        organizationsWithItemsGreaterThan = organizationService.findOrganizationsWithItemsGreaterThan(asList(new Pair<>("3", 15.0), new Pair<>("2", 1.0)));
        assertEquals(1, organizationsWithItemsGreaterThan.size());
        assertEquals("2", organizationsWithItemsGreaterThan.get(0).getOrganizationId());
    }
}
