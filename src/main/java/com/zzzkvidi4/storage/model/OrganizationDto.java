package com.zzzkvidi4.storage.model;

import com.zzzkvidi4.storage.generated.tables.pojos.Organization;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@AllArgsConstructor
public final class OrganizationDto {
    @Nullable
    private String organizationId;
    @Nullable
    private String name;
    @Nullable
    private String itn;
    @Nullable
    private String account;

    @NotNull
    public static OrganizationDto toDto(@NotNull Organization organization) {
        return new OrganizationDto(organization.getOrganizationId(), organization.getName(), organization.getItn(), organization.getAccount());
    }
}
