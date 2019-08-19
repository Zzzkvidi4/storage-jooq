package com.zzzkvidi4.storage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Projection to build report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class OrganizationWithItem {
    @Nullable
    private String organizationId;
    @Nullable
    private String organizationName;
    @Nullable
    private String organizationItn;
    @Nullable
    private String organizationAccount;
    @Nullable
    private String itemId;
    @Nullable
    private String itemName;
    @Nullable
    private String itemCode;

    @NotNull
    public OrganizationDto getOrganization() {
        return new OrganizationDto(organizationId, organizationName, organizationItn, organizationAccount);
    }

    @NotNull
    public ItemDto getItem() {
        return new ItemDto(itemId, itemName, itemCode);
    }
}
