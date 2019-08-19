package com.zzzkvidi4.storage;

import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.PreparedDbRule;
import com.zzzkvidi4.storage.generated.tables.daos.OrganizationDao;
import com.zzzkvidi4.storage.generated.tables.pojos.Organization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public final class OrganizationDaoTests {
    @Rule
    @NotNull
    public PreparedDbRule preparedDbRule = EmbeddedPostgresRules.preparedDatabase(FlywayPreparer.forClasspathLocation("db/migration"));
    @Nullable
    private OrganizationDao organizationDao;

    @Before
    public void setUp() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:" + preparedDbRule.getConnectionInfo().getPort() + "/" + preparedDbRule.getConnectionInfo().getDbName());
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");
        organizationDao = new OrganizationDao(
                new DefaultConfiguration()
                        .set(dataSource)
                        .set(SQLDialect.POSTGRES_9_4)
        );
    }

    @Test
    public void whenFindByIdExistingEntityItReturns() {
        Organization organization = organizationDao.findById("1");
        assertNotNull(organization);
        assertEqualsOrganization(organization, "ibm", "111", "111111111", "1");
    }

    @Test
    public void whenFindNotExistingEntityEmptyOptionalReturned() {
        Organization organizationOpt = organizationDao.findById("700");
        assertNull(organizationOpt);
    }

    @Test
    public void whenUpdatedExistingEntityItUpdated() {
        Organization organization = organizationDao.findById("2");
        assertNotNull(organization);
        String newName = "ibm 2";
        organization.setName(newName);
        String newAccount = "251";
        organization.setAccount(newAccount);
        String newItn = "1111111111";
        organization.setItn(newItn);
        organizationDao.update(organization);
        organization = organizationDao.findById("2");
        assertNotNull(organization);
        assertEqualsOrganization(organization, newName, newAccount, newItn, "2");
    }

    @Test
    public void whenUpdatedNonexistentEntityNothingHappens() {
        Organization organization = new Organization("id", "name", "itn", "account");
        organizationDao.update(organization);
    }

    @Test
    public void whenInsertCorrectEntityItCreated() {
        String id = "id";
        String name = "name";
        String itn = "itn";
        String account = "account";
        Organization organization = new Organization(id, name, itn, account);

        organizationDao.insert(organization);
        Organization createdOrganization = organizationDao.findById(id);
        assertNotNull(createdOrganization);
        assertEqualsOrganization(createdOrganization, name, account, itn, id);
    }

    @Test
    public void testDeleteNotConnectedEntity() {
        Organization organization = organizationDao.findById("13");
        assertNotNull(organization);
        List<Organization> organizations = organizationDao.findAll();
        organizationDao.deleteById("13");

        Organization deletedOrganization = organizationDao.findById("13");
        assertNull(deletedOrganization);
        List<Organization> organizationAfterDelete = organizationDao.findAll();
        assertEquals(organizations.size() - 1, organizationAfterDelete.size());
        assertTrue(organizations.stream()
                .map(Organization::getOrganizationId)
                .collect(toList())
                .containsAll(
                        organizationAfterDelete.stream()
                                .map(Organization::getOrganizationId)
                                .collect(toList())
                )
        );
        assertFalse(organizationAfterDelete.stream().anyMatch(o -> "13".equals(o.getOrganizationId())));
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteConnectedEntity() {
        organizationDao.deleteById("1");
    }

    private void assertEqualsOrganization(@NotNull Organization organization, @NotNull String name, @NotNull String account, @NotNull String itn, @NotNull String id) {
        assertEquals(name, organization.getName());
        assertEquals(account, organization.getAccount());
        assertEquals(itn, organization.getItn());
        assertEquals(id, organization.getOrganizationId());
    }
}
