package com.zzzkvidi4.storage.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * ItemDto class.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class ItemDto {
    @Nullable
    private String id;
    @Nullable
    private String name;
    @Nullable
    private String code;
}
