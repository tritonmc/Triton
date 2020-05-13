package com.rexcantor64.triton.language.item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Objects;

@Data
@AllArgsConstructor
public class SignLocation implements com.rexcantor64.triton.api.language.SignLocation {
    @EqualsAndHashCode.Exclude
    private String server;
    private String world;
    private int x;
    private int y;
    private int z;

    public SignLocation(String name, int x, int y, int z) {
        this(null, name, x, y, z);
    }

    public boolean equalsWithServer(Object that) {
        return this.equals(that) && Objects.equals(server, ((SignLocation) that).server);
    }

}
