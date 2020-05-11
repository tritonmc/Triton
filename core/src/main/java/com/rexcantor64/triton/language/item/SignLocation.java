package com.rexcantor64.triton.language.item;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class SignLocation implements com.rexcantor64.triton.api.language.SignLocation {
    private String server;
    private String world;
    private int x;
    private int y;
    private int z;

    public SignLocation(String name, int x, int y, int z) {
        this(null, name, x, y, z);
    }

    public boolean equalsNoServer(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignLocation that = (SignLocation) o;
        return x == that.x &&
                y == that.y &&
                z == that.z &&
                Objects.equals(world, that.world);
    }
}
